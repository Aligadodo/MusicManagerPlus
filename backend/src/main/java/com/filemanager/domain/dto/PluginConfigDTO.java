package com.filemanager.domain.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件配置DTO
 */
public class PluginConfigDTO {
    private Map<String, Object> configMap = new HashMap<>();

    public PluginConfigDTO() {
    }

    /**
     * 设置配置值
     * @param key 配置键
     * @param value 配置值
     */
    public void setValue(String key, Object value) {
        configMap.put(key, value);
    }

    /**
     * 获取配置值
     * @param key 配置键
     * @return 配置值
     */
    public Object getValue(String key) {
        return configMap.get(key);
    }

    /**
     * 获取所有配置
     * @return 配置映射
     */
    public Map<String, Object> getConfigMap() {
        return configMap;
    }

    /**
     * 设置所有配置
     * @param configMap 配置映射
     */
    public void setConfigMap(Map<String, Object> configMap) {
        this.configMap = configMap;
    }
}
