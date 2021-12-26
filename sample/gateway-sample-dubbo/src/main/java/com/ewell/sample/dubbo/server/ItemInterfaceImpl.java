package com.ewell.sample.dubbo.server;

import com.ewell.sample.dubbo.api.ItemInterface;
import com.ewell.sample.dubbo.dto.ItemDTO;
import com.ewell.sample.dubbo.dto.Result;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * 商品
 * @author     : longchuan
 * @date       : 2021/10/3 3:51 下午
 * @description:
 * @version    :
 */
public class ItemInterfaceImpl implements ItemInterface {
    /**
     * 根据商品id和店铺或者商品详情
     *
     * @param commodityId
     * @param appId
     * @return
     * @description:
     */
    @Override
    public Result<List<ItemDTO>> getItemList(List<String> commodityId, Integer appId) {
        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setAppId(1);
        itemDTO.setCommodityId("110");
        itemDTO.setCommodityUrl("www.baidu.com");
        itemDTO.setStore(0L);
        itemDTO.setOnShelfFlag(Boolean.TRUE);
        itemDTO.setCommodityTitle("上架没库存不可用");

        ItemDTO itemDTO1 = new ItemDTO();
        itemDTO1.setAppId(1);
        itemDTO1.setCommodityId("111");
        itemDTO1.setCommodityUrl("www.baidu.com");
        itemDTO1.setStore(0L);
        itemDTO1.setOnShelfFlag(Boolean.FALSE);
        itemDTO1.setCommodityTitle("没库存下架不可用");

        ItemDTO itemDTO2 = new ItemDTO();
        itemDTO2.setAppId(1);
        itemDTO2.setCommodityId("112");
        itemDTO2.setCommodityUrl("www.baidu.com");
        itemDTO2.setStore(100L);
        itemDTO2.setOnShelfFlag(Boolean.TRUE);
        itemDTO2.setCommodityTitle("可用");
        return Result.newSuccessResult(Lists.newArrayList(itemDTO2,itemDTO1,itemDTO));
    }
}
