package com.ewell.filters.auth;

import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.redis.AsyncRedisClient;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.collection.IntObjectHashMap;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;

import static com.ewell.common.GatewayMessages.GATEWAY_ERROR;
import static com.ewell.common.GatewayMessages.UNAUTHORIZED;
import static com.ewell.common.IntMapConstant._Header;
import static com.ewell.common.IntMapConstant._RequestURI;

/**
 * @Author: wy
 * @Date: Created in 4:05 PM 2022/3/16
 * @Description: 权限
 * @Modified: By：
 */
@Slf4j
public class AuthFilter extends PreRouteFilter {

    private static final String TIMESTAMP = "timestamp";

    private static final String SIGN = "sign";

    private static final String APP_KEY = "appKey";

    private static final String VERSION = "version";

    private static final String PATH = "path";

    @Inject
    public static AsyncRedisClient redisClient;

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        URI uri = (URI) attr.get(_RequestURI);
        HttpHeaders headers = (HttpHeaders) attr.get(_Header);
        String path = uri.getPath();
        if (path.startsWith("/dss")) {
            //重写一下uri,给自己人留的后门
            String uriStr = "/openapi" + uri.toString().substring(4, uri.toString().length() - 1);
            uri = URI.create(uriStr);
            attr.put(_RequestURI, uri);
            //直接放行
            ctx.doNext(data);
            return;
        }
        if (path.startsWith("/openapi")) {
            String appKey = headers.get(APP_KEY);
            if (appKey == null) {
                ctx.cancel(UNAUTHORIZED("无访问权限"));
                return;
            }
            //查询AppSecret
            redisClient.getConnection().onSuccess(conn -> {
                Request appSecretReq = Request.cmd(Command.HGET).arg("AppSecret").arg(appKey);
                conn.send(appSecretReq).onSuccess(e -> {
                    if (e != null) {
                        String appSecret = new String(e.toBytes());
                        String time = headers.get(TIMESTAMP);
                        String sign = headers.get(SIGN);
                        Map<String, String> map = Maps.newHashMapWithExpectedSize(3);
                        map.put(TIMESTAMP, time);
                        map.put(PATH, path);
                        map.put(VERSION, "1.0.0");
                        if (SignUtils.generateSign(appSecret, map).equals(sign)) {
                            //查询权限
                            redisClient.getConnection().onSuccess(conn2 -> {
                                //特殊的逻辑:查出appKey的所有api，如果是空，直接放行
                                String key = "ApiAuth-" + appKey;
                                Request scardReq = Request.cmd(Command.SCARD)
                                        .arg(key);
                                conn2.send(scardReq).onSuccess(i -> {
                                    if (i.toInteger() <= 0) {
                                        //放行
                                        log.info("直接放行该appKey:{}", key);
                                        conn2.close();
                                        ctx.doNext(data);
                                    } else {
                                        Request authReq = Request.cmd(Command.SISMEMBER)
                                                .arg(key).arg(path);
                                        conn2.send(authReq).onSuccess(j -> {
                                            if (j.toInteger() == 1) {
                                                ctx.doNext(data);
                                            } else {
                                                ctx.cancel(UNAUTHORIZED("无访问权限"));
                                            }
                                        }).onFailure(j -> {
                                            log.error("redis查询失败", j);
                                            ctx.cancel(GATEWAY_ERROR("redis查询失败"));
                                        }).onComplete(j -> conn2.close());
                                    }
                                }).onFailure(i -> {
                                    conn2.close();
                                    log.error("redis查询失败", i);
                                    ctx.cancel(GATEWAY_ERROR("redis查询失败"));
                                });
                            });
                        } else {
                            ctx.cancel(UNAUTHORIZED("无访问权限"));
                        }
                    } else {
                        ctx.cancel(UNAUTHORIZED("无访问权限"));
                    }
                }).onFailure(e -> {
                    log.error("redis查询失败", e);
                    ctx.cancel(GATEWAY_ERROR("redis查询失败"));
                }).onComplete(e -> conn.close());
            }).onFailure(e -> {
                log.error("redis连接获取失败", e);
                ctx.cancel(GATEWAY_ERROR("redis连接获取失败"));
            });
        } else {
            ctx.doNext(data);
        }
    }


    @Override
    public byte order() {
        return 5;
    }
}
