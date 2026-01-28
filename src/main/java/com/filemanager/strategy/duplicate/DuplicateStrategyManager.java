/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy.duplicate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 去重策略管理器
 * 用于管理和应用不同的去重策略
 */
public class DuplicateStrategyManager {
    private final Map<String, DuplicateStrategy> strategies;
    private DuplicateStrategy currentStrategy;
    
    /**
     * 构造函数
     */
    public DuplicateStrategyManager() {
        this.strategies = new HashMap<>();
    }
    
    /**
     * 添加去重策略
     * @param name 策略名称
     * @param strategy 去重策略
     */
    public void addStrategy(String name, DuplicateStrategy strategy) {
        strategies.put(name, strategy);
    }
    
    /**
     * 设置当前使用的去重策略
     * @param name 策略名称
     */
    public void setCurrentStrategy(String name) {
        this.currentStrategy = strategies.get(name);
    }
    
    /**
     * 获取当前使用的去重策略
     * @return 当前去重策略
     */
    public DuplicateStrategy getCurrentStrategy() {
        return currentStrategy;
    }
    
    /**
     * 获取所有可用的去重策略
     * @return 去重策略列表
     */
    public List<DuplicateStrategy> getAvailableStrategies() {
        return new ArrayList<>(strategies.values());
    }
    
    /**
     * 处理重复文件
     * @param duplicates 重复文件列表
     * @return 处理后的文件列表
     */
    public List<File> processDuplicates(List<File> duplicates) {
        if (currentStrategy == null) {
            return duplicates;
        }
        
        return currentStrategy.processDuplicates(duplicates);
    }
    
    /**
     * 创建默认的策略管理器
     * @param keepLargest 是否保留最大文件
     * @param keepNewest 是否保留最新文件
     * @param audioSpecial 是否对音频文件进行特殊处理
     * @param keepExt 文件类型优先级顺序
     * @return 默认的策略管理器
     */
    public static DuplicateStrategyManager createDefaultManager(
            boolean keepLargest, 
            boolean keepNewest, 
            boolean audioSpecial, 
            String keepExt) {
        
        DuplicateStrategyManager manager = new DuplicateStrategyManager();
        
        // 添加保留最佳版本策略
        KeepBestVersionStrategy bestVersionStrategy = 
                new KeepBestVersionStrategy(keepLargest, keepNewest, audioSpecial, keepExt);
        manager.addStrategy(bestVersionStrategy.getName(), bestVersionStrategy);
        
        // 添加添加序号策略
        AddSequenceStrategy sequenceStrategy = 
                new AddSequenceStrategy(true, " (%d)");
        manager.addStrategy(sequenceStrategy.getName(), sequenceStrategy);
        
        // 默认使用保留最佳版本策略
        manager.setCurrentStrategy(bestVersionStrategy.getName());
        
        return manager;
    }
}
