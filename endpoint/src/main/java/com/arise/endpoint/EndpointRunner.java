package com.arise.endpoint;

import com.arise.config.ServerProperties;
import com.arise.os.OSHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @Author: wy
 * @Date: Created in 20:23 2021-07-01
 * @Description: 暴露的端点，用于管理
 * @Modified: By：
 */
@Slf4j
@Component
@Order(1)
public class EndpointRunner implements CommandLineRunner {

    @Resource
    private ServerProperties serverProperties;

    @Override
    public void run(String... args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(OSHelper.acceptChannelType())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpResponseEncoder());
                            ch.pipeline().addLast(new HttpRequestDecoder());
                            ch.pipeline().addLast(new HttpObjectAggregator(512 << 10));
                            ch.pipeline().addLast(new HttpMappingHandler());
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ServerProperties.Endpoint endpoint = serverProperties.getEndpoint();
            b.bind(endpoint.getAddress(), endpoint.getPort()).sync()
                    .addListener(e ->
                            log.info("Endpoint startup complete！[{}:{}]", endpoint.getAddress(), endpoint.getPort()))
                    .channel().closeFuture()
                    .addListener(e ->
                            log.info("Endpoint Stopped！"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
