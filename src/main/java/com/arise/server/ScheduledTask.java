package com.arise.server;

import com.arise.modules.EventProcessor;
import com.arise.modules.TimerReadyProcessor;

/**
 * @Author: wy
 * @Date: Created in 18:12 2021-04-26
 * @Description: 调度任务
 * @Modified: By：
 */
public class ScheduledTask implements Comparable<ScheduledTask> {

    //超时(秒)
    private final int timeout;

    private final TimerReadyProcessor processor;

    public ScheduledTask(int timeout, TimerReadyProcessor processor) {
        this.timeout = timeout;
        this.processor = processor;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public EventProcessor getProcess() {
        return this.processor;
    }

    @Override
    public int compareTo(ScheduledTask o) {
        return timeout - o.getTimeout();
    }
}
