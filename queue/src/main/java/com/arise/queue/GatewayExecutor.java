package com.arise.queue;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @Author: wy
 * @Date: Created in 2:15 下午 2021/10/19
 * @Description:
 * @Modified: By：
 */
@SuppressWarnings("all")
public class GatewayExecutor implements Executor {

    private final RingBuffer<Element<Runnable>> ringBuffer;

    public GatewayExecutor(int queueSize, int workerSize) {
        Disruptor<Element<Runnable>> disruptor = new Disruptor<>(
                Element::new,
                workerSize,
                (ThreadFactory) Thread::new,
                ProducerType.MULTI,
                new BlockingWaitStrategy());
        WorkHandler[] worker = new WorkHandler[workerSize];
        for (int i = 0; i < workerSize; i++) {
            worker[i] = new WorkHandler<Element<Runnable>>() {
                @Override
                public void onEvent(Element<Runnable> event) throws Exception {
                    event.get().run();
                }
            };
        }
        disruptor.handleEventsWithWorkerPool(worker);
        this.ringBuffer = disruptor.start();
    }


    @Override
    public void execute(Runnable command) {
        long sequence = ringBuffer.next();
        try {
            // 返回可用位置的元素
            Element<Runnable> event = ringBuffer.get(sequence);
            // 设置该位置元素的值
            event.set(command);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    static class Element<T> {
        T e;

        public void set(T e) {
            this.e = e;
        }

        public T get() {
            return this.e;
        }
    }
}
