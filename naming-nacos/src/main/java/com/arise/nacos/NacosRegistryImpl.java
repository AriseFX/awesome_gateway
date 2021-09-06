package com.arise.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.arise.base.config.GatewayConfig;
import com.arise.base.exception.ServiceRegistryException;
import com.arise.naming.ServiceRegistry;
import com.arise.naming.ServiceInfo;
import com.arise.spi.Join;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @Author: wy
 * @Date: Created in 10:38 2021-02-25
 * @Description:
 * @Modified: By：
 */
@Slf4j
@Join
public class NacosRegistryImpl implements ServiceRegistry {

    private NamingService naming;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final Set<String> subscribeList = new HashSet<>();

    @Override
    public void init(GatewayConfig config, Consumer<ServiceInfo> handler) {
        try {
            GatewayConfig.Registry registry = config.getRegistry();
            //构造Nacos相关
            Properties properties = new Properties();
            properties.setProperty("serverAddr", registry.getServerAddr());
            properties.setProperty("namespace", registry.getNamespace());
            naming = NamingFactory.createNamingService(properties);

            //注册服务
            naming.registerInstance(registry.getServiceName(), registry.getIp()
                    , config.getPort(), "DEFAULT");
            executor.scheduleWithFixedDelay(() -> {
                try {
                    List<String> services = naming.getServicesOfServer(1, 999).getData()
                            .stream()
                            .filter(e -> !subscribeList.contains(e))
                            .collect(Collectors.toList());
                    if (services.size() == 0) {
                        return;
                    }
                    for (String svr : services) {
                        subscribeList.add(svr);
                        List<Instance> allInstances = naming.getAllInstances(svr);
                        handler.accept(new ServiceInfo(svr, map2InstanceInfo(allInstances)));
                        naming.subscribe(svr, event -> {
                            if (event instanceof NamingEvent) {
                                NamingEvent namingEvent = (NamingEvent) event;
                                String serviceName = namingEvent.getServiceName();
                                List<ServiceInfo.InstanceInfo> instances = map2InstanceInfo(namingEvent.getInstances());
                                handler.accept(new ServiceInfo(serviceName.split("@@")[1], instances));
                            }
                        });
                    }
                } catch (NacosException e) {
                    e.printStackTrace();
                }
            }, 0, 15, TimeUnit.SECONDS);
        } catch (NacosException e) {
            log.error(e.getErrMsg());
            throw new ServiceRegistryException();
        }
    }

    private List<ServiceInfo.InstanceInfo> map2InstanceInfo(List<Instance> instances) {
        return instances
                .stream().map(e ->
                        new ServiceInfo.InstanceInfo(e.getIp(), e.getPort(), e.getWeight(),
                                e.isHealthy(), e.getMetadata())
                ).collect(Collectors.toList());
    }

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
}
