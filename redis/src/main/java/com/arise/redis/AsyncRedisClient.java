package com.arise.redis;

import com.arise.base.config.GatewayConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

/**
 * @Author: wy
 * @Date: Created in 16:34 2021-06-30
 * @Description: 非阻塞redis 连接池
 * @Modified: By：
 */
@Slf4j
public class AsyncRedisClient {

    private final BoundedAsyncPool<StatefulRedisConnection<String, Object>> pool;

    public AsyncRedisClient(GatewayConfig.Redis config) {
        RedisClient client = RedisClient.create();
        RedisURI uri = RedisURI.create(config.getUri());
        //创建异步连接池
        this.pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(new JdkSerializableRedisCodec(), uri),
                // 使用默认的连接池配置
                BoundedPoolConfig.builder().minIdle(3).maxTotal(4000).maxIdle(4000).build());
    }

    /**
     * 非阻塞实现redis命令
     */
    public void asyncExec(BiFunction<RedisAsyncCommands<String, Object>, ? super Throwable, CompletionStage<?>> func) {
        pool.acquire().whenComplete((conn, ex) -> {
            func.apply(conn.async(), ex)
                    .whenComplete((e, ex1) -> {
                                if (ex1 != null) {
                                    ex1.printStackTrace();
                                }
                                pool.release(conn);
                            }
                    );
        });
    }
}
