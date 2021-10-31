package com.arise.server.route.pool;

import com.arise.base.config.GatewayConfig;
import com.arise.server.NettyBootstrapFactory;
import com.arise.server.route.logging.LogStorageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wy
 * @Date: Created in 18:21 2021-06-05
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class AsyncChannelPool {

    private final ConcurrentHashMap<String, ChannelPool> pools = new ConcurrentHashMap<>();
    @Setter
    private int connectTimeout;
    @Setter
    private int maxConnections;
    @Setter
    private int maxPendingAcquires;

    private final AttributeKey<Boolean> added = AttributeKey.valueOf("added");

    public AsyncChannelPool(GatewayConfig.Pool pool) {
        this.connectTimeout = pool.getTimeout();
        this.maxConnections = pool.getMaxConnections();
        this.maxPendingAcquires = pool.getMaxPendingAcquires();
    }

    /**
     * 异步获取channel
     */
    public void acquireChannel(boolean ssl, String host, int port, EventLoop eventLoop,
                               Promise<Channel> promise) {
        ChannelPool channelPool = pools.computeIfAbsent(host + eventLoop.hashCode() + port,
                k ->
                        newFixedChannelPool(ssl, host, port, eventLoop)
        );
        channelPool.acquire(promise);
    }

    public void releaseChannel(SocketChannel channel) {
        InetSocketAddress address = channel.remoteAddress();
        String host = address.getHostString();
        int port = address.getPort();
        ChannelPool channelPool = pools.get(host + channel.eventLoop().hashCode() + port);
        if (channelPool != null) {
            channelPool.release(channel);
        }
    }

    private ChannelPool newFixedChannelPool(boolean ssl, String host, int port, EventLoop eventLoop) {
        Bootstrap b = NettyBootstrapFactory.getBootstrap().group(eventLoop)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(host, port);
        return new FixedChannelPool(
                b,
                new AbstractChannelPoolHandler() {

                    @Override
                    public void channelCreated(Channel ch) {
//                        log.debug("Channel created:{}", ch);
                    }

                    @Override
                    public void channelReleased(Channel ch) {
                        //清空状态
                        DefaultChannelPipeline pipeline = (DefaultChannelPipeline) ch.pipeline();
                        //连接复用时SSL会话保持
                        ChannelHandler handler;
                        while ((handler = pipeline.last()) != null && !(handler instanceof SslHandler)) {
                            pipeline.removeLast();
                        }
                    }

                    /**
                     * write -> http request encode -> bytebuf
                     * read -> bytebuf -> http response decode
                     */
                    @Override
                    public void channelAcquired(Channel ch) throws SSLException {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (!ch.hasAttr(added) && ssl) {
                            SslContext context = SslContextBuilder.forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .build();
                            InetSocketAddress address = (InetSocketAddress) ch.remoteAddress();
                            SslHandler sslHandler = context.newHandler(ch.alloc(), address.getHostString(), address.getPort());
                            ch.pipeline().addFirst(sslHandler);
                            ch.attr(added);
                        }
                        pipeline.addLast("reqEncoder", new HttpRequestEncoder());
                        pipeline.addLast("respDecoder", new HttpResponseDecoder());
                        pipeline.addLast("log", new LogStorageHandler());
                    }
                }, maxConnections, maxPendingAcquires);
    }
}
