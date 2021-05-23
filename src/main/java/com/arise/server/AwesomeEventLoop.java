package com.arise.server;

import com.arise.internal.pool.AwesomeSocketChannel;
import com.arise.modules.EventProcessor;
import com.arise.modules.ReadReadyProcessor;
import com.arise.modules.TimerReadyProcessor;
import com.arise.modules.WriteReadyProcessor;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.Socket;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import lombok.SneakyThrows;
import net.openhft.chronicle.core.OS;

import java.io.IOException;
import java.net.InetSocketAddress;
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

    //拒绝java的包装类型
    private final IntObjectMap<EventProcessor> fpMapping = new IntObjectHashMap<>();

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
            ScheduledTask sTask = scheduledQueue.poll();
            if (sTask != null) {
                timeout = sTask.getTimeout();
            }
            wakeuped.compareAndSet(true, false);
            //timeout使用timerFd对应的定时器 //TODO 秒和纳秒要处理
            System.out.println("fpMapping size: " + fpMapping.size());
            int i = epollWait0(ep_fd, events.memoryAddress(), 4096, timerFd, timeout, timeout);
            wakeuped.compareAndSet(false, true);
            if (i > 0) {
                for (int index = 0; index < i; index++) {
                    int event = events.events(index);
                    System.out.println("-------start------");

                    int fd = events.fd(index);
                    FileDescriptor wrapedFd = new FileDescriptor(fd);
                    //EPOLLIN必须先于EPOLLRDHUP处理，不然数据读不完整
                    System.out.println("EPOLLERR:" + ((event & EPOLLERR) != 0));
                    if ((event & (EPOLLIN)) != 0) {
                        System.out.println("EPOLLIN:" + ((event & EPOLLIN) != 0));
                        if (fd == wakeupFd) {
                            System.out.println("wakeupFd");
                            /*void*/
                        } else if (fd == timerFd && sTask != null) {
                            System.out.println("timerFd");
                            sTask.getProcess().onReady(wrapedFd, this);
                        } else {
                            System.out.println("connFd");
                            EventProcessor processor = fpMapping.get(fd);
                            if (processor == null) {
                                epollCtlDel0(ep_fd, fd);
                            }
                            if (processor instanceof ReadReadyProcessor) {
                                System.out.println("执行");
                                processor.onReady(wrapedFd, this);
                            }
                        }
                    }
                    if ((event & (EPOLLOUT | EPOLLERR)) != 0) {
                        System.out.println("EPOLLOUT");
                        EventProcessor processor = fpMapping.get(fd);
                        if (processor == null) {
                            epollCtlDel0(ep_fd, fd);
                        } else if (processor != null && processor instanceof WriteReadyProcessor) {
                            if ((event & EPOLLERR) != 0) {
                                processor.onError(wrapedFd, this);
                            } else {
                                processor.onReady(wrapedFd, this);
                            }
                        }
                    }
                    if (((event & EPOLLRDHUP) != 0)) {
                        wrapedFd.close();
                        fpMapping.remove(fd);
                        System.out.println("EPOLLRDHUP：" + fd);
                    }
                    System.out.println("-------end------");
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
    public void pushFd(int fd, EventProcessor processor) {
        int flag = EPOLLET;
        if (processor instanceof ReadReadyProcessor) {
            flag |= EPOLLIN | EPOLLRDHUP;
        } else if (processor instanceof WriteReadyProcessor) {
            flag |= EPOLLOUT | EPOLLRDHUP;
        } else if (processor instanceof TimerReadyProcessor) {
            flag |= EPOLLIN;
        }
        EventProcessor old = fpMapping.put(fd, processor);
        if (old == null) {
            epollCtlAdd0(ep_fd, fd, flag);
        } else if (old == processor) {
            //忽略重复add的情况
        } else {
            epollCtlModify0(ep_fd, fd, flag);
        }
        wakeupReactor();
    }

    public boolean contains(int fd) {
        return fpMapping.containsKey(fd);
    }

    public void remove(int fd) {
        //TODO 异常处理
        epollCtlDel0(ep_fd, fd);
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
     * 创建socket channel
     */
    public AwesomeSocketChannel newAwesomeChannel(InetSocketAddress remote) {
        return new AwesomeSocketChannel(this, remote);
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
