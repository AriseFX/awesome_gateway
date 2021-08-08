package com.arise.server.logging;

import com.arise.mq.DiskQueue;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import lombok.extern.slf4j.Slf4j;
import org.jboss.marshalling.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.LockSupport;

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
            queue = new DiskQueue();
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
                            if (apiLog != null)
                                System.out.println(apiLog.getRespBodyStr());
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
        //请求行
        dBuffer.clear();
        dBuffer.position(4);
        ByteBufferOutput output = new ByteBufferOutput(dBuffer);
        marshaller.start(output);
        marshaller.writeObject(log.getReq());
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
        //响应行
        int position = dBuffer.position();
        dBuffer.position(position + 4);
        marshaller.start(output);
        marshaller.writeObject(log.getResp());
        marshaller.finish();
        dBuffer.putInt(position, dBuffer.position() - position - 4);
        //响应体
        HttpContent respBody = log.getRespBody();
        if (reqBody == null) {
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
        apiLog.setReq((DefaultHttpRequest) unmarshaller.readObject());
        unmarshaller.finish();
        //只有序列化阶段需要限制limit
        buffer.limit(buffer.capacity());
        //reqBody
        len = buffer.getInt();
        if (len > 0) {
            buffer.get(heapBuffer, 0, len);
            apiLog.setReqBodyStr(new String(heapBuffer, 0, len));
        }
        len = buffer.getInt();
        position = buffer.position();
        buffer.limit(position + len);
        //反序列化
        unmarshaller.start(input);
        apiLog.setResp((DefaultHttpResponse) unmarshaller.readObject());
        unmarshaller.finish();
        buffer.limit(buffer.capacity());
        len = buffer.getInt();
        if (len > 0) {
            buffer.get(heapBuffer, 0, len);
            apiLog.setRespBodyStr(new String(heapBuffer, 0, len));
        }
        return apiLog;
    }


}
