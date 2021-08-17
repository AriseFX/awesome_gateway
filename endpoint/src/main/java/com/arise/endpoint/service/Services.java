package com.arise.endpoint.service;

import com.alibaba.fastjson.JSON;
import com.arise.config.ServerProperties;
import com.arise.endpoint.service.dto.EndpointResponse;
import com.arise.endpoint.service.dto.RouteDto;
import com.arise.internal.util.JsonUtils;
import com.arise.redis.AsyncRedisClient;
import com.arise.server.route.RouteBean;
import com.arise.server.route.manager.RouteManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.arise.endpoint.service.dto.EndpointResponse.standJsonResp;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @Author: wy
 * @Date: Created in 14:05 2021-07-02
 * @Description:
 * @Modified: By：
 */
@Slf4j
public enum Services implements BiConsumer<FullHttpRequest, Channel> {
    route_refresh {
        @Override
        public void accept(FullHttpRequest request, Channel channel) {
            AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
            RouteManager routeManager = ServerProperties.getBean(RouteManager.class);
            client.asyncExec(e -> e.hgetall("ROUTE").whenComplete((v, ex) -> {
                if (ex != null) {
                    channel.writeAndFlush(standJsonResp(new EndpointResponse(ex.getMessage()), INTERNAL_SERVER_ERROR));
                    return;
                }
                routeManager.clear();
                v.values().forEach(x -> {
                    //更新路由
                    routeManager.addRoute((RouteBean) x);
                });
                channel.writeAndFlush(standJsonResp(new EndpointResponse("route refresh successful"), OK));
            }));
        }
    },
    route_get {
        @Override
        public void accept(FullHttpRequest request, Channel channel) {
            AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
            client.asyncExec(e -> e.hgetall("ROUTE").whenComplete((v, ex) -> {
                if (ex != null) {
                    channel.writeAndFlush(standJsonResp(new EndpointResponse("route get fail"), INTERNAL_SERVER_ERROR));
                    return;
                }
                channel.writeAndFlush(standJsonResp(new EndpointResponse("route get successful", v.values()), OK));
            }));
        }
    },
    route_put {
        @Override
        public void accept(FullHttpRequest request, Channel channel) {

            AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
            String s = JsonUtils.toJson(request.content());
            RouteDto dto = JSON.parseObject(s, RouteDto.class);
            log.info("route_put,数目:{}", dto.getRoutes().size());
            Map<String, Object> map = dto.getRoutes().stream()
                    .collect(Collectors.toMap(RouteBean::getId, v -> v, (v1, v2) -> v1));
            client.asyncExec(e ->
                    e.hset("ROUTE", map).whenComplete((v, ex) -> {
                                if (ex != null) {
                                    channel.writeAndFlush(standJsonResp(new EndpointResponse("server error"), INTERNAL_SERVER_ERROR));
                                }
                                channel.writeAndFlush(standJsonResp(new EndpointResponse("route put successful"), OK));
                            }
                    ));

        }
    }
}