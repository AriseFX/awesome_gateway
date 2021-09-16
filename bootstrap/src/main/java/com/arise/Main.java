package com.arise;

import com.arise.base.config.Components;
import com.arise.base.config.GatewayConfig;
import com.arise.base.config.ServerProperties;
import com.arise.endpoint.EndpointRunner;
import com.arise.naming.ServiceManager;
import com.arise.rabbitmq.RabbitmqClient;
import com.arise.redis.AsyncRedisClient;
import com.arise.server.ServerRunner;
import com.arise.server.route.filter.FilterInitializer;
import com.arise.server.route.manager.RouteManager;
import com.arise.server.route.pool.AsyncChannelPool;

/**
 * @Author: wy
 * @Description: 启动类
 * @Modified: By：
 */
public class Main {

    public static void main(String[] args) throws Exception {
        //内存统计
        /*        e.scheduleWithFixedDelay(() -> {
            List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactoryHelper.getBufferPoolMXBeans();
            BufferPoolMXBean directBufferMXBean = bufferPoolMXBeans.get(0);
            String s = JSON.toJSONString(directBufferMXBean);
            System.out.println("hasCleaner内存使用:"+s);
            System.out.println("noCleaner内存使用:"+PlatformDependent.usedDirectMemory());
        }, 0, 5, TimeUnit.SECONDS);*/
        //初始化配置
        ServerProperties.init();
        //初始化组件
        GatewayConfig config = ServerProperties.gatewayConfig;
        Components.put(new AsyncRedisClient(config.getRedis()));
        Components.put(new RabbitmqClient(config.getRabbitmq()));
        Components.put(new AsyncChannelPool(config.getPool()));
        Components.put(new RouteManager());
        //加载注册中心相关SPI
        ServiceManager.init();
        //加载过滤器相关SPI
        FilterInitializer.init();
        //服务启动
        new EndpointRunner().run();
        new ServerRunner().run();
    }
}