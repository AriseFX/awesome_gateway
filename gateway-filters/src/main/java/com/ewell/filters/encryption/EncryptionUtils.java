package com.ewell.filters.encryption;

import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;

import java.util.Iterator;
import java.util.List;

import static com.ewell.common.util.HttpUtils.unCompressGzip;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @Author: wy
 * @Date: Created in 1:10 下午 2021/11/18
 * @Description:
 * @Modified: By：
 */
public class EncryptionUtils {

    //构建aes
    static SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, "ewell_seafront_1".getBytes(UTF_8));

    /**
     * @param encryption true 加密 ,false 解密
     */
    public static void encryptionBody(List<HttpObject> object, HttpHeaders headers, boolean encryption) {
        Iterator<HttpObject> iterator = object.iterator();
        ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer(1024);
        iterator.next();
        while (iterator.hasNext()) {
            HttpObject next = iterator.next();
            HttpContent content = (HttpContent) next;
            buf.writeBytes(content.content());
            iterator.remove();
            //释放初始buf
            content.release();
        }
        String encoding = headers.get("content-encoding");
        if (encoding != null && encoding.contains("gzip")) {
            byte[] res = unCompressGzip(buf.array(), buf.arrayOffset(), buf.readableBytes());
            buf.release(); //释放旧聚合buf
            buf = Unpooled.wrappedBuffer(res); //产生新的聚合buf
        }
        //加密解密
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        String resStr;
        //加密
        if (encryption) {
            resStr = aes.encryptHex(bytes);
        } else {
            resStr = aes.decryptStr(new String(bytes));
        }
        ByteBuf res = Unpooled.wrappedBuffer(resStr.getBytes(UTF_8));
        //释放聚合buf
        DefaultLastHttpContent content = new DefaultLastHttpContent(res);
        object.add(1, content);
        headers.set("Content-Length", res.readableBytes());
    }
}
