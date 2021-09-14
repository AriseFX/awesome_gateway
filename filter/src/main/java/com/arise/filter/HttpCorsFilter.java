package com.arise.filter;

import com.arise.base.config.Headers;
import com.arise.server.route.filter.Filter;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.Lifecycle;
import com.arise.spi.Join;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.FutureListener;

import java.util.List;
import java.util.UUID;

import static com.arise.base.config.IntMapConstant.Header;
import static com.arise.base.config.IntMapConstant.TraceId;

/**
 * @Author: wy
 * @Date: Created in 16:17 2021-06-29
 * @Description: 处理跨域/生成追踪id
 * @Modified: By：
 */
@Join
public class HttpCorsFilter implements Filter {

    @Override
    public int order() {
        return 0;
    }

    @Override
    public Lifecycle lifecycle() {
        return Lifecycle.Forward;
    }

    @Override
    public void doFilter(FilterContext ctx) {
        //生成唯一追踪id
        String traceId = UUID.randomUUID().toString();
        List<HttpObject> p = (List<HttpObject>) ctx.getPram();
        HttpRequest request = (HttpRequest) (p.get(0));
        request.headers().set("x-trace-id", traceId);
        ctx.getPromise().addListener((FutureListener<Object>) future -> {
            if (future.isSuccess()) {
                List<HttpObject> object = (List<HttpObject>) future.get();
                HttpHeaders headers = ((HttpResponse) object.get(0)).headers();
                ctx.attr().put(TraceId, traceId);
                headers.set(Headers.TraceId, traceId);
                if (request.headers().get("Origin") != null) {
                    headers.set("Access-Control-Allow-Origin", "*");
                    headers.set("Access-Control-Allow-Methods", "*");
                    headers.set("Access-Control-Max-Age", "3600");
                    headers.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Connection, User-Agent, Cookie");
                    headers.set("Access-Control-Allow-Credentials", "true");
                }
                ctx.handleNext();
            }
        });

    }
}