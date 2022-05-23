package com.ewell.core.filer.context;

import com.ewell.common.message.GatewayMessage;
import com.ewell.common.message.Message;
import com.ewell.spi.ExtensionLoader;
import io.netty.channel.EventLoop;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.Promise;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static com.ewell.common.GatewayMessages.GATEWAY_ERROR;
import static com.ewell.common.IntMapConstant._RespObserver;
import static com.ewell.core.filer.context.FilterFuture.SUCCESS;

/**
 * @Author: wy
 * @Date: Created in 10:06 上午 2021/11/26
 * @Description: 过滤器上下文(线程安全)
 */
@Slf4j
public class FilterContext {

    private Iterator<? extends GatewayFilter> iterator;

    private final Promise<Object> callback;
    @Getter
    private final IntObjectHashMap<Object> attr;
    @Getter
    private final EventLoop eventLoop;

    private static final Map<GatewayFilter.FilterType, List<GatewayFilter>> filterMap;

    static {
        filterMap = ExtensionLoader
                .getExtensionLoader(GatewayFilter.class).getAllJoin()
                .stream().collect(Collectors.groupingBy(GatewayFilter::type));
        filterMap.forEach((key, value) ->
                value.sort(Comparator.comparingInt(GatewayFilter::order))
        );
    }

    public FilterContext(Promise<Object> callback, EventLoop eventLoop, IntObjectHashMap<Object> attr,
                         GatewayFilter.FilterType filterType) {
        this.eventLoop = eventLoop;
        List<GatewayFilter> gatewayFilters = filterMap.get(filterType);
        if (gatewayFilters != null) {
            this.iterator = gatewayFilters.iterator();
        }
        this.callback = callback;
        this.attr = attr;
    }

    public void start(Object data) {
        try {
            if (iterator != null && iterator.hasNext()) {
                iterator.next().doFilter(this, data);
            } else {
                callback.setSuccess(SUCCESS(null));
            }
        } catch (Exception e) {
            log.error("过滤器异常:", e);
            cancel(GATEWAY_ERROR(e.getMessage()));
        }
    }

    /**
     * 添加响应观察者
     */
    public void addRespObserver(Observer<Message> observer) {
        TreeSet<Observer<Message>> o = (TreeSet<Observer<Message>>) attr.get(_RespObserver);
        o.add(observer);
    }

    public void success() {
        success(null);
    }

    public void success(Object result) {
        callback.setSuccess(FilterFuture.SUCCESS(result));
    }

    public void cancel(GatewayMessage message) {
        callback.setSuccess(FilterFuture.CANCEL(message));
    }

    public void doNext(Object data) {
        if (iterator.hasNext()) {
            iterator.next().doFilter(this, data);
        } else {
            this.success();
        }
    }
}
