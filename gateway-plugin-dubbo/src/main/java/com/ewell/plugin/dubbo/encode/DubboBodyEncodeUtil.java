package com.ewell.plugin.dubbo.encode;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.nio.charset.Charset;

/**
 * body解析
 * @author     : MrFox
 * @date       : 2021/12/26 3:04 PM
 * @description:
 * @version    :
 */
@Slf4j
@Singleton
public class DubboBodyEncodeUtil {


    /**
     * 解析body
     * @param
     * @return
     * @description:
     */
    public static String encodeBody(HttpObject httpObject) {
        HttpContent content = (HttpContent) httpObject;
        ByteBuf byteBuf = content.content();
        return byteBuf.toString(Charset.defaultCharset());
    }
}
