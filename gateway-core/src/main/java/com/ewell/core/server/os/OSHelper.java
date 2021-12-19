package com.ewell.core.server.os;

import com.ewell.common.GatewayConfig;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.core.OS;

import com.google.inject.Inject;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static net.openhft.affinity.AffinityStrategies.DIFFERENT_CORE;

/**
 * @Author: wy
 * @Date: Created in 16:49 2021-06-23
 * @Description: 当前os相关
 * @Modified: By：
 */

public class OSHelper {

    private static final Class<? extends Channel> channelType;

    private static final Class<? extends ServerChannel> acceptChannelType;

    @Inject
    private static GatewayConfig gatewayConfig;

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
            return new EpollEventLoopGroup(num, getFactory(name + "-epoll-"));
        } else if (OS.isMacOSX()) {
            return new KQueueEventLoopGroup(num, getFactory(name + "-kqueue-"));
        } else {
            return new NioEventLoopGroup(num, getFactory(name + "-nio-"));
        }
    }

    public static ThreadFactory getFactory(String name) {
        return new ThreadFactory() {
            private AffinityLock lastAffinityLock = null;
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public synchronized Thread newThread(Runnable r) {
                return new Thread(() -> {
                    if (gatewayConfig.isAffinity()) {
                        try (AffinityLock ignored = acquireLockBasedOnLast()) {
                            r.run();
                        }
                    } else {
                        r.run();
                    }
                }, name + "-" + counter.getAndIncrement());
            }

            private synchronized AffinityLock acquireLockBasedOnLast() {
                AffinityLock al = lastAffinityLock == null ?
                        AffinityLock.acquireLock() : lastAffinityLock.acquireLock(DIFFERENT_CORE);
                if (al.cpuId() >= 0)
                    lastAffinityLock = al;
                return al;
            }
        };
    }
}
