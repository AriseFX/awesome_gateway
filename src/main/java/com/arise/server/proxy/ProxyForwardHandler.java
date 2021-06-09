package com.arise.server.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @Author: wy
 * @Date: Created in 22:47 2021-06-01
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ProxyForwardHandler extends ChannelInboundHandlerAdapter {

    private final EpollSocketChannel channel;

    public ProxyForwardHandler(EpollSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        //处理背压
        channel.config().setAutoRead(ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (channel.isActive()) {
            if (msg instanceof HttpRequest) {
                modRequest((HttpRequest) msg);
            }
            channel.writeAndFlush(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (channel.isActive()) {
            channel.flush();
            channel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    /**
     * 修改报文内容
     */
    private void modRequest(HttpRequest request) {
        request.headers().remove("Proxy-Authorization");
        String proxyConnection = request.headers().get("Proxy-Connection");
        if (Objects.nonNull(proxyConnection)) {
            request.headers().set("Connection", proxyConnection);
            request.headers().remove("Proxy-Connection");
        }
        //获取Host和port
        String hostAndPortStr = request.headers().get("Host");
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        int port = Integer.parseInt(portStr);

        try {
            String url = request.uri();
            int index = url.indexOf(host) + host.length();
            url = url.substring(index);
            if (url.startsWith(":")) {
                url = url.substring(1 + String.valueOf(port).length());
            }
            request.setUri(url);
        } catch (Exception e) {
            System.err.println("无法获取url：" + request.uri() + " " + host);
        }
    }
}
