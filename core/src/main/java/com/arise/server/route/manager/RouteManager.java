package com.arise.server.route.manager;

import com.arise.config.ServerProperties;
import com.arise.redis.AsyncRedisClient;
import com.arise.server.route.RouteBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    public static void main(String[] args) {
        RestRouteTrie tree = new RestRouteTrie();
        tree.addRoute("fds/asd/asd", new RouteBean());
        tree.addRoute("fds/fds/das", new RouteBean());
        System.out.println(args);
    }

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        AsyncRedisClient client = ServerProperties.getBean(AsyncRedisClient.class);
        try {
            client.commands().hgetall("ROUTE").get(20, TimeUnit.SECONDS).values()
                    .forEach(e -> {
                        //初始化路由
                        addRoute((RouteBean) e);
                    });
        } catch (TimeoutException e) {
            log.error("初始化路由超时:{}", e.getMessage());
        }
    }

    public List<RouteBean> match(String url) {
        return tree.matching(url);
    }

    public void clear() {
        tree.clear();
    }

    public void addRoute(RouteBean route) {
        tree.addRoute(route.getGatewayPath(), route);
    }
}
