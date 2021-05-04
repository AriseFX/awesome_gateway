package com.arise.server;

import com.arise.internal.pool.AwesomeSocketChannel;
import com.arise.modules.EventProcessor;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import lombok.SneakyThrows;
import net.openhft.chronicle.core.OS;
import org.jctools.queues.SpscArrayQueue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.arise.linux.NativeSupport.*;
import static io.netty.channel.epoll.Native.*;

/**
 * @Author: wy
 * @Date: Created in 15:43 2021-04-15
 * @Description: Sub Reactor
 * @Modified: By：
 */
@SuppressWarnings("all")
public class AwesomeEventLoop implements Runnable {

    private static final AtomicInteger counter = new AtomicInteger();

    //线程通信无锁队列
    private Queue<FileDescriptor> threadNoticeQueue;

    //唤醒Reactor专用
    private int wakeupFd;

    //Readctor本身
    private int ep_fd;

    //定时任务专用
    private int timerFd;

    private PriorityQueue<ScheduledTask> scheduledQueue;

    public Thread currentThread;

    private EpollEventArray events;

    //拒绝java的包装类型
    private final IntObjectMap<EventProcessor> fpMapping = new IntObjectHashMap<>();

    public AwesomeEventLoop(int eventQueueSize) throws IOException {
        this.events = new EpollEventArray(4096);
        //TODO 使用pipe还是eventfd？
        this.wakeupFd = eventFd();
        this.timerFd = timerFd();
        this.ep_fd = epollCreate();
        this.threadNoticeQueue = new SpscArrayQueue<>(eventQueueSize);
        this.scheduledQueue = new PriorityQueue<ScheduledTask>(ScheduledTask::compareTo);
        this.currentThread = new Thread(this, "awesome-gateway-worker-io-thread_" + counter.getAndIncrement());
        //上epoll树
        epollCtlAdd0(ep_fd, wakeupFd, EPOLLIN | EPOLLET);
        //避免storeStore重排序
        OS.memory().storeFence();
        currentThread.start();
    }

    @SneakyThrows
    @Override
    public void run() {
        for (; ; ) {
            FileDescriptor polledFD = threadNoticeQueue.poll();
            if (polledFD != null) {
                epollCtlAdd0(ep_fd, polledFD.intValue(), EPOLLOUT | EPOLLIN | EPOLLET);
            }
            int timeout = -1;
            //定时任务相关
            ScheduledTask task = scheduledQueue.poll();
            if (task != null) {
                timeout = task.getTimeout();
            }
            //timeout使用timerFd对应的定时器
            int i = epollWait0(ep_fd, events.memoryAddress(), 4096, timerFd, timeout, 0);
            if (i > 0) {
                for (int index = 0; index < i; index++) {
                    int event = events.events(index);
                    if ((event & (EPOLLERR | EPOLLIN | EPOLLOUT)) != 0) {
                        int fd = events.fd(index);
                        //各种fd
                        if (fd == timerFd) {
                            task.getProcess().doProcess(new FileDescriptor(fd), this);
                        } else if (fd == wakeupFd) {
                            /*void*/
                        } else {
                            FileDescriptor conn_fd = new FileDescriptor(fd);
                            //处理epoll事件
                            EventProcessor processor = fpMapping.remove(fd);
                            if (processor != null) {
                                processor.doProcess(conn_fd, this);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 提交任务到Reactor
     *
     * @param fd
     * @param processor
     */
    public void pushFd(FileDescriptor fd, EventProcessor processor) {
        threadNoticeQueue.offer(fd);
        fpMapping.put(fd.intValue(), processor);
        if (Thread.currentThread() != currentThread) {
            //用来唤醒阻塞状态的reactor
            write2EventFd(wakeupFd);
        }
    }

    /**
     * 提交定时任务到Reactor
     */
    public void scheduled(ScheduledTask task) {
        scheduledQueue.offer(task);
    }

    /**
     * 创建socket channel
     */
    public AwesomeSocketChannel newAwesomeChannel(InetSocketAddress remote) {
        return new AwesomeSocketChannel(this, remote);
    }
}
