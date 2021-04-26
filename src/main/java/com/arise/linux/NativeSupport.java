package com.arise.linux;

/**
 * @Author: wy
 * @Date: Created in 12:06 2020/12/28
 * @Description: 包装了一些系统函数
 * @Modified: By：
 */
public class NativeSupport {

    static {
        //默认打包路径
        System.load("/mnt/d/work/project/OpenHft/awesome_gateway/docker/com_arise_linux_NativeSupport.so");
    }

    /**
     * @param address ep_event（）
     */
    public static native int epollWait0(int efd, long address, int len, int timeout);

    public static native int epollCtlAdd0(int efd, int fd, int flags);

    public static native int sizeofEpollEvent();

    public static native int epollCreate();

    /**
     * 获取data在epoll_event struct中的偏移量
     */
    public static native int offsetofEpollData();

    /**
     * 线程亲缘
     */
    public static native int taskset();

}
