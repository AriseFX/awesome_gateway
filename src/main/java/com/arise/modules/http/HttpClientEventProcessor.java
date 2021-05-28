package com.arise.modules.http;

import com.arise.modules.SimpleEventProcessor;
import com.arise.modules.http.constant.StandardHttpResponse;
import io.netty.channel.unix.FileDescriptor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 10:45 2021-05-28
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class HttpClientEventProcessor extends SimpleEventProcessor {

    private FileDescriptor mainFd;

    private Runnable writeHock;

    private Runnable readHock;

    public HttpClientEventProcessor(FileDescriptor fd, FileDescriptor mainFd, Runnable writeHock, Runnable readHock) {
        super(fd);
        this.mainFd = mainFd;
        this.writeHock = writeHock;
        this.readHock = readHock;
    }

    @Override
    public void onRead() {
        if (active) {
            readHock.run();
        }
    }

    @Override
    public void onWrite() {
        if (active) {
            writeHock.run();
        }
    }

    @Override
    public void onError() {
        try {
            super.onError();
            eventLoop.remove(mainFd.intValue());
            ByteBuffer cache = StandardHttpResponse.ServerError.cache();
            mainFd.write(cache, 0, cache.remaining());
            mainFd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
