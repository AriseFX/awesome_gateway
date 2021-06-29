package com.arise.os;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * @Author: wy
 * @Date: Created in 12:06 2020/12/28
 * @Description: 包装了一些系统函数
 * @Modified: By：
 */
public class NativeSupport {

    static {
        //默认打包路径
        try {
            Enumeration<URL> dir = Thread.currentThread().getContextClassLoader().getResources("jni");
            while (dir.hasMoreElements()) {
                URL url = dir.nextElement();
                File file = new File(url.getPath());
                File[] files = file.listFiles();
                if (files == null) {
                    System.err.println("libNativeSupport.so不存在！");
                } else {
                    Arrays.stream(files).forEach(lib -> {
                        if (lib.getName().endsWith("so")) {
                            System.load(lib.getPath());
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static native int epollCtlModify0(int efd, int fd, int flags);

    public static native int epollCtlDel0(int efd, int fd);

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
