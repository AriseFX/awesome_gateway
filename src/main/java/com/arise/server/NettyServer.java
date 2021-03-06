package com.arise.server;

import com.arise.config.ServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;

/**
 * @Author: wy
 * @Date: Created in 13:52 2021/1/18
 * @Description:
 * @Modified: By：
 */
//@Component
@Order(value = Ordered.LOWEST_PRECEDENCE)
public class NettyServer implements CommandLineRunner {

    @Resource(name = "serverProperties")
    private ServerProperties properties;


    @Override
    public void run(String... args) {
        EventLoopGroup bossGroup = new EpollEventLoopGroup(1, new DefaultThreadFactory("boss"));
        EventLoopGroup workerGroup = new EpollEventLoopGroup(1, new DefaultThreadFactory("worker"));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(EpollServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(1024));
                        }
                    });
            //获取系统参数
            ChannelFuture channelFuture = serverBootstrap.bind(
                    properties.getAddress(),
                    properties.getPort()
            ).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
