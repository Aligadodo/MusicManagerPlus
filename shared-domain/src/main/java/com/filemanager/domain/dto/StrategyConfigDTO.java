package com.filemanager.domain.dto;

import java.util.List;
import java.util.Map;

public class StrategyConfigDTO {
    private Map<String, Object> configValues;
    private List<PreconditionGroupDTO> preconditionGroups;

    public StrategyConfigDTO() {
    }

    public StrategyConfigDTO(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    public StrategyConfigDTO(Map<String, Object> configValues, List<PreconditionGroupDTO> preconditionGroups) {
        this.configValues = configValues;
        this.preconditionGroups = preconditionGroups;
    }

    public Map<String, Object> getConfigValues() {
        return configValues;
    }

    public void setConfigValues(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    public List<PreconditionGroupDTO> getPreconditionGroups() {
        return preconditionGroups;
    }

    public void setPreconditionGroups(List<PreconditionGroupDTO> preconditionGroups) {
        this.preconditionGroups = preconditionGroups;
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
