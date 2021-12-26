package com.ewell.sample.dubbo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品
 * @author     : longchuan
 * @date       : 2021/10/3 3:24 下午
 * @description:
 * @version    :
 */
@Data
public class ItemDTO  implements Serializable {

    private static final long serialVersionUID = -5795089018013798231L;


    /**
     * 店铺id
     * */
    private Integer appId;

    /**
     * 商品id
     * */
    private String commodityId;

    /**
     * 商品价格
     * */
    private Long originalPrice;

    /**
     * 商品链接
     * */
    private String commodityUrl;

    /**
     * 商品名
     * */
    private String commodityTitle;

    /**
     * 商品属性
     * */
    private String properties;

    /**
     * 商品库存
     * */
    private Long store;

    /**
     * 商品是否上架
     * */
    private Boolean onShelfFlag;
}
