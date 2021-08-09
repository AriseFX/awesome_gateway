package com.arise.server;

import com.arise.config.ServerProperties;
import com.arise.os.OSHelper;
import com.arise.server.logging.LogService;
import com.arise.server.logging.LogStorageHandler;
import com.arise.server.proxy.HttpProxyHandler;
import com.arise.server.route.filter.PreRouteFilter;
import com.arise.server.route.filter.ForwardFilter;
import com.arise.server.route.ApiRouteHandler;
import com.arise.server.route.filter.RouteFilter;
import com.arise.server.route.filter.SchedulableFilter;
import com.arise.server.route.match.RouteMatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import net.openhft.affinity.AffinityLock;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: wy
 * @Date: Created in 22:05 2021-06-02
 * @Description:
 * @Modified: By：
 */
@Slf4j
@Component
@Order(2)
public class ServerRunner implements CommandLineRunner {

    @Resource(name = "serverProperties")
    private ServerProperties prop;

    @Bean(name = "filterInit")
    public Object init(List<ForwardFilter> forwardFilters, List<RouteFilter> routeFilters, List<PreRouteFilter> preRouteFilters, RouteMatcher matcher) {
        ApiRouteHandler.forwardFilters = sort(forwardFilters);
        ApiRouteHandler.preRouteFilters = sort(preRouteFilters);
        RouteMatcher.routeFilters = sort(routeFilters);
        ApiRouteHandler.matcher = matcher;
        return new Object();
    }

    private <P1, P2> List<SchedulableFilter<P1, P2>> sort(List<? extends SchedulableFilter<P1, P2>> filters) {
        return filters.stream()
                .sorted(Comparator.comparing(Ordered::getOrder))
                .collect(Collectors.toList());
    }

    @Override
    public void run(String... args) throws InterruptedException {
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
        EventLoopGroup boss = OSHelper.eventLoopGroup(1);
        EventLoopGroup worker = OSHelper.eventLoopGroup(0);
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 4096);
        b.group(boss, worker)
                .channel(OSHelper.acceptChannelType())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpRequestDecoder());
                        p.addLast(new HttpProxyHandler());
                    }
                });
        Channel channel = b.bind(prop.getAddress(), prop.getPort())
                .addListener(future -> {
                    if (future.isSuccess()) {
                        log.debug("CPU布局如下: \r\n{}", AffinityLock.cpuLayout());
                        log.info("Server startup complete！[{}:{}]", prop.getAddress(), prop.getPort());
                        //为了让LogStorageHandler先初始化
                        LogStorageHandler.logService = new LogService();
                    }
                }).sync().channel();
        channel.closeFuture().sync().addListener(
                e -> log.info("Server Stopped！")
        );
    }
}
