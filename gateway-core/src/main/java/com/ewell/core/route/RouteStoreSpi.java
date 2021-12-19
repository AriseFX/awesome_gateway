package com.ewell.core.route;

import com.ewell.common.RouteBean;
import com.ewell.spi.SPI;

import java.util.Collection;

/**
 * @Author: wy
 * @Date: Created in 10:34 下午 2021/11/28
 * @Description: 路由持久化spi
 * @Modified: By：
 */
@SPI
public interface RouteStoreSpi {

    /**
     * 新增路由(批量)
     */
    void putRoutes(Collection<RouteBean> routes, RoutePromise<String> promise);

    /**
     * 查询路由(所有)
     */
    void getRoutes(RoutePromise<Object> promise);


    /**
     * 删除路由(所有)
     */
    void deleteRoutes(RoutePromise<String> promise);
}
