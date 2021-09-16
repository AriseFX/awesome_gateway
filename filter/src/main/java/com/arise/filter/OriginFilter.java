package com.arise.filter;

import com.arise.server.route.PluginBean;
import com.arise.server.route.RouteBean;
import com.arise.server.route.filter.Filter;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.Lifecycle;
import com.arise.spi.Join;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.arise.base.config.IntMapConstant.*;
import static com.arise.server.route.filter.Lifecycle.Route;

/**
 * @Author: wy
 * @Date: Created in 17:42 2021-07-09
 * @Description: 用户域/Backend过滤
 * @Modified: By：
 */
@Slf4j
@Join
public class OriginFilter implements Filter {

    private static final String TopLevelDomain = "0";

    @Override
    public int order() {
        return 0;
    }

    @Override
    public Lifecycle lifecycle() {
        return Route;
    }

    @Override
    public void doFilter(FilterContext ctx) {
        IntObjectHashMap<Object> attr = ctx.attr();
        List<RouteBean>[] pram = (List<RouteBean>[]) ctx.getPram();
        Map<String, List<RouteBean>> group =
                pram[0].stream().collect(Collectors.groupingBy(e -> {
                    String originCode = e.getMetadata().get("originCode");
                    if (originCode == null) {
                        return TopLevelDomain;
                    }
                    return originCode;
                }));
        String currentOriginCode = (String) attr.get(OriginCode);
        List<RouteBean> topRoute = group.get(TopLevelDomain);
        if (currentOriginCode == null) {
            pram[0] = topRoute;
        } else {
            List<RouteBean> currentRoute = group.get(currentOriginCode);
            if (currentRoute == null) {
                pram[0] = topRoute;
            } else {
                pram[0] = currentRoute;
            }
        }
        //手动指定目标服务
        Object service = attr.get(Backend);
        if (service != null) {
            //根据Backend匹配
            pram[0] = pram[0].stream()
                    .filter(e -> service.equals(e.getTag()))
                    .collect(Collectors.toList());
        } else {
            //插件,TODO 插件为了适配旧系统，后面要去掉！
            if (pram[0] != null && pram[0].size() > 0) {
                HttpHeaders headers = (HttpHeaders) attr.get(Header);
                pram[0] = pram[0].stream().filter(x -> {
                    List<PluginBean> plugins = x.getPlugins();
                    if (plugins == null || plugins.size() == 0) {
                        return true;
                    }
                    return plugins.stream().allMatch(e -> {
                        EvaluationContext context = new StandardEvaluationContext();
                        //设置变量值
                        headers.forEach(entry -> {
                                    context.setVariable(entry.getKey() != null ? entry.getKey().toUpperCase() : "",
                                            entry.getValue() != null ? entry.getValue().toUpperCase() : "");
                                }
                        );
                        log.info("网关路由[{}]开始执行插件逻辑:{}", x.getGatewayPath(), e.getName());
                        //执行脚本
                        return new SpelExpressionParser()
                                .parseExpression(e.getScript())
                                .getValue(context, Boolean.class);
                    });
                }).collect(Collectors.toList());
            }
        }
        ctx.handleNext();
    }
}
