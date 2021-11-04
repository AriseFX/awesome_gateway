package com.arise.server.route.logging;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.arise.base.config.Components;
import com.arise.base.config.ServerProperties;
import com.arise.queue.GatewayDiskQueue;
import com.arise.rabbitmq.PooledRabbitmqClient;
import com.rabbitmq.client.Channel;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-20
 * @Description: 日志相关
 * @Modified: By：
 */
@Slf4j
public class AweLogService {

    private static final FastThreadLocal<Channel> localChannel = new FastThreadLocal<Channel>() {
        @Override
        protected Channel initialValue() throws Exception {
            return Components.get(PooledRabbitmqClient.class)
                    .newConnection().createChannel();
        }
    };
    private static final List<GatewayDiskQueue> queues = new CopyOnWriteArrayList<>();

    private static final FastThreadLocal<GatewayDiskQueue> localQueue =
            new FastThreadLocal<GatewayDiskQueue>() {
                @Override
                protected GatewayDiskQueue initialValue() {
                    String name = Thread.currentThread().getName();
                    int diskQueueSize = ServerProperties.gatewayConfig.getLogging().getDiskQueueSize();
                    if (diskQueueSize == 0) {
                        diskQueueSize = 209715200;
                    }
                    GatewayDiskQueue queue = new GatewayDiskQueue("./data", name, diskQueueSize);
                    queues.add(queue);
                    return queue;
                }
            };

    public static class LogConsumer implements Runnable {

        @Override
        public void run() {
            boolean sleep = false;
            while (!Thread.interrupted()) {
                if (sleep) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sleep = true;
                for (GatewayDiskQueue queue : queues) {
                    byte[] message = queue.read();
                    if (message == null) {
                        continue;
                    }
                    sleep = false;
                    ByteBuffer mapped = ByteBuffer.wrap(message);
                    int reqBodyLen = mapped.getInt();
                    int respBodyLen = mapped.getInt();
                    int offset = reqBodyLen + respBodyLen + 8;
                    try {
                        ApiLog apiLog = new ApiLog(message, offset, message.length - offset);
                        ApiLog.Info info = apiLog.getInfo();
                        Map<String, String> headers = info.getHeaders();
                        if (reqBodyLen > 0) {
                            //requestBody
                            String type = headers.get("Content-Type");
                            if (type != null && type.toLowerCase().contains("json")) {
                                info.setRequestBody(JSON.parse(new String(message, 8,
                                        reqBodyLen)));
                            } else {
                                JSONObject json = new JSONObject();
                                json.put("data", new String(message, 8,
                                        reqBodyLen));
                                info.setRequestBody(json);
                            }
                        }
                        if (respBodyLen > 0) {
                            //responseBody
                            Map<String, String> respHeaders = info.getRespHeaders();
                            String encoding = respHeaders.get("content-encoding");
                            String type = respHeaders.get("Content-Type");
                            String bodyStr;
                            if (encoding != null && encoding.toLowerCase().contains("gzip")) {
                                bodyStr = unCompressGzip(message, reqBodyLen + 8, respBodyLen);
                            } else {
                                bodyStr = new String(message, reqBodyLen + 8, respBodyLen);
                            }
                            if (type != null && type.toLowerCase().contains("json")) {
                                info.setResponseBody(JSON.parse(bodyStr));
                            } else {
                                JSONObject json = new JSONObject();
                                json.put("data", bodyStr);
                                info.setResponseBody(json);
                            }
                        }
                        Channel channel = localChannel.get();
                        channel.basicPublish("", "gateway-queue", null,
                                JSON.toJSONString(info).getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static {
        new Thread(new LogConsumer(), "log_consumer").start();
    }


    public static void pushLog(ApiLog log) {
        localQueue.get().write(log);
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
