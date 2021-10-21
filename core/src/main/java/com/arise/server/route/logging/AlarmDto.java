package com.arise.server.route.logging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: wy
 * @Date: Created in 5:50 下午 2021/10/20
 * @Description:
 * @Modified: By：
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlarmDto {
    private String path;
    private String message;
    private String service;
    private String originCode;
    private String backendHeader;
}
