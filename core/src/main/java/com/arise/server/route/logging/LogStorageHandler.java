package com.arise.server.route.logging;

import com.arise.base.config.GatewayConfig;
import com.arise.base.config.ServerProperties;
import com.arise.server.route.ApiRouteHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

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

    private static final GatewayConfig.Logging config = ServerProperties.gatewayConfig.getLogging();

    private static final Function<String, Boolean> pathFilter;

    private static final Function<HttpHeaders, Boolean> reqFilter;

    private static final Function<HttpHeaders, Boolean> respFilter;

    static {
        pathFilter = x -> config.getExcludePath().contains(x);

        reqFilter = x -> config.getReqHeader().entrySet()
                .stream().anyMatch(f -> {
                    String s = x.get(f.getKey());
                    if (s == null) {
                        return false;
                    }
                    return f.getValue().contains(s);
                });

        respFilter = x -> config.getRespHeader().entrySet()
                .stream().anyMatch(f -> {
                    String s = x.get(f.getKey());
                    if (s == null) {
                        return false;
                    }
                    return f.getValue().contains(s);
                });
    }

    private HttpHeaders reqHeader;

    private HttpHeaders respHeader;

    private boolean skip = false;

    private final ApiLog apiLog;

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
        } else {
            apiLog.free();
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
            DefaultHttpResponse response = (DefaultHttpResponse) msg;
            respHeader = response.headers();
            apiLog.getInfo().setResp(response);
        } else if (msg instanceof DefaultHttpContent && !skip && respFilter.apply(respHeader)) {
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
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            URI uri = (URI) ctx.channel().attr(ApiRouteHandler.Attr).get().get(RequestURI);
            if (pathFilter.apply(uri.getPath())) {
                skip = true;
                ctx.pipeline().remove(this);
                super.write(ctx, msg, promise);
                return;
            }
            reqHeader = request.headers();
            apiLog.getInfo().setReq(request);
        } else if (msg instanceof DefaultHttpContent && !skip && reqFilter.apply(reqHeader)) {
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
}
