package com.ewell.core.route;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.InetSocketAddress;

/**
 * @Author: wy
 * @Date: Created in 5:11 下午 2021/8/8
 * @Description:
 * @Modified: By：
 */
@Data
@AllArgsConstructor
public class MatchRes {
    private boolean ssl;
    private InetSocketAddress address;
    private String rewriteUrl;
}
