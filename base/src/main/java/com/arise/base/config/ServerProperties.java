package com.arise.base.config;

import lombok.SneakyThrows;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @Author: wy
 * @Date: Created in 5:09 下午 2021/8/30
 * @Description:
 * @Modified: By：
 */
public class ServerProperties {

    public static GatewayConfig gatewayConfig;

    @SneakyThrows
    public static void init() {
        String path = System.getProperty("gateway.config.path");
        Yaml yaml = new Yaml();
        InputStream inputStream;
        if (path == null) {
            inputStream = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream("application.yml");
        } else {
            inputStream = new FileInputStream(path);
        }
        //读入文件
        gatewayConfig = yaml.loadAs(inputStream, GatewayConfig.class);
    }
}
