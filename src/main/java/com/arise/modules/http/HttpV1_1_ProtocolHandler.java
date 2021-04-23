package com.arise.modules.http;

import com.arise.internal.chain.ChainContext;
import com.arise.modules.ProtocolHandler;
import io.netty.channel.unix.FileDescriptor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.arise.modules.http.HttpV1_1_ProtocolHandler.BodyState.FIX_SIZE_BODY;
import static com.arise.modules.http.HttpV1_1_ProtocolHandler.State.*;

/**
 * @Author: wy
 * @Date: Created in 16:11 2021-02-22
 * @Description: 解析http协议
 * @Modified: By：
 */
public class HttpV1_1_ProtocolHandler implements ProtocolHandler {

    private State currentState = REQUEST_STATUS;

    private int chunkSize;

    private BodyState bodyState = null;

    private final HashMap<String, String> headers = new HashMap<>(5);

    private final HttpServerRequest request = HttpServerRequest.builder().build();

    private final List<ByteBuffer> body = new LinkedList<>();

    private CharactersLine old;

    private final char CR = '\r';

    private final char LF = '\n';

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
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            try {
                int num = fd.read(buffer, 0, buffer.limit());
                if (num > 0) {
                    buffer.limit(num);
                    //内部协议处理
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
    public void innerHandleRequest(ByteBuffer buffer) {
        switch (currentState) {
            case REQUEST_STATUS: {
                CharactersLine line = parseLine(buffer);
                if (line == null) {
                    return;
                }
                String[] token = line.getNewString().split(" ");
                if (token.length != 3) {
                    return;
                }
                request.methodName = token[0];
                request.url = token[1];
                request.httpVersion = token[2].trim();
                currentState = REQUEST_HEADERS;
            }
            case REQUEST_HEADERS: {
                Map<String, String> map = readHeader(buffer);
                if (map != null) {
                    request.headers = map;
                    //获取下一个状态
                    String length = map.get(HttpConstant.Http_Content_Length);
                    if (length != null) {
                        int l = Integer.parseInt(length);
                        if (l > 0) {
                            request.contentLength = l;
                            this.chunkSize = l;
                            this.bodyState = FIX_SIZE_BODY;
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
                /*if (bodyState == FIX_SIZE_BODY) {
                    int toRead = buffer.remaining();
                    if (toRead > chunkSize) {
                        toRead = chunkSize;
                    }
                    if (toRead == 0) {
                        return;
                    }
                    //获取body的切片，后续将切片组合
                    buffer.limit(buffer.position() + toRead);
                    ByteBuffer slice = buffer.slice();
                    body.add(slice);
                    chunkSize -= toRead;
                    if (chunkSize <= 0) {
                        request.content = body;
                        currentState = REQUEST_DONE;
                    } else {
                        return;
                    }
                }*/
            }
        }
    }

    @Override
    public void handleResponse(ChainContext ctx, Object msg) {

    }

    /**
     * 读取头部
     */
    @SneakyThrows
    private Map<String, String> readHeader(ByteBuffer buffer) {
        while (buffer.remaining() > 0) {
            CharactersLine line = parseLine(buffer);
            if (line != null) {
                String[] token = line.getNewString().split(":");
                switch (token.length) {
                    case 2:
                        headers.put(token[0], token[1].trim());
                        break;
                    case 1:
                        if (token[0].charAt(0) == CR && token[0].charAt(1) == LF) {
                            return headers;
                        }
                        return null;
                }
            }
        }
        return null;
    }

    private byte lastTimeByte = 0;

    /**
     * 解析
     */
    public CharactersLine parseLine(ByteBuffer buffer) {
        int remaining = buffer.remaining();
        byte[] requestLine = new byte[remaining];
        for (int i = 0; buffer.hasRemaining(); i++) {
            byte b = buffer.get();
            requestLine[i] = b;
            if ((char) (b & 0xFF) == LF && (char) (lastTimeByte & 0xFF) == CR) {
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
            lastTimeByte = b;
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