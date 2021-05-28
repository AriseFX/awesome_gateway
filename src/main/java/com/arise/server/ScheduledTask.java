package com.arise.server;

import java.util.function.Consumer;

/**
 * @Author: wy
 * @Date: Created in 18:12 2021-04-26
 * @Description: 调度任务
 * @Modified: By：
 */
public class ScheduledTask implements Comparable<ScheduledTask> {

    //超时(秒)
    private final int timeout;

    private final Consumer<AwesomeEventLoop> task;

    public ScheduledTask(int timeout, Consumer<AwesomeEventLoop> task) {
        this.timeout = timeout;
        this.task = task;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public Consumer<AwesomeEventLoop> getTask() {
        return this.task;
    }

    @Override
    public int compareTo(ScheduledTask o) {
        return timeout - o.getTimeout();
    }
}
