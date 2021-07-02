package com.arise.endpoint.service;

import com.alibaba.fastjson.JSON;
import com.arise.internal.util.JsonUtils;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.function.Function;

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
            return EndpointResponse.standResp(new EndpointResponse("route refresh successful"), OK);
        }
    },
    route_get {
        @Override
        public DefaultFullHttpResponse apply(FullHttpRequest request) {
            return EndpointResponse.standResp(new EndpointResponse("route get successful"), OK);
        }
    },
    route_put {
        @Override
        public DefaultFullHttpResponse apply(FullHttpRequest request) {
            return EndpointResponse.standResp(new EndpointResponse("route put successful"), OK);
        }
    }


}
