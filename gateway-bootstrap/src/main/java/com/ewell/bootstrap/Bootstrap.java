package com.ewell.bootstrap;

import com.ewell.core.server.ServerRunner;
import com.ewell.endpoint.EndpointRunner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

/**
 * @Author: wy
 * @Date: Created in 6:10 下午 2021/11/25
 * @Description: 启动类
 * @Modified: By：
 */
public class Bootstrap {

    public static void main(String[] args) {
        //关闭默认nacos日志
        System.setProperty("nacos.logging.default.config.enabled", "false");
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new CoreModule());
        EndpointRunner endpoint = injector.getInstance(EndpointRunner.class);
        ServerRunner server = injector.getInstance(ServerRunner.class);
        //暴露端点
        endpoint.run();
        //启动gateway
        server.run();
    }

}
