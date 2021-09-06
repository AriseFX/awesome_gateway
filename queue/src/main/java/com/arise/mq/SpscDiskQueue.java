package com.arise.mq;

import sun.misc.Contended;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;

import static com.arise.mq.IOHelper.mmap;

/**
 * @Author: wy
 * @Date: Created in 10:32 下午 2021/7/28
 * @Description: 磁盘队列{[ridx][widx][         ]}
 * @Modified: By：
 */
public class SpscDiskQueue {

    public static Map<Thread, SpscDiskQueue> map = new ConcurrentHashMap<>();

    private MappedByteBuffer buffer;

    @Contended
    private volatile int consumerOffset;

    @Contended
    private volatile int producerOffset;

    private static final AtomicIntegerFieldUpdater<SpscDiskQueue> CONSUMER_OFFSET =
            AtomicIntegerFieldUpdater.newUpdater(SpscDiskQueue.class, "consumerOffset");

    static final AtomicIntegerFieldUpdater<SpscDiskQueue> PRODUCER_OFFSET =
            AtomicIntegerFieldUpdater.newUpdater(SpscDiskQueue.class, "producerOffset");

    public SpscDiskQueue(String path) {
        try {
            buffer = mmap(path, 100 * (1 << 20));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int putAttr(byte type) {
        int i = producerOffset;
        buffer.put(type);
        buffer.putInt(0);
        PRODUCER_OFFSET.lazySet(this, i + 5);
        return i;
    }


    public void product(int head, ByteBuffer msg) {
        int i = this.producerOffset;
        int len = msg.remaining();
        int newLen = buffer.getInt(head + 1) + len;
        buffer.putInt(head + 1, newLen);
        buffer.put(msg);
        System.out.println(newLen);
        PRODUCER_OFFSET.lazySet(this, i + len);
    }

    public void consume(byte[] dst, BiConsumer<Byte, Integer> consumer) {
        byte b = buffer.get();
        int len = buffer.getInt();
        buffer.get(dst, 0, len);
        consumer.accept(b, len);
    }

    private static byte[] buf = new byte[20480];

    public static Thread consumer = new Thread(() -> {
        while (true) {
            map.forEach((k, v) -> {
                v.consume(buf, (type, len) -> {
                    if (len > 0) {
                        System.out.println(new String(buf, 0, len));
                    }
                });
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(4));
            });
        }
    }, "consumer");

    static {
        consumer.start();
    }

}
