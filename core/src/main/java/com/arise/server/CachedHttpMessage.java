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

import static com.alibaba.nacos.common.constant.HttpHeaderConsts.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @Author: wy
 * @Date: Created in 14:54 2021-06-05
 * @Description:
 * @Modified: By：
 */
public class CachedHttpMessage {

    public static DefaultHttpResponse Established =
            new DefaultHttpResponse(HTTP_1_1, new HttpResponseStatus(200, "Connection Established"));

    public static StandardResponse _404 = new StandardResponse(new HashMap<String, Object>() {
        {
            put("code", 404);
            put("success", false);
            put("message", "路由未找到");
        }
    }, NOT_FOUND);

    public static StandardResponse _200 = new StandardResponse(new HashMap<String, Object>() {
        {
            put("msg", "ok");
        }
    }, OK);

    public static StandardResponse _500 = new StandardResponse(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway内部错误,请联系管理员");
        }
    }, INTERNAL_SERVER_ERROR);

    public static StandardResponse _TimeOut = new StandardResponse(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway错误,与目标服务连接超时");
        }
    }, INTERNAL_SERVER_ERROR);

    public static StandardResponse _UnknownHost = new StandardResponse(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway错误,后端地址解析失败");
        }
    }, INTERNAL_SERVER_ERROR);

    public static StandardResponse _503 = new StandardResponse(new HashMap<String, Object>() {
        {
            put("code", 503);
            put("success", false);
            put("message", "Gateway错误,服务未找到");
        }
    }, SERVICE_UNAVAILABLE);

    public static StandardResponse _ConnectionClose = new StandardResponse(new HashMap<String, Object>() {
        {
            put("code", 500);
            put("success", false);
            put("message", "Gateway错误,后端服务已关闭连接");
        }
    }, SERVICE_UNAVAILABLE);


    public static class StandHttpResponseEncoder extends HttpResponseEncoder {

        @Override
        public void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
            super.encode(ctx, msg, out);
        }
    }

    public static class StandardResponse {

        private List<Object> data = null;
        private final Map<String, Object> body;
        private final HttpResponseStatus status;

        public StandardResponse(Map<String, Object> body, HttpResponseStatus status) {
            this.body = body;
            this.status = status;
        }

        public List<Object> toByteBuf(ChannelHandlerContext ctx) {
            if (data == null) {
                synchronized (StandardResponse.this) {
                    if (data == null) {
                        try {
                            List<Object> data = new ArrayList<>();
                            byte[] bodyByte = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
                            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(bodyByte.length).writeBytes(bodyByte);
                            FullHttpResponse response = new DefaultFullHttpResponse(
                                    HTTP_1_1, status, byteBuf);
                            response.headers().set(CONTENT_TYPE, "application/json");
                            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                            response.retain();
                            new StandHttpResponseEncoder().encode(ctx, response, data);
                            this.data = data;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return data;
        }

        /**
         * 缓存过后的消息体
         *
         * @param channel 目标连接
         * @param ctx 初始化的时候需要ChannelHandlerContext
         */
        public void write2Channel(Channel channel, ChannelHandlerContext ctx) {
            toByteBuf(ctx).forEach(e ->
                    channel.writeAndFlush(((ByteBuf) e).retainedDuplicate()));
        }
    }
}
