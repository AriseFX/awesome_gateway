package com.arise.server.logging.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import static com.arise.os.OSHelper.mmap;

/**
 * @Author: wy
 * @Date: Created in 18:03 2021-06-24
 * @Description:
 * @Modified: By：
 */
public class MappedFileService {

    public static String dir;

    private MappedByteBuffer mapBuffer;
    private MappedByteBuffer indexBuffer;
    private MappedByteBuffer metaBuffer;

    private long offset;

    private int index;


    public MappedFileService() {
        try {
            mapBuffer = mmap(dir + "data", 1 << 20); //日志
            indexBuffer = mmap(dir + "index", 1 << 20); //索引
            metaBuffer = mmap(dir + "meta", 32); //meta
            index = metaBuffer.getInt();
            if (index == 0) {
                offset = 0;
            } else {
                indexBuffer.position((index - 1) << 7);
                offset = indexBuffer.getLong() + indexBuffer.getInt() + indexBuffer.getInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLog(ByteBuffer[] req, ByteBuffer[] resp) {
        int reqLen = 0;
        int respLen = 0;
        for (ByteBuffer buffer : req) {
            mapBuffer.put(buffer);
            reqLen += buffer.limit();
        }
        for (ByteBuffer buffer : resp) {
            mapBuffer.put(buffer);
            respLen += buffer.limit();
        }
        writeIndex(reqLen, respLen);
        //同步刷盘
        mapBuffer.force();
        indexBuffer.force();
        metaBuffer.force();
    }

    /**
     * 索引文件
     * [64][32][32]
     */
    private void writeIndex(int reqLen, int respLen) {
        indexBuffer.putLong(offset);
        indexBuffer.putInt(reqLen);
        indexBuffer.putInt(respLen);
        metaBuffer.putInt(0, ++index);
        offset += reqLen + respLen;
    }
}
