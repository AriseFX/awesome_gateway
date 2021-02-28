package com.arise.internal.utils;

import java.net.*;
import java.util.*;

/**
 * @Author: wy
 * @Date: Created in 16:57 2021-02-25
 * @Description:
 * @Modified: By：
 */
public class NetUtils {

    private static final Map<String, InetAddress> netList;

    static {
        netList = new HashMap<>();
        try {
            Enumeration<?> netInterfaces = NetworkInterface.getNetworkInterfaces();//获取当前环境下的所有网卡
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
                if (ni.isLoopback()) {
                    continue;//过滤回环网卡
                }
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                if (inetAddresses.hasMoreElements()) {
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
        return address.getHostAddress();
    }
}
