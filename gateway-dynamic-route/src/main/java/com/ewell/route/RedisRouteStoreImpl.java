package com.ewell.route;

import com.ewell.common.RouteBean;
import com.ewell.common.exception.SimpleRuntimeException;
import com.ewell.core.route.RoutePromise;
import com.ewell.core.route.RouteStoreSpi;
import com.ewell.redis.AsyncRedisClient;
import com.google.inject.Inject;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.impl.types.BulkType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ewell.redis.AsyncRedisClient.decode;
import static com.ewell.redis.AsyncRedisClient.encode;

/**
 * @Author: wy
 * @Date: Created in 10:42 下午 2021/11/28
 * @Description:
 * @Modified: By：
 */
public class RedisRouteStoreImpl implements RouteStoreSpi {

    @Inject
    private AsyncRedisClient asyncRedisClient;

    @Override
    public void putRoutes(Collection<RouteBean> routes, RoutePromise<String> promise) {
        Map<String, Object> map = routes.stream()
                .collect(Collectors.toMap(RouteBean::getId, v -> v, (v1, v2) -> v1));
        //生成批量操作
        List<Request> requests = map.entrySet().stream().map(e ->
                        Request.cmd(Command.HSET).arg("ROUTE").arg(e.getKey()).arg(encode(e.getValue())))
                .collect(Collectors.toList());

        asyncRedisClient.getConnection().onSuccess(conn -> {
            conn.batch(requests)
                    .onSuccess(e -> {
                        promise.trySuccess("route put successful");
                    }).onFailure(e -> {
                        promise.tryFailure(new SimpleRuntimeException("路由添加失败,错误信息:[" + e.getMessage() + "]"));
                    }).onComplete(e -> conn.close());
        });
    }

    @Override
    public void getRoutes(RoutePromise<Object> promise) {
        Request request = Request.cmd(Command.HGETALL).arg("ROUTE");

        asyncRedisClient.getConnection().onSuccess(conn -> {
            conn.send(request)
                    .onSuccess(e -> {
                        List<Object> collect = e.getKeys().stream().map(k -> {
                            BulkType response = (BulkType) e.get(k);
                            return decode(response.toBytes());
                        }).collect(Collectors.toList());
                        promise.trySuccess(collect);
                    }).onFailure(e -> {
                        promise.tryFailure(
                                new SimpleRuntimeException("路由获取失败,错误信息:[" + e.getMessage() + "]"));
                    }).onComplete(e -> conn.close());
        });
    }

    @Override
    public void deleteRoutes(RoutePromise<String> promise) {
        Request request = Request.cmd(Command.DEL).arg("ROUTE");

        asyncRedisClient.getConnection().onSuccess(conn -> {
            conn.send(request).onSuccess(e -> {
                promise.trySuccess("route clear successful");
            }).onFailure(e -> {
                promise.tryFailure(
                        new SimpleRuntimeException("路由删除失败,错误信息:[" + e.getMessage() + "]"));
            }).onComplete(e -> conn.close());

        });
    }
}
