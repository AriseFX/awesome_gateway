package com.ewell.endpoint.service;

import com.alibaba.fastjson.JSON;
import com.ewell.common.RouteBean;
import com.ewell.core.monitor.MetricsCollector;
import com.ewell.core.route.RoutePromise;
import com.ewell.endpoint.JsonUtils;
import com.ewell.endpoint.service.dto.EndpointResponse;
import com.ewell.endpoint.service.dto.RouteDto;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.concurrent.FutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ewell.endpoint.service.dto.EndpointResponse.standJsonResp;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: wy
 * @Date: Created in 11:20 下午 2021/11/28
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ServiceFunctions {

    /**
     * 刷新路由
     */
    public static RouteServicesConsumer route_refresh = new RouteServicesConsumer() {
        @Override
        public void accept(FullHttpRequest request, Channel channel) {
            RoutePromise<Object> promise = new RoutePromise<>();
            promise.addListener((FutureListener<Object>) future -> {
                        if (!future.isSuccess()) {
                            String message = future.cause().getMessage();
                            channel.writeAndFlush(standJsonResp(new EndpointResponse(message), INTERNAL_SERVER_ERROR));
                        }
                        Collection<RouteBean> routeBeans = (Collection<RouteBean>) future.get();
                        routeCache.clear();
                        routeBeans.forEach(e -> routeCache.addRoute(e));
                        channel.writeAndFlush(standJsonResp(
                                new EndpointResponse("route refresh successful"), OK));
                    }
            );
            routeStoreSpi.getRoutes(promise);
        }
    };

    /**
     * 获取路由
     */
    public static RouteServicesConsumer route_get = new RouteServicesConsumer() {
        @Override
        public void accept(FullHttpRequest request, Channel channel) {
            RoutePromise<Object> promise = new RoutePromise<>();
            promise.addListener((FutureListener<Object>) future -> {
                        if (!future.isSuccess()) {
                            String message = future.cause().getMessage();
                            channel.writeAndFlush(standJsonResp(new EndpointResponse(message), INTERNAL_SERVER_ERROR));
                        }
                        channel.writeAndFlush(standJsonResp(
                                new EndpointResponse("route get successful", future.get()), OK));
                    }
            );
            routeStoreSpi.getRoutes(promise);
        }
    };

    /**
     * 添加路由
     */
    public static RouteServicesConsumer route_put = new RouteServicesConsumer() {
        @Override
        public void accept(FullHttpRequest request, Channel channel) {
            String s = JsonUtils.toJson(request.content());
            RouteDto dto = JSON.parseObject(s, RouteDto.class);
            log.info("route_put,数目:{}", dto.getRoutes().size());
            Map<String, RouteBean> map = dto.getRoutes().stream()
                    .collect(Collectors.toMap(RouteBean::getId, v -> v, (v1, v2) -> v1));
            RoutePromise<String> promise = new RoutePromise<>();
            promise.addListener((FutureListener<Object>) future -> {
                        if (!future.isSuccess()) {
                            String message = future.cause().getMessage();
                            channel.writeAndFlush(standJsonResp(new EndpointResponse(message), INTERNAL_SERVER_ERROR));
                        }
                        channel.writeAndFlush(standJsonResp(
                                new EndpointResponse("route put successful"), OK));
                    }
            );
            routeStoreSpi.putRoutes(map.values(), promise);
        }
    };

    /**
     * 清除路由
     */
    public static RouteServicesConsumer route_clear = new RouteServicesConsumer() {
        @Override
        public void accept(FullHttpRequest request, Channel channel) {
            RoutePromise<String> promise = new RoutePromise<>();
            promise.addListener((FutureListener<String>) future -> {
                        if (!future.isSuccess()) {
                            String message = future.cause().getMessage();
                            channel.writeAndFlush(standJsonResp(new EndpointResponse(message), INTERNAL_SERVER_ERROR));
                        }
                        channel.writeAndFlush(standJsonResp(
                                new EndpointResponse(future.get()), OK));
                    }
            );
            routeStoreSpi.deleteRoutes(promise);
        }
    };

    /**
     * 指标
     */
    public static RouteServicesConsumer metrics = new RouteServicesConsumer() {

        private final MetricsCollector collector = new MetricsCollector();

        @Override
        public void accept(FullHttpRequest request, Channel channel) {
            String str = collector.collect();
            ByteBuf buf = JsonUtils.toBuff(str);
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buf);
            response.headers().set(CONTENT_TYPE, "text/plain;version=0.0.4;charset=utf-8");
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            channel.writeAndFlush(response);
        }
    };

}

