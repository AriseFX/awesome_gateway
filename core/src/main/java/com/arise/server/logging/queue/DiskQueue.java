//package com.arise.server.logging.queue;
//
//import com.arise.server.logging.ApiLog;
//
//import java.io.EOFException;
//import java.io.File;
//import java.io.IOException;
//import java.nio.BufferOverflowException;
//import java.nio.MappedByteBuffer;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static com.arise.os.OSHelper.mmap;
//
///**
// * @Author: wy
// * @Date: Created in 10:32 下午 2021/7/28
// * @Description: 磁盘队列
// * @Modified: By：
// */
//public class DiskQueue {
//
//    public static String dir = "./";
//
//    public static final String suffix = ".awe";
//
//    private final Map<String, MappedLogFile> dataMap = new ConcurrentHashMap<>();
//
//    private final MappedByteBuffer metaBuffer;
//
//    private final String dataPrefix;
//
//    public DiskQueue() throws IOException {
//        File dirFile = new File(dir);
//        if (dirFile.exists()) {
//            if (!dirFile.isDirectory()) {
//                throw new RuntimeException("目录错误!");
//            }
//        } else {
//            if (!dirFile.mkdir()) {
//                throw new RuntimeException("创建" + dir + "文件夹失败!");
//            }
//        }
//        //[readNum][offset]
//        this.metaBuffer = mmap(dir + "meta" + suffix, 4 << 10); //meta
//        dataPrefix = dir + "data";
//        String readFilePath = (dataPrefix + readFileNum() + suffix).intern();
//        dataMap.put(readFilePath,
//                new MappedLogFile(readFilePath, readOffset(), writeOffset()));
//        String writeFilePath = (dataPrefix + writeFileNum() + suffix).intern();
//        dataMap.computeIfAbsent(writeFilePath,
//                path -> new MappedLogFile(path, readOffset(), writeOffset()));
//    }
//
//    public ApiLog read() {
//        MappedLogFile file = currentReadFile();
//        ApiLog apiLog;
//        try {
//            apiLog = file.readLog();
//            if (apiLog != null) {
//                readOffset(file.getRidx());
//                System.out.println("当前read offset：" + file.getRidx());
//                return apiLog;
//            } else {
//                readFileNum(readFileNum() + 1);
//                return read();
//            }
//        } catch (IOException | ClassNotFoundException | InterruptedException e) {
//            System.err.println(e.getMessage());
//        }
//        return null;
//    }
//
//    public void write(ApiLog apiLog) {
//        MappedLogFile file = currentWriteFile();
//        int widx = file.getWidx();
//        try {
//            file.writeLog(apiLog);
//            writeOffset(file.getWidx());
//            System.out.println("当前write offset：" + file.getWidx());
//        } catch (BufferOverflowException | EOFException e) {
//            MappedLogFile.clearBuffer();
//            writeFileNum(writeFileNum() + 1);
//            file.markFull();
//            file.setWidx(widx);
//            dataMap.computeIfAbsent((dataPrefix + writeFileNum() + suffix).intern(),
//                    path -> new MappedLogFile(path, 0, 0));
//            System.out.println("写到新的file");
//            write(apiLog);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public MappedLogFile currentReadFile() {
//        return dataMap.computeIfAbsent(dataPrefix + readFileNum() + suffix,
//                path ->
//                        new MappedLogFile(path, readOffset(), writeOffset()));
//    }
//
//    public MappedLogFile currentWriteFile() {
//        return dataMap.computeIfAbsent(dataPrefix + writeFileNum() + suffix,
//                path ->
//                        new MappedLogFile(path, readOffset(), writeOffset()));
//    }
//
//    private int readOffset() {
//        return metaBuffer.getInt();
//    }
//
//    private void readOffset(int offset) {
//        metaBuffer.putInt(0, offset);
//        metaBuffer.force();
//    }
//
//    public int readFileNum() {
//        return metaBuffer.getInt(4);
//    }
//
//    private void readFileNum(int num) {
//        metaBuffer.putInt(4, num);
//    }
//
//    private int writeOffset() {
//        return metaBuffer.getInt(8);
//    }
//
//    private void writeOffset(int offset) {
//        metaBuffer.putInt(8, offset);
//        metaBuffer.force();
//    }
//
//    private int writeFileNum() {
//        return metaBuffer.getInt(12);
//    }
//
//    private void writeFileNum(int num) {
//        metaBuffer.putInt(12, num);
//    }
//}
