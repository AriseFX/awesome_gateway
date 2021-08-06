package com.arise.mq;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.arise.mq.IOHelper.mmap;

/**
 * @Author: wy
 * @Date: Created in 18:03 2021-06-24
 * @Description:
 * @Modified: By：
 */
public class MappedLogFile {

    private MappedByteBuffer mappedBuffer;

    public static int logFileSize = (1 << 20);

    private int ridx;

    private AtomicInteger widx;

    private final AtomicBoolean full = new AtomicBoolean(false);

    public MappedLogFile(String path, int ridx, int widx) {
        try {
            this.ridx = ridx;
            this.widx = new AtomicInteger(widx);
            this.mappedBuffer = mmap(path, logFileSize); //日志
            this.mappedBuffer.position(widx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @return 大于等于0：正常，-1：写到文件结尾
     */
    public int write(ByteBuffer msg) {
        ByteBuffer buffer = mappedBuffer.duplicate();
        //后续加上cas
        int w = widx.get();
        int remaining = msg.remaining();
        if (remaining <= logFileSize - w) {
            buffer.position(w);
            buffer.put(msg);
            widx.lazySet(w + remaining);
            return widx.get();
        } else {
            full.compareAndSet(false, true);
            return -1;
        }
    }

    /**
     * @return 大于等于0：正常，-1：读到文件结尾，-2：读到最后写的位置，-3：消费失败
     */
    public int read(Function<ByteBuffer, Boolean> consumer) {
        if (widx.get() > ridx) {
            ByteBuffer buffer = mappedBuffer.duplicate();
            buffer.position(ridx);
            //开始读
            if (!consumer.apply(buffer)) {
                return -3;
            }
            ridx = buffer.position();
            return ridx;
        } else {
            if (full.get()) {
                return -1;
            }
            return -2;
        }
    }


    public void flush() {
        //同步刷盘
        mappedBuffer.force();
    }
}
