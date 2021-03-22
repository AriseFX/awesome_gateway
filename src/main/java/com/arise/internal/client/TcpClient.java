package com.arise.internal.client;

import io.netty.channel.unix.Socket;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * @Author: wy
 * @Date: Created in 11:05 2021-03-18
 * @Description:
 * @Modified: By：
 */
public class TcpClient {

    /**
     * 连接目标服务
     */
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        try {
            Socket socket = Socket.newSocketStream();
            if (localAddress != null) {
                socket.bind(localAddress);
            }
            boolean connected = socket.connect(remoteAddress);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
