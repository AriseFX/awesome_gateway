package com.arise.server;

import com.arise.base.config.GatewayConfig;
import com.arise.base.config.ServerProperties;
import com.arise.server.proxy.HttpProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
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
public class ServerRunner implements Runnable {

    @Override
    @SneakyThrows
    public void run() {
        /*

                        +------------+      +-----------
           request----> |LocalChannel+----> |HttpDecode|
                        +------------+      +-----+----
                                                  v
           +-------------+       +-----------+  +-+-----+
           |RemoteChannel| <-----+Mod Request+--+Forward|
           +-------------+       +-----------+  +-------+

                       +-------------+          +-------+
          response---->+RemoteChannel+--------> |Forward|
                       +-------------+          +---+---+
                                                    v
                                           +------------+
                                           |LocalChannel|
                                           +------------+

         */
        ServerBootstrap b = NettyBootstrapFactory.getServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new HttpRequestDecoder());
                p.addLast(new HttpProxyHandler());
            }
        });
        GatewayConfig gatewayConfig = ServerProperties.gatewayConfig;
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
