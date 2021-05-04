package com.arise.internal.pool;

import com.arise.modules.Bufferable;
import com.arise.modules.WriteReadyProcessor;
import com.arise.server.AwesomeEventLoop;
import com.arise.server.ScheduledTask;
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

    private boolean active;

    public AwesomeSocketChannel(AwesomeEventLoop eventLoop, InetSocketAddress remoteAddress) {
        //非阻塞状态的socket文件
        this.socket = Socket.newSocketStream();
        this.remote = remoteAddress;
        this.eventLoop = eventLoop;
        OS.memory().storeFence();
    }

    public void connect(int timeout, Runnable command) {
        try {
            boolean connected = socket.connect(remote);
            if (connected) {
                command.run();
            } else {
                eventLoop.pushFd(socket, (WriteReadyProcessor) (callback_fd, callback_eventLoop) ->
                        command.run()
                );
                //超时执行
                eventLoop.scheduled(new ScheduledTask(timeout,
                        (callback_fd, callback_eventLoop) -> {
                            //TODO epollCTL移除事件
                            log.error("连接超时!");
                        }));
            }
            //TODO 发起一个epoll_out事件去监听连接成功事件
            //暂时先直接sleep
            Thread.sleep(100);
            if (connected) {
                active = true;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void write(Bufferable bufferable) {
        //TODO 修改buffer起始结尾
        ByteBuffer buffer = bufferable.toBuffer();
        if (buffer != null) {
            buffer.flip();
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
