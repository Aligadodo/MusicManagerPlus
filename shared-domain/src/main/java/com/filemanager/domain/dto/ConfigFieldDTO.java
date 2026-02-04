package com.filemanager.domain.dto;

import java.util.List;
import java.util.Map;

public class ConfigFieldDTO {
    private String name;
    private String label;
    private String type;
    private Object defaultValue;
    private String description;
    private boolean required;
    
    // 条件参数支持
    private String dependsOn;
    private String dependsValue;
    private List<String> options;
    private List<EnumOptionDTO> enumOptions;
    private Map<String, Object> subFields;
    
    // 模块化配置支持
    private boolean isModule;
    private String moduleType;

    public ConfigFieldDTO() {
    }

    public ConfigFieldDTO(String name, String label, String type, Object defaultValue, String description, boolean required) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public String getDependsOn() {
        return dependsOn;
    }
    
    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }
    
    public String getDependsValue() {
        return dependsValue;
    }
    
    public void setDependsValue(String dependsValue) {
        this.dependsValue = dependsValue;
    }
    
    public List<String> getOptions() {
        return options;
    }
    
    public void setOptions(List<String> options) {
        this.options = options;
    }
    
    public List<EnumOptionDTO> getEnumOptions() {
        return enumOptions;
    }
    
    public void setEnumOptions(List<EnumOptionDTO> enumOptions) {
        this.enumOptions = enumOptions;
    }
    
    public Map<String, Object> getSubFields() {
        return subFields;
    }
    
    public void setSubFields(Map<String, Object> subFields) {
        this.subFields = subFields;
    }
    
    public boolean isModule() {
        return isModule;
    }
    
    public void setModule(boolean module) {
        isModule = module;
    }
    
    public String getModuleType() {
        return moduleType;
    }
    
    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }
}
