package com.ewell.core.filer;


import com.ewell.core.filer.context.GatewayFilter;

/**
 * @Author: wy
 * @Date: Created in 3:25 下午 2021/11/30
 * @Description:
 * @Modified: By：
 */
public abstract class PreRouteFilter implements GatewayFilter {

    @Override
    public FilterType type() {
        return FilterType.PreRoute;
    }
}
