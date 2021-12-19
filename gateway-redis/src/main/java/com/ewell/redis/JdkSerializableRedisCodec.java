package com.ewell.redis;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 15:11 2021-07-01
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class JdkSerializableRedisCodec implements RedisCodec<String, Object> {

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

    @Override
    public ByteBuffer encodeValue(Object value) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(value);
            return ByteBuffer.wrap(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
