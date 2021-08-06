//package com.arise.server.logging.queue;
//
//import com.arise.server.logging.ApiLog;
//import io.netty.buffer.ByteBuf;
//import io.netty.handler.codec.http.DefaultHttpRequest;
//import io.netty.handler.codec.http.DefaultHttpResponse;
//import lombok.Getter;
//import lombok.Setter;
//import org.jboss.marshalling.*;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.MappedByteBuffer;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import static com.arise.os.OSHelper.mmap;
//
///**
// * @Author: wy
// * @Date: Created in 18:03 2021-06-24
// * @Description:
// * @Modified: By：
// */
//public class MappedLogFile {
//
//    private MappedByteBuffer mappedBuffer;
//
//    private static Marshaller marshaller;
//    private static Unmarshaller unmarshaller;
//    private static final ByteBuffer directBuffer;
//    private static final byte[] heapBuffer;
//
//    public static int logFileSize = (1 << 20);
//
//    public static void clearBuffer() {
//        directBuffer.clear();
//    }
//
//    @Getter
//    @Setter
//    private int ridx;
//    @Getter
//    @Setter
//    private volatile int widx;
//
//    private final AtomicBoolean full = new AtomicBoolean(false);
//
//    static {
//        directBuffer = ByteBuffer.allocateDirect(1 << 20);
//        heapBuffer = new byte[1 << 20];
//        MarshallerFactory marshallerFactory = Marshalling
//                .getProvidedMarshallerFactory("serial");
//        MarshallingConfiguration configuration = new MarshallingConfiguration();
//        configuration.setVersion(5);
//        configuration.setSerializabilityChecker(clazz -> true);
//        try {
//            marshaller = marshallerFactory.createMarshaller(configuration);
//            unmarshaller = marshallerFactory.createUnmarshaller(configuration);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void markFull() {
//        full.lazySet(true);
//    }
//
//    public MappedLogFile(String path, int ridx, int widx) {
//        try {
//            if (!path.endsWith(DiskQueue.suffix)) {
//                throw new RuntimeException("文件后缀错误：" + path);
//            }
//            this.ridx = ridx;
//            this.widx = widx;
//            this.mappedBuffer = mmap(path, logFileSize); //日志
//            this.mappedBuffer.position(widx);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void writeLog(ApiLog apiLog) throws IOException {
//        writeObjWithLen(apiLog.getReq());
//        if (apiLog.getReqBody() != null) {
//            writeBodyWithLen(apiLog.getReqBody().content());
//        } else {
//            writeLen(0);
//        }
//        writeObjWithLen(apiLog.getResp());
//        if (apiLog.getRespBody() != null) {
//            writeBodyWithLen(apiLog.getRespBody().content());
//        } else {
//            writeLen(0);
//        }
//    }
//
//    public ApiLog readLog() throws IOException, ClassNotFoundException, InterruptedException {
//        if (ridx < widx) {
//            ApiLog apiLog = new ApiLog();
//            apiLog.setReq((DefaultHttpRequest) readObj());
//            apiLog.setReqBodyStr(readBodyStr());
//            apiLog.setResp((DefaultHttpResponse) readObj());
//            apiLog.setRespBodyStr(readBodyStr());
//            return apiLog;
//        } else {
//            if (!full.get()) {
//                Thread.sleep(200);
//                return readLog();
//            }
//            return null;
//        }
//    }
//
//    private Object readObj() throws IOException, ClassNotFoundException {
//        ByteBuffer buffer = mappedBuffer.duplicate();
//        buffer.position(ridx);
//        int len = buffer.getInt();
//        if (len == 0) {
//            return null;
//        }
//        buffer.limit(ridx + len + 4);
//        //反序列化
//        ByteBufferInput input = new ByteBufferInput(buffer);
//        unmarshaller.start(input);
//        Object res = unmarshaller.readObject();
//        unmarshaller.finish();
//        ridx += len + 4;
//        return res;
//    }
//
//    private String readBodyStr() {
//        ByteBuffer buffer = mappedBuffer.duplicate();
//        buffer.position(ridx);
//        int len = buffer.getInt();
//        if (len > 0) {
//            buffer.get(heapBuffer, 0, len);
//            ridx += len + 4;
//            return new String(heapBuffer, 0, len);
//        } else {
//            ridx += 4;
//            return "";
//        }
//    }
//
//    private void writeLen(int value) {
//        mappedBuffer.putInt(value);
//        widx += 4;
//    }
//
//    private void writeBodyWithLen(ByteBuf body) {
//        int remaining = body.readableBytes();
//        if (remaining > 1) {
//
//        }
//        ByteBuffer buffer = body.nioBuffer();
//        mappedBuffer.putInt(remaining);
//        mappedBuffer.put(buffer);
//        widx += remaining + 4;
//        body.release();
//    }
//
//    /**
//     * 存储结构[len][obj]
//     */
//    private void writeObjWithLen(Object obj) throws IOException {
//        directBuffer.position(4);
//        ByteBufferOutput output = new ByteBufferOutput(directBuffer);
//        marshaller.start(output);
//        marshaller.writeObject(obj);
//        marshaller.finish();
//        directBuffer.putInt(0, directBuffer.position() - 4);
//        directBuffer.flip();
//        mappedBuffer.put(directBuffer);
//        mappedBuffer.force();
//        widx += directBuffer.limit();
//        directBuffer.clear();
//    }
//
//    public void flush() {
//        //同步刷盘
//        mappedBuffer.force();
//    }
//}
