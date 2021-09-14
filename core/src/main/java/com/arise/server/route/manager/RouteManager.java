package com.arise.server.route.manager;


import com.arise.base.config.Components;
import com.arise.redis.AsyncRedisClient;
import com.arise.server.route.RouteBean;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 10:03 2021-07-01
 * @Description: 路由管理
 * @Modified: By：
 */
@Slf4j
public class RouteManager {

    private final RestRouteTrie tree = new RestRouteTrie();

    public RouteManager() {
        AsyncRedisClient client = Components.get(AsyncRedisClient.class);
        client.asyncExec((e, throwable) ->
                e.hgetall("ROUTE")
                        .whenComplete((v, ex) -> {
                                    if (ex != null) {
                                        log.error("路由初始化失败，JVM退出!:{}", ex.getMessage());
                                        System.exit(0);
                                    }
                                    v.values().forEach(x -> addRoute((RouteBean) x));
                                    log.info("路由初始化成功，数目:{}", v.size());
                                }
                        ));

    }

    public List<RouteBean> match(String url, IntObjectHashMap<Object> attr) {
        return tree.matching(url, attr);
    }

    public void clear() {
        tree.clear();
    }

    public void addRoute(RouteBean route) {
        tree.addRoute(route.getGatewayPath(), route);
    }
}
