package com.ewell.common.exception;

/**
 * @Author: wy
 * @Date: Created in 1:25 下午 2021/9/15
 * @Description:
 * @Modified: By：
 */
public class GatewayException extends RuntimeException {

    public GatewayException(String message) {
        super("gateway error:{" + message + "}");
    }
}
