package com.arise.modules.http;

import com.arise.modules.Bufferable;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;

import java.nio.ByteBuffer;

import static com.arise.server.AwesomeEventLoop.Allocator;

/**
 * @Author: wy
 * @Date: Created in 16:29 2021-02-22
 * @Description: 仅限于内部使用，不是完整的http
 * @Modified: By：
 */
public class HttpServerRequest implements Bufferable {

    public AsciiString method;

    public AsciiString url;

    public AsciiString httpVersion;

    public HttpHeaders headers;

    public ByteBuf partContent;

    public int contentLength;

    public void setMethod(byte[] v, int start, int len) {
        this.method = new AsciiString(v, start, len, true);
    }

    public void setUrl(byte[] v, int start, int len) {
        this.url = new AsciiString(v, start, len, true);
    }

    public void setHttpVersion(byte[] v, int start, int len) {
        this.httpVersion = new AsciiString(v, start, len, true);
    }


    @Override
    public String toString() {
        return "methodName:" + method + " url:" + url;
    }

    @Override
    public ByteBuffer toBuffer() {
        //TODO 需要动态生成一个url
        int len = method.length() + url.length() + httpVersion.length() + headers.getMsgLen() + 6;
        if (partContent != null) {
            len += partContent.readableBytes();
        }
        ByteBuf buffer = Allocator.directBuffer(len);
        buffer.writeBytes(method.array());
        buffer.writeByte(' ');
        buffer.writeBytes(url.array());
        buffer.writeByte(' ');
        buffer.writeBytes(httpVersion.array());
        buffer.writeByte('\r');
        buffer.writeByte('\n');
        headers.forEach((k, v) -> {
            buffer.writeBytes(k.array());
            buffer.writeByte(':');
            buffer.writeBytes(v.array());
            buffer.writeByte('\r');
            buffer.writeByte('\n');
        });
        buffer.writeByte('\r');
        buffer.writeByte('\n');
        if (partContent != null) {
            buffer.writeBytes(partContent);
        }
        return buffer.nioBuffer(0, buffer.writerIndex());
    }
}
