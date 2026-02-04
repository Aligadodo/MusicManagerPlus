package com.filemanager.domain.dto;

public class EnumOptionDTO {
    
    private String code;
    private String nameZh;
    private String nameEn;
    private String descriptionZh;
    private String descriptionEn;
    private Object value;
    
    public EnumOptionDTO() {
    }
    
    public EnumOptionDTO(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
        this.code = code;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getNameZh() {
        return nameZh;
    }
    
    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
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
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
}