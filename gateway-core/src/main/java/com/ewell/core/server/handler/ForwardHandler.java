package com.ewell.core.server.handler;

import com.ewell.common.GatewayConfig;
import com.ewell.common.message.ForwardMessage;
import com.ewell.core.pool.AsyncChannelPool;
import com.google.inject.Inject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ewell.common.GatewayMessages.CONN_CLOSED;


/**
 * @Author: wy
 * @Date: Created in 22:47 2021-06-01
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ForwardHandler extends ChannelDuplexHandler {

    @Inject
    private static AsyncChannelPool pool;

    @Inject
    private static GatewayConfig gatewayConfig;

    private final Channel inbound;

    private final List<HttpObject> payloads = new ArrayList<>(3);

    public ForwardHandler(Channel inbound) {
        this.inbound = inbound;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        //处理背压
        inbound.config().setAutoRead(ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        SocketChannel outbound = (SocketChannel) ctx.channel();
        if (inbound.isActive()) {
            payloads.add((HttpObject) msg);
            if (msg instanceof LastHttpContent) {
                //执行outbound过滤器
                inbound.write(new ForwardMessage(payloads));
                pool.releaseChannel(outbound);
            }
        } else {
            pool.releaseChannel(outbound);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof LastHttpContent) {
            Channel outbound = ctx.channel();
            outbound.pipeline().addBefore("reqEncoder", "timeoutHandler",
                    new ProxyReadTimeoutHandler(gatewayConfig.getRespTimeout(),
                            TimeUnit.MILLISECONDS, inbound));
        }
        super.write(ctx, msg, promise);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocketChannel channel = (SocketChannel) ctx.channel();
        pool.releaseChannel(channel);
        if (inbound.isActive()) {
            inbound.write(CONN_CLOSED());
            inbound.close().addListener(future -> {
                        if (future.isSuccess()) {
                            log.error("连接被关闭:{}", channel.remoteAddress().getHostName());
                        }
                    }
            );
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        pool.releaseChannel((SocketChannel) ctx.channel());
        ctx.close();
        inbound.close().addListener(future -> {
            if (future.isSuccess()) {
                log.error("ForwardHandler,发生错误:{}", cause.getMessage());
            }
        });
    }
}
