package com.arise.internal.chain;

import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.FileDescriptor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChainContext {

    public HandleChain.Node node;

    private final AwesomeEventLoop eventLoop;

    private final FileDescriptor currentFd;

    /**
     * 当前线程是否和当前eventLoop所属线程一致
     */
    public boolean inEventLoop() {
        return eventLoop.loopThread == Thread.currentThread();
    }

    public AwesomeEventLoop getEventLoop() {
        return this.eventLoop;
    }

    public FileDescriptor getCurrentFd() {
        return this.currentFd;
    }

    /**
     * 执行下个读处理器
     */
    public void fireNextReadHandler(ChainContext ctx, Object msg) {
        node = node.next;
        if (node == null || node.handler == null) {
            return;
        }
        node.handler.handleRequest(ctx, msg);
    }

    /**
     * 执行下个写处理器
     */
    public void fireNextWriteHandler(ChainContext ctx, Object msg) {
        if (node == null || node.handler == null) {
            return;
        }
        node.handler.handleResponse(ctx, msg);
        node = node.prev;
    }


}