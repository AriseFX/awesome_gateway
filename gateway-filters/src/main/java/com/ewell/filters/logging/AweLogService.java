package com.ewell.filters.logging;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.ewell.common.GatewayConfig;
import com.ewell.common.dto.AlarmDto;
import com.ewell.common.dto.ApiLog;
import com.ewell.rabbitmq.PooledRabbitmqClient;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.Map;

import static com.ewell.common.util.HttpUtils.unCompressGzip;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-20
 * @Description: 日志相关
 * @Modified: By：
 */
@Slf4j
@Singleton
public class AweLogService {

    @Inject
    public static PooledRabbitmqClient rabbitmqClient;

    @Inject
    public static GatewayConfig gatewayConfig;

    private static final FastThreadLocal<Channel> localChannel = new FastThreadLocal<Channel>() {
        @Override
        protected Channel initialValue() throws Exception {
            return rabbitmqClient
                    .newConnection().createChannel();
        }
    };

    private static final ChronicleQueue queue;

    static {
        queue = SingleChronicleQueueBuilder.single("./queue").build();
    }

    public static class LogConsumer implements Runnable {

        //限制消费速率
        private RateLimiter rateLimiter = RateLimiter.create(5000);

        @Override
        public void run() {
            boolean sleep = false;
            while (!Thread.interrupted()) {
                if (sleep) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        log.error("发生异常", e);
                    }
                }

                ExcerptTailer tailer = queue.createTailer("a");
                sleep = !tailer.readBytes(bytes -> {
                    byte[] message = bytes.toByteArray();
                    if (message == null) {
                        return;
                    }
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
                            if (type != null && type.contains("json")) {
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
                            if (encoding != null && encoding.contains("gzip")) {
                                bodyStr = new String(unCompressGzip(message, reqBodyLen + 8, respBodyLen), UTF_8);
                            } else {
                                bodyStr = new String(message, reqBodyLen + 8, respBodyLen, UTF_8);
                            }
                            if (type != null && type.contains("application/json")) {
                                info.setResponseBody(JSON.parse(bodyStr));
                            } else {
                                //处理xml消息体,防止mongo报错
                                JSONObject json = new JSONObject();
                                json.put("data", bodyStr);
                                info.setResponseBody(json);
                            }
                        }
                        rateLimiter.acquire();
                        Channel channel = localChannel.get();
                        channel.basicPublish("", "gateway-queue", null,
                                JSON.toJSONString(info).getBytes(UTF_8));
                    } catch (JSONException e) {
                        log.error("json解析异常:", e);
                    } catch (Exception e) {
                        log.error("其他异常:", e);
                    }
                });


            }
            log.error("LogConsumer意外退出!");
        }
    }

    static {
        new Thread(new LogConsumer(), "log_consumer").start();
    }


    public static void pushLog(ApiLog apiLog) {
        try {
            ByteBuffer msgBody = apiLog.marshaller();
            ExcerptAppender appender = queue.acquireAppender();
            appender.writeBytes(BytesStore.wrap(msgBody));
        } catch (Exception e) {
            log.error("发生异常", e);
        } finally {
            apiLog.destructor();
        }
    }

    public static void alarm(AlarmDto dto) {
        try {
            Channel channel = localChannel.get();
            channel.basicPublish("", "gateway-alarm-queue", null,
                    JSON.toJSONString(dto).getBytes(UTF_8));
        } catch (IOException e) {
            log.error("alarm发生异常:", e);
        }
    }


}
