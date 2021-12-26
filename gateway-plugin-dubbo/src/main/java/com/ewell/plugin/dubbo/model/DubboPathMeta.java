package com.ewell.plugin.dubbo.model;

import lombok.Data;

import java.util.List;

/**
 * dubbo元数据
 * @author     : MrFox
 * @date       : 2021/12/26 3:55 PM
 * @description:
 * @version    :
 */
@Data
public class DubboPathMeta {

    private String registryAddress;

    private String interfaceName;

    private String version;

    private String protocol;

    private String methodName;

    private List<DubboPathParamMeta> paramsMeta;
}
