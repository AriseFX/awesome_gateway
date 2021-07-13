package com.arise.server.route;

import com.arise.server.route.filter.SchedulableFilter;
import io.netty.handler.codec.http.HttpObject;

import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 16:43 2021-07-13
 * @Description: 请求响应内容过滤器
 * @Modified: By：
 */
public abstract class ReqRespFilter implements SchedulableFilter<List<HttpObject>> {

}
