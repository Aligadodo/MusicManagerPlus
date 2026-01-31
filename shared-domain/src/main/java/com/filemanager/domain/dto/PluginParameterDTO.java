package com.filemanager.domain.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginParameterDTO {
    private String name;
    private String label;
    private String description;
    private String type;
    private Object defaultValue;
    private boolean required;
    private String[] options;
    private String validationPattern;
    private Integer minValue;
    private Integer maxValue;
    
    private List<Map<String, Object>> visibilityConditions;
    private List<Map<String, Object>> exclusiveParams;
    private Map<String, Object> autoDetectParams;

    public PluginParameterDTO() {
    }

    public PluginParameterDTO(String name, String label, String description, String type, Object defaultValue, boolean required) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getValidationPattern() {
        return validationPattern;
    }

    public void setValidationPattern(String validationPattern) {
        this.validationPattern = validationPattern;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public List<Map<String, Object>> getVisibilityConditions() {
        return visibilityConditions;
    }

    public void setVisibilityConditions(List<Map<String, Object>> visibilityConditions) {
        this.visibilityConditions = visibilityConditions;
    }

    public List<Map<String, Object>> getExclusiveParams() {
        return exclusiveParams;
    }

    public void setExclusiveParams(List<Map<String, Object>> exclusiveParams) {
        this.exclusiveParams = exclusiveParams;
    }

    public Map<String, Object> getAutoDetectParams() {
        return autoDetectParams;
    }

    public void setAutoDetectParams(Map<String, Object> autoDetectParams) {
        this.autoDetectParams = autoDetectParams;
    }

    public static class Builder {
        private PluginParameterDTO parameter;

        public Builder() {
            this.parameter = new PluginParameterDTO();
        }

        public Builder name(String name) {
            parameter.setName(name);
            return this;
        }

        public Builder label(String label) {
            parameter.setLabel(label);
            return this;
        }

        public Builder description(String description) {
            parameter.setDescription(description);
            return this;
        }

        public Builder type(String type) {
            parameter.setType(type);
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            parameter.setDefaultValue(defaultValue);
            return this;
        }

        public Builder required(boolean required) {
            parameter.setRequired(required);
            return this;
        }

        public Builder options(String[] options) {
            parameter.setOptions(options);
            return this;
        }

        public Builder validationPattern(String validationPattern) {
            parameter.setValidationPattern(validationPattern);
            return this;
        }

        public Builder minValue(Integer minValue) {
            parameter.setMinValue(minValue);
            return this;
        }

        public Builder maxValue(Integer maxValue) {
            parameter.setMaxValue(maxValue);
            return this;
        }

        public Builder addVisibilityCondition(String dependentParam, Object expectedValue) {
            if (parameter.getVisibilityConditions() == null) {
                parameter.setVisibilityConditions(new ArrayList<>());
            }
            Map<String, Object> condition = new HashMap<>();
            condition.put("dependentParam", dependentParam);
            condition.put("expectedValue", expectedValue);
            parameter.getVisibilityConditions().add(condition);
            return this;
        }

        public Builder addExclusiveParam(String name, String condition) {
            if (parameter.getExclusiveParams() == null) {
                parameter.setExclusiveParams(new ArrayList<>());
            }
            Map<String, Object> exclusiveParam = new HashMap<>();
            exclusiveParam.put("name", name);
            exclusiveParam.put("condition", condition);
            parameter.getExclusiveParams().add(exclusiveParam);
            return this;
        }

        public Builder autoDetectParams(List<String> triggerValues, List<String> paths, String targetParam) {
            Map<String, Object> autoDetectParams = new HashMap<>();
            autoDetectParams.put("triggerValues", triggerValues);
            autoDetectParams.put("paths", paths);
            autoDetectParams.put("targetParam", targetParam);
            parameter.setAutoDetectParams(autoDetectParams);
            return this;
        }

        public PluginParameterDTO build() {
            return parameter;
        }
    }
}