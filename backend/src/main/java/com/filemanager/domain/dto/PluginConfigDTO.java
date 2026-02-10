package com.filemanager.domain.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件配置DTO
 * 用于存储和传输插件的配置信息
 */
public class PluginConfigDTO {

    private Map<String, Object> configValues;

    public PluginConfigDTO() {
        this.configValues = new HashMap<>();
    }

    public Map<String, Object> getConfigValues() {
        return configValues;
    }

    public void setConfigValues(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    /**
     * 获取配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 配置值
     */
    public <T> T getValue(String key, T defaultValue) {
        if (configValues == null) {
            return defaultValue;
        }
        Object value = configValues.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 获取配置值（简化版，使用null作为默认值）
     * @param key 配置键
     * @param <T> 值类型
     * @return 配置值
     */
    public <T> T getValue(String key) {
        return getValue(key, null);
    }

    /**
     * 设置配置值
     * @param key 配置键
     * @param value 配置值
     */
    public void setValue(String key, Object value) {
        if (configValues == null) {
            configValues = new HashMap<>();
        }
        configValues.put(key, value);
    }

    /**
     * 检查配置是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return configValues == null || configValues.isEmpty();
    }

    /**
     * 获取配置项数量
     * @return 配置项数量
     */
    public int size() {
        return configValues != null ? configValues.size() : 0;
    }
}