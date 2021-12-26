

package com.ewell.plugin.dubbo.cache;

import com.ewell.plugin.dubbo.consts.Constants;
import com.ewell.plugin.dubbo.consts.DubboPathParamMetaTypeEnum;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.dubbo.config.RegistryConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * The type Application config cache.
 */
public final class ApacheDubboConfigCache  {

    private static final Cache<String,RegistryConfig> REGISTRY_CONFIG_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(600, TimeUnit.MINUTES)
            .maximumSize(Constants.CACHE_SIZE)
            .build();



    public static RegistryConfig getRegistryConfigCache(final String address) throws ExecutionException {
        return REGISTRY_CONFIG_CACHE.get(address, () -> {
            RegistryConfig registryConfig = new RegistryConfig();
            // 注册中心这里需要配置上注册中心协议，例如下面的zookeeper
            registryConfig.setAddress(address);
            return registryConfig;
        });
    }

}
