package com.arise.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
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
    private Integer port;
    /**
     * 地址
     */
    private String address;

    /**
     * 注册中心相关
     */
    private Map<String, RegistryDefinition> registry;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistryDefinition {
        private String serviceName;
        private String serverAddr;
        private String namespace;
        private String networkInterface;
        private String ip;
    }
}
