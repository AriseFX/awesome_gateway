package com.ewell.filters.cors;


import com.ewell.common.Headers;
import com.ewell.common.message.GatewayMessage;
import com.ewell.common.message.Message;
import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.core.filer.context.Observer;
import com.ewell.spi.Join;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import io.netty.handler.codec.http.*;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
 * @Author: wy
 * @Date: Created in 16:17 2021-06-29
 * @Description: 处理跨域
 * @Modified: By：
 */
@Join
public class HttpCorsFilter extends PreRouteFilter {

    private static final TimeBasedGenerator idGen = Generators.timeBasedGenerator();

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        //生成唯一追踪id
        String traceId = idGen.generate().toString();
        HttpRequest request = (HttpRequest) ((List<HttpObject>) data).get(0);
        HttpHeaders reqHeaders = request.headers();
        reqHeaders.set(Headers.LogId, traceId);
        String origin = reqHeaders.get("Origin");
        if (request.method() == HttpMethod.OPTIONS) {
            ctx.cancel(OPTIONS_RESP(origin));
            return;
        }
        ctx.addRespObserver(new Observer<>(1, message -> {
            HttpResponse response = (HttpResponse) message.getResponse().get(0);
            HttpHeaders respHeaders = response.headers();
            if (origin != null) {
                addCorsHeader(respHeaders, origin);
            }
        }));
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 2;
    }

    public static GatewayMessage OPTIONS_RESP(String origin) {
        List<HttpObject> objects = new ArrayList<>(2);
        DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, NO_CONTENT);
        //添加跨域头
        addCorsHeader(response.headers(), origin);
        DefaultLastHttpContent content = new DefaultLastHttpContent();
        objects.add(response);
        objects.add(content);
        return new GatewayMessage(objects);
    }

    public static void addCorsHeader(HttpHeaders headers, String origin) {
        headers.set("Access-Control-Allow-Origin", origin);
        headers.set("Access-Control-Allow-Methods", "*");
        headers.set("Access-Control-Max-Age", "3600");
        headers.set("Access-Control-Allow-Headers", "x-originCode,x-appType,x-appName,x-platform,x-longitude,x-latitude,authorization,x-clientIp,targetService,content-type,x-Encryption");
        headers.set("Access-Control-Allow-Credentials", "true");
    }

}