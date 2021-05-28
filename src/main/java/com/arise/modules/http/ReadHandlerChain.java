package com.arise.modules.http;

import com.arise.internal.chain.HandleChain;
import com.arise.modules.SimpleSocketChannel;
import io.netty.channel.unix.Socket;


/**
 * @Author: wy
 * @Date: Created in 16:25 2021-04-22
 * @Description:
 * @Modified: By：
 */
public class ReadHandlerChain extends SimpleSocketChannel {

    public ReadHandlerChain(Socket fd) {
        super(fd);
    }

    @Override
    public void onRead() {
        HandleChain chain = new HandleChain();
        chain.addHandler(new HttpV1_1_ProtocolHandler());
        chain.addHandler(new HttpV1_1_RouteHandler());
        chain.handleRead(super.getEventLoop(), super.fd);
    }
}
