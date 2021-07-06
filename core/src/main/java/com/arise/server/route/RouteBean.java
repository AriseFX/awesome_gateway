package com.arise.server.route;

import lombok.Data;

import javax.script.CompiledScript;
import java.io.Serializable;

@Data
public class RouteBean implements Serializable {

    private String id;

    /**
     * js脚本
     */
    private String script;

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