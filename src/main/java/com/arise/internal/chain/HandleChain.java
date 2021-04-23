package com.arise.internal.chain;

import com.arise.modules.ProtocolHandler;
import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.FileDescriptor;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.openhft.chronicle.core.OS;

import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 14:06 2021-03-01
 * @Description: 文件句柄处理链
 * @Modified: By：
 */
public class HandleChain {

    private Node cursor;

    private Node first;

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("[");
        Node t = first;
        while ((t = t.next) != null) {
            res.append(t.handler.toString());
            if (t.next != null) {
                res.append(",");
            } else {
                res.append("]");
            }
        }
        return res.toString();
    }

    public HandleChain() {
        cursor = new Node(null, null, null);
        first = cursor;
        OS.memory().storeFence();
    }

    public void addHandler(ProtocolHandler handler) {
        cursor = cursor.next = new Node(handler, null, cursor);
    }

    public void handleRead(AwesomeEventLoop eventLoop, FileDescriptor currentFd) {
        Node next = first.next;
        if (next != null) {
            //头节点的handler执行，生成新的执行上下文，后续执行走向由各个handler自己控制
            next.handler.handleRequest(new ChainContext(next, eventLoop, currentFd), null);
        }
    }

    @Data
    @AllArgsConstructor
    public static class Node {
        public ProtocolHandler handler;
        public Node next;
        public Node prev;

        @Override
        public String toString() {
            return handler.toString();
        }
    }
}
