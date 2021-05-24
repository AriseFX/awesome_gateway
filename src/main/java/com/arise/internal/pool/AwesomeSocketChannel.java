package com.arise.internal.pool;

import com.arise.modules.Bufferable;
import com.arise.modules.TimerReadyProcessor;
import com.arise.modules.WriteReadyProcessor;
import com.arise.server.AwesomeEventLoop;
import com.arise.server.ScheduledTask;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.Socket;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.core.OS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 13:49 2021-03-18
 * @Description: 抽象了对Socket的操作
 * @Modified: By：
 */
@Slf4j
public class AwesomeSocketChannel {

    public Socket socket;

    private InetSocketAddress remote;

    private AwesomeEventLoop eventLoop;

    private volatile boolean active;

    public AwesomeSocketChannel(AwesomeEventLoop eventLoop, InetSocketAddress remoteAddress) {
        //非阻塞状态的socket文件
        this.socket = Socket.newSocketStream();
        this.remote = remoteAddress;
        this.eventLoop = eventLoop;
        OS.memory().storeFence();
    }

    public void connect(int timeout, Runnable errorHock, Runnable command) {
        try {
            boolean connected = socket.connect(remote);
            if (connected) {
                command.run();
            } else {
                eventLoop.pushFd(socket.intValue(), new WriteReadyProcessor() {
                            @Override
                            public void onReady(FileDescriptor callback_fd, AwesomeEventLoop callback_eventLoop) {
                                active = true;
                                command.run();
                            }

                            @Override
                            public void onError(FileDescriptor callback_fd, AwesomeEventLoop callback_eventLoop) {
                                try {
                                    log.error("error no:{},fd:{}", new Socket(callback_fd.intValue()).getSoError(), callback_fd.intValue());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                callback_eventLoop.remove(callback_fd.intValue());
                                errorHock.run();
                                log.error("连接错误!");
                            }
                        }
                );
                //处理超时
                eventLoop.scheduled(new ScheduledTask(timeout,
                        (TimerReadyProcessor) (callback_fd, callback_eventLoop) -> {
                            if (!active) {
                                callback_eventLoop.remove(callback_fd.intValue());
                                log.error("连接超时!");
                            }
                        }));
            }
            if (connected) {
                active = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(Bufferable bufferable) {
        //TODO 修改buffer起始结尾
        ByteBuffer buffer = bufferable.toBuffer();
        if (buffer != null) {
            write0(buffer, 0, buffer.limit());
        }
    }

    public void write0(ByteBuffer buffer, int pos, int limit) {
        try {
            socket.write(buffer, pos, limit);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}