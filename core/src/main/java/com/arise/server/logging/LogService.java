package com.arise.server.logging;

import com.alibaba.fastjson.JSON;
import com.arise.mq.DiskQueue;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import lombok.extern.slf4j.Slf4j;
import org.jboss.marshalling.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.GZIPInputStream;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-20
 * @Description: 日志相关
 * @Modified: By：
 */
@Slf4j
public class LogService implements Runnable {

    private final Thread thread;

    private final MpscArrayQueue<ApiLog> logQueue = new MpscArrayQueue<>(1 << 10);

    private DiskQueue queue;

    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    private static final ByteBuffer dBuffer;
    private static final byte[] heapBuffer;

    //缺省值是当前目录
    public static String dir = "./";

    static {
        dBuffer = ByteBuffer.allocateDirect(20 << 20);
        heapBuffer = new byte[1 << 20];
        MarshallerFactory marshallerFactory = Marshalling
                .getProvidedMarshallerFactory("serial");
        MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);
        configuration.setSerializabilityChecker(clazz -> true);
        try {
            marshaller = marshallerFactory.createMarshaller(configuration);
            unmarshaller = marshallerFactory.createUnmarshaller(configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LogService() {
        //消费线程
        try {
            queue = new DiskQueue(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new ConsumerService(), "log_consumer").start();
        this.thread = new Thread(this, "log_provider");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            ApiLog polled = logQueue.relaxedPoll();
            if (polled == null) {
                LockSupport.park();
            } else {
                try {
                    queue.produce(marshaller(polled));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void pushLog(ApiLog log) {
        logQueue.offer(log);
        LockSupport.unpark(thread);
    }

    private class ConsumerService implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    queue.consume(buffer -> {
                        try {
                            ApiLog apiLog = unmarshaller(buffer);
                            if (apiLog != null) {
                                RequestLogEntity entity = map2Entity(apiLog);
                                ApiLogUtils.saveMsg(entity);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        return true;
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ByteBuffer marshaller(ApiLog log) throws IOException {
        //请求/响应行
        dBuffer.clear();
        dBuffer.position(4);
        ByteBufferOutput output = new ByteBufferOutput(dBuffer);
        marshaller.start(output);
        marshaller.writeObject(log.getInfo());
        marshaller.finish();
        dBuffer.putInt(0, dBuffer.position() - 4);
        //请求体
        HttpContent reqBody = log.getReqBody();
        if (reqBody == null) {
            dBuffer.putInt(0);
        } else {
            ByteBuffer buffer = reqBody.content().nioBuffer();
            dBuffer.putInt(buffer.remaining());
            dBuffer.put(buffer);
        }
        //响应体
        HttpContent respBody = log.getRespBody();
        if (respBody == null) {
            dBuffer.putInt(0);
        } else {
            ByteBuffer buffer = respBody.content().nioBuffer();
            dBuffer.putInt(buffer.remaining());
            dBuffer.put(buffer);
        }
        dBuffer.flip();
        return dBuffer;
    }

    private ApiLog unmarshaller(ByteBuffer buffer) throws IOException, ClassNotFoundException {
        ApiLog apiLog = new ApiLog();
        int len = buffer.getInt();
        int position = buffer.position();
        if (len == 0) {
            return null;
        }
        buffer.limit(position + len);
        //反序列化
        ByteBufferInput input = new ByteBufferInput(buffer);
        unmarshaller.start(input);
        ApiLog.Info info = (ApiLog.Info) unmarshaller.readObject();
        apiLog.setInfo(info);
        //处理gzip
        String encoding = info.getReq().headers().get(HttpHeaderNames.CONTENT_ENCODING);
        unmarshaller.finish();
        //只有序列化阶段需要限制limit
        buffer.limit(buffer.capacity());
        //reqBody
        len = buffer.getInt();
        if (len > 0) {
            buffer.get(heapBuffer, 0, len);
            apiLog.setReqBodyStr(new String(heapBuffer, 0, len));
        }
        //respBody
        len = buffer.getInt();
        if (len > 0) {
            buffer.get(heapBuffer, 0, len);
            String respBody;
            if ("gzip".equals(encoding)) {
                respBody = unCompressGzip(heapBuffer, len);
            } else {
                respBody = new String(heapBuffer, 0, len);
            }
            apiLog.setRespBodyStr(respBody);
        }
        return apiLog;
    }

    /**
     * 转换为运维中心需要的日志格式
     */
    private RequestLogEntity map2Entity(ApiLog log) {
        ApiLog.Info info = log.getInfo();
        DefaultHttpRequest req = info.getReq();
        DefaultHttpResponse resp = info.getResp();
        HttpHeaders headers = req.headers();

        //构造运维中心日志
        RequestLogEntity entity = new RequestLogEntity();
        //req
        URI uri = URI.create(req.uri());
        entity.setPath(uri.getPath());
        entity.setHeaders(req.headers());
        entity.setResponseCode(resp.status().code() + "");
        entity.setOrgCode(headers.get("x-originCode"));
        entity.setTargetUri(req.uri());
        entity.setRequestTime(new Date(info.getTimestamp()));
        entity.setResponseBody(JSON.parseObject(log.getRespBodyStr()));
        entity.setRequestBody(JSON.parseObject(log.getReqBodyStr()));
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
