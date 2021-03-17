package com.arise.server;

import io.netty.channel.unix.Buffer;

import java.nio.ByteBuffer;

import static com.arise.linux.NativeSupport.*;

/**
 * @Author: wy
 * @Date: Created in 14:54 2021/1/22
 * @Description: epoll_event数组的镜像
 * <p>
 * typedef union epoll_data
 * {
 * void *ptr;
 * int fd;
 * uint32_t u32;
 * uint64_t u64;
 * } epoll_data_t;
 * <p>
 * struct epoll_event
 * {
 * uint32_t events;
 * epoll_data_t data;
 * }
 */
public class EpollEventArray {

    private static final int EpollEventSize = sizeofEpollEvent();

    private static final int OffsetofEpollData = offsetofEpollData();

    private final long address;

    private final ByteBuffer memory;

    private final int len;

    public EpollEventArray(int len) {
        this.len = len;
        this.memory = Buffer.allocateDirectWithNativeOrder(len * EpollEventSize);
        this.address = Buffer.memoryAddress(memory);
    }

    public long memoryAddress() {
        return this.address;
    }

    public int len() {
        return this.len;
    }

    /**
     * 获取fd在内存中的位置
     * <p>
     * java中int是int32，epoll_event中data的位置放的是int32的fd，所以直接getInt()获取有符号整数
     */
    public int fd(int index) {
        return memory.getInt(index * EpollEventSize + OffsetofEpollData);
    }

    /**
     * 获取events在内存中的位置
     * <p>
     * events是一个int32的tag值
     */
    public int events(int index) {
        return memory.getInt(index * EpollEventSize);
    }

}
