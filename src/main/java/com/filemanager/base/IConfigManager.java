package com.filemanager.base;

import java.util.Map;

/**
 * 配置管理器接口
 * 定义配置管理相关的方法
 */
public interface IConfigManager {
    /**
     * 保存配置操作
     */
    void saveConfigAction();

    /**
     * 加载配置操作
     */
    void loadConfigAction();
    
    /**
     * 设置线程池模式
     * @param newVal 新的线程池模式
     * @return 是否设置成功
     */
    boolean setThreadPoolMode(String newVal);
    
    /**
     * 获取根路径线程配置
     * @return 根路径线程配置映射
     */
    Map<String, Integer> getRootPathThreadConfig();
}