package com.arise.server.route.logging;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.arise.base.config.Components;
import com.arise.rabbitmq.PooledRabbitmqClient;
import com.rabbitmq.client.Channel;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-20
 * @Description: 日志相关
 * @Modified: By：
 */
@Slf4j
public class AweLogService {

    private static final PooledRabbitmqClient POOLED_RABBITMQ_CLIENT =
            Components.get(PooledRabbitmqClient.class);

    private static final FastThreadLocal<Channel> localChannel = new FastThreadLocal<Channel>() {
        @Override
        protected Channel initialValue() throws Exception {
            return POOLED_RABBITMQ_CLIENT.newConnection().createChannel();
        }
    };

    public static void pushLog(ApiLog log) {
        try {
            Channel channel = localChannel.get();
            RequestLogEntity entity = map2Entity(log);
            channel.basicPublish("", "gateway-queue", null,
                    JSON.toJSONString(entity).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void alarm(AlarmDto dto) {
        try {
            Channel channel = localChannel.get();
            channel.basicPublish("", "gateway-alarm-queue", null,
                    JSON.toJSONString(dto).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 转换为运维中心需要的日志格式
     */
    public static RequestLogEntity map2Entity(ApiLog log) {
        ApiLog.Info info = log.getInfo();
        DefaultHttpRequest req = info.getReq();
        DefaultHttpResponse resp = info.getResp();
        HttpHeaders headers = req.headers();
        Map<String, Object> map = new HashMap<>();
        headers.forEach(e -> map.put(e.getKey(), e.getValue()));
        //构造运维中心日志
        RequestLogEntity entity = new RequestLogEntity();
        //req
        entity.setLogId(info.getLogId());
        entity.setPath(info.getPath());
        entity.setTimestamp(info.getTimestamp());
        entity.setHandleTime(info.getHandleTime());
        entity.setHeaders(map);
        entity.setResponseCode(resp.status().code() + "");
        entity.setOrgCode(headers.get("x-originCode"));
        entity.setTargetUri(req.uri());
        ByteBuf body_req = log.getBody_req();
        ByteBuf body_resp = log.getBody_resp();
        if (body_req != null) {
            byte[] array = body_req.array();
            int offset = body_req.arrayOffset();
            String type = headers.get(HttpHeaderNames.CONTENT_TYPE);
            //TODO抽取逻辑
            if (type != null && type.contains("application/json")) {
                entity.setRequestBody(JSON.parse(new String(array, offset,
                        body_req.writerIndex())));
            } else {
                JSONObject json = new JSONObject();
                json.put("data", new String(array, offset,
                        body_req.writerIndex()));
                entity.setRequestBody(json);
            }
        }
        if (body_resp != null) {
            HttpHeaders respHeader = resp.headers();
            String encoding = respHeader.get(HttpHeaderNames.CONTENT_ENCODING);
            String type = respHeader.get(HttpHeaderNames.CONTENT_TYPE);
            byte[] array = body_resp.array();
            String bodyStr;
            try {
                if (encoding != null && encoding.contains("gzip")) {
                    bodyStr = unCompressGzip(array, body_resp.arrayOffset(),
                            body_resp.writerIndex());
                } else {
                    bodyStr = new String(array, body_resp.arrayOffset(),
                            body_resp.writerIndex());
                }
            } finally {
                body_resp.release();
            }
            if (type != null && type.contains("application/json")) {
                entity.setResponseBody(JSON.parse(bodyStr));
            } else {
                JSONObject json = new JSONObject();
                json.put("data", bodyStr);
                entity.setResponseBody(json);
            }
        }
        entity.setUsername(info.getUsername());
        entity.setPreTime(info.getPreTime());
        entity.setHandleTime(info.getHandleTime());
        entity.setRequestParams(info.getQueryPram());
        entity.setToken(info.getToken());
        return entity;
    }

    public static String unCompressGzip(byte[] in, int offset, int len) {
        ByteArrayOutputStream out = null;
        GZIPInputStream gunzip = null;
        try {
            if (in == null || len == 0) {
                return "";
            }
            out = new ByteArrayOutputStream();
            gunzip = new GZIPInputStream(new ByteArrayInputStream(in, offset, len));
            byte[] buffer = new byte[1024];
            int n;
            while ((n = gunzip.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            out.flush();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(e.getMessage());
            return "";
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (gunzip != null) {
                    gunzip.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

}
