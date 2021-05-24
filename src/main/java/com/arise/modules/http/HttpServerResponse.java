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
public class HttpServerResponse implements Bufferable {

    private final AsciiString httpVersion;

    private final AsciiString respCode;

    public HttpHeaders headers;

    public ByteBuf fullContent;

    private volatile ByteBuffer cache;

    public HttpServerResponse(AsciiString httpVersion, AsciiString respCode) {
        this.headers = new HttpHeaders();
        this.httpVersion = httpVersion;
        this.respCode = respCode;
    }

    public void setBody(ByteBuf body) {
        this.fullContent = body;
    }

    public HttpHeaders getHttpHeaders() {
        return this.headers;
    }

    public ByteBuffer cache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    this.cache = toBuffer();
                }
            }
        }
        return this.cache;
    }

    @Override
    public ByteBuffer toBuffer() {
        //TODO 需要动态生成一个url
        int len = respCode.length() + httpVersion.length() + headers.getMsgLen() + 6;
        if (fullContent != null) {
            len += fullContent.readableBytes();
        }
        ByteBuf buffer = Allocator.directBuffer(len);
        buffer.writeBytes(httpVersion.array());
        buffer.writeByte(' ');
        buffer.writeBytes(respCode.array());
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
        if (fullContent != null) {
            buffer.writeBytes(fullContent);
        }
        return buffer.nioBuffer(0, buffer.writerIndex());
    }

    @Override
    public String toString() {
        return "HttpServerResponse:" + respCode.toString();
    }
}
