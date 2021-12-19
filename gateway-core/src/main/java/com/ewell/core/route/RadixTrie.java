package com.ewell.core.route;

import com.ewell.common.RouteBean;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 21:15 2021-03-02
 * @Description: 基数树（基于rest规范实现）
 */
public class RadixTrie {

    public static final String PathPram = "PathPram";
    public static final String Placeholder = "{key}";
    public Node root;

    public RadixTrie() {
        this.root = new Node("/", null);
    }

    public void clear() {
        this.root = newEmptyNode();
    }

    private Node newEmptyNode() {
        return new Node("/", null);
    }

    /**
     * 匹配数据
     */
    public List<RouteBean> matching(String url) {
        String[] tokens = url.split("/");
        int i = StringUtils.isEmpty(tokens[0]) ? 1 : 0;
        Node prev = this.root;
        for (; i < tokens.length; i++) {
            String token = tokens[i];
            prev = prev.findToken(token + "/");
            if (prev == null) {
                return null;
            }
            if (i == tokens.length - 1) {
                //尾结点
                return prev.getRouteBeans();
            }
        }
        return null;
    }


    /**
     * 添加路由
     */
    public void addRoute(String url, RouteBean pointer) {
        String[] tokens = url.split("/");
        int i = StringUtils.isEmpty(tokens[0]) ? 1 : 0;
        Node prev = this.root;
        for (; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}') {
                token = Placeholder;
            }
            //初始化节点
            prev = prev.addChild(new Node(token + "/", prev));
            if (i == tokens.length - 1) {
                //尾结点
                prev.addPointer(pointer);
            }
        }
    }

    public static void main(String[] args) {
        RouteBean routeBean1 = new RouteBean();
        routeBean1.setId("1");
        RouteBean routeBean2 = new RouteBean();
        routeBean2.setId("2");
        RouteBean routeBean3 = new RouteBean();
        routeBean3.setId("3");

        RadixTrie trie = new RadixTrie();
        trie.addRoute("/test/use/{}", routeBean1);
        trie.addRoute("/test/user/get", routeBean2);
        trie.addRoute("/tat/use/qwe", routeBean3);

        List<RouteBean> matching = trie.matching("/tat/use/qwe");
        System.out.println(matching);
    }


    @Data
    static class Node {
        private String path;
        private Node parent;
        private List<Node> child;

        private List<RouteBean> routeBeans;

        public Node(String path, Node parent) {
            this.path = path;
            this.parent = parent;
        }

        public Node findToken(String token) {
            OUTER:
            for (Node node : child) {
                String path = node.getPath();
                if (token.length() < path.length()) {
                    continue;
                }
                for (int i = 0; i < path.length(); i++) {
                    if (path.charAt(i) != token.charAt(i)) {
                        continue OUTER;
                    }
                }
                char last = path.charAt(path.length() - 1);
                if (last == '/') {
                    return node;
                }
                return node.findToken(token.substring(path.length()));
            }
            return null;
        }

        public void addPointer(RouteBean route) {
            if (routeBeans == null) {
                routeBeans = new ArrayList<>(1);
            }
            routeBeans.add(route);
        }

        public Node addChild(Node node) {
            String token = node.getPath();
            if (token.length() == 0) {
                return node;
            }
            //判断当前节点是否可以向上合并
            Node parent = this.getParent();
            if (parent != null) {
                List<Node> child1 = parent.getChild();
                if (child1.size() == 1) {
                    String path = parent.getPath();
                    if (path.charAt(path.length() - 1) != '/') {
                        Node p_parent = parent.getParent();
                        p_parent.getChild().remove(parent);
                        this.setPath(parent.getPath() + this.getPath());
                        p_parent.getChild().add(this);
                        this.parent = p_parent;
                    }
                }
            }
            if (child == null) {
                child = new ArrayList<>(1);
            }
            //以下为分裂逻辑
            char c = token.charAt(0);
            ArrayList<Node> waitSplit = new ArrayList<>(1);
            Iterator<Node> iterator = child.iterator();
            while (iterator.hasNext()) {
                Node n = iterator.next();
                String path = n.getPath();
                //首字符相同
                if (path.charAt(0) == c) {
                    if (path.length() == 1) {
                        //已经压缩前缀，直接添加节点
                        String sub = token.substring(1);
                        if (sub.length() == 0) {
                            return n;
                        }
                        return n.addChild(new Node(sub, this));
                    } else {
                        //压缩前缀
                        iterator.remove();
                        waitSplit.add(n);
                    }
                }
            }
            if (waitSplit.size() == 0) {
                child.add(node);
                return node;
            } else {
                //分裂新的节点
                Node newNode = new Node(c + "", this);
                child.add(newNode);
                waitSplit.forEach(e -> {
                    Node n = e.removeFirstChar();
                    n.setParent(newNode);
                    newNode.addChild(n);
                });
                String sub = token.substring(1);
                return newNode.addChild(new Node(sub, newNode));
            }
        }

        public Node removeFirstChar() {
            this.path = path.substring(1);
            return this;
        }
    }
}
