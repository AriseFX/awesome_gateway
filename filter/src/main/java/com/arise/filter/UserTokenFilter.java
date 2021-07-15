package com.arise.filter;

import com.arise.redis.AsyncRedisClient;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.PreRouteFilter;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.arise.internal.util.HttpUtils.parseQueryString;

/**
 * @Author: wy
 * @Date: Created in 16:17 2021-06-29
 * @Description: 处理用户域
 * @Modified: By：
 */
@Component
public class UserTokenFilter extends PreRouteFilter {

    @Resource
    private AsyncRedisClient redisClient;

    public static String OriginCode = "x-originCode";

    public static String HttpQueryParam = "httpQueryParam";

    public static String RequestURI = "RequestURI";

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void doFilter(FilterContext<List<HttpObject>, Object> ctx) {
        HttpRequest request = (HttpRequest) ctx.getPram().get(0);
        HttpHeaders headers = request.headers();
        URI uri = URI.create(request.uri());
        //解析query参数
        Map<String, String> queryString = parseQueryString(uri.getQuery());
        redisClient.commands().get("WY").whenCompleteAsync((v, ex) -> {
            if (ex != null) {
                ctx.getCallback().setFailure(ex);
                return;
            }
            //假装获取到了token
            String originCode = headers.get(OriginCode);
            if (originCode == null) {

            }
            Map<String, Object> attr = ctx.attr();
            attr.put(OriginCode, originCode);
            attr.put(HttpQueryParam, queryString);
            attr.put(RequestURI, uri);
            //获取token
            ctx.getCallback().setSuccess(null);
        });
    }
}