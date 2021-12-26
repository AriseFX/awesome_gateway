package com.ewell.filters.route;

import com.ewell.common.RouteBean;
import com.ewell.common.dto.AlarmDto;
import com.ewell.core.discovery.ServiceManager;
import com.ewell.core.fade.OutsidePluginRouter;
import com.ewell.core.filer.RouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.core.route.MatchRes;
import com.ewell.filters.logging.AweLogService;
import com.ewell.spi.Join;
import com.google.inject.Inject;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Queue;

import static com.ewell.common.GatewayMessages.GATEWAY_ERROR;
import static com.ewell.common.GatewayMessages.SERVICE_NOT_FOUND;
import static com.ewell.common.IntMapConstant.*;

/**
 * @Author: wy
 * @Date: Created in 5:27 下午 2021/12/1
 * @Description:
 * @Modified: By：
 */
@Slf4j
@Join
public class SelectServiceFilter extends RouteFilter {

    @Inject
    private static ServiceManager serviceManager;

    @Inject
    private static OutsidePluginRouter outsidePluginRouter;

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        RouteBean route = (RouteBean) attr.get(_FinalRouteBean);
        log.info("网关路由:{},后端地址:{},后端路由:{}", route.getGatewayPath(), route.getService(), route.getServicePath());
        String remotePath = route.getServicePath();
        Object p = attr.get(_PathPram);
        URI requestURI = (URI) attr.get(_RequestURI);
        if (p != null) {
            //url重写
            remotePath = rewriteUrl((Queue<String>) p, remotePath);
        }
        URI remoteUri = URI.create(route.getService() + remotePath);
        String scheme = remoteUri.getScheme();
        int port = remoteUri.getPort();
        String host = remoteUri.getHost();
        //重写url
        String query = requestURI.getRawQuery();
        String url = remoteUri.getPath() + (query == null ? "" : "?" + query);

        switch (scheme) {
            case "lb":
                InetSocketAddress address = serviceManager.selectService(remoteUri.getHost());
                if (address == null) {
                    AlarmDto alarmDto = new AlarmDto(((URI) attr.get(_RequestURI)).getPath(),
                            "服务未找到:" + remoteUri.getHost(), "GATEWAY",
                            (String) attr.get(_OriginCode), (String) attr.get(_Backend));
                    AweLogService.alarm(alarmDto);
                    ctx.cancel(SERVICE_NOT_FOUND(remoteUri.getHost()));
                    return;
                }
                ctx.success(new MatchRes(false, address, url));
                return;
            case "http":
                ctx.success(new MatchRes(false,
                        new InetSocketAddress(host, port == -1 ? 80 : port), url));
                return;
            case "https":
                ctx.success(new MatchRes(true,
                        new InetSocketAddress(host, port == -1 ? 443 : port), url));
                return;
                default:
                outsidePluginRouter.router(scheme,ctx,data);
        }
    }

    @Override
    public byte order() {
        return 3;
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
