package com.arise.server.logging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @Author: wy
 * @Date: Created in 5:49 下午 2021/8/10
 * @Description: 对log的一些处理。比如
 * @Modified: By：
 */
public class ApiLogUtils {

    public static RabbitTemplate rabbitTemplate;

    public static void saveMsg(RequestLogEntity msg) {
        try {
            rabbitTemplate.convertAndSend("gateway-queue", msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //报警
    public static void alarm() {

    }

}
