package com.arise.modules;

/**
 * @Author: wy
 * @Date: Created in 18:45 2021-04-15
 * @Description:
 * @Modified: By：
 */
public interface EventProcessor {

    void onRead();

    void onWrite();

    void onError();

    void onClose();
}
