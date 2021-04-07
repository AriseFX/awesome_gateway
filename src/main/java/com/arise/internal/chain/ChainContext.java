package com.arise.internal.chain;

import lombok.AllArgsConstructor;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class ChainContext {

    public HandleChain.Node node;

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