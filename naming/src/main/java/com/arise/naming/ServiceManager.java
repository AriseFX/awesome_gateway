package com.arise.naming;

import com.arise.base.config.GatewayConfig;
import com.arise.base.config.ServerProperties;
import com.arise.base.exception.ServiceRegistryException;
import com.arise.base.util.NetUtils;
import com.arise.spi.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;

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
public class ServiceManager {

    private static final Map<String, ServiceInfo> nodeContainer = new ConcurrentHashMap<>();

    public static void init() {
        GatewayConfig config = ServerProperties.gatewayConfig;
        GatewayConfig.Registry definition = config.getRegistry();
        //加载SPI
        ExtensionLoader<ServiceRegistry> extensionLoader = ExtensionLoader.getExtensionLoader(ServiceRegistry.class);
        ServiceRegistry registry = extensionLoader.getJoin(definition.getNaming());
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
        registry.init(ServerProperties.gatewayConfig,
                e ->
                        nodeContainer.put(e.getServiceName(), e)
        );
    }

    private static final Random RANDOM = new Random();

    public static InetSocketAddress selectService(String name) {
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
