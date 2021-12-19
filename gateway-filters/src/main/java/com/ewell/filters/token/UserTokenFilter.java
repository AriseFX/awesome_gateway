package com.ewell.filters.token;

import com.ewell.common.Headers;
import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.redis.AsyncRedisClient;
import com.ewell.spi.Join;
import com.google.inject.Inject;
import com.xcewell.esb.common.Token;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.collection.IntObjectHashMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.ewell.common.GatewayMessages.GATEWAY_ERROR;
import static com.ewell.common.IntMapConstant.*;
import static com.ewell.common.util.HttpUtils.parseQueryString;

/**
 * @Author: wy
 * @Date: Created in 10:59 上午 2021/12/3
 * @Description:
 * @Modified: By：
 */
@Join
@Slf4j
public class UserTokenFilter extends PreRouteFilter {

    @Inject
    private static AsyncRedisClient redisClient;

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        IntObjectHashMap<Object> attr = ctx.getAttr();
        URI uri = (URI) attr.get(_RequestURI);
        List<HttpObject> objects = (List<HttpObject>) data;
        HttpRequest request = (HttpRequest) objects.get(0);
        HttpHeaders headers = request.headers();
        //解析query参数
        Map<String, String> queryString = parseQueryString(uri.getQuery());
        //解析token
        String auth = headers.get("Authorization");
        final String[] originCode = new String[]{headers.get(Headers.OriginCode)};
        //解析目标服务
        attr.put(_ShortToken, auth);
        attr.put(_Backend, headers.get(Headers.Backend));
        attr.put(_OriginCode, originCode[0]);
        attr.put(_HttpQueryParam, queryString);
        attr.put(_Header, headers);
        if (auth != null && auth.length() > 0 && !auth.startsWith("Basic")) {
            RandomToken wrapToken = parseShortToken(auth);
            if (wrapToken != null) {
                //解密出用户名
                String username = wrapToken.getUsernameKey().split("-")[0];
                attr.put(_Username, username);
                redisClient.asyncExec((e, throwable) -> {
                            if (throwable != null) {
                                throwable.printStackTrace();
                                ctx.cancel(GATEWAY_ERROR("获取redis连接查询出错:" + throwable.getMessage()));
                            }
                            return e.get(wrapToken.getRedisKey()).whenComplete((v, ex) -> {
                                if (ex != null) {
                                    ex.printStackTrace();
                                    ctx.cancel(GATEWAY_ERROR("token查询出错:" + ex.getMessage()));
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
                                    if (originCode[0] == null && queryString != null) {
                                        originCode[0] = queryString.get(Headers.OriginCode);
                                    }
                                }
                                if (originCode[0] != null) {
                                    attr.put(_OriginCode, originCode[0]);
                                    headers.set("x-originCode", originCode[0]);
                                }
                                //获取token
                                ctx.doNext(data);
                            });
                        }
                );
                return;
            }
        }
        if (originCode[0] == null && queryString != null) {
            originCode[0] = queryString.get(Headers.OriginCode);
            if (originCode[0] != null) {
                attr.put(_OriginCode, originCode[0]);
                headers.set("x-originCode", originCode[0]);
            }
        }
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 4;
    }

    public static RandomToken parseShortToken(String shortToken) {
        try {
            String usernameShortToken = new String(Base64.getDecoder().decode(shortToken), StandardCharsets.UTF_8);
            if (usernameShortToken.length() > 0) {
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
