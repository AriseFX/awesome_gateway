package com.arise.server.logging;

import com.arise.server.logging.service.CommitLogService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: 日志存储相关
 * @Modified: By：
 */
@Slf4j
public class LogStorageHandler extends ChannelDuplexHandler {

    private static final CommitLogService logService = new CommitLogService();

    private final List<ByteBuf>[] buf;

    public LogStorageHandler() {
        buf = new List[]{new ArrayList<ByteBuf>(1), new ArrayList<ByteBuf>(1)};
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        logService.pushLog(new ApiLog(
                buf[0].stream().map(ByteBuf::nioBuffer).toArray(ByteBuffer[]::new),
                buf[1].stream().map(ByteBuf::nioBuffer).toArray(ByteBuffer[]::new)
        ));
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
