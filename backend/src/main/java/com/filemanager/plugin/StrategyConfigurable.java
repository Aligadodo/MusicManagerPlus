package com.filemanager.plugin;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import java.util.List;

/**
 * 策略配置管理接口
 * 每个策略类实现此接口以管理自己的配置
 */
public interface StrategyConfigurable {

    /**
     * 获取策略ID
     */
    String getId();

    /**
     * 获取策略名称
     */
    String getName();

    /**
     * 获取策略描述
     */
    String getDescription();

    /**
     * 获取策略版本
     */
    String getVersion();

    /**
     * 获取配置字段列表
     * @return 配置字段列表
     */
    List<ConfigFieldDTO> getConfigFields();

    /**
     * 初始化默认配置
     * @return 默认配置
     */
    StrategyConfigDTO initializeDefaultConfig();

    /**
     * 验证配置是否有效
     * @param config 要验证的配置
     * @return 是否有效
     */
    boolean validateConfig(StrategyConfigDTO config);

    /**
     * 获取配置值
     * @param config 配置对象
     * @param key 配置键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 配置值
     */
    <T> T getConfigValue(StrategyConfigDTO config, String key, T defaultValue);

    /**
     * 设置配置值
     * @param config 配置对象
     * @param key 配置键
     * @param value 配置值
     */
    void setConfigValue(StrategyConfigDTO config, String key, Object value);
}
