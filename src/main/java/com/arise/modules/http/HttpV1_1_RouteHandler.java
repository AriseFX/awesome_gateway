package com.arise.modules.http;

import com.arise.internal.chain.ChainContext;
import com.arise.modules.ProtocolHandler;
import com.arise.modules.http.constant.StandardHttpResponse;
import com.arise.server.AwesomeEventLoop;
import com.arise.server.ScheduledTask;
import io.netty.channel.unix.FileDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 9:29 2021-04-07
 * @Description: 处理路由的逻辑，单线程
 * @Modified: By：
 */
@Slf4j
public class HttpV1_1_RouteHandler implements ProtocolHandler {

    @Override
    public void handleRequest(ChainContext ctx, Object msg) {
        HttpServerRequest request = (HttpServerRequest) msg;
        FileDescriptor currentFd = ctx.getCurrentFd();
        AwesomeEventLoop eventLoop = ctx.getEventLoop();
        HttpRouteChannel channel = new HttpRouteChannel(
                new InetSocketAddress("localhost", 8081),
                currentFd);
        channel.putRemoteBuf(request);
        eventLoop.startMonitor(channel);
        eventLoop.scheduled(new ScheduledTask(5, callback_eventLoop -> {
            if (!channel.isConnected()) {
                try {
                    ByteBuffer cache = StandardHttpResponse.TimeoutError.cache();
                    currentFd.write(cache, 0, cache.remaining());
                    eventLoop.remove(channel.getFd().intValue());
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }
}
