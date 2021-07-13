package com.arise.filter;

import com.arise.server.route.filter.RequestContext;
import com.arise.server.route.filter.RouteFilter;
import org.springframework.stereotype.Component;

/**
 * @Author: wy
 * @Date: Created in 17:42 2021-07-09
 * @Description:
 * @Modified: By：
 */
@Component
public class MyRouteFilter extends RouteFilter {

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void doFilter(Object pram, RequestContext<Object> ctx) {

    }
}
