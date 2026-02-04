package com.filemanager.plugin.impl.audioconverter.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum SampleRate implements PluginEnum {
    
    ORIGINAL("original", "保持原样 (Original)", "Original", "保持原始采样率", "Keep original sample rate", null),
    SR_44100("44100", "44100", "44100", "CD标准采样率", "CD standard sample rate", 44100),
    SR_48000("48000", "48000", "48000", "专业音频采样率", "Professional audio sample rate", 48000),
    SR_88200("88200", "88200", "88200", "高分辨率音频采样率", "High resolution audio sample rate", 88200),
    SR_96000("96000", "96000", "96000", "高分辨率音频采样率", "High resolution audio sample rate", 96000),
    SR_192000("192000", "192000", "192000", "超高分辨率音频采样率", "Ultra high resolution audio sample rate", 192000);
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    private final Integer value;
    
    SampleRate(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn, Integer value) {
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
    
    public static SampleRate fromCode(String code) {
        return PluginEnum.fromCode(code, SampleRate.class, ORIGINAL);
    }
}