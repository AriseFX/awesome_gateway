package com.arise.modules.http;

import com.arise.internal.chain.ChainContext;
import com.arise.internal.pool.AwesomeSocketChannel;
import com.arise.modules.ProtocolHandler;
import com.arise.server.AwesomeEventLoop;
import io.netty.channel.epoll.Native;
import io.netty.channel.unix.FileDescriptor;

import java.io.IOException;
import java.net.InetSocketAddress;

import static io.netty.channel.unix.FileDescriptor.pipe;

/**
 * @Author: wy
 * @Date: Created in 9:29 2021-04-07
 * @Description: 处理路由的逻辑
 * @Modified: By：
 */
public class HttpV1_1_RouteHandler implements ProtocolHandler {

    @Override
    public void handleRequest(ChainContext ctx, Object msg) {
        try {
            HttpServerRequest request = (HttpServerRequest) msg;
            AwesomeEventLoop eventLoop = ctx.getEventLoop();


            AwesomeSocketChannel channel = new AwesomeSocketChannel(
                    new InetSocketAddress("192.168.150.102", 8099));
            //TODO 连接复用 超时处理
            channel.connect();
            if (ctx.inEventLoop()) {
                FileDescriptor currentFd = ctx.getCurrentFd();
                //创建pipe用于socket splice
                FileDescriptor[] reqPipe = pipe();
                //先写不完整的http
                channel.write(request);
                //body
                int contentLength = request.contentLength;
                if (contentLength > 0) {
                    Native.splice(currentFd.intValue(), -1, reqPipe[1].intValue(), -1, contentLength);
                    Native.splice(reqPipe[0].intValue(), -1, channel.socket.intValue(), -1, contentLength);
                }
                ctx.getEventLoop().pushFd(new FileDescriptor(channel.socket.intValue()),
                        (callback_fd, callback_ep) -> {
                            //[0] = read end, [1] = write end
                            FileDescriptor[] respPipe = pipe();
                            //转发给客户端
                            //TODO 连接复用的情况下len如何考虑？
                            Native.splice(callback_fd.intValue(), -1, respPipe[1].intValue(), -1, 1 << 30);
                            Native.splice(respPipe[0].intValue(), -1, ctx.getCurrentFd().intValue(), -1, 1 << 30);
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(ChainContext ctx, Object msg) {

    }

}
