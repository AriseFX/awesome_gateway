package com.arise.filter.route;

import com.arise.naming.registry.ServiceManager;
import com.arise.server.route.RouteMatcher;
import io.netty.handler.codec.http.HttpRequest;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 11:24 2021-06-30
 * @Description:
 * @Modified: By：
 */
@Component
public class MyRouteMatcher implements RouteMatcher {


    public MyRouteMatcher() throws ScriptException {
        //TODO 加载路由
       /* List<Route> routes = new ArrayList<>();
        for (Route r : routes) {
            Compilable compEngine = (Compilable) engine;
            r.setScript(compEngine.compile(""));
        }*/
    }

    @Override
    public InetSocketAddress matching(HttpRequest request) {
       /* List<Route> matching = tree.matching(request.uri());
        if (matching.size() > 0) {
            URI remoteUri = URI.create(matching.get(0).getUrl());
            //对路由执行规则
            if (remoteUri.getScheme().equals("lb")) {
                return ServiceManager.selectService(remoteUri.getHost());
            }
        }*/
        return null;
    }
}
