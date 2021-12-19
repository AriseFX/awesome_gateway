package com.ewell.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @Author: wy
 * @Date: Created in 11:22 上午 2021/11/26
 * @Description:
 * @Modified: By：
 */
@Data
public class GatewayConfig {

    //端口
    private int port;
    //地址
    private String address;
    //响应超时
    private int respTimeout = 5000;
    //连接池
    private Pool pool;
    //注册中心相关
    private Registry registry;
    //redis
    private Redis redis;
    //rabbitmq
    private Rabbitmq rabbitmq;
    //endpint
    private Endpoint endpoint;
    //logging
    private Logging logging;
    private boolean affinity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Logging {
        //单个磁盘队列大小(字节)
        private int diskQueueSize;
        //排除
        private Set<String> excludePath;
        //只记录某些header的日志
        private Map<String, Set<String>> reqHeader;
        private Map<String, Set<String>> respHeader;

        public void setReqHeader(Map<String, Set<String>> reqHeader) {
            reqHeader.entrySet().forEach(e -> e.setValue(toTreeSet(e.getValue())));
            this.reqHeader = reqHeader;
        }

        public void setRespHeader(Map<String, Set<String>> respHeader) {
            respHeader.entrySet().forEach(e -> e.setValue(toTreeSet(e.getValue())));
            this.respHeader = respHeader;
        }

        private TreeSet<String> toTreeSet(Set<String> in) {
            return in.stream().collect(Collectors
                    .toCollection(() ->
                            new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endpoint {
        //端口
        private int port;
        //地址
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Redis {
        /**
         * 单机无ssl模式: redis://[password@]host[:port][/databaseNumber][?[timeout=timeout[d|h|m|s|ms|us|ns]]
         * ex: redis://mypassword@127.0.0.1:6379/0?timeout=10s
         * 哨兵模式: redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber][?[timeout=timeout[d|h|m|s|ms|us|ns]]#sentinelMasterId
         * ex: redis-sentinel://mypassword@127.0.0.1:6379,127.0.0.1:6380/0?timeout=10s#mymaster
         */
        private String uri;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbitmq {
        /**
         * amqp://username:password@hostname:port/vhost
         */
        private String uri;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Registry {
        private String naming;
        private String serviceName;
        private String serverAddr;
        private String namespace;
        private String networkInterface;
        private String ip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pool {
        private int timeout;
        private int maxConnections;
        private int maxPendingAcquires;
    }
}
