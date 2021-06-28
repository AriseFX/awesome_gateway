package com.arise.server.route;

import com.arise.naming.registry.ServiceManager;
import com.arise.internal.util.RestRouteRadixTree;
import com.arise.server.StandardHttpMessage;
import com.arise.server.route.pool.RemoteChannelPool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.Affinity;

import java.net.InetSocketAddress;
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

    private static String host;

    private static int port;

    private List<HttpObject> contents = new ArrayList<>();

    private HttpRequest request;

    private Channel outbound;

    static {
        tree.init();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("channelInactive:{}", ctx.channel().toString());
        ctx.channel().close();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.debug("收到msg:{},this:{}", msg.getClass(), this.hashCode());
        log.debug("当前thread id:{},cpu id:{}", Affinity.getThreadId(), Affinity.getCpu());
        Channel inbound = ctx.channel();
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
                        InetSocketAddress address = ServiceManager.selectService(remoteUri.getHost());
                        if (address == null) {
                            StandardHttpMessage._503.toByteBuf(ctx).forEach(e ->
                                    inbound.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
                            return;
                        }
                        host = address.getHostName();
                        port = address.getPort();
                    } else {
                        //TODO 这段代码抽出来
                        return;
                    }
                    //TODO 获取IP端口 modeRequest()
                    Promise<Channel> promise = ctx.executor().newPromise();
                    promise.addListener((FutureListener<Channel>) future -> {
                        if (future.isSuccess()) {
                            outbound = future.getNow();
                            //转发
                            outbound.pipeline().addLast(new ForwardHandler(inbound));
                            outbound.writeAndFlush(request);
                            contents.forEach(outbound::writeAndFlush);
                        } else {
                            if (inbound.isActive()) {
                                StandardHttpMessage._500.toByteBuf(ctx).forEach(e ->
                                        inbound.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
                            }
                        }
                    });
                    //获取连接
                    RemoteChannelPool.acquireChannel(host, port, inbound.eventLoop(), promise);
                } else {
                    if (inbound.isActive()) {
                        StandardHttpMessage._404.toByteBuf(ctx).forEach(e ->
                                inbound.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        log.error("ApiRouteHandler:{}", throwable.toString());
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            StandardHttpMessage._500.toByteBuf(ctx).forEach(e ->
                    channel.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
        }
    }
}
