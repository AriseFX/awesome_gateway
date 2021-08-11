package com.arise.server.logging;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

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
    private JSONObject requestParams;
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

    private HttpHeaders headers;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date requestTime;

    /**
     * 总耗时
     */
    private Long handleTime;

    /**
     * 前置处理时间
     */
    private Long preTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

}
