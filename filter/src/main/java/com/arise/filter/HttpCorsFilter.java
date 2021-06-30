package com.arise.filter;

import com.arise.server.route.filter.HttpObjectFilter;
import com.arise.server.route.filter.RequestContext;
import io.netty.handler.codec.http.HttpObject;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 16:17 2021-06-29
 * @Description: 处理跨域
 * @Modified: By：
 */
@Component
public class HttpCorsFilter implements HttpObjectFilter {
    @Override
    public void doFilter(List<HttpObject> req, RequestContext ctx) {
        ctx.filter(req);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
