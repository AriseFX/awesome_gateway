package com.ewell.core.route;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @Author: wy
 * @Date: Created in 11:00 下午 2021/11/28
 * @Description:
 * @Modified: By：
 */
public class RoutePromise<T> extends DefaultPromise<T> {

    public RoutePromise() {
        super(GlobalEventExecutor.INSTANCE);
    }
}
