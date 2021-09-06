package com.arise.base.util;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 16:57 2021-02-25
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class NetUtils {

    private static final Map<String, InetAddress> netList;

    static {
        netList = new HashMap<>();
        try {
            Enumeration<?> netInterfaces = NetworkInterface.getNetworkInterfaces();//获取当前环境下的所有网卡
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
                if (ni.isLoopback()) {
                    //过滤环回网卡
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (address.getHostAddress().equalsIgnoreCase("127.0.0.1")) {
                            continue;
                        }
                        if (address instanceof Inet6Address) {
                            continue;
                        }
                        if (address instanceof Inet4Address) {
                            netList.put(ni.getName(), address);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定网卡的绑定的IPV4地址
     */
    public static String getAddress(String interfaceName) {
        InetAddress address = netList.get(interfaceName);
        if (address == null) {
            log.error("无指定网卡,使用默认网卡。可用网卡列表:{}", netList.keySet());
            if (netList.size() > 0) {
                return netList.values()
                        .toArray(new InetAddress[]{})[0]
                        .getHostAddress();
            }
            throw new RuntimeException("无可用网卡！");
        }
        return address.getHostAddress();
    }

    private static Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long addressOf(Object o) {
        Object[] array = new Object[]{o};
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        //arrayBaseOffset方法是一个本地方法，可以获取数组第一个元素的偏移地址
        int addressSize = unsafe.addressSize();
        long objectAddress;
        switch (addressSize) {
            case 4:
                objectAddress = unsafe.getInt(array, baseOffset);
                //getInt方法获取对象中offset偏移地址对应的int型field的值
                break;
            case 8:
                objectAddress = unsafe.getLong(array, baseOffset);
                //getLong方法获取对象中offset偏移地址对应的long型field的值
                break;
            default:
                throw new Error("unsupported address size: " + addressSize);
        }
        return (objectAddress);
    }
}
