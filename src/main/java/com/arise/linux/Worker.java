package com.arise.linux;

/**
 * @Author: wy
 * @Date: Created in 16:09 2020/12/29
 * @Description: 这是一个拉任务的工作线程
 * @Modified: By：
 */
public class Worker implements Runnable {

    private final Thread workerThread;

    private final NioScheduledThreadPoolExecutor.TimeHeap timeHeap;

    public Worker(NioScheduledThreadPoolExecutor.TimeHeap timeHeap, int num) {
        this.timeHeap = timeHeap;
        this.workerThread = new Thread(this);
        workerThread.setName("sched-worker-thread-" + num);
    }

    public void start() {
        workerThread.start();
    }

    @Override
    public void run() {
        while (true) {
            NioScheduledThreadPoolExecutor.ScheduledTask task = null;
            try {
                task = timeHeap.getTask();
                task.callback();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //TODO 暂时不做超时处理
        }
    }
}
