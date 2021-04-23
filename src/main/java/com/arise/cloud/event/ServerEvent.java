package com.arise.cloud.event;

/**
 * @Author: wy
 * @Date: Created in 16:31 2021-02-26
 * @Description: 服务节点事件
 * @Modified: By：
 */
public enum ServerEvent {
    UP(event -> {

    }),
    DOWN(event -> {

    }),
    UPDATE(event -> {

    });
    public EventHandler handler;

    ServerEvent(EventHandler handler) {
        this.handler = handler;
    }
}
