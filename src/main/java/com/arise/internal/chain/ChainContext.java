package com.arise.internal.chain;

import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.FileDescriptor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChainContext {

    public HandleChain.Node node;

    private final AwesomeEventLoop eventLoop;

    private final FileDescriptor currentFd;


    public AwesomeEventLoop getEventLoop() {
        return this.eventLoop;
    }

    /**
     * 当前上下文的文件描述符
     */
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

}