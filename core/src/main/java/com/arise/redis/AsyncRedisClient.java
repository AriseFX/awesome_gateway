package com.arise.redis;


import com.arise.config.ServerProperties;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * @Author: wy
 * @Date: Created in 16:34 2021-06-30
 * @Description: 非阻塞redis 连接池
 * @Modified: By：
 */
@Slf4j
@Component("redisClient")
@ConditionalOnProperty(prefix = "gateway.redis", name = "uri", matchIfMissing = false)
public class AsyncRedisClient {

    @Resource(name = "serverProperties")
    private ServerProperties properties;

    private BoundedAsyncPool<StatefulRedisConnection<String, Object>> pool;

    @PostConstruct
    private void init() {
        RedisClient client = RedisClient.create();
        RedisURI uri = RedisURI.create(properties.getRedis().getUri());
        //创建异步连接池
        this.pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(new JdkSerializableRedisCodec(), uri),
                // 使用默认的连接池配置
                BoundedPoolConfig.create());
    }

    public void asyncExec(Function<RedisAsyncCommands<String, Object>, CompletionStage<?>> func) {
        pool.acquire()
                .thenApply(conn -> {
                    RedisAsyncCommands<String, Object> async = conn.async();
                    async.setTimeout(Duration.ofSeconds(10));
                    return func.apply(async)
                            .whenComplete((e, ex) -> {
                                        log.info("释放redis连接");
                                        pool.release(conn);
                                    }
                            );
                });

    }
}
