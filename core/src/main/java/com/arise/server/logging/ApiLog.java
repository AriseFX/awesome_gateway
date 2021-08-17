package com.arise.server.logging;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 22:56 2021-06-16
 * @Description: 日志
 * @Modified: By：
 */
@Data
@NoArgsConstructor
public class ApiLog {

    private Info info = new Info();
    private List<HttpContent> reqBody;
    private List<HttpContent> respBody = new LinkedList<>();

    private transient String reqBodyStr;
    private transient String respBodyStr;

    private int reqBodyLen;
    private int respBodyLen;

    public void addReqBody(HttpContent body) {
        if (reqBody == null) {
            reqBody = new LinkedList<>();
        }
        reqBodyLen += body.content().readableBytes();
        reqBody.add(body);
    }

    public void addRespBody(HttpContent body) {
        respBodyLen += body.content().readableBytes();
        respBody.add(body);
    }


    /**
     * 详细信息
     */
    @Data
    public static class Info {

        private String logId;

        private long timestamp;
        /**
         * 总耗时
         */
        private Long handleTime;

        /**
         * 网关处理耗时
         */
        private Long preTime;

        /**
         * 网关原始的path
         */
        private String path;

        /**
         * 用户名
         */
        private String username;

        private DefaultHttpRequest req;

        private DefaultHttpResponse resp;
    }

}
