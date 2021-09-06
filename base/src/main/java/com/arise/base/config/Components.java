package com.arise.base.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wy
 * @Date: Created in 5:31 下午 2021/8/30
 * @Description:
 * @Modified: By：
 */
public class Components {

    private static final Map<Class<?>, Object> map = new ConcurrentHashMap<>(13);

    public static <T> T get(Class<T> clazz) {
        return (T) map.get(clazz);
    }

    public static void put(Object obj) {
        map.put(obj.getClass(), obj);
    }
}
