package com.arise.redis;


import com.arise.spring.ServerProperties;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @Author: wy
 * @Date: Created in 16:34 2021-06-30
 * @Description: 非阻塞redis客户端
 * @Modified: By：
 */
@Component("redisClient")
@ConditionalOnProperty(prefix = "gateway.redis", name = "uri", matchIfMissing = false)
public class AsyncRedisClient {

    private RedisAsyncCommands<String, Object> commands;

    @Resource(name = "serverProperties")
    private ServerProperties properties;

    @PostConstruct
    public void init() {
        RedisClient redisClient = RedisClient.create();
        RedisURI uri = RedisURI.create(properties.getRedis().getUri());
        StatefulRedisConnection<String, Object> connect = redisClient.connect(new JdkSerializableRedisCodec(), uri);
        commands = connect.async();
    }

    public RedisAsyncCommands<String, Object> commands() {
        return this.commands;
    }

}
