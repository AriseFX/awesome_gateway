package com.arise.server.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.util.ArrayList;

/**
 * @Author: wy
 * @Date: Created in 23:54 2021-05-31
 * @Description:
 * @Modified: By：
 */
public class EpollHttpRouteHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final Bootstrap b = new Bootstrap();

    private static final DefaultHttpResponse Established =
            new DefaultHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "Connection Established"));
    private HttpRequest request;
    private String host;
    private int port;
    private final ArrayList<HttpContent> contents = new ArrayList<>();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest) {
            //获取目标ip端口
            request = (HttpRequest) msg;
            String hostAndPortStr = HttpMethod.CONNECT.equals(request.method()) ? request.uri() : request.headers().get("Host");
            String[] hostPortArray = hostAndPortStr.split(":");
            host = hostPortArray[0];
            String portStr = hostPortArray.length == 2 ? hostPortArray[1] : !HttpMethod.CONNECT.equals(request.method()) ? "80" : "443";
            port = Integer.parseInt(portStr);
        } else {
            EpollSocketChannel inbound = (EpollSocketChannel) ctx.channel();
            Promise<Channel> promise = ctx.executor().newPromise();
            //处理https代理
            if (request.method().equals(HttpMethod.CONNECT)) {
                promise.addListener((FutureListener<Channel>) future -> {
                    //直到连接远程服务器成功
                    EpollSocketChannel outbound = (EpollSocketChannel) future.getNow();
                    inbound.pipeline().addLast(new HttpResponseEncoder());
                    inbound.writeAndFlush(Established)
                            .addListener(res -> {
                                if (res.isSuccess()) {
                                    //去掉所有handler(后续走tunnel)
                                    ctx.pipeline().remove(HttpResponseEncoder.class);
                                    ctx.pipeline().remove(HttpRequestDecoder.class);
                                    ctx.pipeline().remove(EpollHttpRouteHandler.class);
                                    ctx.pipeline().addLast(new EpollForwardHandler(outbound));
                                    outbound.pipeline().addLast(new EpollForwardHandler(inbound));
                                }
                            });
                });
            } else {
                ((HttpContent) msg).content().retain();
                contents.add((HttpContent) msg);
                if (msg instanceof LastHttpContent) {
                    promise.addListener((FutureListener<Channel>) future -> {
                        if (promise.isSuccess()) {
                            EpollSocketChannel outbound = (EpollSocketChannel) future.getNow();
                            //考虑http连接复用的情况，后续就不走协议栈了
                            ctx.pipeline().remove(EpollHttpRouteHandler.class);
                            ctx.pipeline().addLast(new EpollForwardHandler(outbound));
                            //转发请求
                            outbound.pipeline().addLast(new HttpRequestEncoder());
                            outbound.pipeline().addLast(new EpollForwardHandler(inbound));
                            outbound.writeAndFlush(request);
                            contents.forEach(outbound::writeAndFlush);
                        }
                    });
                }
            }
            b.group(inbound.eventLoop())
                    .channel(EpollSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new RouteChannelActiveHandler(promise))
                    .connect(host, port);
        }
    }

}
