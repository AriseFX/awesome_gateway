package com.arise.modules.http;

import com.arise.internal.chain.ChainContext;
import com.arise.modules.ProtocolHandler;
import com.arise.modules.http.constant.HttpHeaderConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.AsciiString;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.arise.modules.http.HttpV1_1_ProtocolHandler.State.*;
import static com.arise.server.AwesomeEventLoop.Allocator;

/**
 * @Author: wy
 * @Date: Created in 16:11 2021-02-22
 * @Description: 解析http协议
 * @Modified: By：
 */
public class HttpV1_1_ProtocolHandler implements ProtocolHandler {

    private State currentState = REQUEST_STATUS;

    private BodyState bodyState = null;

    private final HttpHeaders headers = new HttpHeaders();

    private final HttpServerRequest request = new HttpServerRequest();

    private CharactersLine old;

    private final byte CR = '\r';

    private final byte LF = '\n';

    enum State {
        //请求状态
        REQUEST_STATUS,
        //请求头
        REQUEST_HEADERS,
        //请求体
        REQUEST_BODY,
        //完成
        REQUEST_DONE
    }

    enum BodyState {
        //固定大小的body
        FIX_SIZE_BODY
    }

    @Override
    public void handleRequest(ChainContext ctx, Object msg) {
        FileDescriptor fd = ctx.getCurrentFd();
        do {
            //读完header使用splice
            ByteBuf buffer = Allocator.directBuffer(256);
            int writerIndex = buffer.writerIndex();
            ByteBuffer nioBuffer = buffer.nioBuffer(writerIndex, buffer.writableBytes());
            try {
                int num = fd.read(nioBuffer, nioBuffer.position(), buffer.writableBytes());
                if (num > 0) {
                    //内部协议处理
                    buffer.writerIndex(writerIndex + num);
                    innerHandleRequest(buffer);
                    if (currentState == REQUEST_DONE) {
                        request.partContent = buffer;
                        break;
                    }
                }
                if (num <= 0) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (true);
        ctx.fireNextReadHandler(ctx, request);
    }

    @SneakyThrows
    public void innerHandleRequest(ByteBuf buffer) {
        switch (currentState) {
            case REQUEST_STATUS: {
                CharactersLine line = parseLine(buffer);
                if (line == null) {
                    return;
                }
                int index1 = line.findByteIndex((byte) ' ');
                int index2 = line.findByteIndex((byte) ' ');
                if (index1 == -1 || index2 == -1) {
                    return;
                }
                //TODO 是否原地取字符串? 真的要我
                byte[] innerData = line.getInnerData();
                request.setMethod(innerData, 0, index1 - 1);
                request.setUrl(innerData, index1, index2 - index1 - 1);
                request.setHttpVersion(innerData, index2, innerData.length - index2 - 2);
                currentState = REQUEST_HEADERS;
            }
            case REQUEST_HEADERS: {
                HttpHeaders map = readHeader(buffer);
                if (map != null) {
                    request.headers = map;
                    //获取下一个状态
                    AsciiString len = map.getHeader(HttpHeaderConstant.CONTENT_LENGTH);
                    if (len != null) {
                        int l = len.parseInt();
                        if (l > 0) {
                            request.contentLength = l;
                            this.bodyState = BodyState.FIX_SIZE_BODY;
                        }
                    }
                    currentState = REQUEST_BODY;
                } else {
                    return;
                }
            }
            case REQUEST_BODY: {
                //TODO 要支持chunk类型消息体
                //如果当前内核支持就直接让splice接管body
                //TODO  多版本内核支持
                currentState = REQUEST_DONE;
            }
        }
    }

    /**
     * 读取头部
     */
    @SneakyThrows
    private HttpHeaders readHeader(ByteBuf buffer) {
        while (buffer.readableBytes() > 0) {
            CharactersLine line = parseLine(buffer);
            if (line != null) {
                byte[] innerData = line.getInnerData();
                int index = line.findByteIndex((byte) ':');
                if (index == -1) {
                    if (innerData[0] == CR && innerData[1] == LF) {
                        return headers;
                    }
                    return null;
                } else {
                    headers.addHeader(new AsciiString(innerData, 0, index - 1, true)
                            , new AsciiString(innerData, index + 1, innerData.length - index - 3, true));
                }
            }
        }
        return null;
    }

    private byte lastByte = 0;

    /**
     * 解析
     */
    public CharactersLine parseLine(ByteBuf buffer) {
        int remaining = buffer.readableBytes();
        byte[] requestLine = new byte[remaining];
        for (int i = 0; buffer.isReadable(); i++) {
            byte b = buffer.readByte();
            requestLine[i] = b;
            if ((char) (b & 0xFF) == LF && (char) (lastByte & 0xFF) == CR) {
                //找到一行结束
                if (old != null) {
                    //有旧的，累加
                    CharactersLine line = old.appendCharacters(requestLine, i + 1);
                    old = null;
                    return line;
                } else {
                    return new CharactersLine(requestLine, i + 1);
                }
            }
            lastByte = b;
        }
        //遍历完都没找到一行
        if (old != null) {
            old.appendCharacters(requestLine, remaining);
        } else {
            old = new CharactersLine(requestLine, remaining);
        }
        return null;
    }
}
