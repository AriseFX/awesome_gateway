package com.arise.server.logging.service;

import com.arise.server.logging.ApiLog;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;

import java.util.concurrent.locks.LockSupport;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-20
 * @Description:
 * @Modified: By：
 */
public class CommitLogService implements Runnable {

    private final Thread thread;

    private final MpscArrayQueue<ApiLog> logQueue = new MpscArrayQueue<>(1024);

    private final MappedFileService mfs = new MappedFileService();

    public CommitLogService() {
        this.thread = new Thread(this, "LogStorage");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            ApiLog polled = logQueue.relaxedPoll();
            if (polled == null) {
                LockSupport.park();
            } else {
                mfs.writeLog(polled.getReq(), polled.getResp());
            }
        }
    }

    public void pushLog(ApiLog log) {
        logQueue.offer(log);
        LockSupport.unpark(thread);
    }
}
