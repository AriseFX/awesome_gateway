//package com.arise.server;
//
//import com.arise.Main;
//import net.openhft.affinity.AffinityLock;
//import net.openhft.affinity.AffinityStrategies;
//
//import java.util.concurrent.ThreadFactory;
//
///**
// * @Author: wy
// * @Date: Created in 14:20 2020/12/27
// * @Description: cpu亲和的线程工厂
// * @Modified: By：
// */
//public class AwesomeThreadFactory implements ThreadFactory {
//    private int id = 1;
//
//    @Override
//    public Thread newThread(Runnable r) {
//        Thread thread = new Thread(() -> {
//            try (AffinityLock sub = Main.mainBinder.acquireLock(AffinityStrategies.SAME_SOCKET,
//                    AffinityStrategies.DIFFERENT_CORE)) {
//                r.run();
//            }
//        });
//        thread.setName("epoll_thread-" + id);
//        id++;
//        return thread;
//    }
//}
