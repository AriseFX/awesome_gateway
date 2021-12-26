package com.ewell.sample.dubbo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回结果对象
 *
 * @Author: QingBo
 * @Date: 2020/9/4 4:07 下午
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = -4667239466889169994L;

    /**
     * 业务响应数据
     */
    private T                 value;
    private boolean           success          = true;
    private String            errorMessage;
    private String            errorCode;

    /**
     * 创建一个result。
     */
    public Result(){
    }

    /**
     * 创建一个result。
     *
     * @param success 是否成功
     */
    public Result(boolean success){
        this.success = success;
    }

    /**
     * 创建一个result。
     *
     * @param value
     * @param <T>
     * @return
     */
    public static <T> Result<T> newSuccessResult(T value) {
        Result<T> result = new Result<T>();
        result.success = true;
        result.setValue(value);
        return result;
    }

    /**
     * 创建一个result。
     *
     * @param errorMessage
     * @param <T>
     * @return
     */
    public static <T> Result<T> newErrorResult(String errorMessage) {
        Result<T> result = new Result<T>();
        result.success = false;
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 创建一个result。
     *
     * @param errorMessage
     * @param errorCode
     * @param <T>
     * @return
     */
    public static <T> Result<T> newErrorResult(String errorMessage, String errorCode) {
        Result<T> result = new Result<T>();
        result.success = false;
        result.setErrorMessage(errorMessage);
        result.setErrorCode(errorCode);
        return result;
    }




    /**
     * 创建一个result 该result包含状态
     *
     * @param errorMessage
     * @param errorCode
     * @param value
     * @param <T>
     * @return
     */
    public static <T> Result<T> newErrorResult(String errorMessage, String errorCode, T value) {
        Result<T> result = new Result<T>();
        result.success = false;
        result.setErrorMessage(errorMessage);
        result.setErrorCode(errorCode);
        result.setValue(value);
        return result;
    }
}
