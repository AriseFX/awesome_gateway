package com.arise.filter.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.script.CompiledScript;

/**
 * @Author: wy
 * @Date: Created in 23:37 2021-06-30
 * @Description: 定义一个路由
 * @Modified: By：
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Route {

    private String url;

    private CompiledScript script;
}
