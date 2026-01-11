package com.filemanager.base;

/**
 * 日志提供者接口
 * 定义日志与反馈相关的方法
 */
public interface ILoggingProvider {
    /**
     * 记录日志信息
     * @param s 日志内容
     */
    void log(String s);

    /**
     * 记录错误日志
     * @param s 错误日志内容
     */
    void logError(String s);
}