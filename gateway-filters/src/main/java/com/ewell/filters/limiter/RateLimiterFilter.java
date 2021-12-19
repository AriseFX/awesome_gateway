package com.ewell.filters.limiter;

import com.ewell.common.GatewayMessages;
import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.spi.Join;
import com.google.common.util.concurrent.RateLimiter;
import io.netty.util.collection.IntObjectHashMap;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ewell.common.IntMapConstant._RequestURI;

/**
 * @Author: wy
 * @Date: Created in 3:32 PM 2021/12/8
 * @Description: 限流过滤器
 * @Modified: By：
 */
@Join
@SuppressWarnings(value = "all")
public class RateLimiterFilter extends PreRouteFilter {

    private Map<String, RateLimiter> map = new ConcurrentHashMap<>();

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        URI uri = (URI) attr.get(_RequestURI);
        String path = uri.getPath();
        RateLimiter rateLimiter = map.computeIfAbsent(path, key -> RateLimiter.create(500));
        if (!rateLimiter.tryAcquire()) {
            ctx.cancel(GatewayMessages.TOO_MANY_REQUESTS());
            return;
        }
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 3;
    }
}
