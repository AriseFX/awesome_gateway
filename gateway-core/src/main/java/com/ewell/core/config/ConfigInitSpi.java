package com.ewell.core.config;

import com.ewell.common.GatewayConfig;
import com.ewell.spi.SPI;

/**
 * @Author: wy
 * @Date: Created in 11:18 上午 2021/11/29
 * @Description:
 * @Modified: By：
 */
@SPI(value = "default")
public interface ConfigInitSpi {

    GatewayConfig init();
}
