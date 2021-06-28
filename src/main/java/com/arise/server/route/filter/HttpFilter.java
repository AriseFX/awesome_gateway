package com.arise.server.route.filter;

import io.netty.handler.codec.http.HttpRequest;

/**
 * @Author: wy
 * @Date: Created in 13:08 2021-06-05
 * @Description: 前置过滤器
 * @Modified: By：
 */
public interface HttpFilter {

    void doFilter(HttpRequest req, RequestContext ctx);
}
