package com.arise.server.logging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

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
    private ByteBuffer[] req;
    private ByteBuffer[] resp;

    public ApiLog(ByteBuffer[] req, ByteBuffer[] resp) {
        this.timestamp = System.currentTimeMillis();
        this.req = req;
        this.resp = resp;
    }

}
