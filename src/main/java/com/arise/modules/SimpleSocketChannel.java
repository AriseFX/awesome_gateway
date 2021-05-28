package com.arise.modules;

import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.Socket;

import java.io.IOException;

/**
 * @Author: wy
 * @Date: Created in 10:22 2021-05-28
 * @Description:
 * @Modified: By：
 */
public class SimpleSocketChannel implements Channel {

    protected Socket fd;

    protected int opFlag = 0;

    protected AwesomeEventLoop eventLoop;

    protected volatile boolean active;

    public SimpleSocketChannel(Socket fd) {
        this.active = true;
        this.fd = fd;
    }

    public void setEventLoop(AwesomeEventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public int getOpFlag() {
        return opFlag;
    }

    public boolean isActive() {
        return this.active;
    }

    public Socket getFd() {
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
