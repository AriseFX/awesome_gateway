package com.arise.server;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.nacos.common.constant.HttpHeaderConsts.CONTENT_LENGTH;
import static com.alibaba.nacos.common.constant.HttpHeaderConsts.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: wy
 * @Date: Created in 14:54 2021-06-05
 * @Description:
 * @Modified: By：
 */
public class GatewayMessage {

    public static DefaultHttpResponse Established =
            new DefaultHttpResponse(HTTP_1_1, new HttpResponseStatus(200, "Connection Established"));

    public static FullHttpResponse _404 = build(new HashMap<String, Object>() {
        {
            put("code", 404);
            put("success", false);
            put("message", "路由未找到");
        }
    }, NOT_FOUND, null);

    public static FullHttpResponse _500 = build(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway内部错误,请联系管理员");
        }
    }, INTERNAL_SERVER_ERROR, null);

    public static FullHttpResponse _503 = build(new HashMap<String, Object>() {
        {
            put("code", 503);
            put("success", false);
            put("message", "Gateway错误,服务未找到");
        }
    }, SERVICE_UNAVAILABLE, null);

    public static FullHttpResponse _TimeOut = build(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway错误,与目标服务连接超时");
        }
    }, INTERNAL_SERVER_ERROR, null);

    public static FullHttpResponse _UnknownHost = build(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway错误,后端地址解析失败");
        }
    }, INTERNAL_SERVER_ERROR, null);


    public static FullHttpResponse _ConnectionClose = build(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway错误,后端服务已关闭连接");
        }
    }, SERVICE_UNAVAILABLE, null);

    public static FullHttpResponse build(Map<String, Object> body, HttpResponseStatus status, ByteBufAllocator allocator) {
        if (allocator == null) {
            allocator = ByteBufAllocator.DEFAULT;
        }
        byte[] bodyByte = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = allocator.directBuffer(bodyByte.length).writeBytes(bodyByte);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, byteBuf);
        response.headers().set(CONTENT_TYPE, "application/json");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    public static void write2Channel(Channel channel, FullHttpResponse response) {
        if (channel.isActive()) {
            channel.writeAndFlush(response.retainedDuplicate());
        }
    }
}
