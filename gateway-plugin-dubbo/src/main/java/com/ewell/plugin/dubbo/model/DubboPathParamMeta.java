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

    private String paramName;

    private Integer sort;
}
