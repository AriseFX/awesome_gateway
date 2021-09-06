package com.arise.server.route.logging;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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

    private ByteBuf body_req;
    private ByteBuf body_resp;

    public void free() {
        if (body_req != null) {
            body_req.release();
        }
        if (body_resp != null) {
            body_resp.release();
        }
    }

    /**
     * 详细信息
     */
    @Data
    public static class Info {

        private String logId;

        private Long timestamp;
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

        /**
         * query
         */
        private Map<String,String> queryPram;

        private DefaultHttpRequest req;

        private DefaultHttpResponse resp;
    }

}
