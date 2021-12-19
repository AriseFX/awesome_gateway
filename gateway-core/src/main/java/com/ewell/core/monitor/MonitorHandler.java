package com.ewell.core.monitor;

import com.ewell.common.util.ScheduledPool;
import io.netty.channel.ChannelHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import lombok.Getter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @Author: wy
 * @Date: Created in 2:42 下午 2021/11/19
 * @Description: 网络吞吐量监控
 * @Modified: By：
 */
@ChannelHandler.Sharable
public class MonitorHandler extends GlobalTrafficShapingHandler {

    public static MonitorHandler INSTANCE = new MonitorHandler(ScheduledPool.EXECUTOR, 10000);

    public static AtomicLong apiCounter = new AtomicLong();

    @Getter
    private double apiTps = 0.0d;

    private long lastCollectTimestamp;

    @Getter
    private long write;

    @Getter
    private long read;

    public MonitorHandler(ScheduledExecutorService executor, long checkInterval) {
        super(executor, checkInterval);
        lastCollectTimestamp = System.currentTimeMillis();
        ScheduledPool.EXECUTOR.scheduleAtFixedRate(() -> {
            long count = apiCounter.getAndSet(0);
            long timestamp = System.currentTimeMillis();
            double temp = (double) ((timestamp - lastCollectTimestamp) / 1000);
            if (temp == 0) {
                this.apiTps = 0.0d;
            } else {
                this.apiTps = count / temp;
            }
            this.lastCollectTimestamp = timestamp;
        }, 0, 10, SECONDS);
    }

    @Override
    protected void doAccounting(TrafficCounter counter) {
        this.write = counter.lastWriteThroughput();
        this.read = counter.lastReadThroughput();
    }
}
