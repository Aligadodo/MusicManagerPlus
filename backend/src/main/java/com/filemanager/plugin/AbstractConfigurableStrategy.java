package com.filemanager.plugin;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * 可配置策略的基础实现类
 * 提供策略配置管理的默认实现
 */
public abstract class AbstractConfigurableStrategy implements StrategyConfigurable {

    protected List<ConfigFieldDTO> configFields;

    public AbstractConfigurableStrategy() {
        this.configFields = new ArrayList<>();
        initConfigFields();
    }

    /**
     * 初始化配置字段
     * 子类需要实现此方法来定义自己的配置字段
     */
    protected abstract void initConfigFields();



    @Override
    public List<ConfigFieldDTO> getConfigFields() {
        return configFields;
    }

    @Override
    public StrategyConfigDTO initializeDefaultConfig() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        initDefaultConfigValues(config);
        return config;
    }

    /**
     * 初始化默认配置值
     * 子类需要实现此方法来设置默认配置值
     * @param config 配置对象
     */
    protected abstract void initDefaultConfigValues(StrategyConfigDTO config);

    @Override
    public boolean validateConfig(StrategyConfigDTO config) {
        if (config == null || config.getConfigValues() == null) {
            return false;
        }

        // 验证必填字段
        for (ConfigFieldDTO field : configFields) {
            if (field.isRequired()) {
                Object value = config.getConfigValues().get(field.getName());
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public <T> T getConfigValue(StrategyConfigDTO config, String key, T defaultValue) {
        if (config == null || config.getConfigValues() == null) {
            return defaultValue;
        }

        Object value = config.getConfigValues().get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    @Override
    public void setConfigValue(StrategyConfigDTO config, String key, Object value) {
        if (config == null) {
            config = new StrategyConfigDTO();
        }

        if (config.getConfigValues() == null) {
            config.setConfigValues(new java.util.HashMap<>());
        }

        config.getConfigValues().put(key, value);
    }

    /**
     * 添加配置字段
     * @param name 字段名称
     * @param label 字段标签
     * @param type 字段类型
     * @param defaultValue 默认值
     * @param description 字段描述
     * @param required 是否必填
     */
    public void addConfigField(String name, String label, String type, Object defaultValue, String description, boolean required) {
        ConfigFieldDTO field = new ConfigFieldDTO(name, label, type, defaultValue, description, required);
        configFields.add(field);
    }

    /**
     * 添加带选项的配置字段
     * @param name 字段名称
     * @param label 字段标签
     * @param type 字段类型
     * @param defaultValue 默认值
     * @param description 字段描述
     * @param required 是否必填
     * @param options 选项列表
     */
    public void addConfigField(String name, String label, String type, Object defaultValue, String description, boolean required, List<String> options) {
        ConfigFieldDTO field = new ConfigFieldDTO(name, label, type, defaultValue, description, required);
        field.setOptions(options);
        configFields.add(field);
    }
}
