package com.arise.server.route.logging;

import com.alibaba.fastjson.JSON;
import com.arise.base.config.Components;
import com.arise.rabbitmq.RabbitmqClient;
import com.rabbitmq.client.Channel;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.GZIPInputStream;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-20
 * @Description: 日志相关
 * @Modified: By：
 */
@Slf4j
public class AweLogService {

    private static final RabbitmqClient rabbitmqClient = Components.get(RabbitmqClient.class);

    private static final MpscArrayQueue<ApiLog> logQueue = new MpscArrayQueue<>(4000);

    public static void pushLog(ApiLog log) {
        logQueue.offer(log);
    }

    private static final long pause = TimeUnit.MILLISECONDS.toNanos(200);

    public static Thread consumer = new Thread(new Runnable() {

        private final Channel channel = rabbitmqClient.getChannel();

        {
            try {
                channel.queueDeclare("gateway-queue", true,
                        false, false, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                ApiLog polled = logQueue.relaxedPoll();
                if (polled == null) {
                    LockSupport.parkNanos(pause);
                } else {
                    RequestLogEntity entity = map2Entity(polled);
                    try {
                        channel.basicPublish("", "gateway-queue", null,
                                JSON.toJSONString(entity).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }, "consumer");

    static {
        consumer.start();
    }

    private static final byte[] heapBuffer = new byte[20480];

    /**
     * 转换为运维中心需要的日志格式
     */
    private static RequestLogEntity map2Entity(ApiLog log) {
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
            int len = body_req.readableBytes();
            body_req.getBytes(0, body_resp, 0, body_req.readableBytes());
            body_req.release();
            entity.setRequestBody(JSON.parseObject(new String(heapBuffer, 0, len)));
        }
        if (body_resp != null) {
            int len = body_resp.readableBytes();
            String encoding = resp.headers().get(HttpHeaderNames.CONTENT_ENCODING);
            body_resp.getBytes(0, heapBuffer, 0, len);
            body_resp.release();
            String bodyStr;
            if ("gzip".equals(encoding)) {
                bodyStr = unCompressGzip(heapBuffer, len);
            } else {
                bodyStr = new String(heapBuffer, 0, len);
            }
            entity.setResponseBody(JSON.parseObject(bodyStr));
        }
        entity.setUsername(info.getUsername());
        entity.setPreTime(info.getPreTime());
        entity.setHandleTime(info.getHandleTime());
        entity.setRequestParams(null);//TODO 从attr中获取
        return entity;
    }

    public static String unCompressGzip(byte[] in, int len) {
        ByteArrayOutputStream out = null;
        GZIPInputStream gunzip = null;
        try {
            if (in == null || len == 0) {
                return "";
            }
            out = new ByteArrayOutputStream();
            gunzip = new GZIPInputStream(new ByteArrayInputStream(in, 0, len));
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
