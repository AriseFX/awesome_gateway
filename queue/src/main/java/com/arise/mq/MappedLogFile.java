package com.arise.mq;

import com.arise.base.config.ServerProperties;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    public static int logFileSize = ServerProperties.gatewayConfig.getLogFileSize();

    private int ridx;

    private AtomicInteger widx;

    private AtomicBoolean full;

    public MappedLogFile(String path) {
        try {
            mappedBuffer = mmap(path, logFileSize); //日志
            ridx = ridx();
            int widx = widx();
            if (ridx == 0) {
                ridx(ridx = 9);
            }
            if (widx == 0) {
                widx(widx = 9);
            }
            this.widx = new AtomicInteger(widx);
            this.full = new AtomicBoolean(isFull());
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
            int newWidx = w + remaining;
            widx.lazySet(newWidx);
            widx(newWidx);
            return newWidx;
        } else {
            full.compareAndSet(false, true);
            setFull();
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
                //消费失败
                return -3;
            }
            //设置读偏移
            ridx(ridx = buffer.position());
            return ridx;
        } else {
            if (full.get()) {
                return -1;
            }
            return -2;
        }
    }

    private int ridx() {
        return mappedBuffer.getInt(0);
    }

    private void ridx(int offset) {
        mappedBuffer.putInt(0, offset);
        mappedBuffer.force();
    }

    private int widx() {
        return mappedBuffer.getInt(4);
    }

    private void widx(int offset) {
        mappedBuffer.putInt(4, offset);
        mappedBuffer.force();
    }

    private void setFull() {
        mappedBuffer.put(8, (byte) 1);
    }

    private boolean isFull() {
        return mappedBuffer.get(8) != 0;
    }


    public void flush() {
        //同步刷盘
        mappedBuffer.force();
    }
}
