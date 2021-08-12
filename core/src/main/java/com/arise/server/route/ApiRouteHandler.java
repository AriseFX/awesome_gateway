package com.arise.server.route;

import com.arise.internal.exception.ServiceNotFoundException;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.SchedulableFilter;
import com.arise.server.route.match.MatchRes;
import com.arise.server.route.match.RouteMatcher;
import com.arise.server.route.pool.RemoteChannelPool;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.Affinity;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.arise.server.GatewayMessage.*;

/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: api路由
 * @Modified: By：
 */
@Slf4j
public class ApiRouteHandler extends ChannelInboundHandlerAdapter {

    public static List<SchedulableFilter<List<HttpObject>, List<HttpObject>>> forwardFilters;

    public static List<SchedulableFilter<List<HttpObject>, Object>> preRouteFilters;

    public static RouteMatcher matcher;

    private List<HttpObject> contents;

    private HttpRequest request;

    private Channel outbound;

    public static String RequestURI = "RequestURI";

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
        EventLoop eventLoop = inbound.eventLoop();
        if (msg instanceof HttpRequest) {
            contents = new ArrayList<>(3);
            contents.add((HttpObject) msg);
            request = (HttpRequest) msg;
        } else {
            contents.add((HttpObject) msg);
            if (msg instanceof LastHttpContent) {
                Map<String, Object> attr = new HashMap<>(4);
                attr.put(RequestURI, URI.create(request.uri()));
                //最后一个http 请求
                log.debug("最后一个http content");
                Promise<Object> p = ctx.executor().newPromise();
                //为了避免阻塞
                p.addListener((FutureListener<Object>) future1 -> {
                    if (!future1.isSuccess()) {
                        log.error(future1.cause().getMessage());
                        writeMsg(inbound, _500);
                        return;
                    }
                    MatchRes matchRes;
                    try {
                        matchRes = matcher.match(eventLoop, attr, request);
                    } catch (ServiceNotFoundException e) {
                        writeMsg(inbound, _503);
                        return;
                    }
                    if (matchRes == null) {
                        writeMsg(inbound, _404);
                    } else {
                        InetSocketAddress inetAddress = matchRes.getAddress();
                        InetAddress address = inetAddress.getAddress();
                        if (address == null) {
                            writeMsg(inbound, _UnknownHost);
                            return;
                        }
                        Promise<Channel> promise = eventLoop.newPromise();
                        promise.addListener((FutureListener<Channel>) future2 -> {
                            if (future2.isSuccess()) {
                                outbound = future2.getNow();
                                Promise<List<HttpObject>> respPromise = eventLoop.newPromise();
                                DefaultChannelPipeline pipeline = (DefaultChannelPipeline) outbound.pipeline();
                                if (matchRes.isSsl()) {
                                    String hostName = address.getHostName();
                                    SslContext context = SslContextBuilder.forClient()
                                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                            .build();
                                    SslHandler sslHandler = context.newHandler(outbound.alloc(), hostName, inetAddress.getPort());
                                    pipeline.addFirst(sslHandler);
                                    //保证Host指向正确
                                    request.headers().set(HttpHeaderNames.HOST, hostName);
                                }
                                pipeline.addLast(new ForwardHandler(respPromise, inbound));
                                new FilterContext<>(contents, respPromise, forwardFilters, eventLoop, attr, null).handleNext();
                                contents.forEach(outbound::writeAndFlush);
                            } else {
                                Throwable cause = future2.cause();
                                if (cause instanceof ConnectTimeoutException) {
                                    writeMsg(inbound, _TimeOut);
                                } else {
                                    writeMsg(inbound, _500);
                                }
                            }
                        });
                        //获取连接
                        RemoteChannelPool.acquireChannel(address.getHostAddress(), inetAddress.getPort(),
                                eventLoop, promise);
                    }
                });
                new FilterContext<>(contents, preRouteFilters, eventLoop, attr, p)
                        .handleNext();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        log.error("ApiRouteHandler:{}", throwable.toString());
        Channel channel = ctx.channel();
        writeMsg(channel, _500);
    }
}
