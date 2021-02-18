#include "com_arise_linux_NativeSupport.h"

#include <stdint.h>
#include <stddef.h>
#include <errno.h>
#include <sys/epoll.h>

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_epollWait0
        (JNIEnv *env, jclass jc, jint efd, jlong address, jint len, jint timeout) {
    //address是java里malloc的
    struct epoll_event *ev = (struct epoll_event *) (intptr_t) address;
    int result, err;
    do {
        result = epoll_wait(efd, ev, len, timeout);
        if (result >= 0) {
            return result;
        }
        //处理被信号打断
    } while ((err = errno) == EINTR);
    return -err;
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_epollCtlAdd0
        (JNIEnv *env, jclass jc, jint efd, jint fd, jint flags) {
    uint32_t events = flags;
    struct epoll_event ev = {
            .events = events,
            .data.fd = fd
    };
    return epoll_ctl(efd, EPOLL_CTL_ADD, fd, &ev);
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_sizeofEpollEvent
        (JNIEnv *env, jclass jc) {
    return sizeof(struct epoll_event);
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_epollCreate
        (JNIEnv *env, jclass jc) {
    return epoll_create1(EPOLL_CLOEXEC);
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_offsetofEpollData
        (JNIEnv *env, jclass jc) {

    return offsetof(struct epoll_event, data);
}