package com.ewell.core.route;


import com.ewell.common.RouteBean;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 10:03 2021-07-01
 * @Description: 路由管理
 * @Modified: By：
 */
@Slf4j
@Singleton
public class RouteCache {

    private final RestRouteTrie tree = new RestRouteTrie();

    @Inject
    public RouteCache(RouteStoreSpi routeStoreSpi) {
        RoutePromise<Object> routePromise = new RoutePromise<>();
        routePromise.addListener(future -> {
            if (future.isSuccess()) {
                Collection<RouteBean> routeBeans = (Collection<RouteBean>) future.get();
                routeBeans.forEach(this::addRoute);
                log.info("路由缓存加载成功!数目:{}", routeBeans.size());
            } else {
                log.error("路由缓存加载失败!");
                System.exit(-1);
            }
        });
        routeStoreSpi.getRoutes(routePromise);
    }

    public List<RouteBean> match(String path, IntObjectHashMap<Object> attr) {
        return tree.matching(path, attr);
    }

    public void clear() {
        tree.clear();
    }

    public void addRoute(RouteBean route) {
        tree.addRoute(route.getGatewayPath(), route);
    }
}
