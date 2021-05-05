package com.arise.modules.http;

import com.arise.internal.chain.ChainContext;
import com.arise.internal.pool.AwesomeSocketChannel;
import com.arise.modules.ProtocolHandler;
import com.arise.modules.ReadReadyProcessor;
import com.arise.server.AwesomeEventLoop;
import io.netty.channel.epoll.Native;
import io.netty.channel.unix.FileDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;

import static io.netty.channel.unix.FileDescriptor.pipe;

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

        AwesomeSocketChannel channel = eventLoop.newAwesomeChannel(
                new InetSocketAddress("192.168.150.102", 8099));
        //TODO 连接复用 超时处理
        //连接成功后执行write
        channel.connect(3, () -> {
            try {
                //先写不完整的http
                channel.write(request);
                //body
                int contentLength = request.contentLength;
                if (contentLength > 0) {
                    //创建pipe用于socket splice
                    FileDescriptor[] reqPipe = pipe();
                    Native.splice(currentFd.intValue(), -1, reqPipe[1].intValue(), -1, contentLength);
                    Native.splice(reqPipe[0].intValue(), -1, channel.socket.intValue(), -1, contentLength);
                }
                eventLoop.pushFd(channel.socket.intValue(),
                        (ReadReadyProcessor) (i_callback_fd, i_callback_ep) -> {
                            //[0] = read end, [1] = write end
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            FileDescriptor[] respPipe = pipe();
                            //转发给客户端
                            //TODO 连接复用的情况下len如何考虑？
                            int toPipe = Native.splice(i_callback_fd.intValue(), -1, respPipe[1].intValue(), -1, 1 << 30);
                            int toSocket = Native.splice(respPipe[0].intValue(), -1, currentFd.intValue(), -1, 1 << 30);
                            log.info("toPipe:{},toSocket:{}", toPipe, toSocket);
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

}
