package com.arise.server;

import com.arise.modules.chain.HandleChain;
import com.arise.modules.http.HttpProtocolHandler;
import io.netty.channel.epoll.Native;
import io.netty.channel.unix.FileDescriptor;
import net.openhft.chronicle.core.OS;
import org.jctools.queues.SpscArrayQueue;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.arise.linux.NativeSupport.*;
import static io.netty.channel.epoll.Native.*;

/**
 * @Author: wy
 * @Date: Created in 9:36 2021-02-04
 * @Description: 事件循环，用于分离文件描述符事件
 * @Modified: By：
 */
public class WorkerEventLoop implements Runnable {

    private SpscArrayQueue<FdEvent> threadNoticeQueue;

    private int ep_fd;

    private int time_fd;

    private EpollEventArray events;

    public WorkerEventLoop() {
        events = new EpollEventArray(4096);
        ep_fd = epollCreate();
        threadNoticeQueue = new SpscArrayQueue<>(20);
        //实现final关键字的语义（避免storeStore重排序）
        OS.memory().storeFence();
        new Thread(this).start();
    }

    public void pushFd(FileDescriptor fd) {
        threadNoticeQueue.offer(new FdEvent(fd));
    }

    @Override
    public void run() {
        for (; ; ) {
            FdEvent fdEvent = threadNoticeQueue.poll();
            if (fdEvent != null) {
                epollCtlAdd0(ep_fd, fdEvent.getFd().intValue(), EPOLLIN | Native.EPOLLET);
            }
            int i = epollWait0(ep_fd, events.memoryAddress(), 4096, 1);
            if (i > 0) {
                for (int index = 0; index < i; index++) {
                    int event = events.events(index);
                    if ((event & (EPOLLERR | EPOLLOUT)) != 0) {
                        System.out.println("writeable");
                    }
                    if ((event & (EPOLLERR | EPOLLIN)) != 0) {
                        FileDescriptor conn_fd = new FileDescriptor(events.fd(index));
                        //处理epoll_in事件
                        processReadEvent(conn_fd);
                    }
                }
            }
        }
    }

    public void processReadEvent(FileDescriptor fd) {
        HandleChain chain = new HandleChain();
        chain.addHandler(new HttpProtocolHandler());
        do {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            try {
                int num = fd.read(buffer, 0, buffer.limit());
                if (num > 0) {
                    buffer.limit(num);
                    chain.handleRead(buffer);
                }
                if (num <= 0) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (true);
    }

}
