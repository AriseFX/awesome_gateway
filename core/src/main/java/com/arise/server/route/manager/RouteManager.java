package com.arise.server.route.manager;

import com.arise.config.ServerProperties;
import com.arise.redis.AsyncRedisClient;
import com.arise.server.route.RouteBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 10:03 2021-07-01
 * @Description: 路由管理
 * @Modified: By：
 */
@Slf4j
@Component
@DependsOn(value = "redisClient")
public class RouteManager {

    private final RestRouteTrie tree = new RestRouteTrie();

    @PostConstruct
    public void init() {
        AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
        client.asyncExec( e ->
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

    public List<RouteBean> match(String url, Map<String, Object> attr) {
        return tree.matching(url, attr);
    }

    public void clear() {
        tree.clear();
    }

    public void addRoute(RouteBean route) {
        tree.addRoute(route.getGatewayPath(), route);
    }
}
