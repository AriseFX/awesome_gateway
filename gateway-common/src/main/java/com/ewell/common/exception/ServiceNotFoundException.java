package com.ewell.common.exception;

/**
 * @Author: wy
 * @Date: Created in 22:55 2021-02-25
 * @Description:
 * @Modified: By：
 */
public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
