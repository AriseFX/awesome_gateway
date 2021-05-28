//package com.arise.modules.pool;
//
//import com.arise.modules.http.HttpRouteChannel;
//
//import java.net.InetSocketAddress;
//import java.util.Deque;
//import java.util.concurrent.ConcurrentLinkedDeque;
//
///**
// * @Author: wy
// * @Date: Created in 14:33 2021-05-28
// * @Description:
// * @Modified: By：
// */
//public class HttpRouteChannelPool {
//
//    private final Deque<HttpRouteChannel> deque = new ConcurrentLinkedDeque<>();
//
//    private InetSocketAddress remote;
//
//    public HttpRouteChannelPool(String host, int port) {
//        this.remote = new InetSocketAddress(host, port);
//    }
//
//    public HttpRouteChannel getChannel() {
//        HttpRouteChannel channel = deque.poll();
//    }
//
//    public void release(HttpRouteChannel channel) {
//
//    }
//
//    private void createOrFormPool() {
//
//    }
//
//}
