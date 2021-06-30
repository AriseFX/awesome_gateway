package com.arise.server.route;

import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;

/**
 * @Author: wy
 * @Date: Created in 11:01 2021-06-30
 * @Description:
 * @Modified: By：
 */
public interface RouteMatcher {

    InetSocketAddress matching(HttpRequest request);
}
