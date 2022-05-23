package com.ewell.redis;

import com.ewell.common.GatewayConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisConnection;
import io.vertx.redis.client.RedisOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @Author: wy
 * @Date: Created in 16:34 2021-06-30
 * @Description: 非阻塞redis 连接池
 * @Modified: By：
 */
@Slf4j
@Singleton
public class AsyncRedisClient {

    private final Redis client;

    @Inject
    public AsyncRedisClient(GatewayConfig gatewayConfig) {
        GatewayConfig.Redis redis = gatewayConfig.getRedis();
        this.client = Redis.createClient(
                Vertx.vertx(),
                new RedisOptions()
                        .setType(RedisClientType.STANDALONE)
                        .addConnectionString(redis.getUri())
                        .setPassword(redis.getPassword())
                        .setMaxPoolSize(4)
                        .setMaxPoolWaiting(16));
    }

    public Future<RedisConnection> getConnection() {
        return client.connect();
    }

    //jdk反序列化
    @SneakyThrows
    public static Object decode(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readObject();
        }
    }

    //jdk序列化
    public static byte[] encode(Object value) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(value);
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
