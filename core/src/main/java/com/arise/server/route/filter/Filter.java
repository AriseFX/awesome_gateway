package com.arise.server.route.filter;

import com.arise.spi.SPI;

/**
 * @Author: wy
 * @Date: Created in 16:11 2021-07-13
 * @Description:
 * @Modified: By：
 */
@SPI
public interface Filter {

    int order();

    Lifecycle lifecycle();

    void doFilter(FilterContext ctx);
}
