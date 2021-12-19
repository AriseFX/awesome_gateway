package com.ewell.route;

import com.ewell.common.RouteBean;
import com.ewell.common.exception.SimpleRuntimeException;
import com.ewell.core.route.RoutePromise;
import com.ewell.core.route.RouteStoreSpi;
import com.ewell.redis.AsyncRedisClient;
import com.ewell.spi.Join;

import com.google.inject.Inject;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: wy
 * @Date: Created in 10:42 下午 2021/11/28
 * @Description:
 * @Modified: By：
 */
@Join
public class RedisRouteStoreImpl implements RouteStoreSpi {

    @Inject
    private AsyncRedisClient asyncRedisClient;

    @Override
    public void putRoutes(Collection<RouteBean> routes, RoutePromise<String> promise) {
        Map<String, Object> map = routes.stream()
                .collect(Collectors.toMap(RouteBean::getId, v -> v, (v1, v2) -> v1));
        asyncRedisClient.asyncExec((e, throwable) ->
                e.hset("ROUTE", map).whenComplete((v, ex) -> {
                            if (ex != null) {
                                promise.tryFailure(new SimpleRuntimeException("路由添加失败,错误信息:[" + ex.getMessage() + "]"));
                                return;
                            }
                            promise.trySuccess("route put successful");
                        }
                ));

    }

    @Override
    public void getRoutes(RoutePromise<Object> promise) {
        asyncRedisClient.asyncExec((e, throwable) -> e.hgetall("ROUTE").whenComplete((v, ex) -> {
            if (ex != null) {
                promise.tryFailure(
                        new SimpleRuntimeException("路由获取失败,错误信息:[" + ex.getMessage() + "]"));
                return;
            }
            promise.trySuccess(v.values());
        }));
    }

    @Override
    public void deleteRoutes(RoutePromise<String> promise) {
        asyncRedisClient.asyncExec((e, throwable) ->
                e.del("ROUTE").whenComplete((v, ex) -> {
                            if (ex != null) {
                                promise.tryFailure(
                                        new SimpleRuntimeException("路由删除失败,错误信息:[" + ex.getMessage() + "]"));
                            }
                            promise.trySuccess("route clear successful");
                        }
                ));
    }
}
