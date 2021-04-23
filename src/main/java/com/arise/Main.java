package com.arise;

import com.arise.server.ServerRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static io.netty.channel.epoll.Native.KERNEL_VERSION;

/**
 * @Author: wy
 * @Description: 基于Springboot构建，服务入口是{@link ServerRunner}
 * @Modified: By：
 */
@Slf4j
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}