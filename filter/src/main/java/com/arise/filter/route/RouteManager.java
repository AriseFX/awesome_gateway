package com.arise.filter.route;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @Author: wy
 * @Date: Created in 10:03 2021-07-01
 * @Description: 路由管理
 * @Modified: By：
 */
@Component
public class RouteManager {

    private final RestRouteRadixTree<Route> tree = new RestRouteRadixTree<>();

    private ScriptEngine engine =
            new ScriptEngineManager().getEngineByName("javascript");

    @PostConstruct
    public void init() {
//        redisClient.get("","");
    }

    private void match() {

    }
}
