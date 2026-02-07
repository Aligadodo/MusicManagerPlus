package com.filemanager.domain.dto;

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

    public EnumOptionDTO() {
    }

    public EnumOptionDTO(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public EnumOptionDTO(String value, String label, String nameEn, String descriptionZh, String descriptionEn) {
        this.value = value;
        this.label = label;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getDescriptionZh() {
        return descriptionZh;
    }

    public void setDescriptionZh(String descriptionZh) {
        this.descriptionZh = descriptionZh;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }
}