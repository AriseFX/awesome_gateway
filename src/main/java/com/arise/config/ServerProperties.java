package com.arise.config;

import com.arise.server.logging.service.MappedFileService;
import com.arise.server.route.pool.RemoteChannelPool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

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

    /**
     * 端口
     */
    private int port;
    /**
     * 地址
     */
    private String address;

    /**
     * 存储dir
     */
    private String storageDir;

    private Pool pool;

    /**
     * 注册中心相关
     */
    private RegistryDefinition registry;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistryDefinition {
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
        MappedFileService.dir = storageDir;
    }
}
