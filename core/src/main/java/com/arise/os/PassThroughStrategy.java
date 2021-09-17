package com.arise.os;


import io.netty.channel.Channel;

/**
 * @Author: wy
 * @Date: Created in 16:49 2021-06-23
 * @Description: 两个channel之间 流量透传
 * @Modified: By：
 */
@FunctionalInterface
public interface PassThroughStrategy {
    void accept(Channel channel1, Channel channel2);
}