package com.arise.server.route.filter;

import org.springframework.core.Ordered;

/**
 * @Author: wy
 * @Date: Created in 16:11 2021-07-13
 * @Description:
 * @Modified: By：
 */
public interface SchedulableFilter<P1, P2> extends Ordered {

    void doFilter(FilterContext<P1, P2> ctx);
}
