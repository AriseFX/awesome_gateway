package com.ewell.core.server;

import com.ewell.common.GatewayConfig;
import com.ewell.core.monitor.MonitorHandler;
import com.ewell.core.server.handler.ProxyInboundHandler;
import com.ewell.core.server.handler.ProxyOutboundHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.internal.PlatformDependent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.AffinityLock;

/**
 * @Author: wy
 * @Date: Created in 22:05 2021-06-02
 * @Description:
 * @Modified: By：
 */
@Slf4j
@Singleton
public class ServerRunner implements Runnable {

    @Inject
    private NettyBootstrapFactory nettyBootstrapFactory;

    @Inject
    private GatewayConfig gatewayConfig;

    @Override
    @SneakyThrows
    public void run() {
        ServerBootstrap b = nettyBootstrapFactory.getServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                //网络上下行监控
                p.addLast(MonitorHandler.INSTANCE);
                p.addLast(new HttpRequestDecoder());
                p.addLast(new HttpResponseEncoder());
                p.addLast(new ProxyOutboundHandler());
                p.addLast(new ProxyInboundHandler());
            }
        });
        Channel channel = b.bind(gatewayConfig.getAddress(), gatewayConfig.getPort())
                .addListener(future -> {
                    if (future.isSuccess()) {
                        log.info("CPU布局如下: \r\n{}", AffinityLock.cpuLayout());
                        log.info("Server startup complete！[{}:{}]", gatewayConfig.getAddress(), gatewayConfig.getPort());
                        log.info("noCleaner策略:{}", PlatformDependent.useDirectBufferNoCleaner());
                    }
                }).sync().channel();
        channel.closeFuture().sync().addListener(
                e -> log.info("Server Stopped！")
        );
    }
}
