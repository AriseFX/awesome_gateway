package com.ewell.queue;

import com.ewell.common.dto.MySerializable;
import com.ewell.common.exception.SimpleRuntimeException;
import io.netty.util.collection.IntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @Author: wy
 * @Date: Created in 1:37 下午 2021/10/26
 * @Description: 磁盘队列
 * @Modified: By：
 */
@Slf4j
public class GatewayDiskQueue {

    private final IntObjectHashMap<QueueBuffer> dataMap = new IntObjectHashMap<>();

    private final MappedByteBuffer meta;

    private final int len;

    private final String name;

    private final String dir;

    private int wfn; //当前写文件编号

    private int rfn; //当前读文件编号

    public GatewayDiskQueue(String dir, String name, int len) {
        this.dir = dir;
        this.name = name;
        this.len = len;
        File dataDir = new File(dir);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        String metaPath = dir + "/gateway_" + name + ".mate";
        try {
            RandomAccessFile file = new RandomAccessFile(metaPath, "rw");
            this.meta = file.getChannel()
                    .map(FileChannel.MapMode.READ_WRITE, 0, 4096);
            this.wfn = meta.getInt(0);
            this.rfn = meta.getInt(4);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SimpleRuntimeException("meta data mmap error");
        }
    }

    public void write(MySerializable data) {
        try {
            ByteBuffer msgBody = data.marshaller();
            doWrite(msgBody);
        } catch (Exception e) {
            log.error("GatewayDiskQueue.write:{}", e.getMessage());
            e.printStackTrace();
        } finally {
            data.destructor();
        }
    }

    private void doWrite(ByteBuffer msgBody) {
        QueueBuffer buffer = dataMap.computeIfAbsent(wfn, this::newMappedBuffer);
        if (buffer.writeable(msgBody.remaining())) {
            buffer.writeBufAndSize(msgBody);
//            buffer.force();
        } else {
            //切换新文件
            buffer.markFull();
            wfn++;
            meta.putInt(0, wfn);
            doWrite(msgBody);
        }
    }

    public byte[] read() {
        QueueBuffer buffer = dataMap.computeIfAbsent(rfn, this::newMappedBuffer);
        if (buffer.readable()) {
            int len = buffer.readInt();
            byte[] data = new byte[len];
            buffer.readBuf(data);
            return data;
        } else if (buffer.isFull()) {
            //删除失效的文件
            buffer.clean();
            rfn++;
            meta.putInt(0, rfn);
            return read();
        }
        return null;
    }

    private QueueBuffer newMappedBuffer(int num) {
        String dataPath = dir + "/gateway_" + name + "_" + num + ".data";
        try {
            return new QueueBuffer(dataPath, len);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SimpleRuntimeException("queue data mmap error");
        }
    }

}
