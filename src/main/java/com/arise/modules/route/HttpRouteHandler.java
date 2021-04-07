package com.arise.modules.route;

import com.arise.internal.chain.ChainContext;
import com.arise.modules.ProtocolHandler;

/**
 * @Author: wy
 * @Date: Created in 9:29 2021-04-07
 * @Description: 处理路由的逻辑
 * @Modified: By：
 */
public class HttpRouteHandler implements ProtocolHandler {

    @Override
    public void handleRequest(ChainContext ctx, Object msg) {
//        AbstractSocketChannel channel = new AbstractSocketChannel(
//                new InetSocketAddress("192.168.150.102", 8099), null);
//        channel.write0(ctx, , );
        System.out.println(msg);

    }

    @Override
    public void handleResponse(ChainContext ctx, Object msg) {

    }
}
