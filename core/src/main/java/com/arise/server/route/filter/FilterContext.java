package com.arise.server.route.filter;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;
import lombok.Getter;

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
public class FilterContext<P1, P2> {

    private final Iterator<SchedulableFilter<P1, P2>> iterator;
    @Getter
    private final Promise<P2> promise;
    @Getter
    private final Promise<Object> callback;
    @Getter
    private final EventLoop eventLoop;
    @Getter
    private final P1 pram;

    private final Map<String, Object> attr;

    public FilterContext(P1 pram, List<SchedulableFilter<P1, P2>> filters, EventLoop eventLoop, Map<String, Object> attr) {
        this(pram, filters, eventLoop, attr, null);
    }

    public FilterContext(P1 pram, List<SchedulableFilter<P1, P2>> filters, EventLoop eventLoop, Map<String, Object> attr, @Nullable Promise<Object> callback) {
        this(pram, null, filters, eventLoop, attr, callback);
    }

    public FilterContext(P1 pram, @Nullable Promise<P2> promise, List<SchedulableFilter<P1, P2>> filters, EventLoop eventLoop, Map<String, Object> attr, Promise<Object> callback) {
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
