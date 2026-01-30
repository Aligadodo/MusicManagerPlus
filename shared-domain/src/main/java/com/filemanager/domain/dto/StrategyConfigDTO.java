package com.filemanager.domain.dto;

import java.util.Map;

public class StrategyConfigDTO {
    private Map<String, Object> configValues;

    public StrategyConfigDTO() {
    }

    public StrategyConfigDTO(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    public Map<String, Object> getConfigValues() {
        return configValues;
    }

    public void setConfigValues(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    public Object getValue(String key) {
        return configValues != null ? configValues.get(key) : null;
    }

    public void setValue(String key, Object value) {
        if (configValues == null) {
            configValues = new java.util.HashMap<>();
        }
        configValues.put(key, value);
    }
}
