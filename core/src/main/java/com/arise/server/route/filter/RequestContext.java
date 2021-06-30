package com.arise.server.route.filter;

import io.netty.handler.codec.http.HttpObject;
import io.netty.util.concurrent.Promise;

import java.util.Iterator;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 19:05 2021-06-28
 * @Description: 能够跳转下一个filter，能够
 * @Modified: By：
 */
public class RequestContext {

    private Iterator<HttpObjectFilter> iterator;

    private final Promise<List<HttpObject>> respPromise;

    public Promise<List<HttpObject>> getRespPromise() {
        return this.respPromise;
    }

    public void setIterator(Iterator<HttpObjectFilter> iterator) {
        this.iterator = iterator;
    }

    public RequestContext(Promise<List<HttpObject>> promise) {
        this.respPromise = promise;
    }

    public void filter(List<HttpObject> req) {
        if (iterator.hasNext()) {
            iterator.next().doFilter(req, this);
        }
    }
}
