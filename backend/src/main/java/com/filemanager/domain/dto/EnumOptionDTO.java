package com.filemanager.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 枚举选项DTO
 * 用于定义枚举类型配置字段的选项
 */
public class EnumOptionDTO {

    private String value;
    private String label;
    private String nameEn;
    private String descriptionZh;
    private String descriptionEn;
    
    // 兼容前端字段名
    private String code;
    private String nameZh;

    public EnumOptionDTO() {
    }

    public EnumOptionDTO(String value, String label) {
        this.value = value;
        this.label = label;
        this.code = value;
        this.nameZh = label;
    }

    public EnumOptionDTO(String value, String label, String nameEn, String descriptionZh, String descriptionEn) {
        this.value = value;
        this.label = label;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
        this.code = value;
        this.nameZh = label;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
        this.code = value;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
        this.nameZh = label;
    }

    @JsonProperty("nameEn")
    public String getNameEn() {
        return nameEn;
    }

    @JsonProperty("nameEn")
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    @JsonProperty("descriptionZh")
    public String getDescriptionZh() {
        return descriptionZh;
    }

    @JsonProperty("descriptionZh")
    public void setDescriptionZh(String descriptionZh) {
        this.descriptionZh = descriptionZh;
    }

    @JsonProperty("descriptionEn")
    public String getDescriptionEn() {
        return descriptionEn;
    }

    @JsonProperty("descriptionEn")
    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }
    
    @JsonProperty("code")
    public String getCode() {
        return code;
    }
    
    @JsonProperty("code")
    public void setCode(String code) {
        this.code = code;
        this.value = code;
    }
    
    @JsonProperty("nameZh")
    public String getNameZh() {
        return nameZh;
    }
    
    @JsonProperty("nameZh")
    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
        this.label = nameZh;
    }
}