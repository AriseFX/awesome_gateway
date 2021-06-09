package com.arise.server.route;

import com.arise.server.route.pool.RemoteChannelPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wy
 * @Date: Created in 22:47 2021-06-01
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ForwardHandler extends ChannelInboundHandlerAdapter {

    private final EpollSocketChannel otherChannel;

    public ForwardHandler(EpollSocketChannel otherChannel) {
        this.otherChannel = otherChannel;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        //处理背压
        otherChannel.config().setAutoRead(ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (otherChannel.isActive()) {
            otherChannel.writeAndFlush(msg).addListener(future -> {
                if (msg instanceof LastHttpContent && future.isDone()) {
                    SocketChannel channel = (SocketChannel) ctx.channel();
                    RemoteChannelPool.releaseChannel(channel);
                    log.debug("释放连接：{}", ctx.channel().toString());
                }
            });
        } else {
            RemoteChannelPool.releaseChannel((SocketChannel) ctx.channel());
            log.debug("inbound 关闭 ，释放连接：{}", ctx.channel().toString());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (otherChannel.isActive()) {
            otherChannel.flush();
            otherChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage());
        ctx.close();
    }
}
