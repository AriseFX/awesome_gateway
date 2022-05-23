package com.ewell.filters.limiter;

import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import io.netty.util.collection.IntObjectHashMap;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ewell.common.GatewayMessages.TOO_MANY_REQUESTS;
import static com.ewell.common.IntMapConstant._RequestURI;

/**
 * @Author: wy
 * @Date: Created in 3:32 PM 2021/12/8
 * @Description: 限流过滤器
 * @Modified: By：
 */
@SuppressWarnings(value = "all")
public class RateLimiterFilter extends PreRouteFilter {

    private Map<String, Limiter> map = new ConcurrentHashMap<>();

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        URI uri = (URI) attr.get(_RequestURI);
        String path = uri.getPath();
        Limiter limiter = map.computeIfAbsent(path, key -> new Limiter(500));
        if (!limiter.tryAcquire()) {
            ctx.cancel(TOO_MANY_REQUESTS());
            return;
        }
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 3;
    }
}
