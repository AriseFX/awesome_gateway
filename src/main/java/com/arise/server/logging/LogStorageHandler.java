package com.arise.server.logging;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: 日志存储相关
 * @Modified: By：
 */
@Slf4j
public class LogStorageHandler extends ChannelDuplexHandler {

    private static MappedByteBuffer mapBuffer;

    private static final AtomicLong counter = new AtomicLong(0);

    static {
        try {
            mapBuffer = new RandomAccessFile("./test.txt", "rw")
                    .getChannel()
                    .map(FileChannel.MapMode.READ_WRITE, 0, 1 << 20);
            //加载序号
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final List<ByteBuf>[] buf;

    public LogStorageHandler() {
        buf = new List[]{new ArrayList<ByteBuf>(1), new ArrayList<ByteBuf>(1)};
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        buf[0].forEach(e -> {
            mapBuffer.put(e.nioBuffer());
        });
        buf[1].forEach(e -> mapBuffer.put(e.nioBuffer()));
        mapBuffer.force();
//        System.out.println("handlerRemoved");
    }

    /**
     * response
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            buf[1].add(((ByteBuf) msg).retainedDuplicate());
        } else {
            System.out.println(1);
        }
        super.channelRead(ctx, msg);
    }

    /**
     * request
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            buf[0].add(((ByteBuf) msg).retainedDuplicate());
        } else {
            System.out.println(1);
        }
        super.write(ctx, msg, promise);
    }
}
