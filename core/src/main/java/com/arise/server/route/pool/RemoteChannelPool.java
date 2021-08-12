package com.arise.server.route.pool;

import com.arise.os.OSHelper;
import com.arise.server.logging.LogStorageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.util.concurrent.Promise;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wy
 * @Date: Created in 18:21 2021-06-05
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class RemoteChannelPool {

    private static final ConcurrentHashMap<String, ChannelPool> pools = new ConcurrentHashMap<>();
    @Setter
    private static int connectTimeout;
    @Setter
    private static int maxConnections;
    @Setter
    private static int maxPendingAcquires;

    /**
     * 异步获取channel
     */
    public static void acquireChannel(String host, int port, EventLoop eventLoop, Promise<Channel> promise) {
        //拼接eventLoop.hashCode()是为了保证连接池与eventLoop绑定
        ChannelPool channelPool = pools.computeIfAbsent(host + eventLoop.hashCode() + port,
                k ->
                        newFixedChannelPool(host, port, eventLoop)
        );
        channelPool.acquire(promise);
    }

    public static void releaseChannel(SocketChannel channel) {
        log.debug("channel pool:{}", "releaseChannel");
        InetSocketAddress address = channel.remoteAddress();
        String host = address.getHostString();
        int port = address.getPort();
        ChannelPool channelPool = pools.get(host + channel.eventLoop().hashCode() + port);
        if (channelPool != null) {
            channelPool.release(channel);
        }
    }

    private static ChannelPool newFixedChannelPool(String host, int port, EventLoop eventLoop) {
        Bootstrap b = new Bootstrap().group(eventLoop)
                .channel(OSHelper.channelType())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(host, port);
        return new FixedChannelPool(
                b,
                new AbstractChannelPoolHandler() {

                    @Override
                    public void channelCreated(Channel ch) {
                        log.debug("Channel created:{}", ch);
                    }

                    @Override
                    public void channelReleased(Channel ch) {
                        //清空状态
                        ChannelPipeline pipeline = ch.pipeline();
                        while (pipeline.last() != null) {
                            pipeline.removeLast();
                        }
                    }

                    /**
                     * write -> http request encode -> bytebuf
                     * read -> bytebuf -> http response decode
                     */
                    @Override
                    public void channelAcquired(Channel ch) {
                        ch.pipeline().addLast("reqEncoder", new HttpRequestEncoder());
                        ch.pipeline().addLast("respDecoder", new HttpResponseDecoder());
                        ch.pipeline().addLast("log", new LogStorageHandler());
                    }
                }, maxConnections, maxPendingAcquires);
    }
}
