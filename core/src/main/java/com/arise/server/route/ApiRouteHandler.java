package com.arise.server.route;

import com.arise.internal.exception.ServiceNotFoundException;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.SchedulableFilter;
import com.arise.server.route.match.MatchRes;
import com.arise.server.route.match.RouteMatcher;
import com.arise.server.route.pool.RemoteChannelPool;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
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
                //最后一个http 请求
                log.debug("最后一个http content");
                Promise<Object> p = ctx.executor().newPromise();
                //为了避免阻塞
                p.addListener((FutureListener<Object>) future1 -> {
                    if (!future1.isSuccess()) {
                        log.error(future1.cause().getMessage());
                        if (inbound.isActive()) {
                            write2Channel(inbound, _500);
                        }
                        return;
                    }
                    MatchRes matchRes;
                    try {
                        matchRes = matcher.match(eventLoop, attr, request);
                    } catch (ServiceNotFoundException e) {
                        if (inbound.isActive()) {
                            write2Channel(inbound, _503);
                        }
                        return;
                    }
                    if (matchRes == null) {
                        if (inbound.isActive()) {
                            write2Channel(inbound, _404);
                        }
                    } else {
                        InetSocketAddress inetAddress = matchRes.getAddress();
                        InetAddress address = inetAddress.getAddress();
                        if (address == null) {
                            if (inbound.isActive()) {
                                write2Channel(inbound, _UnknownHost);
                            }
                            return;
                        }
                        Promise<Channel> promise = eventLoop.newPromise();
                        promise.addListener((FutureListener<Channel>) future2 -> {
                            if (future2.isSuccess()) {
                                outbound = future2.getNow();
                                Promise<List<HttpObject>> respPromise = eventLoop.newPromise();
                                ChannelPipeline pipeline = outbound.pipeline();
                                if (matchRes.isSsl()) {
                                    SslContext context = SslContextBuilder.forClient()
                                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                            .build();
                                    pipeline.addFirst(new SslHandler(context.newEngine(ByteBufAllocator.DEFAULT, address.getHostName(), inetAddress.getPort())));
                                }
                                pipeline.addLast(new ForwardHandler(respPromise, inbound));
                                new FilterContext<>(contents, respPromise, forwardFilters, eventLoop, attr, null).handleNext();
                                contents.forEach(outbound::writeAndFlush);
                            } else {
                                Throwable cause = future2.cause();

                                if (cause instanceof ConnectTimeoutException) {
                                    write2Channel(inbound, _TimeOut);
                                } else {
                                    write2Channel(inbound, _500);
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
        if (channel.isActive()) {
            write2Channel(channel, _500);
        }
    }
}
