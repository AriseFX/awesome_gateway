package com.arise.endpoint.service.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 22:25 2021-07-04
 * @Description:
 * @Modified: By：
 */
@Data
public class RouteAddDto implements Serializable {

    private List<RouteBean> routes;

    @Data
    public static class RouteBean implements Serializable {

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
    }
}
