package com.arise.mq;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @Author: wy
 * @Date: Created in 3:10 下午 2021/8/4
 * @Description:
 * @Modified: By：
 */
public class IOHelper {

    public static MappedByteBuffer mmap(String path, long len) throws IOException {
        return new RandomAccessFile(path, "rw")
                .getChannel()
                .map(FileChannel.MapMode.READ_WRITE, 0, len);
    }
}
