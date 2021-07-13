package com.arise.filter;

import com.arise.server.route.ReqRespFilter;
import com.arise.server.route.filter.RequestContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.FutureListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 16:17 2021-06-29
 * @Description: 处理跨域
 * @Modified: By：
 */
@Component
public class HttpCorsFilter extends ReqRespFilter {

    @Override
    public void doFilter(Object pram, RequestContext<List<HttpObject>> ctx) {
        List<HttpObject> req = (List<HttpObject>) pram;
        HttpRequest request = (HttpRequest) req.get(0);
        if (request.headers().get("Origin") != null) {
            ctx.getRespPromise().addListener((FutureListener<List<HttpObject>>) future -> {
                if (future.isSuccess()) {
                    List<HttpObject> object = future.get();
                    HttpResponse response = (HttpResponse) object.get(0);
                    HttpHeaders headers = response.headers();
                    if (!headers.contains("Access-Control-Allow-Origin")) {
                        headers.set("Access-Control-Allow-Origin", "*");
                        headers.set("Access-Control-Allow-Methods", "*");
                        headers.set("Access-Control-Max-Age", "3600");
                        headers.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Connection, User-Agent, Cookie");
                        headers.set("Access-Control-Allow-Credentials", "true");
                    }
                }
            });
        }
        ctx.filter(req);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}