package com.ewell.core.config;

import com.ewell.common.GatewayConfig;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @Author: wy
 * @Date: Created in 11:20 上午 2021/11/29
 * @Description: 本地加载配置文件
 * @Modified: By：
 */
@Slf4j
public class DefaultConfigImpl implements ConfigInitSpi {

    @Override
    public GatewayConfig init() {
        String path = System.getProperty("gateway.config.path");
        Yaml yaml = new Yaml();
        InputStream inputStream;
        if (path == null) {
            inputStream = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream("application.yml");
        } else {
            try {
                inputStream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                log.error("指定的配置文件未找到:", e);
                return null;
            }
        }
        log.info("配置文件加载策略:本地。路径:{}", path == null ? "classpath:application.yml" : path);
        //读入文件
        return yaml.loadAs(inputStream, GatewayConfig.class);
    }
}
