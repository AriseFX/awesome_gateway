package com.arise.redis;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 15:11 2021-07-01
 * @Description:
 * @Modified: By：
 */
public class MyRedisCodec implements RedisCodec<String, Object> {

    private final StringCodec codec = new StringCodec();

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return codec.decodeKey(bytes);
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return codec.encodeKey(key);
    }

    @SneakyThrows
    @Override
    public Object decodeValue(ByteBuffer bytes) {
        int remaining = bytes.remaining();
        byte[] dst = new byte[remaining];
        bytes.get(dst, 0, remaining);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dst))) {
            return ois.readObject();
        }
    }

    @SneakyThrows
    @Override
    public ByteBuffer encodeValue(Object value) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(value);
            return ByteBuffer.wrap(outputStream.toByteArray());
        }
    }
}
