package com.arise.queue;

import java.nio.ByteBuffer;

/**
 * @Author: wy
 * @Date: Created in 9:24 下午 2021/10/26
 * @Description:
 * @Modified: By：
 */
public interface MySerializable {

    ByteBuffer marshaller();

    void destructor();
}
