package com.ewell.filters.route;

import com.ewell.common.RouteBean;
import com.ewell.core.filer.RouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.core.route.RouteCache;
import com.google.inject.Inject;
import io.netty.util.collection.IntObjectHashMap;

import java.net.URI;
import java.util.List;

import static com.ewell.common.GatewayMessages.ROUTE_NOT_FOUND;
import static com.ewell.common.IntMapConstant._FinalRouteBean;
import static com.ewell.common.IntMapConstant._RequestURI;

/**
 * @Author: wy
 * @Date: Created in 4:47 下午 2021/12/1
 * @Description: 简单匹配路由
 * @Modified: By：
 */
public class MatchRouteFilter extends RouteFilter {

    @Inject
    private static RouteCache routeCache;

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        URI requestURI = (URI) attr.get(_RequestURI);
        List<RouteBean> matched = routeCache.match(requestURI.getPath(), attr);
        if (matched.size() == 0) {
            ctx.cancel(ROUTE_NOT_FOUND());
            return;
        }
        //匹配多个默认取第一个,也可以自己加filter筛选
        attr.put(_FinalRouteBean, matched.get(0));
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 1;
    }
}
