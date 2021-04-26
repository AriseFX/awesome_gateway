package com.arise.internal.pool;

import com.arise.modules.Bufferable;
import io.netty.channel.unix.Socket;
import lombok.SneakyThrows;
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
public class AwesomeSocketChannel {

    public Socket socket;

    private InetSocketAddress remote;

    private boolean active;

    public AwesomeSocketChannel(InetSocketAddress remoteAddress) {
        //非阻塞状态的socket文件
        this.socket = Socket.newSocketStream();
        this.remote = remoteAddress;
        OS.memory().storeFence();
    }

    @SneakyThrows
    public void connect() {
        try {
            boolean connected = socket.connect(remote);
            //TODO 发起一个epoll_out事件去监听连接成功事件
            //暂时先直接sleep
            Thread.sleep(100);
            if (connected) {
                active = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(Bufferable bufferable) {
        //TODO 修改buffer起始结尾
        ByteBuffer buffer = bufferable.toBuffer();
        if (buffer != null) {
            buffer.flip();
            write0(buffer, 0, buffer.limit());
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
