package com.arise.server;

import com.arise.modules.EventProcessor;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import lombok.SneakyThrows;
import net.openhft.chronicle.core.OS;
import org.jctools.queues.SpscArrayQueue;

import java.io.IOException;
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

    public Thread loopThread;

    private EpollEventArray events;

    //拒绝java的包装类型
    private final IntObjectMap<EventProcessor> fpMapping = new IntObjectHashMap<>();

    public AwesomeEventLoop(int eventQueueSize) throws IOException {
        this.events = new EpollEventArray(4096);
        //TODO 使用pipe还是eventfd？
        this.wakeupFd = eventFd();
        this.ep_fd = epollCreate();
        this.threadNoticeQueue = new SpscArrayQueue<>(eventQueueSize);
        this.scheduledQueue = new PriorityQueue<>(ScheduledTask::compareTo);
        this.loopThread = new Thread(this, "awesome-gateway-worker-io-thread_" + counter.getAndIncrement());
        //上epoll树
        epollCtlAdd0(ep_fd, wakeupFd, EPOLLIN | EPOLLET);
        //避免storeStore重排序
        OS.memory().storeFence();
        loopThread.start();
    }

    @SneakyThrows
    @Override
    public void run() {
        for (; ; ) {
            FileDescriptor polledFD = threadNoticeQueue.poll();
            if (polledFD != null) {
                epollCtlAdd0(ep_fd, polledFD.intValue(), EPOLLIN | EPOLLET);
            }
            int timeout = -1;
            //定时任务相关
            ScheduledTask task = scheduledQueue.poll();
            if (task != null) {
                timeout = task.getTimeout();
            }
            int i = epollWait0(ep_fd, events.memoryAddress(), 4096, timerFd(), timeout);
            if (i > 0) {
                for (int index = 0; index < i; index++) {
                    int event = events.events(index);
                    if ((event & (EPOLLERR | EPOLLOUT)) != 0) {
                        System.out.println("writeable");
                    }
                    if ((event & (EPOLLERR | EPOLLIN)) != 0) {
                        FileDescriptor conn_fd = new FileDescriptor(events.fd(index));
                        //处理epoll_in事件
                        EventProcessor processor = fpMapping.remove(conn_fd.intValue());
                        if (processor != null) {
                            processor.doProcess(conn_fd, this);
                        }
                    }
                }
            }
        }
    }

    public void pushFd(FileDescriptor fd, EventProcessor processor) {
        threadNoticeQueue.offer(fd);
        fpMapping.put(fd.intValue(), processor);
        if (Thread.currentThread() != loopThread) {
            //用来唤醒阻塞状态的reactor
            write2EventFd(wakeupFd);
        }
    }

    /**
     * 提交定时任务到Reactor
     */
    public void scheduled(ScheduledTask task, EventProcessor processor) {
        scheduledQueue.offer(task);
        fpMapping.put(task.getFd(), processor);
    }

    public void newAwesomeChannel() {

    }
}
