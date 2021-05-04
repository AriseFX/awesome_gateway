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
     * 使用timerFd来控制timeout（高精度定时器）
     *
     * @param address    ep_event（）
     * @param timeoutSec 秒
     * @param timeoutSec 纳秒
     */
    public static native int epollWait0(int efd, long address, int len, int timerFd, int timeoutSec, int timeoutNsec);

    public static native int epollCtlAdd0(int efd, int fd, int flags);

    public static native int sizeofEpollEvent();

    public static native int epollCreate();

    //TODO
    public static native int eventFd();

    //TODO
    public static native int timerFd();

    //TODO
    public static native void write2EventFd(int eventFd);

    /**
     * 获取data在epoll_event struct中的偏移量
     */
    public static native int offsetofEpollData();

    /**
     * 线程亲缘
     */
    public static native int taskset();

}
