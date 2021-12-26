package com.ewell.plugin.dubbo.model;

import lombok.Data;

/**
 *
 * 参数元数据
 * @author     : MrFox
 * @date       : 2021/12/26 3:57 PM
 * @description:
 * @version    :
 */
@Data
public class DubboPathParamMeta {

    private String paramType;

    /**
     * 元数据类型,目前暂时支持 eg:
     *  LIST
     *  PRIMITIVE(包含Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE)
     *  OBJECT
     * {@link  com.ewell.plugin.dubbo.consts.DubboPathParamMetaTypeEnum}
     * */
    private String metaType;

    private String paramName;

    private Integer sort;
}
