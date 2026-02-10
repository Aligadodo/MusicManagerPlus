package com.filemanager.domain.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件参数DTO
 * 用于定义插件的参数
 */
public class PluginParameterDTO {

    private String name;
    private String label;
    private String description;
    private String type;
    private Object defaultValue;
    private boolean required;
    private String[] options;
    private List<EnumOptionDTO> enumOptions;
    private List<Map<String, Object>> visibilityConditions;
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
        this.visibilityConditions = new ArrayList<>();
        this.autoDetectParams = new HashMap<>();
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

    public List<Map<String, Object>> getVisibilityConditions() {
        return visibilityConditions;
    }

    public void setVisibilityConditions(List<Map<String, Object>> visibilityConditions) {
        this.visibilityConditions = visibilityConditions;
    }

    public Map<String, Object> getAutoDetectParams() {
        return autoDetectParams;
    }

    public void setAutoDetectParams(Map<String, Object> autoDetectParams) {
        this.autoDetectParams = autoDetectParams;
    }

    public List<EnumOptionDTO> getEnumOptions() {
        return enumOptions;
    }

    public void setEnumOptions(List<EnumOptionDTO> enumOptions) {
        this.enumOptions = enumOptions;
    }
}