package com.filemanager.domain.dto;

import java.util.List;
import java.util.Map;

/**
 * 配置字段DTO
 * 用于定义策略的配置字段
 * 支持细粒度的参数联动关系定义
 */
public class ConfigFieldDTO {

    private String name;
    private String label;
    private String type;
    private Object defaultValue;
    private String description;
    private boolean required;
    private String dependsOn;
    private Object dependsValue;
    private List<String> options;
    private List<EnumOptionDTO> enumOptions;
    private List<ConfigFieldDTO> subFields;
    private boolean isModule;
    private String moduleType;
    
    // 参数关系定义
    private String exclusiveGroup; // 互斥组名称，同一组的参数互斥
    private List<Map<String, Object>> blockConditions; // 阻止条件，当满足条件时阻止当前参数
    private AutoFillConfig autoFillConfig; // 自动填充配置，当依赖参数值变化时自动填充当前参数
    private List<ConfigFieldDTO> childFields; // 子参数列表，当当前参数显示时才显示子参数

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

    public Object getDependsValue() {
        return dependsValue;
    }

    public void setDependsValue(Object dependsValue) {
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

    public List<ConfigFieldDTO> getSubFields() {
        return subFields;
    }

    public void setSubFields(List<ConfigFieldDTO> subFields) {
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
    
    public String getExclusiveGroup() {
        return exclusiveGroup;
    }
    
    public void setExclusiveGroup(String exclusiveGroup) {
        this.exclusiveGroup = exclusiveGroup;
    }
    
    public List<Map<String, Object>> getBlockConditions() {
        return blockConditions;
    }
    
    public void setBlockConditions(List<Map<String, Object>> blockConditions) {
        this.blockConditions = blockConditions;
    }
    
    public AutoFillConfig getAutoFillConfig() {
        return autoFillConfig;
    }
    
    public void setAutoFillConfig(AutoFillConfig autoFillConfig) {
        this.autoFillConfig = autoFillConfig;
    }
    
    public List<ConfigFieldDTO> getChildFields() {
        return childFields;
    }
    
    public void setChildFields(List<ConfigFieldDTO> childFields) {
        this.childFields = childFields;
    }
}