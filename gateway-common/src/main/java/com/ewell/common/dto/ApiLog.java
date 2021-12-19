package com.ewell.common.dto;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 22:56 2021-06-16
 * @Description: 日志
 * @Modified: By：
 */
@Data
@NoArgsConstructor
public class ApiLog implements MySerializable, Serializable {

    private Info info;

    /**
     * 结构: [reqLen][data][respLen][data][infoLen][data]
     */
    private ByteBuf buffer;

    @Override
    public void destructor() {
        if (buffer != null) {
            buffer.release();
        }
    }

    public ApiLog(byte[] data, int offset, int len) {
        ByteBuffer wrap = ByteBuffer.wrap(data, offset, len);
        Info info = new Info();
        info.setLogId(readStr(wrap));
        info.setTimestamp(wrap.getLong());
        info.setHandleTime(wrap.getLong());
        info.setPreTime(wrap.getLong());
        info.setPath(readStr(wrap));
        info.setUsername(readStr(wrap));
        info.setTargetUri(readStr(wrap));
        info.setOrgCode(readStr(wrap));
        info.setResponseCode(readStr(wrap));
        info.setToken(readStr(wrap));
        info.setRequestParams(readMap(wrap));
        info.setHeaders(readHeader(wrap));
        info.setRespHeaders(readHeader(wrap));
        this.info = info;
    }

    private void writeMap(Map<String, String> map) {
        if (map == null || map.size() == 0) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(map.size());
            map.forEach((k, v) -> {
                writeStr(k);
                writeStr(v);
            });
        }
    }

    private void writeHeader(HttpHeaders headers) {
        buffer.writeInt(headers.size());
        headers.forEach(e -> {
            writeStr(e.getKey());
            writeStr(e.getValue());
        });
    }

    private void writeLong(Long num) {
        buffer.writeLong(num);
    }

    private void writeStr(String str) {
        if (str == null) {
            buffer.writeInt(0);
        } else {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            buffer.writeInt(bytes.length);
            if (bytes.length > 0) {
                buffer.writeBytes(bytes);
            }
        }
    }

    private String readStr(ByteBuffer buffer) {
        int len = buffer.getInt();
        if (len == 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return new String(bytes);
    }

    public Map<String, String> readMap(ByteBuffer buffer) {
        int size = buffer.getInt();
        Map<String, String> res = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = readStr(buffer);
            String value = readStr(buffer);
            res.put(key, value);
        }
        return res;
    }

    public Map<String, String> readHeader(ByteBuffer buffer) {
        int size = buffer.getInt();
        Map<String, String> res = new CaseInsensitiveMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = readStr(buffer);
            String value = readStr(buffer);
            res.put(key, value);
        }
        return res;
    }


    @Override
    public ByteBuffer marshaller() {
        writeStr(info.logId);
        writeLong(info.timestamp);
        writeLong(info.handleTime);
        writeLong(info.preTime);
        writeStr(info.path);
        writeStr(info.username);
        writeStr(info.targetUri);
        writeStr(info.orgCode);
        writeStr(info.responseCode);
        writeStr(info.token);
        writeMap(info.requestParams);
        writeHeader(info.request.headers());
        writeHeader(info.response.headers());
        return buffer.nioBuffer();
    }

    /**
     * 详细信息
     */
    @Data
    @NoArgsConstructor
    public static class Info implements Serializable {

        public Info(DefaultHttpRequest request, DefaultHttpResponse response) {
            this.request = request;
            this.response = response;
            this.responseCode = response.status().code() + "";
            this.orgCode = request.headers().get("x-originCode");
            this.targetUri = request.uri();
        }

        private transient DefaultHttpRequest request;
        private transient DefaultHttpResponse response;

        private String logId;

        private long timestamp;
        /**
         * 总耗时
         */
        private long handleTime;

        /**
         * 网关处理耗时
         */
        private long preTime;

        /**
         * 网关原始的path
         */
        private String path;

        /**
         * 用户名
         */
        private String username;

        /**
         * 目标path
         */
        private String targetUri;

        private String orgCode;

        private String responseCode;

        /**
         * 短令牌
         */
        private String token;

        /**
         * query
         */
        private Map<String, String> requestParams;

        private Map<String, String> headers;

        private Map<String, String> respHeaders;

        private Object responseBody;

        private Object requestBody;
    }
}
