package com.arise.modules;

import com.arise.internal.chain.ChainContext;
import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 14:07 2021-03-01
 * @Description: 应用层协议
 * @Modified: By：
 */
public interface ProtocolHandler {

    void handleRequest(ChainContext ctx, ByteBuffer buffer);

    void handleResponse(ChainContext ctx, ByteBuffer buffer);

}
