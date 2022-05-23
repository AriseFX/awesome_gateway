package com.ewell.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.ewell.common.GatewayConfig;
import com.ewell.common.exception.SimpleRuntimeException;
import com.ewell.core.config.ConfigInitSpi;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.yaml.snakeyaml.Yaml;

import java.util.Properties;

/**
 * @author wy
 * @date 2021/12/14 11:11 AM
 */
@Slf4j
public class NacosConfigImpl implements ConfigInitSpi {

    @Override
    public GatewayConfig init() {
        String namespace = System.getProperty("namespace");
        String addr = System.getProperty("server-addr");
        String dataId = System.getProperty("dataid");
        String group = System.getProperty("group");
        //校验
        Asserts.notNull(namespace, "namespace");
        Asserts.notNull(addr, "server-addr");
        if (dataId == null) {
            dataId = "gateway.yaml";
        }
        if (group == null) {
            group = "DEFAULT_GROUP";
        }
        Properties properties = new Properties();
        properties.setProperty("serverAddr", addr);
        properties.setProperty("namespace", namespace);
        ConfigService configService = null;
        try {
            configService = NacosFactory.createConfigService(properties);
            String content = configService.getConfig(dataId, group, 10000);
            log.info("配置文件加载策略:nacos");
            log.info("内容如下:\n{}", content);
            Yaml yaml = new Yaml();
            return yaml.loadAs(content, GatewayConfig.class);
        } catch (NacosException e) {
            log.error("从nacos加载配置发生异常", e);
            throw new SimpleRuntimeException("从nacos加载配置发生错误," + e.getErrMsg());
        }
    }
}
