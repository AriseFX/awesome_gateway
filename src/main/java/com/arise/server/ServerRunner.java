package com.arise.server;

import com.arise.server.proxy.EpollHttpRouteHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Author: wy
 * @Date: Created in 22:05 2021-06-02
 * @Description:
 * @Modified: By：
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ServerRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws InterruptedException {
        /**
         *
         *               +------------+      +----------
         *  request----> |LocalChannel+----> |HttpDecode
         *               +------------+      +-----+----
         *                                         v
         *  +-------------+       +-----------+  +-+-----+
         *  |RemoteChannel| <-----+Mod Request+--+Forward|
         *  +-------------+       +-----------+  +-------+
         *
         *              +-------------+          +-------+
         * response---->+RemoteChannel+--------> |Forward|
         *              +-------------+          +---+---+
         *                                           v
         *                                  +------------+
         *                                  |LocalChannel|
         *                                  +------------+
         *
         */
        EventLoopGroup boss = new EpollEventLoopGroup(1);
        EventLoopGroup worker = new EpollEventLoopGroup(0);
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 4096);
        b.group(boss, worker)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        //解析请求行，后续走splice
                        p.addLast(new HttpRequestDecoder());
                        p.addLast(new EpollHttpRouteHandler());
                    }
                });
        Channel channel = b.bind(8192).sync().channel();
        channel.closeFuture().sync();
    }
}
