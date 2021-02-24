package com.arise.modules.http;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 16:29 2021-02-22
 * @Description:
 * @Modified: By：
 */
public class HttpServerRequest {

    public String methodName;

    public Map<String, Object> headers;

    public String url;

    public String httpVersion;

    public ByteBuffer content;

    public int contentLength;
}
