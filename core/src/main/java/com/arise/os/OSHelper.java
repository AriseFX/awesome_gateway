package com.arise.os;

import com.arise.base.config.ServerProperties;
import com.arise.server.proxy.ProxyForwardHandler;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: wy
 * @Date: Created in 16:49 2021-06-23
 * @Description: 当前os相关
 * @Modified: By：
 */
public class OSHelper {

    private static final Class<? extends Channel> channelType;

    private static final Class<? extends ServerChannel> acceptChannelType;

    static {
        if (OS.isLinux()) {
            channelType = EpollSocketChannel.class;
            acceptChannelType = EpollServerSocketChannel.class;
        } else if (OS.isMacOSX()) {
            channelType = KQueueSocketChannel.class;
            acceptChannelType = KQueueServerSocketChannel.class;
        } else {
            channelType = NioSocketChannel.class;
            acceptChannelType = NioServerSocketChannel.class;
        }
    }

    public static Class<? extends Channel> channelType() {
        return channelType;
    }

    public static Class<? extends ServerChannel> acceptChannelType() {
        return acceptChannelType;
    }

    public static EventLoopGroup eventLoopGroup(int num, String name) {

        if (OS.isLinux()) {
            return new EpollEventLoopGroup(num, newFactory(name + "-epoll-"));
        } else if (OS.isMacOSX()) {
            return new KQueueEventLoopGroup(num, newFactory(name + "-kqueue-"));
        } else {
            return new NioEventLoopGroup(num, newFactory(name + "-nio-"));
        }
    }

    public static ThreadFactory newFactory(String name) {

        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, name + "-" + counter.getAndIncrement());
            }
        };
    }

    public static PassThroughStrategy nativePassThrough() {
        if (OS.isLinux() && ServerProperties.gatewayConfig.isSplice()) {
            return new SpliceImpl();
        }
        return new DefaultImpl();
    }

    //默认策略
    public static class DefaultImpl implements PassThroughStrategy {

        @Override
        public void accept(Channel channel1, Channel channel2) {
            channel1.pipeline().addLast(new ProxyForwardHandler(channel2));
            channel2.pipeline().addLast(new ProxyForwardHandler(channel1));
        }
    }

    //splice策略
    public static class SpliceImpl implements PassThroughStrategy {

        @Override
        public void accept(Channel channel1, Channel channel2) {
            ((EpollSocketChannel) channel1).spliceTo((EpollSocketChannel) channel2, Integer.MAX_VALUE);
            ((EpollSocketChannel) channel2).spliceTo((EpollSocketChannel) channel1, Integer.MAX_VALUE);
        }
    }

}
