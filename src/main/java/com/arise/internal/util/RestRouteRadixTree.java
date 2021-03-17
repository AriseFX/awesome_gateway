package com.arise.internal.util;

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
public class RestRouteRadixTree {

    public static final String Standard_Wildcard = "{key}";
    public Node root;

    public RestRouteRadixTree() {
        this.root = new Node(null, null, new HashMap<>());
    }

    public static void main(String[] args) {
        RestRouteRadixTree tree = new RestRouteRadixTree();
        tree.addRoute("api/ums/user", "we");
        tree.addRoute("bos/xtmls/search/current", "bos/xtmls/search/current");
        tree.addRoute("addOrigin", "addOrigin");
        tree.addRoute("selectOriginInfo", "selectOriginInfo");
        tree.addRoute("selectOriginInfoList", "selectOriginInfoList");
        tree.addRoute("updateOriginInfo", "updateOriginInfo");
        tree.addRoute("fds/medInst/getMedInstList", "fds/medInst/getMedInstList");
        tree.addRoute("getSqm", "getSqm");
        tree.addRoute("getUserName", "getUserName");
        tree.addRoute("verificationGrantCode", "verificationGrantCode");
        tree.addRoute("bos/principal", "bos/principal");
        tree.addRoute("fds/log/{id}/getSignSyncLogList", "fds/log/{id}/getSignSyncLogList");
        tree.addRoute("fds/log/signSync/getSignSyncLogInfo", "fds/log/signSync/getSignSyncLogInfo");
        System.out.println(tree);

        List<Object> matching = tree.matching("fds/log/3123123/getSignSyncLogList");
        System.out.println(matching);
    }

    /**
     * 匹配数据
     */
    public List<Object> matching(String url) {
        List<Object> res = new LinkedList<>();
        HashMap<CharSequence, Node> child = root.child;
        String[] tokens = url.split("/");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            Node node = child.get(token);
            if (node == null) {
                node = child.get(Standard_Wildcard);
                if (node == null) {
                    break;
                }
            }
            //最后一个节点
            Object route = node.getPointer();
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
    public void addRoute(String url, Object pointer) {
        HashMap<CharSequence, Node> child = root.child;
        String[] tokens = url.split("/");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}') {
                token = Standard_Wildcard;
            }
            Node node = child.get(token);
            if (node == null) {
                child.put(token, node = new Node(token, null, new HashMap<>()));
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
    static class Node {
        private String content;
        //该节点
        private Object pointer;
        private HashMap<CharSequence, Node> child;
    }


}
