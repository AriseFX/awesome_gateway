package com.arise.internal.cloud.registry.nacos;


import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Consumer;

/**
 * @Author: wy
 * @Description: 服务注册相关Spi
 * @Modified: By：
 */
public interface ServerRegistrySpi {

    /**
     * 初始化
     */
    void init(String namespace,
              String serverAddr);

    /**
     * 注册服务
     */
    void registerInstance(String serviceName, String ip, int port);

    /**
     * 订阅服务
     *
     * @param listener 节点变化监听器
     */
    void subscribeServices(Consumer<Object> listener);

    /**
     * 服务列表
     */
    List<InetSocketAddress> serviceList(String name);

    /**
     * 服务下线
     */
    void offline(String name, String ip, int port);
}
