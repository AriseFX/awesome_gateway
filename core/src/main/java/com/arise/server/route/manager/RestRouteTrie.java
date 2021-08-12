package com.arise.server.route.manager;

import com.alibaba.nacos.api.utils.StringUtils;
import com.arise.server.route.RouteBean;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
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
public class RestRouteTrie {

    public static final String Standard_Wildcard = "{key}";
    public Node root;

    public RestRouteTrie() {
        this.root = newEmptyNode();
    }

    public void clear() {
        this.root = newEmptyNode();
    }

    private Node newEmptyNode() {
        return new Node(null, null, new HashMap<>());
    }

    /**
     * 匹配数据
     */
    public List<RouteBean> matching(String url) {
        List<RouteBean> res = new LinkedList<>();
        HashMap<CharSequence, Node> child = root.child;
        String[] tokens = url.split("/");
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            Node node = child.get(token);
            if (node == null) {
                node = child.get(Standard_Wildcard);
                if (node == null) {
                    break;
                }
            }
            //最后一个节点
            List<RouteBean> route = node.getPointer();
            if (route != null && i == tokens.length - 1) {
                res.addAll(route);
                break;
            }
            child = node.child;
        }
        return res;
    }


    /**
     * 添加路由
     */
    public void addRoute(String url, RouteBean pointer) {
        HashMap<CharSequence, Node> child = root.child;
        String[] tokens = url.split("/");
        int i = StringUtils.isEmpty(tokens[0]) ? 1 : 0;
        for (; i < tokens.length; i++) {
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
                node.addPointer(pointer);
            }
        }
    }

    @Data
    @AllArgsConstructor
    static class Node {
        private String content;
        private List<RouteBean> pointer;
        private HashMap<CharSequence, Node> child;

        public void addPointer(RouteBean route) {
            if (pointer == null) {
                pointer = new ArrayList<>(1);
            }
            pointer.add(route);
        }
    }
}
