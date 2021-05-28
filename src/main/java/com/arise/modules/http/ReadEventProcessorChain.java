package com.arise.modules.http;

import com.arise.internal.chain.HandleChain;
import com.arise.modules.SimpleEventProcessor;
import io.netty.channel.unix.FileDescriptor;


/**
 * @Author: wy
 * @Date: Created in 16:25 2021-04-22
 * @Description:
 * @Modified: By：
 */
public class ReadEventProcessorChain extends SimpleEventProcessor {

    public ReadEventProcessorChain(FileDescriptor fd) {
        super(fd);
    }

    @Override
    public void onRead() {
        HandleChain chain = new HandleChain();
        chain.addHandler(new HttpV1_1_ProtocolHandler());
        chain.addHandler(new HttpV1_1_RouteHandler());
        chain.handleRead(super.getEventLoop(), super.getFd());
    }
}
