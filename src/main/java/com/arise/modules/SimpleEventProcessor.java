package com.arise.modules;

import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.Socket;

import java.io.IOException;

/**
 * @Author: wy
 * @Date: Created in 10:22 2021-05-28
 * @Description:
 * @Modified: By：
 */
public class SimpleEventProcessor implements EventProcessor {

    protected FileDescriptor fd;

    protected AwesomeEventLoop eventLoop;

    protected boolean active = true;

    public SimpleEventProcessor(FileDescriptor fd) {
        this.fd = fd;
    }

    public void setEventLoop(AwesomeEventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public FileDescriptor getFd() {
        return this.fd;
    }

    public AwesomeEventLoop getEventLoop() {
        return this.eventLoop;
    }

    public void onRead() {

    }

    public void onWrite() {

    }

    public void onError() {
        try {
            if (active) {
                active = false;
                eventLoop.remove(fd.intValue());
                System.err.println("error no:" + new Socket(fd.intValue()).getSoError() + ",fd:" + fd.intValue());
                fd.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClose() {
        try {
            if (active) {
                active = false;
                //默认直接关闭
                eventLoop.remove(fd.intValue());
                fd.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
