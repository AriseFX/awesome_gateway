package com.arise.server.route;

import com.arise.internal.util.RestRouteRadixTree;
import com.arise.server.StandardHttpMessage;
import com.arise.server.route.pool.RemoteChannelPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.Affinity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: api路由
 * @Modified: By：
 */
@Slf4j
public class ApiRouteHandler extends ChannelInboundHandlerAdapter {

    private static final RestRouteRadixTree<String> tree = new RestRouteRadixTree<>();

    private static final String host = "192.168.150.102";

    private static final int port = 10086;

    private List<HttpObject> contents = new ArrayList<>();

    private HttpRequest request;

    static {
        tree.init();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelInactive:{}", ctx.channel().toString());
        ctx.channel().close();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.debug("收到msg:{},this:{}", msg.getClass(), this.hashCode());
        log.debug("当前thread id:{},cpu id:{}", Affinity.getThreadId(), Affinity.getCpu());
        EpollSocketChannel inbound = (EpollSocketChannel) ctx.channel();
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            contents = new ArrayList<>();
        } else {
            contents.add((HttpObject) msg);
            if (msg instanceof LastHttpContent) {
                //最后一个http 请求
                log.debug("最后一个http content");
                String uri = request.uri();
                List<String> res = tree.matching(uri);
                if (res.size() > 0) {
                    URI remoteUri = URI.create(res.get(0));
                    if (remoteUri.getScheme().equals("lb")) {
                        //TODO 获取IP端口 modeRequest()
                        Promise<Channel> promise = ctx.executor().newPromise();
                        promise.addListener((FutureListener<Channel>) future -> {
                            if (future.isSuccess()) {
                                EpollSocketChannel outbound = (EpollSocketChannel) future.getNow();
                                //转发
                                log.debug("inbound:{},hashcode:{},outbound:{},hashcode:{}", inbound.isActive(), inbound.hashCode(), outbound.isActive(), outbound.hashCode());
                                outbound.pipeline().addLast(new HttpRequestEncoder());
                                outbound.pipeline().addLast(new HttpResponseDecoder());
                                outbound.pipeline().addLast(new ForwardHandler(inbound));
                                outbound.writeAndFlush(request);
                                contents.forEach(outbound::writeAndFlush);
                            } else {
                                if (inbound.isActive()) {
                                    StandardHttpMessage._500.toByteBuf(ctx).forEach(e ->
                                            inbound.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
                                    inbound.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(f -> {
                                        if (f.isSuccess()) {
                                            inbound.close();
                                        }
                                    });
                                }
                            }
                        });
                        //获取连接
                        RemoteChannelPool.acquireChannel(host, port, inbound.eventLoop(), promise);
                    }
                } else {
                    if (inbound.isActive()) {
                        StandardHttpMessage._200.toByteBuf(ctx).forEach(e ->
                                inbound.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            StandardHttpMessage._500.toByteBuf(ctx).forEach(e ->
                    channel.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
        }
    }
}
