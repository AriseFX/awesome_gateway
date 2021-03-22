package com.arise.modules.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @Author: wy
 * @Date: Created in 10:37 2021-02-24
 * @Description: http解析后留下的字符行
 * @Modified: By：
 */
public class CharactersLine {

    private byte[] lineData;


    private Charset charset;

    public CharactersLine(byte[] data) {
        this(StandardCharsets.UTF_8, data);
    }

    public CharactersLine(Charset charset, byte[] data) {
        this.charset = charset;
        this.lineData = data.clone();
    }

    /**
     * 累加数据
     */
    public CharactersLine appendCharacters(byte[] data) {
        byte[] newData = new byte[this.lineData.length + data.length];
        System.arraycopy(lineData, 0, newData, 0, lineData.length);
        System.arraycopy(data, 0, newData, lineData.length, data.length);
        this.lineData = newData;
        return this;
    }

    public String getNewString() {
        return new String(lineData, charset);
    }

}
