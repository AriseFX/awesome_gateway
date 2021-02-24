package com.arise.modules.http;

import com.arise.modules.EventHandler;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.arise.modules.http.HttpProtocolHandler.CurrentState.*;

/**
 * @Author: wy
 * @Date: Created in 16:11 2021-02-22
 * @Description: 解析http协议
 * @Modified: By：
 */
public class HttpProtocolHandler implements EventHandler {

    private CurrentState currentState = REQUEST_STATUS;

    private CharactersLine old;

    private final char CR = '\r';

    private final char LF = '\n';

    enum CurrentState {
        //请求状态
        REQUEST_STATUS,
        //请求头
        REQUEST_HEADERS,
        //请求体
        REQUEST_BODY,
        //完成
        REQUEST_DONE
    }

    /**
     * REQUEST_STATUS -> REQUEST_HEADERS -> REQUEST_BODY -> REQUEST_DONE
     */
    @SneakyThrows
    public HttpServerRequest parser(ByteBuffer buffer) {
        HttpServerRequest request = new HttpServerRequest();
        switch (currentState) {
            case REQUEST_STATUS: {
                CharactersLine line = parseLine(buffer);
                if (line == null) {
                    return null;
                }
                String[] token = line.getNewString().split(" ");
                if (token.length != 3) {
                    return null;
                }
                request.methodName = token[0];
                request.url = token[1];
                request.httpVersion = token[2].trim();
                currentState = REQUEST_HEADERS;
            }
            case REQUEST_HEADERS: {
                Map<String, Object> map = readHeader(buffer);
                if (map != null) {
                    System.out.println("请求头: " + map);
                    request.headers = map;
                    currentState = REQUEST_BODY;
                } else {
                    return null;
                }
            }
            case REQUEST_BODY: {

            }
            case REQUEST_DONE: {

            }
        }
        return request;
    }

    /**
     * 读取头部
     */
    @SneakyThrows
    private Map<String, Object> readHeader(ByteBuffer buffer) {
        HashMap<String, Object> headers = new HashMap<>();
        while (buffer.remaining() > 0) {
            CharactersLine line = parseLine(buffer);
            String[] token = line.getNewString().split(":");
            switch (token.length) {
                case 2:
                    headers.put(token[0], token[1].trim());
                    break;
                case 1:
                    if (token[0].charAt(0) == CR) {
                        return headers;
                    }
                    return null;
            }
        }
        return null;
    }

    private byte lastTimeByte = 0;

    /**
     * 解析
     */
    public CharactersLine parseLine(ByteBuffer buffer) {
        byte[] requestLine = new byte[buffer.remaining()];
        for (int i = 0; buffer.hasRemaining(); i++) {
            byte b = buffer.get();
            requestLine[i] = b;
            if ((char) (b & 0xFF) == LF && (char) (lastTimeByte & 0xFF) == CR) {
                //找到一行结束
                if (old != null) {
                    //有旧的，累加
                    CharactersLine line = old.appendCharacters(requestLine);
                    old = null;
                    return line;
                } else {
                    return new CharactersLine(requestLine);
                }
            }
            lastTimeByte = b;
        }
        if (old != null) {
            old.appendCharacters(requestLine);
        } else {
            old = new CharactersLine(requestLine);
        }
        return null;
    }
}
