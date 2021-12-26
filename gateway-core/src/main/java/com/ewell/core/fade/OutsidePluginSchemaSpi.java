package com.ewell.core.fade;

import com.ewell.common.message.GatewayMessage;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.spi.SPI;

/**
 * 外部处理协议
 * @author     : MrFox
 * @date       : 2021/12/26 12:19 AM
 * @description:
 * @version    :
 */
@SPI
public interface OutsidePluginSchemaSpi {

    /**
     * 真实处理
     * @param
     * @return {@link GatewayMessage }
     * @description:
     */
    void handler(FilterContext ctx, Object data);

}
