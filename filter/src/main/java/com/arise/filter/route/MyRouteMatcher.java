package com.arise.filter.route;

import com.arise.naming.registry.ServiceManager;
import com.arise.server.route.RouteBean;
import com.arise.server.route.RouteMatcher;
import com.arise.server.route.manager.RouteManager;
import io.netty.handler.codec.http.HttpRequest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    @Resource
    private RouteManager routeManager;

    @Override
    public InetSocketAddress matching(HttpRequest request) {
        List<RouteBean> matched = routeManager.match(request.uri());
        if (matched.size() > 0) {
            //默认取第一个
            RouteBean route = matched.get(0);
            URI remoteUri = URI.create(route.getService() + route.getServicePath());
            //对路由执行规则
            if (remoteUri.getScheme().equals("lb")) {
                request.setUri(remoteUri.getPath());
                return ServiceManager.selectService(remoteUri.getHost());
            }
        }
        return null;
    }
}
