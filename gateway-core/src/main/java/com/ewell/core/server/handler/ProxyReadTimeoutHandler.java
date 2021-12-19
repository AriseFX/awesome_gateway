package com.ewell.core.server.handler;

import com.ewell.core.pool.AsyncChannelPool;
import com.google.inject.Inject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

import static com.ewell.common.GatewayMessages.GATEWAY_TIMEOUT;

/**
 * @Author: wy
 * @Date: Created in 1:38 PM 2021/12/9
 * @Description:
 * @Modified: By：
 */
public class ProxyReadTimeoutHandler extends ReadTimeoutHandler {

    @Inject
    private static AsyncChannelPool pool;

    private final Channel inbound;

    public ProxyReadTimeoutHandler(long timeout, TimeUnit unit, Channel inbound) {
        super(timeout, unit);
        this.inbound = inbound;
    }

    @Override
    protected void readTimedOut(ChannelHandlerContext ctx) {
        SocketChannel channel = (SocketChannel) ctx.channel();
        channel.close();
        pool.releaseChannel(channel);
        inbound.writeAndFlush(GATEWAY_TIMEOUT());
    }

}
