package com.filemanager.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("defaultValue")
    public Object getDefaultValue() {
        return defaultValue;
    }

    @JsonProperty("defaultValue")
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("required")
    public boolean isRequired() {
        return required;
    }

    @JsonProperty("required")
    public void setRequired(boolean required) {
        this.required = required;
    }

    @JsonProperty("dependsOn")
    public String getDependsOn() {
        return dependsOn;
    }

    @JsonProperty("dependsOn")
    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }

    @JsonProperty("dependsValue")
    public Object getDependsValue() {
        return dependsValue;
    }

    @JsonProperty("dependsValue")
    public void setDependsValue(Object dependsValue) {
        this.dependsValue = dependsValue;
    }

    @JsonProperty("options")
    public List<String> getOptions() {
        return options;
    }

    @JsonProperty("options")
    public void setOptions(List<String> options) {
        this.options = options;
    }

    @JsonProperty("enumOptions")
    public List<EnumOptionDTO> getEnumOptions() {
        return enumOptions;
    }

    @JsonProperty("enumOptions")
    public void setEnumOptions(List<EnumOptionDTO> enumOptions) {
        this.enumOptions = enumOptions;
    }

    @JsonProperty("subFields")
    public List<ConfigFieldDTO> getSubFields() {
        return subFields;
    }

    @JsonProperty("subFields")
    public void setSubFields(List<ConfigFieldDTO> subFields) {
        this.subFields = subFields;
    }

    @JsonProperty("isModule")
    public boolean isModule() {
        return isModule;
    }

    @JsonProperty("isModule")
    public void setModule(boolean module) {
        isModule = module;
    }

    @JsonProperty("moduleType")
    public String getModuleType() {
        return moduleType;
    }

    @JsonProperty("moduleType")
    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }
    
    @JsonProperty("exclusiveGroup")
    public String getExclusiveGroup() {
        return exclusiveGroup;
    }
    
    @JsonProperty("exclusiveGroup")
    public void setExclusiveGroup(String exclusiveGroup) {
        this.exclusiveGroup = exclusiveGroup;
    }
    
    @JsonProperty("blockConditions")
    public List<Map<String, Object>> getBlockConditions() {
        return blockConditions;
    }
    
    @JsonProperty("blockConditions")
    public void setBlockConditions(List<Map<String, Object>> blockConditions) {
        this.blockConditions = blockConditions;
    }
    
    @JsonProperty("autoFillConfig")
    public AutoFillConfig getAutoFillConfig() {
        return autoFillConfig;
    }
    
    @JsonProperty("autoFillConfig")
    public void setAutoFillConfig(AutoFillConfig autoFillConfig) {
        this.autoFillConfig = autoFillConfig;
    }
    
    @JsonProperty("childFields")
    public List<ConfigFieldDTO> getChildFields() {
        return childFields;
    }
    
    @JsonProperty("childFields")
    public void setChildFields(List<ConfigFieldDTO> childFields) {
        this.childFields = childFields;
    }
}