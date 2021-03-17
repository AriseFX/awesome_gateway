package com.arise.modules.chain;

import lombok.AllArgsConstructor;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class ChainContext {

    private HandleChain.Node node;

    /**
     * 执行下个读处理器
     */
    public void fireNextReadHandler(ChainContext ctx, ByteBuffer buffer) {
        node = node.next;
        if (node == null || node.handler == null) {
            return;
        }
        node.handler.handleRequest(ctx, buffer);
    }

    /**
     * 执行下个写处理器
     */
    public void fireNextWriteHandler(ChainContext ctx, ByteBuffer buffer) {
        if (node == null || node.handler == null) {
            return;
        }
        node.handler.handleResponse(ctx, buffer);
        node = node.prev;
    }


}