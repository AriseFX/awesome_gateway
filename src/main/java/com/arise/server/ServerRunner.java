package com.arise.server;

import com.arise.modules.http.HttpReadEventProcessor;
import io.netty.channel.epoll.Native;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.Socket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

import static com.arise.linux.NativeSupport.*;
import static io.netty.channel.epoll.Native.*;


/**
 * @Author: wy
 * @Date: Created in 18:33 2021/1/21
 * @Description: 服务端启动类
 * @Modified: By：
 */
@Slf4j
@Component
@Order(value = Ordered.LOWEST_PRECEDENCE)
public class ServerRunner implements CommandLineRunner {

    private static final byte[] acceptedAddress = new byte[26];

    private final AwesomeEventLoop[] eventLoops = new AwesomeEventLoop[]{new AwesomeEventLoop(10)};

    @Override
    public void run(String... args) throws Exception {
        //必须打印，KERNEL_VERSION加载时<clinit>会去链接动态库
        log.info("\n  >>>  \n              LINUX KERNEL VERSION: {}" +
                "\n  <<<", KERNEL_VERSION);
        AwesomeEventLoop eventLoopLB = eventLoops[0];
        int cap = 4096;
        Socket socket = Socket.newSocketStream();
        socket.bind(new InetSocketAddress(8080));
        socket.listen(128);
        //设置socket的option
        //创建epoll树
        int ep_fd = epollCreate();
        epollCtlAdd0(ep_fd, socket.intValue(), EPOLLIN | Native.EPOLLET);
        //创建epoll_event array
        EpollEventArray events = new EpollEventArray(4096);
        for (; ; ) {
            int i = epollWait0(ep_fd, events.memoryAddress(), cap, -1);
            if (i > 0) {
                for (int index = 0; index < i; index++) {
                    int event = events.events(index);
                    if ((event & (EPOLLERR | EPOLLOUT)) != 0) {
                        System.out.println("writeable");
                        continue;
                    }
                    if ((event & (EPOLLERR | EPOLLIN)) != 0) {
                        int fd = events.fd(index);
                        if (fd == socket.intValue()) {
                            for (; ; ) {
                                int connFd = socket.accept(acceptedAddress);
                                if (connFd < 0) {
                                    break;
                                }
                                eventLoopLB.pushFd(new FileDescriptor(connFd),
                                        new HttpReadEventProcessor());
                            }
                        }
                    }
                }
            }
        }
    }
}

