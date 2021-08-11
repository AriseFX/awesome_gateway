package com.arise.server.logging;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: 日志存储相关
 * @Modified: By：
 */
@Slf4j
public class LogStorageHandler extends ChannelDuplexHandler {

    public static LogService logService;

    private final ApiLog apiLog;

    private boolean skip = false;

    public LogStorageHandler() {
        apiLog = new ApiLog();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        //pushLog
        if (!skip && apiLog.getInfo().getResp() != null) {
            logService.pushLog(apiLog);
        }
    }

    /**
     * response
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttpResponse) {
            if (checkAndHandle(((DefaultHttpResponse) msg).headers(), ctx.channel().pipeline())) {
                apiLog.getInfo().setResp((DefaultHttpResponse) msg);
            }
        } else if (msg instanceof DefaultHttpContent && !skip) {
            apiLog.setRespBody(((DefaultHttpContent) msg).retainedDuplicate());
        }
        super.channelRead(ctx, msg);
    }

    /**
     * request
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof DefaultHttpRequest) {
            if (checkAndHandle(((DefaultHttpRequest) msg).headers(), ctx.channel().pipeline())) {
                apiLog.getInfo().setReq((DefaultHttpRequest) msg);
            }
        } else if (msg instanceof DefaultHttpContent && !skip) {
            apiLog.setReqBody(((DefaultHttpContent) msg).retainedDuplicate());
        }
        super.write(ctx, msg, promise);
    }

    /**
     * 排除非json日志
     */
    private boolean checkAndHandle(HttpHeaders headers, ChannelPipeline pipeline) {
        String type = headers.get("Content-Type");
        if (type != null && !type.contains("application/json")) {
            skip = true;
            pipeline.remove(this);
            return false;
        }
        return true;
    }
}
