package com.arise.server.route.filter;


/**
 * @Author: wy
 * @Date: Created in 16:34 2021-06-29
 * @Description:
 * @Modified: By：
 */
public class FilterHandler<P2> {

    private final RequestContext<P2> ctx;

    public FilterHandler(RequestContext<P2> ctx) {
        this.ctx = ctx;
    }

    public   void handle(Object req) {
        ctx.filter(req);
    }
}
