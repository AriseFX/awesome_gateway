package com.arise.modules.http;

import com.arise.modules.Bufferable;
import lombok.Builder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 16:29 2021-02-22
 * @Description: 仅限于内部使用，不是完整的http
 * @Modified: By：
 */
@Builder
public class HttpServerRequest implements Bufferable {

    public String methodName;

    public Map<String, String> headers;

    public String url;

    public String httpVersion;

    public List<ByteBuffer> content;

    public ByteBuffer partContent;

    public int contentLength;

    @Override
    public String toString() {
        return "methodName:" + methodName + " url:" + url;
    }

    @Override
    public ByteBuffer toBuffer() {
        //TODO 需要动态生成一个url
        ByteBuffer buffer = ByteBuffer.allocateDirect(20480);
        buffer.put(methodName.getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) ' ');
        buffer.put(url.getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) ' ');
        buffer.put(httpVersion.getBytes(StandardCharsets.UTF_8));
        buffer.put((byte)'\r');
        buffer.put((byte)'\n');
        headers.forEach((k, v) -> {
            buffer.put(k.getBytes(StandardCharsets.UTF_8));
            buffer.put((byte)':');
            buffer.put(v.getBytes(StandardCharsets.UTF_8));
            buffer.put((byte)'\r');
            buffer.put((byte)'\n');
        });
        buffer.put((byte)'\r');
        buffer.put((byte)'\n');
        if (contentLength != 0) {
            buffer.put(partContent);
        }
        return buffer;
    }
}
