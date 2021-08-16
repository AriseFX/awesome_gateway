package com.arise.server.route.match;

import com.arise.internal.exception.ServiceNotFoundException;
import com.arise.naming.registry.ServiceManager;
import com.arise.server.route.RouteBean;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.SchedulableFilter;
import com.arise.server.route.manager.RouteManager;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpRequest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static com.arise.server.route.ApiRouteHandler.RequestURI;
import static com.arise.server.route.manager.RestRouteTrie.PathPram;

/**
 * @Author: wy
 * @Date: Created in 11:24 2021-06-30
 * @Description:
 * @Modified: By：
 */
@Component
public class RouteMatcher {

    @Resource
    private RouteManager routeManager;

    public static List<SchedulableFilter<List<RouteBean>[], Object>> routeFilters;

    public MatchRes match(EventLoop eventLoop, Map<String, Object> attr, HttpRequest request) {
        URI requestURI = (URI) attr.computeIfAbsent(RequestURI,
                URI::create);
        List<RouteBean> matched = routeManager.match(requestURI.getPath(), attr);
        if (matched.size() == 0) {
            return null;
        }
        //路由过滤
        List<RouteBean>[] pointer = new List[]{matched};
        new FilterContext<>(pointer, routeFilters, eventLoop, attr).handleNext();
        if (pointer[0] != null && pointer[0].size() > 0) {
            //默认取第一个
            RouteBean route = pointer[0].get(0);
            String remotePath = route.getServicePath();
            Object p = attr.get(PathPram);
            if (p != null) {
                //url重写
                remotePath = rewriteUrl((Queue<String>) p, remotePath);
            }
            URI remoteUri = URI.create(route.getService() + remotePath);
            String scheme = remoteUri.getScheme();
            int port = remoteUri.getPort();
            String host = remoteUri.getHost();
            //重写url
            String query = requestURI.getQuery();
            request.setUri(remoteUri.getPath() + (query == null ? "" : "?" + query));
            switch (scheme) {
                case "lb":
                    InetSocketAddress address = ServiceManager.selectService(remoteUri.getHost());
                    if (address == null) {
                        throw new ServiceNotFoundException();
                    }
                    return new MatchRes(false, address);
                case "http":
                    return new MatchRes(false,
                            new InetSocketAddress(host, port == -1 ? 80 : port));
                case "https":
                    return new MatchRes(true,
                            new InetSocketAddress(host, port == -1 ? 443 : port));
            }
        }
        return null;
    }

    private String rewriteUrl(Queue<String> prams, String url) {
        //解析url参数，如: /user/{id}
        StringBuilder rewriteUrl = new StringBuilder();
        String[] split = url.split("/");
        for (int j = 1; j < split.length; j++) {
            String s = split[j];
            if (s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}') {
                rewriteUrl.append("/").append(prams.poll());
            } else {
                rewriteUrl.append("/").append(s);
            }
        }
        return rewriteUrl.toString();
    }

}
