package com.arise.handler;

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
        ctx.channel().close();
    }
}
