package com.arise;

import com.arise.server.ServerRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @Author: wy
 * @Description: 基于Springboot构建，服务入口是{@link ServerRunner}
 * @Modified: By：
 */
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}