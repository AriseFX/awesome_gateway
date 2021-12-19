package com.ewell.common;

import io.netty.util.AttributeKey;
import io.netty.util.collection.IntObjectMap;

/**
 * @Author: wy
 * @Date: Created in 5:29 下午 2021/9/6
 * @Description:
 * @Modified: By：
 */
public interface NettyAttrKeyConstant {

    AttributeKey<IntObjectMap<Object>> FilterAttr = AttributeKey.newInstance("FilterAttr");

}
