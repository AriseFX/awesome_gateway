package com.ewell.filters.encryption;

import com.ewell.common.message.ForwardMessage;
import com.ewell.core.filer.PreRouteFilter;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.core.filer.context.Observer;
import com.google.inject.Singleton;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

import java.util.List;

import static com.ewell.common.GatewayMessages.GATEWAY_ERROR;
import static com.ewell.filters.encryption.EncryptionUtils.encryptionBody;

/**
 * @Author: wy
 * @Date: Created in 10:36 上午 2021/12/3
 * @Description: 消息对称加密
 * @Modified: By：
 */
@Singleton
public class BodyEncryptionFilter extends PreRouteFilter {

    static String EncryptionHeader = "x-Encryption";

    @Override
    public void doFilter(FilterContext ctx, Object data) {
        List<HttpObject> p = (List<HttpObject>) data;
        HttpRequest request = (HttpRequest) p.get(0);
        HttpHeaders reqHeader = request.headers();
        if (!reqHeader.contains(EncryptionHeader)) {
            ctx.doNext(data);
            return;
        }
        reqHeader.remove(EncryptionHeader);
        if (reqHeader.contains("Content-Length")) {
            //请求体
            try {
                //解密
                encryptionBody(p, reqHeader, false);
            } catch (Exception e) {
                //解密失败
                ctx.cancel(GATEWAY_ERROR("无Content-Length请求头,无法支持解密"));
                return;
            }
        }
        ctx.addRespObserver(new Observer<>(127, message -> {
            if (message instanceof ForwardMessage) {
                List<HttpObject> objects = message.getResponse();
                DefaultHttpResponse response = (DefaultHttpResponse) objects.get(0);
                HttpHeaders respHeader = response.headers();
                String contentType = respHeader.get("Content-Type");
                if (!contentType.contains("json")) {
                    return;
                }
                //响应体
                encryptionBody(objects, respHeader, true);
                respHeader.remove("Content-Encoding");
                respHeader.remove("Transfer-Encoding");
                respHeader.set(EncryptionHeader, "true");
            }
        }));
        ctx.doNext(data);
    }

    @Override
    public byte order() {
        return 6;
    }
}
