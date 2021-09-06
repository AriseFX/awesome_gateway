package com.arise.naming;


import com.arise.base.config.GatewayConfig;
import com.arise.spi.SPI;

import java.util.function.Consumer;

/**
 * @Author: wy
 * @Description: 服务发现
 * @Modified: By：
 */
@SPI
public interface ServiceRegistry {

    /**
     * 初始化
     */
    void init(GatewayConfig config, Consumer<ServiceInfo> handler);
}
