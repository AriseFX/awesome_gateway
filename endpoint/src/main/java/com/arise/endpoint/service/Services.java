package com.arise.endpoint.service;

import com.alibaba.fastjson.JSON;
import com.arise.endpoint.service.dto.RouteAddDto;
import com.arise.internal.util.JsonUtils;
import com.arise.redis.AsyncRedisClient;
import com.arise.spring.ServerProperties;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.arise.endpoint.service.EndpointResponse.standJsonResp;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @Author: wy
 * @Date: Created in 14:05 2021-07-02
 * @Description:
 * @Modified: By：
 */
public enum Services implements Function<FullHttpRequest, DefaultFullHttpResponse> {
    route_refresh {
        @Override
        public DefaultFullHttpResponse apply(FullHttpRequest request) {
            AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
            return standJsonResp(new EndpointResponse("route refresh successful"), OK);
        }
    },
    route_get {
        @Override
        public DefaultFullHttpResponse apply(FullHttpRequest request) {
            AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
            try {
                Map<String, Object> route = client.commands().hgetall("ROUTE").get(5, TimeUnit.SECONDS);
                return standJsonResp(new EndpointResponse("route get successful", route.values()), OK);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                return standJsonResp(new EndpointResponse("route get fail"), INTERNAL_SERVER_ERROR);
            }
        }
    },
    route_put {
        @Override
        public DefaultFullHttpResponse apply(FullHttpRequest request) {
            try {
                AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
                RouteAddDto dto = JSON.parseObject(JsonUtils.toJson(request.content()), RouteAddDto.class);
                Map<String, Object> map = dto.getRoutes().stream()
                        .collect(Collectors.toMap(RouteAddDto.RouteBean::getId, v -> v, (v1, v2) -> v1));
                client.commands().hset("ROUTE", map);
            } catch (Exception e) {
                return standJsonResp(new EndpointResponse("server error"), INTERNAL_SERVER_ERROR);
            }
            return standJsonResp(new EndpointResponse("route put successful"), OK);
        }
    }
}
