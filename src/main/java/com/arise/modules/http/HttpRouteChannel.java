package com.arise.modules.http;

import com.arise.modules.SimpleSocketChannel;
import com.arise.modules.http.constant.StandardHttpResponse;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.Socket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import static io.netty.channel.epoll.Native.EPOLLOUT;
import static io.netty.channel.epoll.Native.splice;
import static io.netty.channel.unix.FileDescriptor.pipe;

/**
 * @Author: wy
 * @Date: Created in 10:45 2021-05-28
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class HttpRouteChannel extends SimpleSocketChannel {

    private FileDescriptor mainFd;

    private volatile boolean connected = false;

    private final Deque<HttpServerRequest> buffer = new ArrayDeque<>();

    public HttpRouteChannel(InetSocketAddress remote, FileDescriptor mainFd) {
        super(Socket.newSocketStream());
        try {
            if (fd.connect(remote)) {
                connected = true;
            }
            super.opFlag = EPOLLOUT;
            this.mainFd = mainFd;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void putRemoteBuf(HttpServerRequest buf) {
        buffer.offer(buf);
    }

    public void close() {
        try {
            fd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRead() {
        try {
            FileDescriptor[] respPipe = pipe();
            //转发给客户端
            while (true) {
                int toPipe = splice(fd.intValue(), -1, respPipe[1].intValue(), -1, 0x7fffffff);
                int toSocket = splice(respPipe[0].intValue(), -1, mainFd.intValue(), -1, 0x7fffffff);
                log.info("toPipe:{},toSocket:{}", toPipe, toSocket);
                if (toSocket == 0) break;
            }
            //TODO 连接复用的情况下len如何考虑？
            //一次请求/响应结束应该移除该fd的监听，考虑连接复用时，
            eventLoop.remove(fd.intValue());
//            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWrite() {
        if (!connected) finishConnect();
        try {
            HttpServerRequest request = buffer.poll();
            if (request != null) {
                ByteBuffer buf = request.toBuffer();
                //先写不完整的http
                fd.write(buf, 0, buf.limit());
                //body
                int contentLength = request.contentLength;
                if (contentLength > 0) {
                    //创建pipe用于socket splice
                    FileDescriptor[] reqPipe = pipe();
                    splice(mainFd.intValue(), -1, reqPipe[1].intValue(), -1, contentLength);
                    splice(reqPipe[0].intValue(), -1, fd.intValue(), -1, contentLength);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishConnect() {
        connected = true;
    }

    @Override
    public void onError() {
        try {
            super.onError();
            eventLoop.remove(mainFd.intValue());
            ByteBuffer cache = StandardHttpResponse.ServerError.cache();
            mainFd.write(cache, 0, cache.remaining());
            mainFd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
