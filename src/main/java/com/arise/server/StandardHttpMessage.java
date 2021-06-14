package com.arise.server;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
 * @Description: 实现这个类的目的是为了
 * @Modified: By：
 */
public class StandardHttpMessage {

    public static DefaultHttpResponse Established =
            new DefaultHttpResponse(HTTP_1_1, new HttpResponseStatus(200, "Connection Established"));

    public static StandardResponse _404 = new StandardResponse(new HashMap<String, Object>() {
        {
            put("msg", "route not found");
        }
    }, NOT_FOUND);

    public static StandardResponse _200 = new StandardResponse(new HashMap<String, Object>() {
        {
            put("msg", "ok");
        }
    }, OK);

    public static StandardResponse _500 = new StandardResponse(new HashMap<String, Object>() {
        {
            put("msg", "server error");
        }
    }, INTERNAL_SERVER_ERROR);


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
    }
}
