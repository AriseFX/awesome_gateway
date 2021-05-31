package com.arise.server;

import java.util.function.Consumer;

/**
 * @Author: wy
 * @Date: Created in 18:12 2021-04-26
 * @Description: 调度任务
 * @Modified: By：
 */
public class ScheduledTask implements Comparable<ScheduledTask> {

    private final long timestamp;

    private final Consumer<AwesomeEventLoop> task;

    public ScheduledTask(int timeout, Consumer<AwesomeEventLoop> task) {
        this.timestamp = System.currentTimeMillis() + timeout * 1000L;
        //超时(秒)
        this.task = task;
    }

    public int getTimeout() {
        return (int) ((timestamp - System.currentTimeMillis()) / 1000);
    }

    public Consumer<AwesomeEventLoop> getTask() {
        return this.task;
    }

    @Override
    public int compareTo(ScheduledTask o) {
        return (int) (timestamp - o.timestamp);
    }
}
