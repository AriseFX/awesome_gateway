package com.ewell.core.filer.context;

import com.ewell.common.message.GatewayMessage;
import lombok.Getter;

/**
 * @Author: wy
 * @Date: Created in 2:44 下午 2021/11/26
 * @Description:
 * @Modified: By：
 */

public class FilterFuture {

    @Getter
    private final boolean success;

    @Getter
    private final GatewayMessage httpMessage;

    @Getter
    private final Object result;

    private FilterFuture(boolean success, GatewayMessage httpMessage, Object result) {
        this.success = success;
        this.httpMessage = httpMessage;
        this.result = result;
    }

    /**
     * 取消(会直接响应httpMessage给inbound)
     */
    public static FilterFuture CANCEL(GatewayMessage httpMessage) {
        return new FilterFuture(false, httpMessage, null);
    }

    /**
     * 成功
     */
    public static FilterFuture SUCCESS(Object result) {
        return new FilterFuture(true, null, result);
    }
}
