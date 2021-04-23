package com.arise.modules.http;

import com.arise.internal.chain.HandleChain;
import com.arise.modules.ReadEventProcessor;
import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.FileDescriptor;


/**
 * @Author: wy
 * @Date: Created in 16:25 2021-04-22
 * @Description:
 * @Modified: By：
 */
public class HttpReadEventProcessor implements ReadEventProcessor {
    @Override
    public void doProcess(FileDescriptor fd, AwesomeEventLoop eventLoop) {
        HandleChain chain = new HandleChain();
        chain.addHandler(new HttpV1_1_ProtocolHandler());
        chain.addHandler(new HttpV1_1_RouteHandler());
        chain.handleRead(eventLoop, fd);
    }
}
