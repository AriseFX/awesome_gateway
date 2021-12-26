package com.ewell.sample.dubbo.bootstrap;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;

/**
 * 泛华调用
 * @author     : longchuan
 * @date       : 2021/10/4 7:40 下午
 * @description:
 * @version    :
 */
public class DubboConsumerGenericRpcMain {
    public static void main(String[] args) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        // 当前dubbo consumer的application配置，不设置会直接抛异常
        // 注册中心配置
        RegistryConfig registryConfig = new RegistryConfig();
        // 注册中心这里需要配置上注册中心协议，例如下面的zookeeper
        registryConfig.setAddress("nacos://127.0.0.1:8848");
        reference.setRegistry(registryConfig);
        // 设置调用的reference属性，下面只设置了协议、接口名、版本、超时时间
        reference.setProtocol("dubbo");
        reference.setInterface("com.ewell.sample.dubbo.api.ItemInterface");
        reference.setVersion("1.0.0");
        reference.setTimeout(1000);
        // 声明为泛化接口
        reference.setGeneric("true");
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        GenericService genericService = cache.get(reference);

        // GenericService可以接住所有的实现
        Object result = genericService.$invoke("getItemList",
                new String[]{"java.util.List", "java.lang.Integer"},
                new Object[]{Lists.newArrayList("111"), 1});
        System.out.println(JSON.toJSONString(result));
    }
}
