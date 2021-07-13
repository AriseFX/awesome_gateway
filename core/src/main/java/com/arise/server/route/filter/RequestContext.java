package com.arise.server.route.filter;

import io.netty.util.concurrent.Promise;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 19:05 2021-06-28
 * @Description: 能够跳转下一个filter，能够
 * @Modified: By：
 */
public class RequestContext<P2> {

    private final Iterator<SchedulableFilter<P2>> iterator;

    private Map<String, Object> routeAttr;

    private final Promise<P2> respPromise;

    public Promise<P2> getRespPromise() {
        return this.respPromise;
    }

    public RequestContext(@Nullable Promise<P2> promise, List<SchedulableFilter<P2>> filters) {
        this.respPromise = promise;
        this.iterator = filters.iterator();
    }

    public void filter(Object req) {
        if (iterator.hasNext()) {
            iterator.next().doFilter(req, this);
        }
    }
}
