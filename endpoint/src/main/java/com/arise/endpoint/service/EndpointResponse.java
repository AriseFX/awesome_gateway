package com.arise.endpoint.service;

import com.alibaba.fastjson.JSON;
import com.arise.internal.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.alibaba.nacos.common.constant.HttpHeaderConsts.CONTENT_LENGTH;
import static com.alibaba.nacos.common.constant.HttpHeaderConsts.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: wy
 * @Date: Created in 15:05 2021-07-02
 * @Description:
 * @Modified: By：
 */
@Data
@NoArgsConstructor
public class EndpointResponse {

    private String msg;

    private Object result;

    public static DefaultFullHttpResponse standJsonResp(Object body, HttpResponseStatus status) {
        ByteBuf buf = JsonUtils.toBuff(JSON.toJSONString(body));
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, buf);
        response.headers().set(CONTENT_TYPE, "application/json");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    public EndpointResponse(String msg) {
        this.msg = msg;
    }

    public EndpointResponse(String msg, Object result) {
        this.msg = msg;
        this.result = result;
    }
}
