package com.ewell.common.message;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpObject;

import java.util.List;
import java.util.function.Consumer;

/**
 * @Author: wy
 * @Date: Created in 12:28 上午 2021/12/1
 * @Description:
 * @Modified: By：
 */
public interface Message {

    void write2Channel(Channel channel);

    void forEach(Consumer<Object> consumer);

    List<HttpObject> getResponse();
}
