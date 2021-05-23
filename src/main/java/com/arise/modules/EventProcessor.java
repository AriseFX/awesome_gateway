package com.arise.modules;

import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.Socket;

import java.io.IOException;


/**
 * @Author: wy
 * @Date: Created in 18:45 2021-04-15
 * @Description:
 * @Modified: By：
 */
@FunctionalInterface
public interface EventProcessor {

    void onReady(FileDescriptor callback_fd, AwesomeEventLoop callback_eventLoop) throws IOException;

    default void onError(FileDescriptor callback_fd, AwesomeEventLoop callback_eventLoop) {
        try {
            System.err.println("error no:" + new Socket(callback_fd.intValue()).getSoError() +",fd:" + callback_fd.intValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
