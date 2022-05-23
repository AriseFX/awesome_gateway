package com.ewell.queue;


import com.ewell.common.exception.SimpleRuntimeException;
import com.ewell.queue.lib.LibC;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Contended;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import static io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess.UNSAFE;
import static io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess.fieldOffset;

/**
 * @Author: wy
 * @Date: Created in 4:37 下午 2021/10/27
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class QueueBuffer {

    private final FileChannel fileChannel;

    private final MappedByteBuffer writeBuf;

    private final MappedByteBuffer readBuf;

    private final String dataPath;

    @Contended//填充缓存行
    private volatile int widx;

    @Contended//填充缓存行
    private volatile boolean isFull;

    private final static long WIDX_OFFSET = fieldOffset(QueueBuffer.class, "widx");

    private final static long ISFULL_OFFSET = fieldOffset(QueueBuffer.class, "isFull");

    private int ridx;

    public QueueBuffer(String dataPath, int size) throws IOException {
        RandomAccessFile file = new RandomAccessFile(dataPath, "rw");
        this.fileChannel = file.getChannel();
        MappedByteBuffer mapped = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        this.dataPath = dataPath;
        this.writeBuf = mapped;
        this.readBuf = (MappedByteBuffer) mapped.duplicate();
        this.isFull = mapped.get(0) == 1;
        int widx = mapped.getInt(1);
        int ridx = mapped.getInt(5);
        this.widx = Math.max(widx, 9);
        this.ridx = Math.max(ridx, 9);
        this.writeBuf.position(this.widx);
        this.readBuf.position(this.ridx);
        //预热
        final long beginTime = System.currentTimeMillis();
        long address = ((DirectBuffer) mapped).address();
        Pointer pointer = new Pointer(address);
        int ret = LibC.INSTANCE.madvise(pointer, new NativeLong(size), LibC.MADV_WILLNEED);
        log.info("当前QueueBuffer位置:{},writeOffset:{},readOffset:{}", dataPath, widx, ridx);
        log.info("madvise {} {} {} ret = {} time consuming = {}", address, dataPath, size, ret, System.currentTimeMillis() - beginTime);
    }

    public void clean() {
        try {
            clean(writeBuf);
            fileChannel.close();
            File file = new File(dataPath);
            if (file.exists()) {
                file.delete();
            }
        } catch (IOException e) {
            log.error("发生异常", e);
            throw new SimpleRuntimeException("clean queue data error");
        }
    }

    public static void clean(final MappedByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect() || buffer.capacity() == 0)
            return;
        invoke(invoke(buffer, "cleaner"), "clean");
    }

    private static Object invoke(final Object target, final String methodName, final Class<?>... args) {
        return AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try {
                Method method = method(target, methodName, args);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static Method method(Object target, String methodName, Class<?>[] args)
            throws NoSuchMethodException {
        try {
            return target.getClass().getMethod(methodName, args);
        } catch (NoSuchMethodException e) {
            return target.getClass().getDeclaredMethod(methodName, args);
        }
    }

    public int getWidx() {
        return UNSAFE.getInt(this, WIDX_OFFSET);
    }

    public void setWidx(int newValue) {
        UNSAFE.putOrderedInt(this, WIDX_OFFSET, newValue);
    }

    public void markFull() {
        readBuf.put(0, (byte) 1);
        UNSAFE.putOrderedInt(this, ISFULL_OFFSET, 1);
    }

    public boolean isFull() {
        return UNSAFE.getBoolean(this, ISFULL_OFFSET);
    }

    public boolean readable() {
        return ridx < getWidx();
    }

    public boolean writeable(int len) {
        return (writeBuf.limit() - getWidx()) > len;
    }

    public void writeInt(int x) {
        int newWidx = getWidx() + 4;
        writeBuf.putInt(x);
        setWidx(newWidx);
        writeBuf.putInt(1, newWidx);
    }

    public void writeBuf(ByteBuffer buf) {
        int newWidx = getWidx() + buf.remaining();
        writeBuf.put(buf);
        setWidx(newWidx);
        writeBuf.putInt(1, newWidx);
    }

    public void writeBufAndSize(ByteBuffer buf) {
        int len = buf.remaining();
        int newWidx = getWidx() + len + 4;
        writeBuf.putInt(len);
        writeBuf.put(buf);
        writeBuf.putInt(1, newWidx);
        setWidx(newWidx);
    }

    public int readInt() {
        int res = readBuf.getInt();
        ridx += 4;
        writeBuf.putInt(5, ridx);
        return res;
    }

    public void readBuf(byte[] dist) {
        readBuf.get(dist);
        ridx += dist.length;
        writeBuf.putInt(5, ridx);
    }

    public void force() {
        writeBuf.force();
    }
}
