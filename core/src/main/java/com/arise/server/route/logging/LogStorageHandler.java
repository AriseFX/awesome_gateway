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
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import static com.arise.base.config.IntMapConstant.*;

/**
 * @Author: wy
 * @Date: Created in 12:51 2021-06-05
 * @Description: 日志存储相关
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

    private DefaultHttpRequest request;

    private DefaultHttpResponse response;

    private boolean skip = false;

    private ApiLog apiLog;

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        //pushLog
        if (!skip) {
            ApiLog.Info info = new ApiLog.Info(request, response);
            apiLog.setInfo(info);
            IntObjectHashMap<Object> attr = ctx.channel().attr(ApiRouteHandler.Attr).get();
            URI uri = (URI) attr.get(RequestURI);
            Long timestamp = (Long) attr.get(Timestamp);
            Long writtenTimestamp = (Long) attr.get(WrittenTimestamp);
            info.setLogId((String) attr.get(TraceId));
            info.setPath(uri.getPath());
            info.setTimestamp(timestamp);
            info.setHandleTime(System.currentTimeMillis() - timestamp);
            info.setPreTime(writtenTimestamp - timestamp);
            info.setRequestParams((Map<String, String>) attr.get(HttpQueryParam));
            info.setUsername((String) attr.get(Username));
            info.setToken((String) attr.get(ShortToken));
            AweLogService.pushLog(apiLog);
        } else {
            apiLog.destructor();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        apiLog.destructor();
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }

    /**
     * response
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DefaultHttpResponse) {
            this.response = (DefaultHttpResponse) msg;
        } else if (msg instanceof DefaultHttpContent && !skip && respFilter.apply(response.headers())) {
            ByteBuf msg_buf = ((DefaultHttpContent) msg).content();
            ByteBuf body = apiLog.getBuffer();
            //复制
            memcpy(msg_buf, body);
            //增加长度
            addCount(body, 4, msg_buf.readableBytes());
        }
        super.channelRead(ctx, msg);
    }

    /**
     * request
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof DefaultHttpRequest) {
            this.apiLog = new ApiLog();
            this.request = (DefaultHttpRequest) msg;
            URI uri = (URI) ctx.channel().attr(ApiRouteHandler.Attr).get().get(RequestURI);
            if (pathFilter.apply(uri.getPath())) {
                skip = true;
                ctx.pipeline().remove(this);
                super.write(ctx, msg, promise);
                return;
            }
            //分配内存
            ByteBuf buf = ctx.alloc().directBuffer(1024);
            buf.writeInt(0);
            buf.writeInt(0);
            apiLog.setBuffer(buf);
        } else if (msg instanceof DefaultHttpContent && !skip && reqFilter.apply(request.headers())) {
            ByteBuf msg_buf = ((DefaultHttpContent) msg).content();
            ByteBuf body = apiLog.getBuffer();
            //复制
            memcpy(msg_buf, body);
            //增加长度
            addCount(body, 0, msg_buf.readableBytes());
        }
        super.write(ctx, msg, promise);
    }

    private static void memcpy(ByteBuf src, ByteBuf dist) {
        dist.writeBytes(src, 0, src.readableBytes());
    }

    private static void addCount(ByteBuf body, int index, int count) {
        int cache = body.writerIndex();
        int i = body.getInt(index);
        body.writerIndex(index);
        body.writeInt(i + count);
        body.writerIndex(cache);
    }
}
