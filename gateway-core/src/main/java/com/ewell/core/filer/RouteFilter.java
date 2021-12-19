package com.ewell.core.filer;


import com.ewell.core.filer.context.GatewayFilter;

/**
 * @Author: wy
 * @Date: Created in 3:25 下午 2021/11/30
 * @Description: 后置过滤器
 * @Modified: By：
 */
public abstract class RouteFilter implements GatewayFilter {

    @Override
    public FilterType type() {
        return FilterType.Route;
    }
}
