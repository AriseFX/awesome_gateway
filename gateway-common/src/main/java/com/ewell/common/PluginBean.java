package com.ewell.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: wy
 * @Date: Created in 3:28 下午 2021/9/9
 * @Description:
 * @Modified: By：
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginBean implements Serializable {

    public String name;
    public String script;
}
