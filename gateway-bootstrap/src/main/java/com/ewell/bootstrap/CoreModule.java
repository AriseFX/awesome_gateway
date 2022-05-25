package com.ewell.bootstrap;

import com.ewell.common.exception.SimpleRuntimeException;
import com.ewell.core.config.ConfigInitSpi;
import com.ewell.common.GatewayConfig;
import com.ewell.core.route.RouteStoreSpi;
import com.ewell.core.server.handler.ForwardHandler;
import com.ewell.core.server.handler.ProxyInboundHandler;
import com.ewell.core.server.handler.ProxyReadTimeoutHandler;
import com.ewell.core.server.os.OSHelper;
import com.ewell.endpoint.service.RouteServicesConsumer;
import com.ewell.filters.auth.AuthFilter;
import com.ewell.filters.logging.AweLogService;
import com.ewell.filters.logging.LoggingFilter;
import com.ewell.filters.route.MatchRouteFilter;
import com.ewell.filters.route.SelectServiceFilter;
import com.ewell.spi.ExtensionLoader;
import com.google.inject.AbstractModule;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 11:18 下午 2021/11/29
 * @Description: 配置文件管理
 * @Modified: By：
 */
public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
        //config
        ExtensionLoader<ConfigInitSpi> configInitSpi = ExtensionLoader.getExtensionLoader(ConfigInitSpi.class);
        String type = System.getProperty("config.type");
        ConfigInitSpi spi;
        if (type == null) {
            spi = configInitSpi.getDefaultJoin();
        } else {
            spi = configInitSpi.getJoin(type);
        }
        if (spi == null) {
            throw new SimpleRuntimeException("");
        }
        GatewayConfig init = spi.init();
        bind(GatewayConfig.class).toInstance(init);

        //route
        ExtensionLoader<RouteStoreSpi> extensionLoader = ExtensionLoader.getExtensionLoader(RouteStoreSpi.class);
        List<RouteStoreSpi> spis = extensionLoader.getAllJoin();
        if (spis != null && spis.size() > 0) {
            bind(RouteStoreSpi.class).toInstance(spis.get(0));
            binder().requestInjection(spis.get(0));
        }
        //静态注入
        binder().requestStaticInjection(MatchRouteFilter.class, SelectServiceFilter.class,
                LoggingFilter.class, AweLogService.class, AuthFilter.class);
        binder().requestStaticInjection(OSHelper.class);
        binder().requestStaticInjection(ProxyInboundHandler.class, ProxyReadTimeoutHandler.class);
        binder().requestStaticInjection(ForwardHandler.class);
        binder().requestStaticInjection(RouteServicesConsumer.class);


    }
}
