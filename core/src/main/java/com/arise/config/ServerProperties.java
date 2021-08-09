package com.arise.config;


import com.arise.mq.MappedLogFile;
import com.arise.server.logging.LogService;
import com.arise.server.route.pool.RemoteChannelPool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @Author: wy
 * @Date: Created in 10:38 2020-12-25
 * @Description:
 * @Modified: By：
 */
@Data
@Configuration
@EnableConfigurationProperties(ServerProperties.class)
@ConfigurationProperties("gateway")
public class ServerProperties {

    private static ApplicationContext applicationContext;

    public ServerProperties(ApplicationContext applicationContext) {
        ServerProperties.applicationContext = applicationContext;
    }

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

    @PostConstruct
    private void after() {
        Pool pool = this.getPool();
        RemoteChannelPool.setConnectTimeout(pool.timeout);
        RemoteChannelPool.setMaxPendingAcquires(pool.maxPendingAcquires);
        RemoteChannelPool.setMaxConnections(pool.maxConnections);
        if (!storageDir.endsWith("/")) {
            storageDir = storageDir + "/";
        }
        LogService.dir = storageDir;
        MappedLogFile.logFileSize = this.logFileSize;
    }

    public static <T> T getBean(Class<T> clz) throws BeansException {
        T result = (T) applicationContext.getBean(clz);
        return result;
    }
}
