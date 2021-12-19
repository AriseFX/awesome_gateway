package com.ewell.core.discovery;

import com.ewell.common.GatewayConfig;
import com.ewell.spi.SPI;

import java.util.function.Consumer;

/**
 * @Author: wy
 * @Date: Created in 9:39 下午 2021/11/28
 * @Description:
 * @Modified: By：
 */
@SPI
public interface ServiceDiscoverySpi {

    /**
     * 初始化服务列表
     */
    void init(GatewayConfig config, Consumer<ServiceInfo> handler);
}
