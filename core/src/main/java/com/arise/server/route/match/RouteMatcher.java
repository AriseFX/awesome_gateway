package com.arise.server.route.match;

import com.alibaba.nacos.api.utils.StringUtils;
import com.arise.internal.exception.ServiceNotFoundException;
import com.arise.naming.registry.ServiceManager;
import com.arise.server.route.RouteBean;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.SchedulableFilter;
import com.arise.server.route.manager.RouteManager;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        URI requestURI = (URI) attr.computeIfAbsent("RequestURI",
                URI::create);
        List<RouteBean> matched = routeManager.match(requestURI.getPath());
        //脚本相关
        HttpHeaders headers = request.headers();
        List<RouteBean> result = matched.stream().filter(e -> {
            CompiledScript script = e.getCompiledScript();
            if (script != null) {
                SimpleBindings bindings = new SimpleBindings();
                for (Map.Entry<String, String> entry : e.getPramMapping().entrySet()) {
                    String value = headers.get(entry.getKey());
                    if (StringUtils.isEmpty(value)) {
                        return false;
                    }
                    bindings.put(entry.getValue(), value);
                }
                try {
                    Object res = script.eval(new SimpleBindings(bindings));
                    return (res instanceof Boolean) ? (Boolean) res : false;
                } catch (ScriptException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
        if (result.size() == 0) {
            return null;
        }
        //路由过滤
        List<RouteBean>[] pointer = new List[]{result};
        new FilterContext<>(pointer, routeFilters, eventLoop, attr).handleNext();
        if (pointer[0] != null && pointer[0].size() > 0) {
            //默认取第一个
            RouteBean route = result.get(0);
            URI remoteUri = URI.create(route.getService() + route.getServicePath());
            String scheme = remoteUri.getScheme();
            int port = remoteUri.getPort();
            String host = remoteUri.getHost();
            request.setUri(remoteUri.getPath());
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
}
