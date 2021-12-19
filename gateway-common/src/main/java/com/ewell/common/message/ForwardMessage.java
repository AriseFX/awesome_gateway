package com.ewell.common.message;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpObject;
import lombok.Getter;

import java.util.List;
import java.util.function.Consumer;

/**
 * @Author: wy
 * @Date: Created in 10:24 上午 2021/12/1
 * @Description:
 * @Modified: By：
 */
public class ForwardMessage implements Message {

    @Getter
    private final List<HttpObject> response;

    public ForwardMessage(List<HttpObject> response) {
        this.response = response;
    }

    @Override
    public void write2Channel(Channel channel) {
        channel.write(this);
    }

    @Override
    public void forEach(Consumer<Object> consumer) {
        response.forEach(consumer);
    }
}
