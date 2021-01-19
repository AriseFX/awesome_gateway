package com.arise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * @Author: wy
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
     *
     */
    private Integer maxContentLength;
}
