package com.arise.server.route;

import com.arise.internal.util.RestRouteRadixTree;
import com.arise.server.StandardHttpMessage;
import com.arise.server.route.pool.RemoteChannelPool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

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

    private static final int port = 8099;

    private List<HttpObject> contents = new ArrayList<>();

    private HttpRequest request;

    static {
        tree.init();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.debug("收到msg:{},this:{}", msg.getClass(), this.hashCode());
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
                            log.debug("future 返回success:{}", future.isSuccess());
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
                                inbound.writeAndFlush(StandardHttpMessage._500).addListener(e -> {
                                    if (e.isSuccess()) {
                                        inbound.close();
                                    }
                                });
                            }
                        });
                        //获取连接
                        RemoteChannelPool.acquireChannel(host, port, inbound.eventLoop(), promise);
                    }
                } else {
                    inbound.writeAndFlush(StandardHttpMessage._404).addListener(future -> {
                        if (future.isSuccess()) {
                            inbound.close();
                        }
                    });
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        Channel channel = ctx.channel();
        channel.pipeline().addLast(new HttpResponseEncoder());
        channel.writeAndFlush(StandardHttpMessage._500).addListener(future -> {
            if (future.isSuccess()) {
                channel.close();
            }
        });
        throwable.printStackTrace();
        log.error(throwable.getMessage());
    }
}
