package com.arise.filter;


import com.arise.internal.util.RestRouteRadixTree;
import com.arise.naming.registry.ServiceManager;
import com.arise.server.route.RouteMatcher;
import io.netty.handler.codec.http.HttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 11:24 2021-06-30
 * @Description:
 * @Modified: By：
 */
@Component
public class MyRouteMatcher implements RouteMatcher {

    private final RestRouteRadixTree<String> tree = new RestRouteRadixTree<>();

    public MyRouteMatcher() {
        tree.init();
    }

    @Override
    public InetSocketAddress matching(HttpRequest request) {
        List<String> res = tree.matching(request.uri());
        if (res.size() > 0) {
            URI remoteUri = URI.create(res.get(0));
            if (remoteUri.getScheme().equals("lb")) {
                return ServiceManager.selectService(remoteUri.getHost());
            }
        }
        return null;
    }
}
