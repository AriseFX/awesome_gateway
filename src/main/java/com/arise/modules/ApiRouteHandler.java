package com.arise.modules;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

/**
 * @Author: wy
 */
public class ApiRouteHandler extends SimpleChannelInboundHandler<FullHttpMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) {
        //response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        // 将html write到客户端
        FullHttpMessage rep = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        ctx.channel().writeAndFlush(rep);
        ctx.channel().close();
    }
}
