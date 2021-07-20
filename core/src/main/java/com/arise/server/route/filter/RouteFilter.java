package com.arise.server.route.filter;


import com.arise.server.route.RouteBean;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 17:09 2021-07-13
 * @Description: 路由过滤器
 * @Modified: By：
 */
public abstract class RouteFilter implements SchedulableFilter<List<RouteBean>[], Object> {
}
