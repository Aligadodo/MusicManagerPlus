package com.filemanager.plugin.impl.audioconverter.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum Channels implements PluginEnum {
    
    ORIGINAL("original", "保持原样 (Original)", "Original", "保持原始声道数", "Keep original channels", null),
    MONO("1", "1 (Mono)", "Mono", "单声道", "Mono channel", 1),
    STEREO("2", "2 (Stereo)", "Stereo", "立体声", "Stereo", 2),
    SURROUND_5_1("6", "6 (5.1)", "5.1 Surround", "5.1环绕声", "5.1 Surround Sound", 6);
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    private final Integer value;
    
    Channels(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn, Integer value) {
        this.code = code;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
        this.value = value;
    }
    
    @Override
    public String getCode() {
        return code;
    }
    
    @Override
    public String getNameZh() {
        return nameZh;
    }
    
    @Override
    public String getNameEn() {
        return nameEn;
    }
    
    @Override
    public String getDescriptionZh() {
        return descriptionZh;
    }
    
    @Override
    public String getDescriptionEn() {
        return descriptionEn;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public boolean isOriginal() {
        return this == ORIGINAL;
    }
    
    public static Channels fromCode(String code) {
        return PluginEnum.fromCode(code, Channels.class, STEREO);
    }
}