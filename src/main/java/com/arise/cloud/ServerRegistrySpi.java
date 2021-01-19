package com.arise.cloud;

/**
 * @Author: wy
 * @Description: 服务注册相关Spi
 * @Modified: By：
 */
public interface ServerRegistrySpi {

    /**
     * 获取服务列表
     */
    void getServices();
}
