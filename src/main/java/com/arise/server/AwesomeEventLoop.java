package com.arise.server;

import com.arise.modules.ReadEventProcessor;
import io.netty.channel.epoll.Native;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import lombok.SneakyThrows;
import net.openhft.chronicle.core.OS;
import org.jctools.queues.SpscArrayQueue;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.arise.linux.NativeSupport.*;
import static io.netty.channel.epoll.Native.*;

/**
 * @Author: wy
 * @Date: Created in 15:43 2021-04-15
 * @Description: 轮询事件
 * @Modified: By：
 */
public class AwesomeEventLoop implements Runnable {

    private static final AtomicInteger counter = new AtomicInteger();

    /**
     * 线程通信无锁队列
     */
    private Queue<FdEvent> threadNoticeQueue;

    private Queue<FdEvent> scheduledQueue;

    private int ep_fd;

    private int time_fd;

    public Thread loopThread;

    private EpollEventArray events;

    //拒绝java的包装类型
    private final IntObjectMap<ReadEventProcessor> fpMapping = new IntObjectHashMap<>();

    public AwesomeEventLoop(int eventQueueSize) {
        this.events = new EpollEventArray(4096);
        this.ep_fd = epollCreate();
        this.threadNoticeQueue = new SpscArrayQueue<>(eventQueueSize);
        this.scheduledQueue = new PriorityQueue<>();
        this.loopThread = new Thread(this, "awesome-gateway-worker-thread_" + counter.getAndIncrement());
        //实现final关键字的语义（避免storeStore重排序）
        OS.memory().storeFence();
        loopThread.start();
    }

    public void pushFd(FileDescriptor fd, ReadEventProcessor processor) {
        threadNoticeQueue.offer(new FdEvent(fd));
        fpMapping.put(fd.intValue(), processor);
    }

    @SneakyThrows
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
                        ReadEventProcessor processor = fpMapping.get(conn_fd.intValue());
                        if (processor != null) {
                            processor.doProcess(conn_fd, this);
                        }
                    }
                }
            }
        }
    }
}
