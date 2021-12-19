package com.ewell.common;

import com.ewell.common.message.GatewayMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: wy
 * @Date: Created in 11:46 上午 2021/12/1
 * @Description:
 * @Modified: By：
 */
public class GatewayMessages {

    public static GatewayMessage ROUTE_NOT_FOUND() {
        return build(404, false,
                "gateway:路由未找到", INTERNAL_SERVER_ERROR);
    }

    public static GatewayMessage GATEWAY_ERROR(String message) {
        return build(500, false,
                "gateway:内部错误,错误原因:" + message, INTERNAL_SERVER_ERROR);
    }

    public static GatewayMessage GATEWAY_TIMEOUT() {
        return build(504, false,
                "gateway:后端服务器响应超时", GATEWAY_TIMEOUT);
    }

    public static GatewayMessage TOO_MANY_REQUESTS() {
        return build(429, false,
                "gateway:请求太频繁了,请稍后再试", TOO_MANY_REQUESTS);
    }

    public static GatewayMessage REQUEST_ERROR(String message) {
        return build(400, false,
                "gateway:请求错误," + message, BAD_REQUEST);
    }

    public static GatewayMessage CONNECT_ERROR(String message) {
        return build(500, false,
                "gateway:获取连接失败," + message, INTERNAL_SERVER_ERROR);
    }


    public static GatewayMessage SERVICE_NOT_FOUND(String service) {
        return build(503, false,
                "gateway:后端服务未找到," + service, SERVICE_UNAVAILABLE);
    }

    public static GatewayMessage CONN_CLOSED() {
        return build(503, false,
                "gateway:连接错误,后端服务已关闭连接", SERVICE_UNAVAILABLE);
    }

    static String responseTemplate = "{" +
            "\"code\":%d," +
            "\"success\":%b," +
            "\"message\": \"%s\"" +
            "}";

    public static GatewayMessage build(Integer code, boolean success, String message, HttpResponseStatus status) {
        byte[] bodyByte = String.format(responseTemplate, code, success, message).getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer(bodyByte.length).writeBytes(bodyByte);
        List<HttpObject> objects = new ArrayList<>(2);
        DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        HttpHeaders headers = response.headers();
        headers.set(CONTENT_TYPE, "application/json");
        headers.set(CONTENT_LENGTH, byteBuf.readableBytes());
        objects.add(response);
        objects.add(new DefaultLastHttpContent(byteBuf));
        return new GatewayMessage(objects);
    }
}
