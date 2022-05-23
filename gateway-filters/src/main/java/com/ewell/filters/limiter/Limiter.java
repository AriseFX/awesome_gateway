package com.ewell.filters.limiter;

import com.google.common.math.LongMath;
import net.openhft.ticker.Ticker;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author wy
 * @date 2022/2/11 3:55 PM
 * @desctiption 限流器(令牌桶)
 */
public class Limiter {

    private final static Watch watch = new Watch();

    private final double stableIntervalMicros;

    private final double permitsPerSecond;

    private double storedPermits;

    private long nextFreeTicketMicros;

    /**
     * @param permitsPerSecond 每秒请求数
     */
    public Limiter(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
        //初始化值，允许刚初始化时的突发流量
        this.storedPermits = permitsPerSecond / 4;
        //补充令牌的时间间隔
        this.stableIntervalMicros = SECONDS.toMicros(1L) / permitsPerSecond;
    }

    private final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

    /**
     * 获取许可(非阻塞）
     *
     * @return true 成功
     */
    public boolean tryAcquire() {
        if (!atomicBoolean.compareAndSet(false, true)) {
            return false;
        }
        long now = watch.readMicros();
        if (now < nextFreeTicketMicros) {
            atomicBoolean.set(false);
            return false;
        }
        //根据时间差值补充令牌
        storedPermits = Math.min(permitsPerSecond,
                storedPermits + (now - nextFreeTicketMicros) / stableIntervalMicros);
        //扣除令牌
        double spend = Math.min(1, storedPermits);
        storedPermits -= spend;
        double waitPermits = 1 - spend;
        nextFreeTicketMicros = now;
        if (waitPermits > 0) {
            //令牌不够1，需要让下次请求承担这个等待时间
            nextFreeTicketMicros = LongMath.saturatedAdd(nextFreeTicketMicros, (long) (waitPermits * stableIntervalMicros));
        }
        atomicBoolean.set(false);
        return true;
    }

    /**
     * 计时器
     */
    private static class Watch {

        private final long startTime;

        /**
         * 创建计时器并立马开始计时
         */
        public Watch() {
            this.startTime = (long) Ticker.INSTANCE.toMicros(Ticker.INSTANCE.ticks());
        }

        /**
         * 查看已经跑了多久
         */
        public long readMicros() {
            return (long) (Ticker.INSTANCE.toMicros(Ticker.INSTANCE.ticks()) - startTime);
        }
    }

}
