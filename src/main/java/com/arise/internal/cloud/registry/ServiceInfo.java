package com.arise.internal.cloud.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @Author: wy
 * @Date: Created in 10:32 2021-03-01
 * @Description: 服务详细信息
 * @Modified: By：
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInfo {

    private String serviceName;

    private List<InstanceInfo> instances;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InstanceInfo {
        private String ip;
        private int port;
        //权重
        private double weight;
        //健康状态
        private boolean healthy = true;
        //元数据
        private Map<String, String> metadata;
    }

}
