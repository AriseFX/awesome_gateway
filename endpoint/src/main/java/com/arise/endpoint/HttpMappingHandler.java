package com.arise.endpoint;

import com.arise.endpoint.service.dto.EndpointResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static com.arise.endpoint.service.Services.*;
import static com.arise.endpoint.service.dto.EndpointResponse.standJsonResp;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

/**
 * @Author: wy
 * @Date: Created in 13:27 2021-07-02
 * @Description: 处理http url映射
 * @Modified: By：
 */
public class HttpMappingHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static Map<String, BiConsumer<FullHttpRequest, Channel>> mapping = new ConcurrentHashMap<>();

    //初始化endpoint
    static {
        mapping.put("/route/get", route_get);
        mapping.put("/route/put", route_put);
        mapping.put("/route/refresh", route_refresh);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        URI uri = URI.create(msg.uri());
        BiConsumer<FullHttpRequest, Channel> function = mapping.get(uri.getPath());
        if (function != null) {
            function.accept(msg, ctx.channel());
            return;
        }
        ctx.channel().writeAndFlush(standJsonResp(new EndpointResponse("mapping not found"), NOT_FOUND));
    }
}
