package com.arise.server.route.logging;

import com.arise.server.route.ApiRouteHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;

import static com.arise.base.config.Constant.*;

/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: 日志存储相关
 * <p>
 * 1.生成日志id
 * 2.记录请求，都序列化，然后进行
 * 3.记录
 * @Modified: By：
 */
@Slf4j
public class LogStorageHandler extends ChannelDuplexHandler {

    private final ApiLog apiLog;

    private boolean skip = false;

    public LogStorageHandler() {
        this.apiLog = new ApiLog();
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        ApiLog.Info info = apiLog.getInfo();
        //pushLog
        if (!skip && info.getResp() != null) {
            Map<String, Object> attr = ctx.channel().attr(ApiRouteHandler.Attr).get();
            URI uri = (URI) attr.get(RequestURI);
            Long timestamp = (Long) attr.get(Timestamp);
            Long writtenTimestamp = (Long) attr.get(WrittenTimestamp);
            info.setLogId((String) attr.get("x-trace-id"));
            info.setPath(uri.getPath());
            info.setTimestamp(timestamp);
            info.setHandleTime(System.currentTimeMillis() - timestamp);
            info.setPreTime(writtenTimestamp - timestamp);
            info.setQueryPram((Map<String, String>) attr.get("httpQueryParam"));
            info.setUsername((String) attr.get(Username));
            AweLogService.pushLog(apiLog);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        apiLog.free();
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
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
            ByteBuf msg_buf = ((DefaultHttpContent) msg).content().duplicate();
            ByteBuf buf = apiLog.getBody_resp();
            if (buf == null) {
                apiLog.setBody_resp(buf = ctx.alloc().buffer());
            }
            if (msg_buf.readableBytes() > 0) {
                buf.writeBytes(msg_buf);
            }
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
            ByteBuf msg_buf = ((DefaultHttpContent) msg).content().duplicate();
            ByteBuf buf = apiLog.getBody_req();
            if (buf == null) {
                apiLog.setBody_req(buf = ctx.alloc().buffer());
            }
            if (msg_buf.readableBytes() > 0) {
                buf.writeBytes(msg_buf);
            }
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
