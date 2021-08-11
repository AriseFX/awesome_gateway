package com.arise.filter;

import com.arise.server.route.RouteBean;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.RouteFilter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.arise.filter.UserTokenFilter.OriginCode;
import static com.arise.filter.UserTokenFilter.TargetService;

/**
 * @Author: wy
 * @Date: Created in 17:42 2021-07-09
 * @Description: 用户域/targetService过滤
 * @Modified: By：
 */
@Component
public class OriginFilter extends RouteFilter {

    private static final String TopLevelDomain = "0";

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void doFilter(FilterContext<List<RouteBean>[], Object> ctx) {
        Map<String, Object> attr = ctx.attr();
        List<RouteBean>[] pram = ctx.getPram();
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
            }
        }
        //匹配目标服务
        Object service = attr.get(TargetService);
        if (service != null) {
            //根据TargetService匹配
            pram[0] = pram[0].stream()
                    .filter(e -> service.equals(e.getTag()))
                    .collect(Collectors.toList());
        }
        ctx.handleNext();
    }
}
