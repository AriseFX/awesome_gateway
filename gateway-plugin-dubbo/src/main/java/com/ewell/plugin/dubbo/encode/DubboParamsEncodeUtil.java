package com.ewell.plugin.dubbo.encode;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ewell.common.RouteBean;
import com.ewell.plugin.dubbo.consts.Constants;
import com.ewell.plugin.dubbo.consts.DubboPathParamMetaTypeEnum;
import com.ewell.plugin.dubbo.model.DubboPathMeta;
import com.ewell.plugin.dubbo.model.DubboPathParamMeta;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * dubbo参数解析
 * @author     : MrFox
 * @date       : 2021/12/26 3:31 PM
 * @description:
 * @version    :
 */
@Slf4j
@Singleton
public class DubboParamsEncodeUtil {

    private static final Cache<String,Pair<String[],String[]>> METHOD_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(300, TimeUnit.MINUTES)
            .maximumSize(Constants.CACHE_SIZE)
            .build();

    private static final String[] PARAMS_EMPTY = new String[0];

    private static final Pair<String[],String[]> PARAMS_META_EMPTY = Pair.of(null,null);

    private static final Pair<String[],Object[]> EMPTY =  Pair.of(PARAMS_EMPTY,new Object[0]);

    /**
     * 解析参数和入参
     * @param
     * @param dubboPathMeta
     * @return
     * @description:
     */
    public static Pair<String[], Object[]> buildParameters(final DubboPathMeta dubboPathMeta, final String params) throws ExecutionException {
        Pair<String[], String[]> pairs = METHOD_CACHE.get(dubboPathMeta.getInterfaceName() + Constants.SPLIT + dubboPathMeta.getMethodName()
                , () -> {
                    List<DubboPathParamMeta> paramsMeta = dubboPathMeta.getParamsMeta();
                    if (CollectionUtils.isEmpty(paramsMeta)) {
                        return PARAMS_META_EMPTY;
                    }

                    String[] paramsTypes = new String[paramsMeta.size()];
                    String[] paramsNames = new String[paramsMeta.size()];
                    paramsMeta.sort(Comparator.comparingInt(DubboPathParamMeta::getSort));
                    for (int i = 0; i < paramsMeta.size(); i++) {
                        DubboPathParamMeta dubboPathParamMeta = paramsMeta.get(i);
                        paramsTypes[i] = dubboPathParamMeta.getParamType();
                        paramsNames[i] = dubboPathParamMeta.getParamName();
                    }

                            return Pair.of(paramsTypes, paramsNames);
                });

        if(ArrayUtils.isEmpty(pairs.getKey())){
            return EMPTY;
        }

        //赋值
        String[] tmpParamsNames = pairs.getValue();
        Object[] paramArr = new Object[tmpParamsNames.length];

        JSONObject jsonObject = JSON.parseObject(params);

        for (int i = 0; i < tmpParamsNames.length; i++) {
            String value = tmpParamsNames[i];
            Object obj = jsonObject.get(value);
            if(Objects.nonNull(obj)){
                paramArr[i] = obj;
            }
        }

        return Pair.of(pairs.getLeft(),paramArr);
    }

}
