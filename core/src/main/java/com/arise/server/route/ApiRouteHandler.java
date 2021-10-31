package com.arise.server.route;

import com.arise.base.config.Components;
import com.arise.base.exception.GatewayException;
import com.arise.base.exception.ServiceNotFoundException;
import com.arise.server.route.filter.Filter;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.logging.AlarmDto;
import com.arise.server.route.logging.AweLogService;
import com.arise.server.route.match.MatchRes;
import com.arise.server.route.match.RouteMatcher;
import com.arise.server.route.pool.AsyncChannelPool;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.Affinity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.arise.base.config.IntMapConstant.*;
import static com.arise.server.route.GatewayMessage.*;

/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: api路由
 * @Modified: By：
 */
@Slf4j
public class ApiRouteHandler extends ChannelInboundHandlerAdapter {

    public static List<Filter> forwardFilters;

    public static List<Filter> preRouteFilters;

    public static AsyncChannelPool asyncChannelPool = Components.get(AsyncChannelPool.class);

    public static RouteMatcher matcher = new RouteMatcher();

    private List<HttpObject> contents;

    private HttpRequest request;

    private Channel outbound;

    public static AttributeKey<IntObjectHashMap<Object>> Attr = AttributeKey.newInstance("attr");

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
//        log.debug("channelInactive:{}", ctx.channel().toString());
        ctx.channel().close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel inbound = ctx.channel();
        EventLoop eventLoop = inbound.eventLoop();
        if (msg instanceof HttpRequest) {
            contents = new ArrayList<>(3);
            contents.add((HttpObject) msg);
            request = (HttpRequest) msg;
        } else {
            contents.add((HttpObject) msg);
            if (msg instanceof LastHttpContent) {
                IntObjectHashMap<Object> attr = new IntObjectHashMap<>(4);
                attr.put(RequestURI, URI.create(request.uri()));
                attr.put(Timestamp, Long.valueOf(System.currentTimeMillis()));
                //最后一个http 请求
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
                        AweLogService.alarm(new AlarmDto(request.uri(),
                                e.getMessage(), "GATEWAY",
                                (String) attr.get(OriginCode), (String) attr.get(Backend)));
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        writeMsg(inbound, _500);
                        return;
                    }
                    if (matchRes == null) {
                        writeMsg(inbound, _404);
                        AweLogService.alarm(new AlarmDto(request.uri(), "路由未找到", "GATEWAY",
                                (String) attr.get(OriginCode), (String) attr.get(Backend)));
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
                                HttpHeaders headers = request.headers();
                                headers.set(HttpHeaderNames.CONNECTION, "keep-alive");
                                if (matchRes.isSsl()) {
                                    //保证Host指向正确
                                    headers.set(HttpHeaderNames.HOST, address.getHostName());
                                }
                                pipeline.addLast(new ForwardHandler(respPromise, inbound));
                                new FilterContext(contents
                                        , respPromise, forwardFilters, eventLoop, attr, null).handleNext();
                                outbound.attr(Attr).set(attr);
                                contents.forEach(outbound::writeAndFlush);
                                attr.put(WrittenTimestamp, Long.valueOf(System.currentTimeMillis()));
                            } else {
                                Throwable cause = future2.cause();
                                if (cause instanceof ConnectTimeoutException) {
                                    writeMsg(inbound, _TimeOut);
                                } else {
                                    cause.printStackTrace();
                                    writeMsg(inbound, _500);
                                }
                            }
                        });
                        //获取连接
                        asyncChannelPool.acquireChannel(matchRes.isSsl(),
                                address.getHostAddress(), inetAddress.getPort(),
                                eventLoop, promise);
                    }
                });
                try {
                    new FilterContext(contents, preRouteFilters, eventLoop, attr, p)
                            .handleNext();
                } catch (GatewayException e) {
                    writeMsg(inbound, _400(e.getMessage()));
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        if (throwable instanceof IOException) {
            ctx.channel().close();
        } else {
            log.error("ApiRouteHandler:{},channel:{},alive:{}", throwable.toString(), ctx.channel(), ctx.channel().isActive());
            Channel channel = ctx.channel();
            writeMsg(channel, _500);
        }
    }
}
