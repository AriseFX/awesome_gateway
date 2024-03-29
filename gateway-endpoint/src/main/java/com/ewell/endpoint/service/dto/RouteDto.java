package com.ewell.endpoint.service.dto;

import com.ewell.common.RouteBean;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: wy
 * @Date: Created in 22:25 2021-07-04
 * @Description:
 * @Modified: By：
 */
@Data
public class RouteDto implements Serializable {

    private List<RouteBean> routes;

}
