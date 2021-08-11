package com.arise.server.logging;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private HttpContent reqBody;
    private HttpContent respBody;

    private transient String reqBodyStr;
    private transient String respBodyStr;


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
