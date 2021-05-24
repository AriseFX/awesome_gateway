package com.arise.modules.http.constant;

import com.alibaba.fastjson.JSON;
import com.arise.modules.http.HttpServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.arise.modules.http.constant.HttpHeaderConstant.APPLICATION_JSON;
import static com.arise.modules.http.constant.HttpHeaderConstant.CONTENT_TYPE;
import static com.arise.modules.http.constant.HttpStatusConstant.INTERNAL_SERVER_ERROR;
import static com.arise.modules.http.constant.HttpStatusConstant.NOT_FOUND;
import static com.arise.modules.http.constant.HttpVersionConstant.*;

/**
 * @Author: wy
 * @Date: Created in 15:14 2021-05-24
 * @Description:
 * @Modified: By：
 */
public class StandardHttpResponse {

    public static final HttpServerResponse NotFound = new HttpServerResponse(HTTP_1_1, NOT_FOUND);

    static {
        NotFound.getHttpHeaders().put(CONTENT_TYPE, APPLICATION_JSON);
        cacheBody(NotFound, new HashMap<String, Object>() {
            {
                put("msg", "gateway route not found");
            }
        });
    }

    public static final HttpServerResponse ServerError = new HttpServerResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);

    static {
        ServerError.getHttpHeaders().put(CONTENT_TYPE, APPLICATION_JSON);
        cacheBody(ServerError, new HashMap<String, Object>() {
            {
                put("msg", "gateway route error");
            }
        });
    }

    private static void cacheBody(HttpServerResponse response, Map<String, Object> map) {
        byte[] body = JSON.toJSONString(map).getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.directBuffer(body.length);
        byteBuf.writeBytes(body);
        response.setBody(byteBuf);
        response.cache();
    }


}
