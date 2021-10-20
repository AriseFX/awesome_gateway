package com.arise.rabbitmq;

import com.arise.base.config.GatewayConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @Author: wy
 * @Date: Created in 2:35 下午 2021/9/6
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class RabbitmqClient {

    private final Connection connection;

    public RabbitmqClient(GatewayConfig.Rabbitmq config) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(config.getUri());
        factory.useNio();
        this.connection = factory.newConnection();
    }

    public Channel getChannel() {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
