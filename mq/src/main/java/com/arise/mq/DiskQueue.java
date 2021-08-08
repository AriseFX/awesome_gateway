package com.arise.mq;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.arise.mq.IOHelper.mmap;

/**
 * @Author: wy
 * @Date: Created in 10:32 下午 2021/7/28
 * @Description: 磁盘队列
 * @Modified: By：
 */
public class DiskQueue {

    //缺省值是当前目录
    public static String dir = "./";

    public static final String suffix = ".awe";

    private final Map<String, MappedLogFile> dataMap = new ConcurrentHashMap<>();

    private String rFilePath;

    private String wFilePath;

    private int rFileNum;

    private int wFileNum;

    private final MappedByteBuffer metaBuffer;

    private final String dataPrefix;

    public DiskQueue() throws IOException {
        File dirFile = new File(dir);
        if (dirFile.exists()) {
            if (!dirFile.isDirectory()) {
                throw new RuntimeException("目录错误!");
            }
        } else {
            if (!dirFile.mkdir()) {
                throw new RuntimeException("创建" + dir + "文件夹失败!");
            }
        }
        //[readNum][offset]
        this.metaBuffer = mmap(dir + "meta" + suffix, 4 << 10); //meta
        this.rFileNum = readFileNum();
        this.wFileNum = writeFileNum();
        dataPrefix = dir + "data";
        this.rFilePath = (dataPrefix + rFileNum + suffix).intern();
        dataMap.put(rFilePath,
                new MappedLogFile(rFilePath));
        this.wFilePath = (dataPrefix + wFileNum + suffix).intern();
        dataMap.computeIfAbsent(wFilePath,
                MappedLogFile::new);
        Runnable deleteLog = new Runnable() {
            private final File file = new File(DiskQueue.dir);

            @Override
            public void run() {
                if (file.isDirectory()) {
                    File[] files = file.listFiles(e -> {
                        String name = e.getName();
                        return name.endsWith(DiskQueue.suffix)
                                && name.startsWith("data")
                                && rFileNum > fileNum(name);
                    });
                    if (files != null) {
                        for (File file1 : files) {
                            if (file1.delete()) {
                                System.out.println("自动删除" + file1.getName() + "文件成功");
                            } else {
                                System.err.println("失败，自动删除" + file1.getName() + "文件失败");
                            }
                        }
                    }
                }
            }

            private int fileNum(String name) {
                int temp = 1;
                int res = 0;
                for (int i = name.length() - 5; ; i--) {
                    int x = name.charAt(i) - 48;
                    if (x >= 0 && x < 10) {
                        res += temp * x;
                        temp *= 10;
                    } else {
                        break;
                    }
                }
                return res;
            }
        };
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(deleteLog, 0,
                        2, TimeUnit.SECONDS);
    }

    /**
     * @param func 消费者，输入ByteBuffer，输出Boolean为消费结果（false失败）
     */
    public void consume(Function<ByteBuffer, Boolean> func) throws InterruptedException {
        MappedLogFile file = dataMap.computeIfAbsent(rFilePath, MappedLogFile::new);
        int res = file.read(func);
        if (res == -1) {
            //切换新的文件
            rFileNum++;
            rFilePath = (dataPrefix + rFileNum + suffix).intern();
            file = dataMap.get(rFilePath);
            if (file != null) {
                readFileNum(rFileNum);
                consume(func);
            } else {
                //当前文件已满，且新文件未产生
                Thread.sleep(500);
            }
        } else if (res == -2) {
            Thread.sleep(200);
        }
    }


    public void produce(ByteBuffer buffer) {
        MappedLogFile file = dataMap.get(wFilePath);
        int res = file.write(buffer);
        if (res == -1) {
            //切换新文件
            wFileNum++;
            wFilePath = (dataPrefix + wFileNum + suffix).intern();
            file = new MappedLogFile(wFilePath);
            dataMap.put(wFilePath, file);
            writeFileNum(wFileNum);
            System.out.println("produce切换新文件:" + wFileNum);
            res = file.write(buffer);
            if (res < 0) {
                throw new RuntimeException("内部错误!");
            } else {
                System.out.println("produce新的offset:" + res);
            }
        } else {
            System.out.println("produce新的offset:" + res);
        }
    }


    public int readFileNum() {
        return metaBuffer.getInt(0);
    }

    private void readFileNum(int num) {
        metaBuffer.putInt(0, num);
    }


    private int writeFileNum() {
        return metaBuffer.getInt(4);
    }

    private void writeFileNum(int num) {
        metaBuffer.putInt(4, num);
    }
}
