package com.ewell.filters.route;

import com.ewell.common.RouteBean;
import com.ewell.common.dto.AlarmDto;
import com.ewell.core.filer.RouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.core.route.RouteCache;
import com.ewell.filters.logging.AweLogService;
import com.ewell.spi.Join;
import com.google.inject.Inject;
import io.netty.util.collection.IntObjectHashMap;

import java.net.URI;
import java.util.List;

import static com.ewell.common.GatewayMessages.ROUTE_NOT_FOUND;
import static com.ewell.common.IntMapConstant.*;

/**
 * @Author: wy
 * @Date: Created in 4:47 下午 2021/12/1
 * @Description: 简单匹配路由
 * @Modified: By：
 */
@Join
public class MatchRouteFilter extends RouteFilter {

    @Inject
    private static RouteCache routeCache;

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        URI requestURI = (URI) attr.get(_RequestURI);
        List<RouteBean> matched = routeCache.match(requestURI.getPath(), attr);
        if (matched.size() == 0) {
            URI uri = (URI) attr.get(_RequestURI);
            AlarmDto alarmDto = new AlarmDto(uri.getPath(), "路由未找到", "GATEWAY", (String) attr.get(_OriginCode), (String) attr.get(_Backend));
            AweLogService.alarm(alarmDto);
            ctx.cancel(ROUTE_NOT_FOUND());
            return;
        }
        attr.put(_RouteBeans, matched);
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 0;
    }
}
