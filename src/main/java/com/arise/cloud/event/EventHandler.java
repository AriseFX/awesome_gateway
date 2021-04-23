package com.arise.cloud.event;


/**
 * @Author: wy
 * @Date: Created in 19:07 2021-02-28
 * @Description:
 * @Modified: By：
 */
public interface EventHandler {

    void onEvent(Object event);
}
