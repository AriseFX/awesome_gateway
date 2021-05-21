package com.arise.modules.http;

import com.arise.internal.chain.ChainContext;
import com.arise.internal.pool.AwesomeSocketChannel;
import com.arise.modules.ProtocolHandler;
import com.arise.modules.ReadReadyProcessor;
import com.arise.server.AwesomeEventLoop;
import io.netty.channel.unix.FileDescriptor;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import static io.netty.channel.epoll.Native.splice;
import static io.netty.channel.unix.FileDescriptor.pipe;

/**
 * @Author: wy
 * @Date: Created in 9:29 2021-04-07
 * @Description: 处理路由的逻辑，单线程
 * @Modified: By：
 */
@Slf4j
public class HttpV1_1_RouteHandler implements ProtocolHandler {

    @Override
    public void handleRequest(ChainContext ctx, Object msg) {
        HttpServerRequest request = (HttpServerRequest) msg;
        FileDescriptor currentFd = ctx.getCurrentFd();
        AwesomeEventLoop eventLoop = ctx.getEventLoop();

        AwesomeSocketChannel channel = eventLoop.newAwesomeChannel(
                new InetSocketAddress("localhost", 8081));
        //TODO 连接复用
        //连接成功后执行write
        channel.connect(3,
                () -> {
                    try {
                        currentFd.close();
                        eventLoop.remove(currentFd.intValue());
                    } catch (IOException e) {
                        System.err.println("错误！ " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                () -> {
                    try {
                        //先写不完整的http
                        channel.write(request);
                        //body
                        int contentLength = request.contentLength;
                        if (contentLength > 0) {
                            //创建pipe用于socket splice
                            FileDescriptor[] reqPipe = pipe();
                            splice(currentFd.intValue(), -1, reqPipe[1].intValue(), -1, contentLength);
                            splice(reqPipe[0].intValue(), -1, channel.socket.intValue(), -1, contentLength);
                        }
                        eventLoop.pushFd(channel.socket.intValue(),
                                (ReadReadyProcessor) (i_callback_fd, i_callback_ep) -> {
                                    if (eventLoop.contains(currentFd.intValue())) {
                                        FileDescriptor[] respPipe = pipe();
                                        //转发给客户端
                                        //TODO 连接复用的情况下len如何考虑？
                                        int toPipe = splice(i_callback_fd.intValue(), -1, respPipe[1].intValue(), -1, 0x7fffffff);
                                        int toSocket = splice(respPipe[0].intValue(), -1, currentFd.intValue(), -1, 0x7fffffff);
                                        log.info("toPipe:{},toSocket:{}", toPipe, toSocket);
                                    }
                                });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private static Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long addressOf(Object o) {
        Object[] array = new Object[]{o};
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        //arrayBaseOffset方法是一个本地方法，可以获取数组第一个元素的偏移地址
        int addressSize = unsafe.addressSize();
        long objectAddress;
        switch (addressSize) {
            case 4:
                objectAddress = unsafe.getInt(array, baseOffset);
                //getInt方法获取对象中offset偏移地址对应的int型field的值
                break;
            case 8:
                objectAddress = unsafe.getLong(array, baseOffset);
                //getLong方法获取对象中offset偏移地址对应的long型field的值
                break;
            default:
                throw new Error("unsupported address size: " + addressSize);
        }
        return (objectAddress);
    }
}
