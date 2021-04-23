package com.arise.modules;

import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 0:16 2021-04-08
 * @Description: 可转成缓冲区的
 * @Modified: By：
 */
public interface Bufferable {

    ByteBuffer toBuffer();
}
