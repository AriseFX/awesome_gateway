package com.arise.modules.http;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @Author: wy
 * @Date: Created in 10:37 2021-02-24
 * @Description: http解析后留下的字符行消
 * @Modified: By：
 */
@NotThreadSafe
public class CharactersLine {

    private byte[] lineData;

    private Charset charset;

    private int cursor = 0;

    public CharactersLine(byte[] data, int len) {
        this(StandardCharsets.UTF_8, data, len);
    }

    public CharactersLine(Charset charset, byte[] data, int len) {
        this.charset = charset;
        this.lineData = new byte[len];
        System.arraycopy(data, 0, lineData, 0, len);
    }

    public byte[] getInnerData() {
        return this.lineData;
    }

    /**
     * 累加数据
     */
    public CharactersLine appendCharacters(byte[] data, int len) {
        byte[] newData = new byte[this.lineData.length + len];
        System.arraycopy(lineData, 0, newData, 0, lineData.length);
        System.arraycopy(data, 0, newData, lineData.length, len);
        this.lineData = newData;
        return this;
    }

    public String getNewString() {
        return new String(lineData, charset);
    }

    /**
     * 基于游标寻找字符
     */
    public int findByteIndex(byte b) {
        for (; cursor < lineData.length; cursor++) {
            if (b == lineData[cursor]) {
                cursor++;
                return cursor;
            }
        }
        return -1;
    }

    public void reset() {
        this.cursor = 0;
    }
}
