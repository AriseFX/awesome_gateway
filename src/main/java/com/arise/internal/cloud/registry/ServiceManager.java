package com.arise.internal.cloud.registry;

import com.arise.config.ServerProperties;
import com.arise.internal.cloud.registry.nacos.ServerRegistrySpi;
import com.arise.internal.exception.ServiceRegistryException;
import com.arise.internal.utils.NetUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wy
 * @Date: Created in 10:17 2021-02-26
 * @Description: 主要用于管理服务节点
 * @Modified: By：
 */
@Component
public class ServiceManager {

    @Resource(name = "nacosProvider")
    private ServerRegistrySpi registrySpi;

    @Resource
    private ServerProperties serverProperties;

    private Map<String, Object> nodeContainer = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        //推断当前注册中心类型
        ServerProperties.RegistryDefinition definition = serverProperties.getRegistry().get("nacos");
        if (definition == null) {
            throw new ServiceRegistryException();
        }
        String ip;
        String networkInterface = definition.getNetworkInterface();
        if (!ObjectUtils.isEmpty(networkInterface)) {
            ip = NetUtils.getAddress(networkInterface);
        } else {
            ip = definition.getIp();
        }
        //以下为钩子方法
        registrySpi.init(definition.getNamespace(), definition.getServerAddr());
        registrySpi.registerInstance(definition.getServiceName(), ip, serverProperties.getPort());
        registrySpi.subscribeServices(event -> {

        });
    }

    public List<InetSocketAddress> serviceList(String name) {
        return registrySpi.serviceList(name);
    }
}
