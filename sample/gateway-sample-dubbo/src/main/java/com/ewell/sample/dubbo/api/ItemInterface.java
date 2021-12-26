package com.ewell.sample.dubbo.api;

import com.ewell.sample.dubbo.dto.ItemDTO;
import com.ewell.sample.dubbo.dto.Result;

import java.util.List;

/**
 * 商品服务
 * @author     : longchuan
 * @date       : 2021/10/3 12:00 下午
 * @description:
 * @version    :
 */
public interface ItemInterface {


    /**
     * 根据商品id和店铺或者商品详情
     * @param
     * @param appId
     * @return
     * @description:
     */
    Result<List<ItemDTO>> getItemList(List<String> commodityId, Integer appId);

}
