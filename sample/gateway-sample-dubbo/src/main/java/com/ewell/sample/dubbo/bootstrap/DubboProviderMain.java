package com.ewell.sample.dubbo.bootstrap;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * test for dubbo-server
 * @author     : MrFox
 * @date       : 2021/12/25 11:57 PM
 * @description:
 * @version    :
 */
public class DubboProviderMain {
    public static void main(String[] args) throws IOException {
        //通过spring的应用上下文加载dubbo-server.xml
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("dubbo-server.xml");

        //启动
        context.start();

        System.out.println("start success");

        //阻塞当前进程
        System.in.read();
    }
}
