package com.arise.internal.pool;

import io.netty.channel.unix.Socket;
import net.openhft.chronicle.core.OS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 13:49 2021-03-18
 * @Description:
 * @Modified: By：
 */
public abstract class AbstractSocketChannel implements SocketChannel {

    private Socket socket;

    private InetSocketAddress remote;

    private InetSocketAddress local;

    private boolean active;

    protected AbstractSocketChannel(InetSocketAddress remoteAddress, InetSocketAddress localAddress) {
        this.socket = Socket.newSocketStream();
        this.remote = remoteAddress;
        this.local = localAddress;
        OS.memory().storeFence();
    }

    public void connect() {
        try {
            if (this.local != null) {
                socket.bind(local);
            }
            boolean connected = socket.connect(remote);
            if (connected) {
                active = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write0(ByteBuffer buffer, int pos, int limit) {
        try {
            socket.write(buffer, pos, limit);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
