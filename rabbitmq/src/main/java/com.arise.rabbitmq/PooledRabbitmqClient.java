package com.arise.rabbitmq;

import com.arise.base.config.GatewayConfig;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @Author: wy
 * @Date: Created in 2:35 下午 2021/9/6
 * @Description: 仅支持内部使用
 * @Modified: By：
 */
@Slf4j
public class PooledRabbitmqClient {

    private final ConnectionFactory connectionFactory;

    public PooledRabbitmqClient(GatewayConfig.Rabbitmq config) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(config.getUri());
        factory.useNio();
        factory.setAutomaticRecoveryEnabled(true);
        this.connectionFactory = factory;
    }

    public Connection newConnection() throws IOException, TimeoutException {
        return connectionFactory.newConnection();
    }
}
