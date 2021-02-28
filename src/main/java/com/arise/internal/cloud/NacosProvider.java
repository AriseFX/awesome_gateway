package com.arise.internal.cloud;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.arise.internal.cloud.registry.nacos.ServerRegistrySpi;
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
    public void subscribeServices(Consumer<Object> listener) {
        int i = 1;
        while (true) {
            ListView<String> services = naming.getServicesOfServer(i++, 50);
            if (services.getCount() == 0) {
                break;
            }
            services.getData().forEach(svr -> {
                try {
                    naming.subscribe(svr, listener::accept);
                } catch (NacosException e) {
                    log.error(e.getErrMsg());
                    throw new ServiceRegistryException();
                }
            });
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
