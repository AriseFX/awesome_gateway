package com.ewell.core.discovery;

import com.ewell.common.GatewayConfig;
import com.ewell.common.exception.ServiceRegistryException;
import com.ewell.common.util.NetUtils;
import com.ewell.spi.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wy
 * @Date: Created in 10:17 2021-02-26
 * @Description: 主要用于管理服务节点
 * @Modified: By：
 */
@Slf4j
@Singleton
public class ServiceManager {

    private final Map<String, ServiceInfo> nodeContainer = new ConcurrentHashMap<>();

    @Inject
    public ServiceManager(GatewayConfig gatewayConfig) {
        GatewayConfig.Registry definition = gatewayConfig.getRegistry();
        //加载SPI
        ExtensionLoader<ServiceDiscoverySpi> extensionLoader = ExtensionLoader.getExtensionLoader(ServiceDiscoverySpi.class);
        ServiceDiscoverySpi registry = extensionLoader.getJoin(definition.getNaming());
        log.info("注册中心组件加载完成:{}", definition.getNaming());
        String ip = definition.getIp();
        if (ip == null) {
            String networkInterface = definition.getNetworkInterface();
            if (networkInterface != null) {
                ip = NetUtils.getAddress(networkInterface);
            } else {
                throw new ServiceRegistryException();
            }
        }
        definition.setIp(ip);
        //以下为钩子方法
        registry.init(gatewayConfig,
                e -> nodeContainer.put(e.getServiceName(), e)
        );
    }

    private final Random RANDOM = new Random();

    public InetSocketAddress selectService(String name) {
        ServiceInfo serviceInfo = nodeContainer.get(name);
        if (serviceInfo == null) {
            return null;
        }
        List<ServiceInfo.InstanceInfo> instances = serviceInfo.getInstances();
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        ServiceInfo.InstanceInfo info = instances
                .get(RANDOM.nextInt(instances.size()));
        return new InetSocketAddress(info.getIp(), info.getPort());
    }

}
