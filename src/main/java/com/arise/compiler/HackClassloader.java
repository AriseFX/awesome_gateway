package com.arise.compiler;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Author: wy
 * @Date: Created in 15:57 2021/1/8
 * @Description: 一个不安全的类加载器, 使用hack的方式去加载类，违背了双亲委派机制
 * @Modified: By：
 */
public class HackClassloader {

    public static Method DEFINE_CLASS_METHOD = null;

    private static Unsafe unsafe;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
            Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass",
                    String.class, byte[].class, int.class, int.class);
            defineClass.setAccessible(true);
            //hack类加载器使用，必须override
            Field overrideField = AccessibleObject.class.getDeclaredField("override");
            overrideField.setAccessible(true);
            //获取override属性的地址偏移量
            long override_offset = unsafe.objectFieldOffset(overrideField);
            //修改override的属性是为了过jvm的检测
            unsafe.putBoolean(defineClass, override_offset, true);
            DEFINE_CLASS_METHOD = defineClass;
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成一个类
     *
     * @param cl        类加载器
     * @param className 类名
     * @param byteCode  字节码
     */
    public static Class<?> defineClass(ClassLoader cl, String className, byte[] byteCode) {
        try {
            return (Class<?>) DEFINE_CLASS_METHOD.invoke(cl, className, byteCode, 0, byteCode.length);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeCompilerException("defineClass 发生 异常：" + e);
        }
    }
}
