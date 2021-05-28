//package com.arise.internal.pool;
//
//import com.arise.modules.Bufferable;
//import com.arise.modules.http.HttpRouteChannel;
//import com.arise.modules.http.constant.StandardHttpResponse;
//import com.arise.server.AwesomeEventLoop;
//import com.arise.server.ScheduledTask;
//import io.netty.channel.unix.FileDescriptor;
//import io.netty.channel.unix.Socket;
//import lombok.extern.slf4j.Slf4j;
//import net.openhft.chronicle.core.OS;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.nio.ByteBuffer;
//
///**
// * @Author: wy
// * @Date: Created in 13:49 2021-03-18
// * @Description: 抽象了对Socket的操作
// * @Modified: By：
// */
//@Slf4j
//public class AwesomeSocketChannel {
//
//    public Socket socket;
//
//    private InetSocketAddress remote;
//
//    private AwesomeEventLoop eventLoop;
//
//    public AwesomeSocketChannel(AwesomeEventLoop eventLoop, InetSocketAddress remoteAddress) {
//        //非阻塞状态的socket文件
//        this.socket = Socket.newSocketStream();
//        this.remote = remoteAddress;
//        this.eventLoop = eventLoop;
//        OS.memory().storeFence();
//    }
//
//    public void connect(int timeout, FileDescriptor mainFd, Runnable writeHock, Runnable readHock) {
//        try {
//            boolean connected = socket.connect(remote);
//            if (connected) {
//                writeHock.run();
//            } else {
//                HttpRouteChannel channel = new HttpRouteChannel(socket, mainFd);
//                eventLoop.pushFd(channel);
//                //处理超时
//                eventLoop.scheduled(new ScheduledTask(timeout,
//                        eventLoop -> {
//                            //timeout
//                            if (!channel.isActive()) {
//                                try {
//                                    ByteBuffer cache = StandardHttpResponse.TimeoutError.cache();
//                                    mainFd.write(cache, 0, cache.remaining());
//                                    eventLoop.remove(mainFd.intValue());
//                                    mainFd.close();
//                                    eventLoop.remove(socket.intValue());
//                                    socket.close();
//                                    log.error("连接超时!");
//                                } catch (IOException e) {
//                                    System.err.println("错误！ " + e.getMessage());
//                                    e.printStackTrace();
//                                }
//                            }
//                        }));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public int write(Bufferable bufferable) {
//        //TODO 修改buffer起始结尾
//        ByteBuffer buffer = bufferable.toBuffer();
//        if (buffer != null) {
//            return write0(buffer, 0, buffer.limit());
//        }
//        return -1;
//    }
//
//    public int write0(ByteBuffer buffer, int pos, int limit) {
//        try {
//            return socket.write(buffer, pos, limit);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return -1;
//        }
//    }
//
//}
