package com.ewell.rabbitmq;

import com.ewell.common.GatewayConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
@Singleton
public class PooledRabbitmqClient {

    private final ConnectionFactory connectionFactory;

    @Inject
    public PooledRabbitmqClient(GatewayConfig gatewayConfig) throws Exception {
        GatewayConfig.Rabbitmq config = gatewayConfig.getRabbitmq();
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
