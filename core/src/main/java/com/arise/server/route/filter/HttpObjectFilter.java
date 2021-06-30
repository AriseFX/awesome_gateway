package com.arise.server.route.filter;

import io.netty.handler.codec.http.HttpObject;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 13:08 2021-06-05
 * @Description: http过滤器
 * @Modified: By：
 */
public interface HttpObjectFilter extends Ordered {

    void doFilter(List<HttpObject> req, RequestContext ctx);
}
