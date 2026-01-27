package com.filemanager.strategy.collection;

import java.util.List;

/**
 * 合集命名策略接口
 */
public interface ICollectionNamingStrategy {
    
    /**
     * 生成合集名称
     * @param filenames 文件名列表
     * @return 合集名称
     */
    String generateCollectionName(List<String> filenames);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 获取策略描述
     * @return 策略描述
     */
    String getStrategyDescription();
}