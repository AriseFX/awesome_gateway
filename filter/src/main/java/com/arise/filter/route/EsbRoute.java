package com.arise.filter.route;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: wy
 * @Date: Created in 10:36 2020/3/25
 * @Description: 路由信息, 序列化时使用
 * @Modified: By：
 */
@Data
public class EsbRoute implements Serializable {

    /**
     * 路由类型: 0:http,1:webService
     */
    private byte type;
    /**
     * 是否需要鉴权：0:不需要,1:需要
     */
    private byte needAuth;

    /**
     * 路径
     */
    private String path;

    /**
     * 后端host
     */
    private String host;

    /**
     * 后端url
     */
    private String backendUrl;

    /**
     * 用户域code
     */
    private String originCode;
}
