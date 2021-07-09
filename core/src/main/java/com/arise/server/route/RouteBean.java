package com.arise.server.route;

import lombok.Data;

import javax.script.CompiledScript;
import java.io.Serializable;
import java.util.Map;

@Data
public class RouteBean implements Serializable {

    private String id;

    /**
     * js脚本
     */
    private String script;

    /**
     * 参数名称映射
     */
    private Map<String, String> pramMapping;

    /**
     * ex:
     * lb://UMS
     * https://cn.bing.com
     */
    private String service;

    private String gatewayPath;

    private String servicePath;

    private transient CompiledScript compiledScript;
}