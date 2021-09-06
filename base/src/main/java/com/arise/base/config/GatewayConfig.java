package com.arise.base.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: wy
 * @Date: Created in 10:38 2020-12-25
 * @Description:
 * @Modified: By：
 */
@Data
public class GatewayConfig {
    //端口
    private int port;
    //地址
    private String address;
    //连接池
    private Pool pool;
    //存储dir
    private String storageDir;
    //注册中心相关
    private Registry registry;
    //redis
    private Redis redis;
    //rabbitmq
    private Rabbitmq rabbitmq;
    //endpint
    private Endpoint endpoint;
    //byte
    private int logFileSize;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endpoint {
        //端口
        private int port;
        //地址
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Redis {
        /**
         * 单机无ssl模式: redis://[password@]host[:port][/databaseNumber][?[timeout=timeout[d|h|m|s|ms|us|ns]]
         * ex: redis://mypassword@127.0.0.1:6379/0?timeout=10s
         * 哨兵模式: redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber][?[timeout=timeout[d|h|m|s|ms|us|ns]]#sentinelMasterId
         * ex: redis-sentinel://mypassword@127.0.0.1:6379,127.0.0.1:6380/0?timeout=10s#mymaster
         */
        private String uri;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbitmq {
        /**
         * amqp://username:password@hostname:port/vhost
         */
        private String uri;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Registry {
        private String naming;
        private String serviceName;
        private String serverAddr;
        private String namespace;
        private String networkInterface;
        private String ip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pool {
        private int timeout;
        private int maxConnections;
        private int maxPendingAcquires;
    }
}
