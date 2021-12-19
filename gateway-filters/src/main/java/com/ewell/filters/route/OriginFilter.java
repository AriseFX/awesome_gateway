package com.ewell.filters.route;

import com.ewell.common.PluginBean;
import com.ewell.common.RouteBean;
import com.ewell.common.dto.AlarmDto;
import com.ewell.core.filer.RouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.filters.logging.AweLogService;
import com.ewell.spi.Join;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ewell.common.GatewayMessages.ROUTE_NOT_FOUND;
import static com.ewell.common.IntMapConstant.*;

/**
 * @Author: wy
 * @Date: Created in 7:39 下午 2021/11/30
 * @Description: 用户域
 * @Modified: By：
 */
@Slf4j
@Join
public class OriginFilter extends RouteFilter {

    private static final String TopLevelDomain = "0";

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        List<RouteBean> routeBeans = (List<RouteBean>) attr.get(_RouteBeans);
        Map<String, List<RouteBean>> group = routeBeans.stream().collect(Collectors.groupingBy(e -> {
            String originCode = e.getMetadata().get("originCode");
            if (originCode == null) {
                return TopLevelDomain;
            }
            return originCode;
        }));
        List<RouteBean> res;
        String currentOriginCode = (String) attr.get(_OriginCode);
        List<RouteBean> topRoute = group.get(TopLevelDomain);
        if (currentOriginCode == null) {
            res = topRoute;
        } else {
            List<RouteBean> currentRoute = group.get(currentOriginCode);
            if (currentRoute == null) {
                res = topRoute;
            } else {
                res = currentRoute;
            }
        }
        //手动指定目标服务
        Object service = attr.get(_Backend);
        if (service != null) {
            //根据Backend匹配
            res = res.stream().filter(e -> service.equals(e.getTag())).collect(Collectors.toList());
        } else {
            //插件,TODO 插件为了适配旧系统，后面要去掉！
            if (res != null && res.size() > 0) {
                HttpHeaders headers = (HttpHeaders) attr.get(_Header);
                res = res.stream().filter(x -> {
                    List<PluginBean> plugins = x.getPlugins();
                    if (plugins == null || plugins.size() == 0) {
                        return true;
                    }
                    return plugins.stream().allMatch(e -> {
                        EvaluationContext context = new StandardEvaluationContext();
                        //设置变量值
                        headers.forEach(entry -> {
                            context.setVariable(entry.getKey() != null ? entry.getKey().toUpperCase() : "", entry.getValue() != null ? entry.getValue().toUpperCase() : "");
                        });
                        log.info("网关路由[{}]开始执行插件逻辑:{}", x.getGatewayPath(), e.getName());
                        //执行脚本
                        return new SpelExpressionParser().parseExpression(e.getScript()).getValue(context, Boolean.class);
                    });
                }).collect(Collectors.toList());
            }
        }
        if (res != null && res.size() > 0) {
            attr.put(_FinalRouteBean, res.get(0));
            ctx.doNext(data);
            return;
        }
        URI uri = (URI) attr.get(_RequestURI);
        AlarmDto alarmDto = new AlarmDto(uri.getPath(), "路由未找到", "GATEWAY", currentOriginCode, service + "");
        AweLogService.alarm(alarmDto);
        ctx.cancel(ROUTE_NOT_FOUND());
    }

    @Override
    public byte order() {
        return 1;
    }

}
