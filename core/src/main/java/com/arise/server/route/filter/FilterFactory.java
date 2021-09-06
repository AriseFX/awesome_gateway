package com.arise.server.route.filter;

import com.arise.server.route.ApiRouteHandler;
import com.arise.server.route.match.RouteMatcher;
import com.arise.spi.ExtensionLoader;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.arise.server.route.filter.Lifecycle.*;

/**
 * @Author: wy
 * @Date: Created in 2:23 下午 2021/8/31
 * @Description:
 * @Modified: By：
 */
@SuppressWarnings("all")
public class FilterFactory {

    public static void init() {
        ExtensionLoader<Filter> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        List<Filter> allJoin = extensionLoader.getAllJoin();
        Map<Lifecycle, List<Filter>> collect =
                allJoin.stream().collect(Collectors.groupingBy(Filter::lifecycle));
        ApiRouteHandler.preRouteFilters =
                sort(collect.getOrDefault(PreRoute, Collections.emptyList()));
        ApiRouteHandler.forwardFilters =
                sort(collect.getOrDefault(Forward, Collections.emptyList()));
        RouteMatcher.routeFilters =
                sort(collect.getOrDefault(Route, Collections.emptyList()));
    }

    private static List<Filter> sort(List<Filter> filters) {
        return filters.stream()
                .sorted(Comparator.comparing(Filter::order))
                .collect(Collectors.toList());
    }
}
