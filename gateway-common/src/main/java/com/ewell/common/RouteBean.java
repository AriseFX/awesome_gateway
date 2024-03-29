package com.ewell.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class RouteBean implements Serializable {

    private String id;

    /**
     * ex:
     * lb://UMS
     * https://cn.bing.com
     */
    private String service;

    private String tag;

    private String gatewayPath;

    private String servicePath;

    private Map<String, String> metadata;

    private List<PluginBean> plugins;

}