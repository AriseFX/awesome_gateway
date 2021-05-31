#include "com_arise_linux_NativeSupport.h"

#include <errno.h>
#include <stddef.h>
#include <stdint.h>
#include <sys/epoll.h>
#include <sys/eventfd.h>
#include <sys/timerfd.h>
#include <unistd.h>

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_epollWait0(
    JNIEnv *env, jclass jc, jint efd, jlong address, jint len, jint timerfd,
    jint timeoutSec, jint timeoutNsec) {
  //设置定时器相关属性
  if (timeoutSec > 0 && timeoutNsec > 0) {
    // TODO 确定在C栈分配？
    struct itimerspec spec;
    spec.it_interval.tv_sec = 0;
    spec.it_interval.tv_nsec = 0;
    spec.it_value.tv_sec = timeoutSec;   // Seconds
    spec.it_value.tv_nsec = timeoutNsec; // Nanoseconds
    if (timerfd_settime(timerfd, 0, &spec, NULL) < 0) {
      return -10086;
    }
  }
  // address是java里malloc的
  struct epoll_event *ev = (struct epoll_event *)(intptr_t)address;
  int result, err;
  do {
    result = epoll_wait(efd, ev, len, timeoutSec==0?0:-1);
    if (result >= 0) {
      return result;
    }
  } while ((err = errno) == EINTR);
  return -err;
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_epollCtlAdd0(
    JNIEnv *env, jclass jc, jint efd, jint fd, jint flags) {
  uint32_t events = flags;
  struct epoll_event ev = {.events = events, .data.fd = fd};
  return epoll_ctl(efd, EPOLL_CTL_ADD, fd, &ev);
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_epollCtlModify0(
    JNIEnv *env, jclass jc, jint efd, jint fd, jint flags) {
  uint32_t events = flags;
  struct epoll_event ev = {.events = events, .data.fd = fd};
  return epoll_ctl(efd, EPOLL_CTL_MOD, fd, &ev);
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_epollCtlDel0(
    JNIEnv *env, jclass jc, jint efd, jint fd) {
  struct epoll_event ev = {0};
  return epoll_ctl(efd, EPOLL_CTL_DEL, fd, &ev);
}

JNIEXPORT jint JNICALL
Java_com_arise_linux_NativeSupport_sizeofEpollEvent(JNIEnv *env, jclass jc) {
  return sizeof(struct epoll_event);
}

JNIEXPORT jint JNICALL
Java_com_arise_linux_NativeSupport_epollCreate(JNIEnv *env, jclass jc) {
  return epoll_create1(EPOLL_CLOEXEC);
}

JNIEXPORT jint JNICALL
Java_com_arise_linux_NativeSupport_offsetofEpollData(JNIEnv *env, jclass jc) {

  return offsetof(struct epoll_event, data);
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_eventFd(JNIEnv *env,
                                                                  jclass jc) {
  //这里非阻塞很重要
  return eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);
}

JNIEXPORT jint JNICALL Java_com_arise_linux_NativeSupport_timerFd(JNIEnv *env,
                                                                  jclass jc) {
  // CLOCK_MONOTONIC 系统启动开始，经过每次时钟中断加一
  //这里非阻塞很重要
  return timerfd_create(CLOCK_MONOTONIC, TFD_CLOEXEC | TFD_NONBLOCK);
}

JNIEXPORT void JNICALL Java_com_arise_linux_NativeSupport_write2EventFd(
    JNIEnv *env, jclass jc, jint fd) {
  //考虑让上层传参？
  int64_t a = 1;
  write(fd, &a, sizeof(int64_t));
}
