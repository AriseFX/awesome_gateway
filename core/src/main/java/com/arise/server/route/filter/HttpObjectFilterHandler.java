package com.arise.server.route.filter;


import io.netty.handler.codec.http.HttpObject;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 16:34 2021-06-29
 * @Description: 用来修改请求响应
 * @Modified: By：
 */
public class HttpObjectFilterHandler {

    private final RequestContext ctx;

    /**
     * 依赖Springboot初始化
     */
    public static List<HttpObjectFilter> sortedFilter;

    public HttpObjectFilterHandler(RequestContext ctx) {
        ctx.setIterator(sortedFilter.iterator());
        this.ctx = ctx;
    }

    public void handle(List<HttpObject> req) {
        ctx.filter(req);
    }
}
