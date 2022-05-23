package com.ewell.core.server.handler;

import com.ewell.common.message.Message;
import com.ewell.core.filer.context.Observer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.collection.IntObjectMap;
import lombok.extern.slf4j.Slf4j;

import java.util.TreeSet;

import static com.ewell.common.IntMapConstant._RespObserver;
import static com.ewell.common.NettyAttrKeyConstant.FilterAttr;

/**
 * @Author: wy
 * @Date: Created in 5:24 下午 2021/11/30
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ProxyOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try {
            IntObjectMap<Object> map = ctx.channel().attr(FilterAttr).get();
            TreeSet<Observer> observers = (TreeSet<Observer>) map.get(_RespObserver);
            //处理http消息
            Message m = (Message) msg;
            observers.forEach(e -> e.getConsumer().accept(m));
            m.forEach(e -> {
                try {
                    ctx.write(e);
                } catch (Exception exception) {
                    log.error("发生消息发生异常:", exception);
                }
            });
            ctx.flush();
        } catch (Exception e) {
            log.error("发生消息发生异常:", e);
        }
    }


}
