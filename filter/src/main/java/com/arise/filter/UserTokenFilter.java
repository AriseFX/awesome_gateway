package com.arise.filter;

import com.arise.redis.AsyncRedisClient;
import com.arise.server.route.filter.FilterContext;
import com.arise.server.route.filter.PreRouteFilter;
import com.xcewell.esb.common.Token;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.arise.internal.util.HttpUtils.parseQueryString;
import static com.arise.server.route.ApiRouteHandler.RequestURI;

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

    public static String TargetService = "TargetService";

    public static String HttpQueryParam = "httpQueryParam";

    public static String FullToken = "FullToken";

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void doFilter(FilterContext<List<HttpObject>, Object> ctx) {
        Map<String, Object> attr = ctx.attr();
        HttpRequest request = (HttpRequest) ctx.getPram().get(0);
        HttpHeaders headers = request.headers();
        URI uri = (URI) attr.get(RequestURI);
        //解析query参数
        Map<String, String> queryString = parseQueryString(uri.getQuery());
        //解析token
        String auth = headers.get("Authorization");
        final String[] originCode = new String[]{headers.get(OriginCode)};
        //解析目标服务
        attr.put(TargetService, headers.get(TargetService));
        attr.put(OriginCode, originCode[0]);
        attr.put(HttpQueryParam, queryString);
        attr.put(RequestURI, uri);
        if (StringUtils.hasLength(auth)) {
            RandomToken wrapToken = parseShortToken(auth);
            if (wrapToken != null) {
                redisClient.commands().get(wrapToken.getRedisKey()).whenCompleteAsync((v, ex) -> {
                    if (ex != null) {
                        fail(ctx, ex);
                        return;
                    }
                    if (v != null) {
                        Token token = (Token) v;
                        String accessToken = token.getAccessToken();
                        headers.set("Token", auth);//短令牌
                        headers.set("Authorization", "Bearer " + accessToken);//长令牌
                        if (originCode[0] == null) {
                            originCode[0] = token.getOriginCode();
                        }
                        if (originCode[0] == null) {
                            originCode[0] = queryString.get(OriginCode);
                        }
                    }
                    attr.put(OriginCode, originCode[0]);
                    headers.set("x-originCode", originCode[0]);
                    //获取token
                    success(ctx);
                });
            } else {
                success(ctx);
            }
        } else {
            success(ctx);
        }
    }

    private static void success(FilterContext<List<HttpObject>, Object> ctx) {
        ctx.getCallback().setSuccess(null);
    }

    private static void fail(FilterContext<List<HttpObject>, Object> ctx, Throwable cause) {
        ctx.getCallback().setFailure(cause);
    }

    public static RandomToken parseShortToken(String shortToken) {
        try {
            String usernameShortToken = new String(Base64.getDecoder().decode(shortToken), StandardCharsets.UTF_8);
            if (StringUtils.hasLength(usernameShortToken)) {
                String[] split = usernameShortToken.split(":");
                if (split.length == 4 && "EWELL".equals(split[0])) {
                    return new RandomToken(split[1], split[2], split[3]);
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    public static class RandomToken {
        private String usernameKey;
        private String randomToken;
        private String redisKey;
        private String originCode;

        public RandomToken(String usernameKey, String randomToken, String originCode) {
            this.originCode = originCode;
            this.usernameKey = usernameKey;
            this.randomToken = randomToken;
            this.redisKey = usernameKey + "-" + randomToken;
        }
    }
}