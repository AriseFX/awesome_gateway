package com.arise.server.route.logging;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 日志记录
 *
 * @author qiqiang
 * @date 2018-09-13
 */
@Data
public class RequestLogEntity implements Serializable {
    private String logId;
    private String path;
    private Map<String, String> requestParams;
    private String orgCode;
    private String ip;
    /**
     * 实际转发地址
     */
    private String targetUri;

    private Object responseBody;
    private Object requestBody;
    private String responseCode;

    private String username;

    private Map<String, Object> headers;

    private Long timestamp;

    /**
     * 总耗时
     */
    private Long handleTime;


    /**
     * 前置处理时间
     */
    private Long preTime;
}
