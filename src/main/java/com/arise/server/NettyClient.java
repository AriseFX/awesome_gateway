package com.arise.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @Author: wy
 * @Date: Created in 16:10 2021/1/19
 * @Description:
 * @Modified: By：
 */
public class NettyClient {

    public void init() {
        EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("worker"));
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
    }

}
