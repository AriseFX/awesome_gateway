package com.ewell.core.filer.context;

import com.ewell.spi.SPI;

/**
 * @Author: wy
 * @Date: Created in 10:00 上午 2021/11/26
 * @Description: PreRouteFilter->匹配路由->PostRouteFilter->PreProxy->开始代理->处理响应->ResponseFilter
 * @Modified: By：
 */
@SPI
public interface GatewayFilter {

    /**
     * 执行过滤器
     */
    void doFilter(FilterContext ctx, Object data);

    /**
     * 优先级
     */
    byte order();

    /**
     * 过滤器类型
     */
    FilterType type();

    enum FilterType {
        Route,
        PreRoute,
        PostRoute,
    }
}

