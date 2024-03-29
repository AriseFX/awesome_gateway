package com.ewell.filters.logging;

import com.ewell.common.GatewayConfig;
import com.ewell.common.dto.ApiLog;
import com.ewell.common.message.ForwardMessage;
import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.core.filer.context.Observer;
import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.*;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static com.ewell.common.Headers.Backend;
import static com.ewell.common.IntMapConstant.*;
import static com.ewell.common.util.HttpUtils.parseQueryString;
import static com.ewell.common.util.MimeTypeUtils.parseMimeType;

/**
 * @Author: wy
 * @Date: Created in 2:34 下午 2021/12/2
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class LoggingFilter extends PreRouteFilter {

    @Inject
    private static GatewayConfig gatewayConfig;

    private final Function<String, Boolean> pathFilter;


    public LoggingFilter() {
        GatewayConfig.Logging config = gatewayConfig.getLogging();
        pathFilter = x -> config.getExcludePath().contains(x);
    }

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        List<HttpObject> fullReq = (List<HttpObject>) data;
        DefaultHttpRequest request = ((DefaultHttpRequest) fullReq.get(0));
        HttpHeaders headers = request.headers();
        IntObjectHashMap<Object> attr = ctx.getAttr();
        URI uri = (URI) attr.get(_RequestURI);
        //解析目标服务
        attr.put(_Backend, headers.get(Backend));
        attr.put(_Header, headers);
        //过滤url
        if (pathFilter.apply(uri.getPath())) {
            ctx.doNext(data);
            return;
        }
        ApiLog apiLog = new ApiLog();
        //分配内存
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);
        buf.writeInt(0);
        buf.writeInt(0);
        apiLog.setBuffer(buf);
        String contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
        if (checkMime(contentType)) {
            //请求体
            copy2Buf(fullReq, buf, 0);
        }
        //处理响应
        ctx.addRespObserver(new Observer<>(2, message -> {
            if (message instanceof ForwardMessage) {
                List<HttpObject> fullResp = message.getResponse();
                DefaultHttpResponse response = ((DefaultHttpResponse) fullResp.get(0));
                if (checkMime(response.headers().get(HttpHeaderNames.CONTENT_TYPE))) {
                    //响应体
                    copy2Buf(fullResp, buf, 4);
                }
                //构造日志
                ApiLog.Info info = new ApiLog.Info(request, response);
                apiLog.setInfo(info);
                Long timestamp = (Long) attr.get(_Timestamp);
                Long writtenTimestamp = (Long) attr.get(_WrittenTimestamp);
                info.setLogId((String) attr.get(_LogId));
                info.setPath(uri.getPath());
                info.setTimestamp(timestamp);
                info.setHandleTime(System.currentTimeMillis() - timestamp);
                info.setPreTime(writtenTimestamp - timestamp);
                info.setRequestParams(parseQueryString(uri.getQuery()));
                info.setUsername((String) attr.get(_Username));
                info.setToken((String) attr.get(_ShortToken));
                AweLogService.pushLog(apiLog);
            } else {
                apiLog.destructor();
            }
        }));
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 7;
    }

    private static void copy2Buf(List<HttpObject> src, ByteBuf dist, int lenOffset) {
        //只取消息体
        for (int i = 1; i < src.size(); i++) {
            HttpObject httpObject = src.get(i);
            ByteBuf msg_buf = ((HttpContent) httpObject).content();
            int len = msg_buf.readableBytes();
            if (len > 0) {
                //复制
                dist.writeBytes(msg_buf, 0, len);
                //增加长度
                dist.setInt(lenOffset, dist.getInt(lenOffset) + len);
            }
        }
    }

    private static boolean checkMime(String contentType) {
        String subType = parseMimeType(contentType);
        //只关注json和xml
        return "json".equalsIgnoreCase(subType) || "xml".equalsIgnoreCase(subType);
    }
}
