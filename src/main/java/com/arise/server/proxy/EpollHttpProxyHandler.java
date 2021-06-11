package com.arise.server.proxy;

import com.arise.server.route.ApiRouteHandler;
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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

import static com.arise.server.StandardHttpMessage.Established;

/**
 * @Author: wy
 * @Date: Created in 23:54 2021-05-31
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class EpollHttpProxyHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final Bootstrap b = new Bootstrap();

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
            request = (HttpRequest) msg;
            if (request.uri().startsWith("/")) {
                //api路由
                ctx.pipeline().remove(this);
                ApiRouteHandler apiRouteHandler = new ApiRouteHandler();
                ctx.pipeline().addLast(apiRouteHandler);
                log.debug("EpollHttpProxyHandler  msg:{}", ((HttpRequest) msg).uri());
                apiRouteHandler.channelRead(ctx, msg);
            } else {
                //http代理
                request = (HttpRequest) msg;
                String hostAndPortStr = HttpMethod.CONNECT.equals(request.method()) ? request.uri() : request.headers().get("Host");
                String[] hostPortArray = hostAndPortStr.split(":");
                host = hostPortArray[0];
                String portStr = hostPortArray.length == 2 ? hostPortArray[1] : !HttpMethod.CONNECT.equals(request.method()) ? "80" : "443";
                port = Integer.parseInt(portStr);
            }
        } else {
            handleProxy(ctx, msg);
        }
    }

    protected void handleProxy(ChannelHandlerContext ctx, HttpObject msg) {
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
                                ctx.pipeline().remove(EpollHttpProxyHandler.class);
                                ctx.pipeline().addLast(new ProxyForwardHandler(outbound));
                                outbound.pipeline().addLast(new ProxyForwardHandler(inbound));
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
                        ctx.pipeline().remove(EpollHttpProxyHandler.class);
                        ctx.pipeline().addLast(new ProxyForwardHandler(outbound));
                        //转发请求
                        outbound.pipeline().addLast(new HttpRequestEncoder());
                        outbound.pipeline().addLast(new ProxyForwardHandler(inbound));
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
                .handler(new RemoteChannelActiveHandler(promise))
                .connect(host, port);
    }

}
