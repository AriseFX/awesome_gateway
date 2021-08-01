package com.arise.server.logging;

import com.arise.server.logging.queue.DiskQueue;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-20
 * @Description: 负责日志的ha
 * @Modified: By：
 */
public class LogService implements Runnable {

    private final Thread thread;

    private final MpscArrayQueue<ApiLog> logQueue = new MpscArrayQueue<>(1024);

    private DiskQueue queue;

    {
        try {
            queue = new DiskQueue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LogService() {
        //消费线程
        new Thread(new ConsumerService(),"log_consumer").start();;
        this.thread = new Thread(this, "log");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            ApiLog polled = logQueue.relaxedPoll();
            if (polled == null) {
                LockSupport.park();
            } else {
                queue.write(polled);
            }
        }
    }

    public void pushLog(ApiLog log) {
        logQueue.offer(log);
        LockSupport.unpark(thread);
    }

    private class ConsumerService implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                ApiLog read = queue.read();
                System.out.println(read.getRespBodyStr());
            }
        }
    }
}
