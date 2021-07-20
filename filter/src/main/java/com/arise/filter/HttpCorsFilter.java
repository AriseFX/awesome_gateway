package com.arise.filter;

import com.arise.server.route.filter.ForwardFilter;
import com.arise.server.route.filter.FilterContext;
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
public class HttpCorsFilter extends ForwardFilter {

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void doFilter(FilterContext<List<HttpObject>, List<HttpObject>> ctx) {
        HttpRequest request = (HttpRequest) (ctx.getPram().get(0));
        if (request.headers().get("Origin") != null) {
            ctx.getPromise().addListener((FutureListener<List<HttpObject>>) future -> {
                if (future.isSuccess()) {
                    List<HttpObject> object = future.get();
                    HttpResponse response = (HttpResponse) object.get(0);
                    HttpHeaders headers = response.headers();
                    headers.set("Access-Control-Allow-Origin", "*");
                    headers.set("Access-Control-Allow-Methods", "*");
                    headers.set("Access-Control-Max-Age", "3600");
                    headers.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Connection, User-Agent, Cookie");
                    headers.set("Access-Control-Allow-Credentials", "true");
                    ctx.handleNext();
                }
            });
        }
    }
}