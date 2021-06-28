package com.arise.os;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: wy
 * @Date: Created in 14:04 2020/12/27
 * @Description: 基于多路复用的timer
 * @Modified: By：
 */
public class NioScheduledThreadPoolExecutor implements Executor {

    private final TimeHeap timeHeap;

    private final int corePoolSize;

    private final AtomicInteger count = new AtomicInteger(0);

    private final Worker[] workers;

    public NioScheduledThreadPoolExecutor(int corePoolSize, int taskSize) {
        this.corePoolSize = corePoolSize;
        this.timeHeap = new TimeHeap(taskSize);
        workers = new Worker[corePoolSize];
    }

//    private static final int EpollEventSize = NativeSupport.sizeofEpollEvent();

    /**
     * struct epoll_event数组起始偏移量
     */
//    private static final long address;

    static {
        //TODO 上来就是1024个event,后续优化
//        ByteBuffer epollEvents = Buffer.allocateDirectWithNativeOrder(1024 * EpollEventSize);
//        address = PlatformDependent.directBufferAddress(epollEvents);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        schedule(command, 0, TimeUnit.SECONDS);
    }

    public void schedule(Runnable command,
                         long delay,
                         TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(delay, unit, command, timeHeap);
        timeHeap.put(task);
        if (count.get() < corePoolSize) {
            int num = count.getAndIncrement();
            Worker worker = new Worker(timeHeap, num);
            workers[num] = worker;
            worker.start();
        }
    }

    private static long triggerTime(long delay, TimeUnit unit) {
        return triggerTime(unit.toNanos((delay < 0) ? 0 : delay));
    }

    private static long triggerTime(long delay) {
        return System.nanoTime() + delay;
    }

    @Override
    public String toString() {
        return "no toString";
    }

    /**
     * 任务
     */
    static class ScheduledTask {
        //执行时间
        private long tp;
        private final long delay;
        private final TimeUnit unit;
        //回调方法
        private final Runnable command;

        private final TimeHeap timeHeap;

        public ScheduledTask(long delay, TimeUnit unit, Runnable runnable, TimeHeap timeHeap) {
            this.tp = triggerTime(delay, unit);
            this.delay = delay;
            this.unit = unit;
            this.command = runnable;
            this.timeHeap = timeHeap;
        }

        public long getTp() {
            return this.tp;
        }

        public void callback() {
            this.command.run();
            this.tp = triggerTime(delay, unit);
            timeHeap.put(this);
        }

    }

    /**
     * 时间堆
     */
    static class TimeHeap {

        private final Object condition = new Object();

        private volatile Thread leader = null;

        private final ArrayList<ScheduledTask> data;

        public TimeHeap(int cap) {
            data = new ArrayList<>(cap);
        }

        public void put(ScheduledTask task) {
            synchronized (condition) {
                data.add(task);
                siftUp(data.size() - 1);
                condition.notifyAll();
            }
        }

        /**
         * 非阻塞
         */
        public ScheduledTask take() {
            if (data.size() == 0) {
                return null;
            }
            swap(0, data.size() - 1);
            ScheduledTask remove = data.remove(data.size() - 1);
            siftDown(0);
            return remove;
        }

        /**
         * 瞄一眼堆顶的数据
         */
        public ScheduledTask peek() {
            if (data.size() == 0) {
                return null;
            }
            return data.get(0);
        }

        /**
         * 阻塞的方式拿堆顶数据
         */
        public ScheduledTask getTask() throws InterruptedException {
            synchronized (condition) {
                try {
                    while (true) {
                        if (leader != null) {
                            condition.wait();
                        } else {
                            leader = Thread.currentThread();
                            ScheduledTask task;
                            while (true) {
                                long interval;
                                while ((task = peek()) == null) {
                                    condition.wait();
                                }
                                if ((interval = (task.getTp() - System.nanoTime())) <= 0) {
                                    leader = null;
                                    return take();
                                } else {
                                    //TODO 换wait为epoll_wait,使用time_fd
                                    condition.wait(interval <= 1000000 ? 1 : (interval / 1000000) + 1);
                                }
                            }
                        }
                    }
                } finally {
                    condition.notifyAll();
                }
            }
        }

        /**
         * 指定位置元素下沉
         * //TODO 优化逻辑
         *
         * @param idx 索引
         */
        private void siftDown(int idx) {
            while (leftChild(idx) < data.size()) {
                ScheduledTask idx_task = data.get(idx);
                int left_idx = leftChild(idx);
                ScheduledTask l_task = data.get(left_idx);
                if (left_idx + 1 >= data.size()) {
                    if (l_task.tp < data.get(idx).tp) {
                        swap(left_idx, idx);
                    }
                    break;
                }
                ScheduledTask r_task = data.get(left_idx + 1);
                if (l_task.tp < r_task.tp) {
                    if (idx_task.tp > l_task.tp) {
                        swap(left_idx, idx);
                        idx = left_idx;
                    } else {
                        break;
                    }
                } else {
                    if (idx_task.tp > r_task.tp) {
                        swap(left_idx + 1, idx);
                        idx = left_idx + 1;
                    } else {
                        break;
                    }
                }
            }
        }

        /**
         * 指定位置元素上浮
         *
         * @param idx 索引
         */
        private void siftUp(int idx) {
            int p_idx;
            while ((p_idx = parent(idx)) >= 0) {
                if (data.get(p_idx).tp > data.get(idx).tp) {
                    swap(p_idx, idx);
                    idx = p_idx;
                } else {
                    break;
                }
            }
        }

        private void swap(int i, int j) {
            if (i == j) {
                return;
            }
            if (i > data.size() || j > data.size()) {
                throw new IllegalArgumentException();
            }
            ScheduledTask t = data.get(i);
            data.set(i, data.get(j));
            data.set(j, t);
        }

        public int parent(int i) {
            return (i - 1) >> 1;
        }

        public int leftChild(int i) {
            return (i << 1) + 1;
        }

        public int rightChild(int i) {
            return (i << 1) + 2;
        }

        public int size() {
            return data.size();
        }

    }
}
