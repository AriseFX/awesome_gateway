package com.arise.server.route.filter;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;
import lombok.Getter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 19:05 2021-06-28
 * @Description: 能够跳转下一个filter
 * @Modified: By：
 */
public class FilterContext {

    private final Iterator<Filter> iterator;
    @Getter
    private final Promise<?> promise;
    @Getter
    private final Promise<Object> callback;
    @Getter
    private final EventLoop eventLoop;
    @Getter
    private final Object pram;

    private final Map<String, Object> attr;

    public FilterContext(Object pram, List<Filter> filters, EventLoop eventLoop, Map<String, Object> attr) {
        this(pram, filters, eventLoop, attr, null);
    }

    public FilterContext(Object pram, List<Filter> filters, EventLoop eventLoop, Map<String, Object> attr, Promise<Object> callback) {
        this(pram, null, filters, eventLoop, attr, callback);
    }

    public FilterContext(Object pram, Promise<?> promise, List<Filter> filters, EventLoop eventLoop, Map<String, Object> attr, Promise<Object> callback) {
        this.promise = promise;
        this.callback = callback;
        this.pram = pram;
        this.eventLoop = eventLoop;
        this.attr = attr;
        this.iterator = filters.iterator();
    }

    public Map<String, Object> attr() {
        return this.attr;
    }

    public void handleNext() {
        if (iterator.hasNext()) {
            iterator.next().doFilter(this);
        }
    }
}
