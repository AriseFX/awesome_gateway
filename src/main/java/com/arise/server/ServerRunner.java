package com.arise.server;

import com.arise.config.ServerProperties;
import com.arise.server.proxy.EpollHttpProxyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.AffinityLock;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityStrategy;
import net.openhft.affinity.AffinityThreadFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Author: wy
 * @Date: Created in 22:05 2021-06-02
 * @Description:
 * @Modified: By：
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ServerRunner implements CommandLineRunner {

    @Resource(name = "serverProperties")
    private ServerProperties prop;

    @Override
    public void run(String... args) throws InterruptedException {
        /**
         *
         *               +------------+      +-----------
         *  request----> |LocalChannel+----> |HttpDecode|
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
        AffinityThreadFactory threadFactory = new AffinityThreadFactory("awesome", AffinityStrategies.SAME_SOCKET, AffinityStrategies.DIFFERENT_CORE);
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
                        p.addLast(new HttpRequestDecoder());
                        p.addLast(new EpollHttpProxyHandler());
                    }
                });
        Channel channel = b.bind(prop.getAddress(), prop.getPort())
                .addListener(future -> {
                    if (future.isSuccess()) {
                        log.debug("CPU布局如下: \r\n{}", AffinityLock.cpuLayout());
                        log.info("Server startup complete！[{}:{}]", prop.getAddress(), prop.getPort());
                    }
                }).sync().channel();
        channel.closeFuture().sync();
    }
}
