package com.arise;

import com.arise.network.NettyServer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.annotation.Resource;

/**
 * @Author: wy
 * @Description: 基于Springboot构建
 * @Modified: By：
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}