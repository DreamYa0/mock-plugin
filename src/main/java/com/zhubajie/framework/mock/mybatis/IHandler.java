package com.zhubajie.framework.mock.mybatis;

/**
 * 通用处理器模版
 * @author dreamyao
 * @version 1.0.0
 */
public interface IHandler {

    /**
     * 无返回值处理器
     * @param t   入参
     * @param <T> 类型
     */
    <T> void handle(T t);

    /**
     * 有返回值处理器
     * @param t   入参
     * @param <T> 类型
     * @return
     */
    <T> T handle2Result(T t);

    /**
     * 设置处理器
     * @param handler 处理器
     */
    void setHandler(IHandler handler);
}
