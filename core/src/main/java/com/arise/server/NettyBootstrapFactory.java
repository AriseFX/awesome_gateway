package com.arise.server;

import com.arise.base.config.ServerProperties;
import com.arise.os.OSHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import net.openhft.chronicle.core.OS;

/**
 * @Author: wy
 * @Date: Created in 1:54 下午 2021/9/17
 * @Description:
 * @Modified: By：
 */
public class NettyBootstrapFactory {

    public static ServerBootstrap getServerBootstrap() {
        EventLoopGroup boss = OSHelper.eventLoopGroup(1, "gateway-boss");
        EventLoopGroup worker = OSHelper.eventLoopGroup(8, "gateway-worker");
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 4096);
        if (OS.isLinux() && ServerProperties.gatewayConfig.isSplice()) {
            b.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        }
        b.group(boss, worker);
        b.channel(OSHelper.acceptChannelType());
        return b;
    }

    public static Bootstrap getBootstrap() {
        Bootstrap b = new Bootstrap();
        b.channel(OSHelper.channelType());
        if (OS.isLinux() && ServerProperties.gatewayConfig.isSplice()) {
            b.option(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        }
        return b;
    }
}
