package com.arise.internal.future;

import java.util.concurrent.Future;

/**
 * @Author: wy
 * @Date: Created in 16:00 2021-04-28
 * @Description:
 * @Modified: By：
 */
public interface Promise<V> extends Future<V> {

    /**
     * 标记成功状态
     */
    void setSuccess(boolean success);

    void
}
