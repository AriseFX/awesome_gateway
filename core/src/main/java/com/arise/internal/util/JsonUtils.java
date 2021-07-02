package com.arise.internal.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.StandardCharsets;

/**
 * @Author: wy
 * @Date: Created in 14:49 2021-07-02
 * @Description:
 * @Modified: By：
 */
public class JsonUtils {

    public static ByteBuf toBuff(String json) {
        byte[] bodyByte = json.getBytes(StandardCharsets.UTF_8);
        return ByteBufAllocator.DEFAULT.directBuffer(bodyByte.length).writeBytes(bodyByte);
    }

    public static String toJson(ByteBuf buf) {
        byte[] body = new byte[buf.readableBytes()];
        buf.readBytes(body);
        return new String(body, StandardCharsets.UTF_8);
    }
}
