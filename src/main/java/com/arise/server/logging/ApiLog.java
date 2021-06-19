package com.arise.server.logging;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: wy
 * @Date: Created in 22:56 2021-06-16
 * @Description: 日志
 * @Modified: By：
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiLog {

    private long num;

    private String url;

    private HttpHeaders reqHeaders;

    private ByteBuf reqBody;

    private int status;

    private HttpHeaders respHeaders;

    private ByteBuf respBody;

}
