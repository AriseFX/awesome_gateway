package com.arise.server;

import io.netty.channel.unix.FileDescriptor;

/**
 * @Author: wy
 * @Date: Created in 9:50 2021-02-04
 * @Description:
 * @Modified: By：
 */
public class FdEvent {

    private FileDescriptor fd;

    public FdEvent(FileDescriptor fd) {
        this.fd = fd;
    }

    public FileDescriptor getFd() {
        return fd;
    }
}
