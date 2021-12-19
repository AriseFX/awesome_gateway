package com.ewell.core.server.handler;

import com.ewell.common.message.Message;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.core.filer.context.FilterFuture;
import com.ewell.core.filer.context.Observer;
import com.ewell.core.pool.AsyncChannelPool;
import com.ewell.core.route.MatchRes;
import com.google.inject.Inject;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static com.ewell.common.GatewayMessages.*;
import static com.ewell.common.IntMapConstant.*;
import static com.ewell.common.NettyAttrKeyConstant.FilterAttr;
import static com.ewell.core.filer.context.GatewayFilter.FilterType.PreRoute;
import static com.ewell.core.filer.context.GatewayFilter.FilterType.Route;
import static com.ewell.core.monitor.MonitorHandler.apiCounter;

/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: api路由
 * @Modified: By：
 */
@Slf4j
public class ProxyInboundHandler extends ChannelInboundHandlerAdapter {

    @Inject
    private static AsyncChannelPool channelPool;

    private List<HttpObject> contents;

    private HttpRequest request;

    private Channel outboundChannel;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.channel().close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel inboundChannel = ctx.channel();
        EventLoop eventLoop = inboundChannel.eventLoop();
        if (msg instanceof HttpRequest) {
            contents = new ArrayList<>(3);
            contents.add((HttpObject) msg);
            request = (HttpRequest) msg;
        } else {
            contents.add((HttpObject) msg);
            if (msg instanceof LastHttpContent) {
                //生成请求上下文
                IntObjectHashMap<Object> attr = new IntObjectHashMap<>(4);
                inboundChannel.attr(FilterAttr).set(attr);
                attr.put(_Timestamp, Long.valueOf(System.currentTimeMillis()));
                attr.put(_RespObserver, new TreeSet<Observer<Message>>());
                URI requestUri;
                try {
                    requestUri = URI.create(request.uri());
                } catch (IllegalArgumentException e) {
                    REQUEST_ERROR("url格式错误:" + request.uri()).write2Channel(inboundChannel);
                    return;
                }
                attr.put(_RequestURI, requestUri);
                //
                Promise<Object> preRoutePromise = eventLoop.newPromise();
                preRoutePromise.addListener((FutureListener<Object>) future1 -> {
                    FilterFuture resp1 = (FilterFuture) future1.get();
                    if (!resp1.isSuccess()) {
                        inboundChannel.writeAndFlush(resp1.getHttpMessage());
                    } else {
                        //获取路由匹配结果
                        Promise<Object> routePromise = eventLoop.newPromise();
                        routePromise.addListener(future -> {
                            FilterFuture fResp = (FilterFuture) future.get();
                            if (!fResp.isSuccess()) {
                                fResp.getHttpMessage()
                                        .write2Channel(inboundChannel);
                                return;
                            }
                            MatchRes matchRes = (MatchRes) fResp.getResult();
                            InetSocketAddress inetAddress = matchRes.getAddress();
                            InetAddress address = inetAddress.getAddress();
                            //开始转发
                            Promise<Channel> forwardPromise = eventLoop.newPromise();
                            forwardPromise.addListener((FutureListener<Channel>) future3 -> {
                                if (future3.isSuccess()) {
                                    outboundChannel = future3.get();
                                    outboundChannel.attr(FilterAttr).set(attr);
                                    ChannelPipeline outboundPipe = outboundChannel.pipeline();
                                    outboundPipe.addLast(new ForwardHandler(inboundChannel));
                                    //重写url
                                    String rewriteUrl = matchRes.getRewriteUrl();
                                    if (rewriteUrl != null) {
                                        request.setUri(rewriteUrl);
                                    }
                                    //修改请求
                                    HttpHeaders headers = request.headers();
                                    headers.set(HttpHeaderNames.CONNECTION, "keep-alive");
                                    headers.set(HttpHeaderNames.HOST, inetAddress.getHostString());
                                    //运维指标
                                    apiCounter.incrementAndGet();
                                    contents.forEach(outboundChannel::writeAndFlush);
                                    attr.put(_WrittenTimestamp, Long.valueOf(System.currentTimeMillis()));
                                } else {
                                    Throwable cause = future3.cause();
                                    CONNECT_ERROR(cause.getMessage()).write2Channel(inboundChannel);
                                }
                            });
                            //获取连接
                            channelPool.acquireChannel(matchRes.isSsl(),
                                    address.getHostAddress(), inetAddress.getPort(),
                                    eventLoop, forwardPromise);
                        });
                        //执行route过滤器
                        FilterContext fctx2 =
                                new FilterContext(routePromise, eventLoop,
                                        attr, Route);
                        fctx2.start(contents);
                    }
                });
                //执行preRoute过滤器
                FilterContext fctx1 =
                        new FilterContext(preRoutePromise, eventLoop,
                                attr, PreRoute);
                fctx1.start(contents);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        if (throwable instanceof IOException) {
            ctx.channel().close();
        } else {
            throwable.printStackTrace();
            log.error("ApiRouteHandler:{},channel:{},alive:{}", throwable, ctx.channel(), ctx.channel().isActive());
            Channel channel = ctx.channel();
            GATEWAY_ERROR(throwable.getMessage()).write2Channel(channel);
        }
    }
}