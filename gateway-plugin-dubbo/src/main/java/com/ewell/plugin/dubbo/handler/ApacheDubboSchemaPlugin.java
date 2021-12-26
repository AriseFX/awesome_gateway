package com.ewell.plugin.dubbo.handler;

import com.alibaba.fastjson.JSON;
import com.ewell.common.GatewayConfig;
import com.ewell.common.GatewayMessages;
import com.ewell.common.RouteBean;
import com.ewell.common.message.GatewayMessage;
import com.ewell.core.fade.OutsidePluginSchemaSpi;
import com.ewell.core.filer.context.FilterContext;
import com.ewell.plugin.dubbo.cache.ApacheDubboConfigCache;
import com.ewell.plugin.dubbo.convert.DubboPathMetaConvert;
import com.ewell.plugin.dubbo.encode.DubboBodyEncodeUtil;
import com.ewell.plugin.dubbo.encode.DubboParamsEncodeUtil;
import com.ewell.plugin.dubbo.model.DubboPathMeta;
import com.ewell.spi.Join;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static com.ewell.common.GatewayMessages.GATEWAY_ERROR;
import static com.ewell.common.IntMapConstant._FinalRouteBean;

/**
 * apache dubbo 协议处理
 *
 * @author : MrFox
 * @version :
 * @date : 2021/12/26 12:58 AM
 * @description:
 */
@Slf4j
@Join
public class ApacheDubboSchemaPlugin implements OutsidePluginSchemaSpi {

    private final ApplicationConfig applicationConfig = new ApplicationConfig("gateway-dubbo");

    static {
        //忽略校验重复生成实例,返回之前的实例
        System.setProperty("dubbo.config.ignore-duplicated-interface", "true");
    }

    /**
     * 真实处理
     *
     * @param ctx
     * @param data
     * @return {@link GatewayMessage }
     * @description:
     */
    @Override
    public void handler(FilterContext ctx, Object data) {
        //异步调用
        RouteBean routeBean = (RouteBean) ctx.getAttr().get(_FinalRouteBean);
        final DubboPathMeta dubboPathMeta = DubboPathMetaConvert.convertByMap(routeBean.getMetadata());
        final RegistryConfig registryConfigCache;


        try {
            registryConfigCache = ApacheDubboConfigCache.getRegistryConfigCache(dubboPathMeta.getRegistryAddress());
        } catch (ExecutionException e) {
            log.error("getRegistryConfigCache  dubbo params error:{}", routeBean, e);
            ctx.cancel(GATEWAY_ERROR("不支持该协议:" + GatewayMessages.GATEWAY_ERROR("获取注册中心异常")));
            return;
        }

        Pair<String[], Object[]> pair;
        try {
            pair = DubboParamsEncodeUtil.buildParameters(dubboPathMeta, DubboBodyEncodeUtil.encodeBody(((List<HttpObject>) data).get(1)));
        } catch (ExecutionException e) {
            log.error("convert dubbo params error:{}", routeBean, e);
            ctx.cancel(GatewayMessages.GATEWAY_ERROR("转换参数异常"));
            return;
        }

        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setApplication(applicationConfig);
        reference.setRegistry(registryConfigCache);
        // 设置调用的reference属性，下面只设置了协议、接口名、版本、超时时间
        reference.setProtocol(dubboPathMeta.getProtocol());
        reference.setInterface(dubboPathMeta.getInterfaceName());
        reference.setVersion(dubboPathMeta.getVersion());
        reference.setTimeout(1000);
        // 声明为泛化接口
        reference.setGeneric("true");
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();

        GenericService genericService;
        try {
            genericService = cache.get(reference);
        } catch (Exception e) {
            //暂时不在线,则直接剔除,防止下次调用还是错误
            ReferenceConfigCache.getCache().destroy(reference);
            log.error("get service error:{}", routeBean, e);
            ctx.cancel(GatewayMessages.build(500, Boolean.FALSE, "服务不在线", HttpResponseStatus.OK));
            return;
        }

        CompletableFuture<Object> completableFuture = genericService.$invokeAsync(dubboPathMeta.getMethodName(), pair.getKey(), pair.getValue());
        completableFuture.whenComplete((ret, throwable) -> {
            if (Objects.nonNull(throwable)) {
                log.error("invoke dubbo error:{}", routeBean, throwable);
                ctx.cancel(GatewayMessages.build(500, Boolean.FALSE, "调用异常,稍后再试", HttpResponseStatus.OK));
                return;
            }

            ctx.cancel(GatewayMessages.build(200, Boolean.TRUE, JSON.toJSONString(
                    ret), HttpResponseStatus.OK));
        });

    }
}
