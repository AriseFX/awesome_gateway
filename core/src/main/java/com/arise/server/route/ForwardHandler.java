package com.arise.server.route;

import com.arise.server.route.pool.RemoteChannelPool;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.arise.server.GatewayMessage.*;

/**
 * @Author: wy
 * @Date: Created in 22:47 2021-06-01
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ForwardHandler extends ChannelInboundHandlerAdapter {

    private final Channel forwardChannel;
    private final Promise<List<HttpObject>> respPromise;

    private final List<HttpObject> payloads = new ArrayList<>(1);

    public ForwardHandler(Promise<List<HttpObject>> respPromise, Channel forwardChannel) {
        this.forwardChannel = forwardChannel;
        this.respPromise = respPromise;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        //处理背压
        forwardChannel.config().setAutoRead(ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        SocketChannel channel = (SocketChannel) ctx.channel();
        if (forwardChannel.isActive()) {
            payloads.add((HttpObject) msg);
            if (msg instanceof LastHttpContent) {
                respPromise.setSuccess(payloads);
                payloads.forEach(forwardChannel::writeAndFlush);
                forwardChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(future -> {
                    if (future.isDone()) {
                        RemoteChannelPool.releaseChannel(channel);
                        log.debug("释放连接：{}", ctx.channel().toString());
                    }
                });
            }
        } else {
            RemoteChannelPool.releaseChannel(channel);
            log.debug("inbound关闭,释放连接：{}", ctx.channel().toString());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocketChannel channel = (SocketChannel) ctx.channel();
        RemoteChannelPool.releaseChannel(channel);
        if (forwardChannel.isActive()) {
            writeMsg(forwardChannel, _ConnectionClose);
            forwardChannel.close().addListener(future -> {
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
        RemoteChannelPool.releaseChannel((SocketChannel) ctx.channel());
        ctx.close();
        forwardChannel.close().addListener(future -> {
            if (future.isSuccess()) {
                log.error("ForwardHandler,发生错误:{}", cause.getMessage());
            }
        });
    }
}
