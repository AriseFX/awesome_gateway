package com.arise.naming.registry;

import com.arise.config.ServerProperties;
import com.arise.internal.exception.ServiceRegistryException;
import com.arise.internal.util.NetUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
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
@ConditionalOnProperty(prefix = "gateway.registry", name = "naming", matchIfMissing = false)
public class ServiceManager {

    @Resource
    private ServerRegistrySpi registrySpi;

    @Resource
    private ServerProperties serverProperties;

    private static final Map<String, ServiceInfo> nodeContainer = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        //TODO 推断当前注册中心类型
        ServerProperties.Registry definition = serverProperties.getRegistry();
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
        registrySpi.subscribeServices(e ->
                nodeContainer.put(e.getServiceName(), e)
        );
    }

    public static InetSocketAddress selectService(String name) {
        ServiceInfo serviceInfo = nodeContainer.get(name);
        if (serviceInfo == null) {
            return null;
        }
        List<ServiceInfo.InstanceInfo> instances = serviceInfo.getInstances();
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        }
        ServiceInfo.InstanceInfo info = instances.get(0);
        return new InetSocketAddress(info.getIp(), info.getPort());
    }

}
