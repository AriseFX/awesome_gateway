package com.arise.internal.pool;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * @Author: wy
 * @Date: Created in 20:46 2021-03-31
 * @Description: 用于管理SocketChanel的生命周期
 * @Modified: By：
 */
//@Component
public class ProxyChannelFactory implements PooledObjectFactory<SocketChannel> {

    @Override
    public PooledObject<SocketChannel> makeObject() {
        return new DefaultPooledObject<>(new SocketChannel(null, null));
    }

    @Override
    public void destroyObject(PooledObject<SocketChannel> p) {

    }

    @Override
    public boolean validateObject(PooledObject<SocketChannel> p) {
        return false;
    }

    @Override
    public void activateObject(PooledObject<SocketChannel> p) {

    }

    @Override
    public void passivateObject(PooledObject<SocketChannel> p) {

    }
}
