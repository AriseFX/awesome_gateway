package com.arise.internal.cloud.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.arise.internal.cloud.registry.ServiceInfo;
import com.arise.internal.cloud.registry.ServerRegistrySpi;
import com.arise.internal.exception.ServiceRegistryException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @Author: wy
 * @Date: Created in 10:38 2021-02-25
 * @Description:
 * @Modified: By：
 */
@Slf4j
@Component
public class NacosProvider implements ServerRegistrySpi {

    private NamingService naming;

    @Override
    public void init(String namespace,
                     String serverAddr) {
        try {
            //构造Nacos相关
            Properties properties = new Properties();
            properties.setProperty("serverAddr", serverAddr);
            properties.setProperty("namespace", namespace);
            naming = NamingFactory.createNamingService(properties);
        } catch (NacosException e) {
            log.error(e.getErrMsg());
            throw new ServiceRegistryException();
        }
    }

    @SneakyThrows
    @Override
    public void registerInstance(String serviceName, String ip, int port) {
        naming.registerInstance(serviceName, ip
                , port, "DEFAULT");

    }

    @SneakyThrows
    @Override
    public void subscribeServices(Consumer<ServiceInfo> handler) {
        int i = 1;
        Thread.sleep(10000);
        while (true) {
            try {
                ListView<String> services = naming.getServicesOfServer(i++, 10);
                if (services.getData().size() == 0) {
                    break;
                }
                services.getData().forEach(svr -> {
                    try {
                        naming.subscribe(svr, event -> {
                            if (event instanceof NamingEvent) {
                                NamingEvent namingEvent = (NamingEvent) event;
                                String serviceName = namingEvent.getServiceName();
                                List<ServiceInfo.InstanceInfo> instances = namingEvent.getInstances()
                                        .stream().map(e ->
                                                new ServiceInfo.InstanceInfo(e.getIp(), e.getPort(), e.getWeight(),
                                                        e.isHealthy(), e.getMetadata())
                                        ).collect(Collectors.toList());
                                handler.accept(new ServiceInfo(serviceName, instances));
                            }
                        });
                    } catch (NacosException e) {
                        log.error(e.getErrMsg());
                        throw new ServiceRegistryException();
                    }
                });
            } catch (NacosException ignore) {
                break;
            }
        }
    }

    @Override
    public List<InetSocketAddress> serviceList(String name) {
        try {
            List<Instance> allInstances = naming.getAllInstances(name);
            return allInstances.stream()
                    .map(e -> InetSocketAddress.createUnresolved(e.getIp(), e.getPort()))
                    .collect(Collectors.toList());
        } catch (NacosException e) {
            log.error(e.getErrMsg());
            throw new ServiceRegistryException();
        }
    }

    @Override
    public void offline(String name, String ip, int port) {

    }
}
