package com.arise.endpoint;

import com.arise.base.config.GatewayConfig;
import com.arise.base.config.ServerProperties;
import com.arise.os.OSHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: wy
 * @Date: Created in 20:23 2021-07-01
 * @Description: 暴露的端点，用于管理
 * @Modified: By：
 */
@Slf4j
public class EndpointRunner implements Runnable {

    @Override
    public void run() {
        EventLoopGroup bossGroup = OSHelper.eventLoopGroup(1, "endpoint-boss");
        EventLoopGroup workerGroup = OSHelper.eventLoopGroup(1, "endpoint-worker");
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
            GatewayConfig.Endpoint endpoint = ServerProperties.gatewayConfig.getEndpoint();
            b.bind(endpoint.getAddress(), endpoint.getPort()).sync()
                    .addListener(e -> {
                        if (e.isSuccess()) {
                            log.info("Endpoint startup complete！[{}:{}]", endpoint.getAddress(),
                                    endpoint.getPort());
                        } else {
                            log.info("Endpoint startup fail!");
                        }
                    })
                    .channel().closeFuture()
                    .addListener(e ->
                            log.info("Endpoint Stopped！"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
