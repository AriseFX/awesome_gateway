package com.ewell.endpoint.service;

import com.ewell.common.GatewayConfig;
import com.ewell.core.route.RouteCache;
import com.ewell.core.route.RouteStoreSpi;
import com.google.inject.Inject;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.function.BiConsumer;

/**
 * @Author: wy
 * @Date: Created in 11:11 下午 2021/11/28
 * @Description:
 * @Modified: By：
 */
public abstract class RouteServicesConsumer implements BiConsumer<FullHttpRequest, Channel> {

    @Inject
    protected static RouteStoreSpi routeStoreSpi;

    @Inject
    protected static RouteCache routeCache;

    @Inject
    protected static GatewayConfig gatewayConfig;

}
