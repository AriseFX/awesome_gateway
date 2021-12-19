package com.ewell.common.exception;

/**
 * @Author: wy
 * @Date: Created in 11:08 下午 2021/11/28
 * @Description:
 * @Modified: By：
 */
public class SimpleRuntimeException extends RuntimeException {

    public SimpleRuntimeException(String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
