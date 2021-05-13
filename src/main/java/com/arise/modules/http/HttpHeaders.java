package com.arise.modules.http;

import io.netty.util.AsciiString;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: wy
 * @Date: Created in 10:13 2021-05-13
 * @Description: 内部使用
 * @Modified: By：
 */
public class HttpHeaders extends HashMap<AsciiString, AsciiString> {

    //消息长度
    private final AtomicInteger msgLen = new AtomicInteger(0);

    public void addHeader(AsciiString key, AsciiString value) {
        int len = 0;
        AsciiString old = super.put(key, value);
        if (old != null) {
            len += (value.length() - old.length());
        } else {
            len += (value.length() + key.length());
        }
        msgLen.addAndGet(len);
    }

    public void removeHeader(AsciiString key) {
        AsciiString removed = super.remove(key);
        if (removed != null) {
            msgLen.addAndGet(-(removed.length() + key.length()));
        }
    }

    public AsciiString getHeader(AsciiString key) {
        return super.get(key);
    }

    public int getMsgLen() {
        int size = super.size();
        if (size > 0) {
            return this.msgLen.get() + 3 * size;
        }
        return 0;
    }
}
