package com.ewell.plugin.dubbo.convert;

import com.alibaba.fastjson.JSON;
import com.ewell.plugin.dubbo.consts.Constants;
import com.ewell.plugin.dubbo.model.DubboPathMeta;
import com.ewell.plugin.dubbo.model.DubboPathParamMeta;

import java.util.Map;

/**
 * 转换器
 * @author     : MrFox
 * @date       : 2021/12/26 5:31 PM
 * @description:
 * @version    :
 */
public class DubboPathMetaConvert {

    /**
     * 转换成 DubboPathMeta
     * @param
     * @return
     * @description:
     */
    public static DubboPathMeta convertByMap(Map<String, String> metadata){
        DubboPathMeta dubboPathMeta = new DubboPathMeta();
        dubboPathMeta.setRegistryAddress(metadata.get(Constants.REGISTRY_ADDRESS));
        dubboPathMeta.setMethodName(metadata.get(Constants.METHOD_NAME));
        dubboPathMeta.setProtocol(metadata.get(Constants.PROTOCOL));
        dubboPathMeta.setVersion(metadata.get(Constants.VERSION));
        dubboPathMeta.setInterfaceName(metadata.get(Constants.INTERFACE_NAME));
        dubboPathMeta.setParamsMeta(JSON.parseArray(metadata.get(Constants.PARAM_META), DubboPathParamMeta.class));
        return dubboPathMeta;
    }

}
