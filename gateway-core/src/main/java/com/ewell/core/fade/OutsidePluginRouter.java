package com.ewell.core.fade;

import com.ewell.common.message.GatewayMessage;
import com.ewell.core.discovery.ServiceDiscoverySpi;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.spi.ExtensionLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ewell.common.GatewayMessages.GATEWAY_ERROR;

/**
 * 外部插件路由管理
 * @author     : MrFox
 * @date       : 2021/12/26 12:40 AM
 * @description:
 * @version    :
 */
@Slf4j
@Singleton
public class OutsidePluginRouter {

    private static Map<String, OutsidePluginSchemaSpi> SCHEMA_SPI_MAP;

    public OutsidePluginRouter(){
        //预填充所有外部协议
        ExtensionLoader<OutsidePluginSchemaSpi> extensionLoader = ExtensionLoader.getExtensionLoader(OutsidePluginSchemaSpi.class);
        SCHEMA_SPI_MAP =
                extensionLoader.getAllJoinSpi().stream().collect(Collectors.toMap(Function.identity(), extensionLoader::getJoin));
        log.info("外部插件加载成功:{}",SCHEMA_SPI_MAP.keySet());
    }

    /**
     * 外部插件路由
     * @param   schema 协议
     * @return
     * @description:
     */
    public void router(String schema, FilterContext ctx, Object data) {
        OutsidePluginSchemaSpi outsidePluginSchemaSpi = SCHEMA_SPI_MAP.get(schema);
        if(Objects.isNull(outsidePluginSchemaSpi)){
            ctx.cancel(GATEWAY_ERROR("不支持该协议:" + schema));
        }
        outsidePluginSchemaSpi.handler(ctx,data);
    }

}
