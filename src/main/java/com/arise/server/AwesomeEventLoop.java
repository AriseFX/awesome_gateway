package com.arise.server;

import com.arise.modules.Channel;
import com.arise.modules.SimpleSocketChannel;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import lombok.SneakyThrows;
import net.openhft.chronicle.core.OS;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public static final PooledByteBufAllocator Allocator = new PooledByteBufAllocator(true);

    private static final AtomicInteger counter = new AtomicInteger();

    //唤醒Reactor专用
    private int wakeupFd;

    //Readctor本身
    private int ep_fd;

    //定时任务专用
    private int timerFd;

    private PriorityQueue<ScheduledTask> scheduledQueue;

    public Thread currentThread;

    private EpollEventArray events;

    private final IntObjectMap<SimpleSocketChannel> fpMapping = new IntObjectHashMap<>();

    //避免重复唤醒使用
    private AtomicBoolean wakeuped = new AtomicBoolean(false);

    public AwesomeEventLoop(int eventQueueSize) throws IOException {
        this.events = new EpollEventArray(4096);
        //TODO 使用pipe还是eventfd？
        this.wakeupFd = eventFd();
        this.timerFd = timerFd();
        this.ep_fd = epollCreate();
        this.scheduledQueue = new PriorityQueue<ScheduledTask>(ScheduledTask::compareTo);
        this.currentThread = new Thread(this, "awesome-gateway-worker-io-thread_" + counter.getAndIncrement());
        //上epoll树
        epollCtlAdd0(ep_fd, wakeupFd, EPOLLIN | EPOLLET);
        epollCtlAdd0(ep_fd, timerFd, EPOLLIN | EPOLLET);
        //避免storeStore重排序
        OS.memory().storeFence();
        currentThread.start();
    }

    @SneakyThrows
    @Override
    public void run() {
        for (; ; ) {
            int timeout = -1;
            //定时任务相关
            ScheduledTask sTask = scheduledQueue.peek();
            if (sTask != null) {
                timeout = sTask.getTimeout();
            }
            wakeuped.compareAndSet(true, false);
            //timeout使用timerFd对应的定时器 //TODO 秒和纳秒要处理
            System.out.println("睡眠之前打印 fpMapping: " + fpMapping.keySet());
            int i = epollWait0(ep_fd, events.memoryAddress(), 4096, timerFd, timeout, timeout);
            wakeuped.compareAndSet(false, true);
            System.out.println("-------start------");
            if (i > 0) {
                for (int index = 0; index < i; index++) {
                    int event = events.events(index);
                    int fd = events.fd(index);
                    if (((event & EPOLLRDHUP) != 0)) {
                        System.err.println("收到EPOLLRDHUP事件:" + fd);
                    }
                    if (((event & EPOLLERR) != 0)) {
                        System.err.println("收到EPOLLERR事件:" + fd);
                    }
                    if (((event & EPOLLOUT) != 0)) {
                        System.err.println("收到EPOLLOUT事件:" + fd);
                    }
                    if (((event & EPOLLIN) != 0)) {
                        System.err.println("收到EPOLLIN事件: fd:" + fd + ", wakeupFd," + (fd == wakeupFd) + ", timerFd:" + (fd == timerFd));
                    }
                    if (fd == wakeupFd) {
                        /*void*/
                    } else if (fd == timerFd) {
                        ScheduledTask pollTask = scheduledQueue.poll();
                        if (pollTask != null) {
                            sTask.getTask().accept(this);
                        }
                    } else {
                        Channel processor = fpMapping.get(fd);
                        if (processor == null) {
                            epollCtlDel0(ep_fd, fd);
                            System.err.println("processor是空！epollCtlDel0：------> " + fd);
                            continue;
                        }
                        //EPOLLIN必须先于EPOLLRDHUP处理，不然数据读不完整
                        if ((event & EPOLLERR) != 0) {
                            System.err.println("执行了onError:" + fd);
                            processor.onError();
                        }
                        if ((event & (EPOLLIN)) != 0) {
                            System.err.println("执行了onRead:" + fd);
                            processor.onRead();
                        }
                        if ((event & (EPOLLOUT)) != 0) {
                            System.err.println("执行了onWrite:" + fd);
                            processor.onWrite();
                        }
                        if (((event & EPOLLRDHUP) != 0)) {
                            System.err.println("执行了onClose:" + fd);
                            processor.onClose();
                        }
                    }
                }
            }
            System.out.println("-------end------");
        }
    }

    /**
     * 提交任务到Reactor
     *
     * @param fd
     * @param channel
     */
    public void startMonitor(SimpleSocketChannel channel) {
        System.out.println("--->");
        channel.setEventLoop(this);
        FileDescriptor fd = channel.getFd();
        System.out.println("推送事件给reactor:" + fd.intValue() + " " + channel);
        int flag = EPOLLET | EPOLLIN | EPOLLRDHUP;
        int opFlag = channel.getOpFlag();
        if (opFlag != 0) {
            flag |= opFlag;
        }
        Channel old = fpMapping.put(fd.intValue(), channel);
        if (old == null) {
            epollCtlAdd0(ep_fd, fd.intValue(), flag);
            System.out.println("epollCtlAdd:" + fd);
        }
        wakeupReactor();
        System.out.println("<---");
    }

    public boolean contains(int fd) {
        return fpMapping.containsKey(fd);
    }

    public void remove(int fd) {
        //TODO 异常处理
        System.err.println("epollCtlDel:" + fd);
        int i = epollCtlDel0(ep_fd, fd);
        fpMapping.remove(fd);
    }

    /**
     * 提交定时任务到reactor
     */
    public void scheduled(ScheduledTask task) {
        scheduledQueue.offer(task);
        wakeupReactor();
    }

    /**
     * 唤醒reactor
     */
    public void wakeupReactor() {
        if (Thread.currentThread() != currentThread && !wakeuped.get()) {
            //用来唤醒阻塞状态的Reactor
            write2EventFd(wakeupFd);
        }
    }
}
