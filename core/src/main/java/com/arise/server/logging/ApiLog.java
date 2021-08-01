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

    /**
     * id结构如下:
     * 85899345921 [1234][1234]
     * 92233720368  5477  5807
     * [offset][len1][len2]
     */
    private long id;
    private long timestamp;

    private DefaultHttpRequest req;
    private HttpContent reqBody;
    private String reqBodyStr;
    private DefaultHttpResponse resp;
    private HttpContent respBody;
    private String respBodyStr;
}
