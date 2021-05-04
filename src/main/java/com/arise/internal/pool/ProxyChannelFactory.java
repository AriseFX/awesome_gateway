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
public class ProxyChannelFactory implements PooledObjectFactory<AwesomeSocketChannel> {

    @Override
    public PooledObject<AwesomeSocketChannel> makeObject() {
        return new DefaultPooledObject<>(new AwesomeSocketChannel(null, null));
    }

    @Override
    public void destroyObject(PooledObject<AwesomeSocketChannel> p) {

    }

    @Override
    public boolean validateObject(PooledObject<AwesomeSocketChannel> p) {
        return false;
    }

    @Override
    public void activateObject(PooledObject<AwesomeSocketChannel> p) {

    }

    @Override
    public void passivateObject(PooledObject<AwesomeSocketChannel> p) {

    }
}
