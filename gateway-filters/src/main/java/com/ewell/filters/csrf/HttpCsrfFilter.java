package com.ewell.filters.csrf;

import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

import java.util.ArrayList;
import java.util.List;

import static com.ewell.common.GatewayMessages.REQUEST_ERROR;

/**
 * @Author: wy
 * @Date: Created in 5:02 下午 2021/12/5
 * @Description:
 * @Modified: By：
 */
public class HttpCsrfFilter extends PreRouteFilter {
    @Override
    public void doFilter(FilterContext ctx, Object data) {
        HttpRequest request = (HttpRequest) ((List<HttpObject>) data).get(0);
        String referer = request.headers().get("Referer");
        if (referer != null) {
            if (list.stream().noneMatch(referer::startsWith)) {
                ctx.cancel(REQUEST_ERROR("不允许的Referer"));
                return;
            }
        }
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 1;
    }

    public static List<String> list = new ArrayList<String>() {
        {
            add("https://dev.wiseheartdoctor.cn/");
            add("https://test.wiseheartdoctor.cn/");
            add("https://uat.wiseheartdoctor.cn/");
            add("https://xinchang.wiseheartdoctor.cn/");
            add("https://mapi.zjzwfw.gov.cn/");
            add("https://dev-ggws.wiseheartdoctor.cn/");
            add("http://localhost:8080/");
            add("https://test-ggws.wiseheartdoctor.cn/");
            add("https://uat-ggws.wiseheartdoctor.cn/");
            add("https://dev-ggws.wiseheartdoctor.cn/");
            add("https://testsp.wiseheartdoctor.cn/");
            add("https://testsp.wiseheartdoctor.cn/");
            add("https://dev-bjeb.wiseheartdoctor.cn/");
            add("https://test-bjeb.wiseheartdoctor.cn/");
            add("https://bjeb.wiseheartdoctor.cn/");
            add("https://znfuture.wiseheartdoctor.cn/");
        }
    };
}
