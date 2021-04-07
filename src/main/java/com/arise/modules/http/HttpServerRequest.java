package com.arise.modules.http;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 16:29 2021-02-22
 * @Description:
 * @Modified: By：
 */
public class HttpServerRequest {

    public String methodName;

    public Map<CharSequence, String> headers;

    public String url;

    public String httpVersion;

    public List<ByteBuffer> content;

    public int contentLength;

    @Override
    public String toString() {
        return "MethodName:" + methodName + " url:" + url;
    }
}
