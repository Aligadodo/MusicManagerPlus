package com.filemanager.domain.dto;

import java.util.List;
import java.util.Map;

public class PluginConfigDTO {
    private Map<String, Object> configValues;
    private List<PluginParameterDTO> parameters;
    private List<PreconditionGroupDTO> preconditionGroups;

    public PluginConfigDTO() {
    }

    public PluginConfigDTO(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    public PluginConfigDTO(Map<String, Object> configValues, List<PluginParameterDTO> parameters, List<PreconditionGroupDTO> preconditionGroups) {
        this.configValues = configValues;
        this.parameters = parameters;
        this.preconditionGroups = preconditionGroups;
    }

    public Map<String, Object> getConfigValues() {
        return configValues;
    }

    public void setConfigValues(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    public List<PluginParameterDTO> getParameters() {
        return parameters;
    }

    public void setParameters(List<PluginParameterDTO> parameters) {
        this.parameters = parameters;
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

    public Object getValue(String key, Object defaultValue) {
        return configValues != null && configValues.containsKey(key) ? configValues.get(key) : defaultValue;
    }

    public void setValue(String key, Object value) {
        if (configValues == null) {
            configValues = new java.util.HashMap<>();
        }
        configValues.put(key, value);
    }
}
