package com.arise.endpoint;

import com.arise.endpoint.service.EndpointResponse;
import com.arise.endpoint.service.Services;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

/**
 * @Author: wy
 * @Date: Created in 13:27 2021-07-02
 * @Description: 处理http url映射
 * @Modified: By：
 */
public class HttpMappingHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static Map<String, Function<FullHttpRequest, DefaultFullHttpResponse>> mapping = new ConcurrentHashMap<>();

    //初始化endpoint
    static {
        mapping.put("/route/get", Services.route_get);
        mapping.put("/route/put", Services.route_put);
        mapping.put("/route/refresh", Services.route_refresh);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        URI uri = URI.create(msg.uri());
        Function<FullHttpRequest, DefaultFullHttpResponse> function = mapping.get(uri.getPath());
        if (function != null) {
            ctx.channel().writeAndFlush(function.apply(msg));
            return;
        }
        ctx.channel().writeAndFlush(EndpointResponse.standResp(new EndpointResponse("mapping not found"), NOT_FOUND));
    }
}
