package com.arise.server.route.manager;

import com.alibaba.nacos.api.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 21:15 2021-03-02
 * @Description: 路由字典树（基于rest规范实现）
 * <p>
 * request1:   /api/ums/user/{id}
 * request2:   /api/oms/test/{id}
 * route1:     /api/ums/user/{id}    ->  http://UMS/**
 * route2:     /api/oms/**           ->  http://OMS/**
 * route2:     /api/user             ->  http://OMS/**
 */
public class RestRouteTrie<T> {

    public static final String Standard_Wildcard = "{key}";
    public Node<T> root;

    public RestRouteTrie() {
        this.root = new Node<>(null, null, new HashMap<>());
    }

    public void init() {
        this.addRoute("test", (T) "lb://BM/test");
        this.addRoute("bos/xtmls/search/current", (T) "lb://FDSbos/xtmls/search/current");
        this.addRoute("addOrigin", (T) "lb://FDSaddOrigin");
        this.addRoute("selectOriginInfo", (T) "lb://FDSselectOriginInfo");
        this.addRoute("selectOriginInfoList", (T) "lb://FDSselectOriginInfoList");
        this.addRoute("updateOriginInfo", (T) "lb://FDSupdateOriginInfo");
        this.addRoute("fds/medInst/getMedInstList", (T) "lb://FDSfds/medInst/getMedInstList");
        this.addRoute("getSqm", (T) "lb://FDS/getSqm");
        this.addRoute("getUserName", (T) "lb://FDS/getUserName");
        this.addRoute("verificationGrantCode", (T) "lb://FDS/verificationGrantCode");
        this.addRoute("bos/principal", (T) "lb://FDS/bos/principal");
        this.addRoute("fds/log/{id}/getSignSyncLogList", (T) "lb://FDS/fds/log/{id}/getSignSyncLogList");
        this.addRoute("fds/log/signSync/getSignSyncLogInfo", (T) "lb://FDS/fds/log/signSync/getSignSyncLogInfo");
        //System.out.println(tree);
    }

    public void clear() {
        this.root = new Node<>(null, null, new HashMap<>());
    }

    /**
     * 匹配数据
     */
    public List<T> matching(String url) {
        List<T> res = new LinkedList<>();
        HashMap<CharSequence, Node<T>> child = root.child;
        String[] tokens = url.split("/");
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            Node<T> node = child.get(token);
            if (node == null) {
                node = child.get(Standard_Wildcard);
                if (node == null) {
                    break;
                }
            }
            //最后一个节点
            T route = node.getPointer();
            if (route != null && i == tokens.length - 1) {
                res.add(route);
                break;
            }
            child = node.child;
        }
        return res;
    }


    /**
     * 添加路由
     */
    public void addRoute(String url, T pointer) {
        HashMap<CharSequence, Node<T>> child = root.child;
        String[] tokens = url.split("/");
        int i = StringUtils.isEmpty(tokens[0]) ? 1 : 0;
        for (; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}') {
                token = Standard_Wildcard;
            }
            Node<T> node = child.get(token);
            if (node == null) {
                child.put(token, node = new Node<>(token, null, new HashMap<>()));
            }
            child = node.child;
            //最后一位
            if (i == tokens.length - 1) {
                node.setPointer(pointer);
            }
        }
    }

    @Data
    @AllArgsConstructor
    static class Node<T> {
        private String content;
        //该节点
        private T pointer;
        private HashMap<CharSequence, Node<T>> child;
    }
}
