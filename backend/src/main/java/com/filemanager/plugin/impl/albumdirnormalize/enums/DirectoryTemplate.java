package com.filemanager.plugin.impl.albumdirnormalize.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum DirectoryTemplate implements PluginEnum {
    
    ARTIST_YEAR_ALBUM("%artist% - %year% - %album%", "%artist% - %year% - %album%", "%artist% - %year% - %album%", "艺术家 - 年份 - 专辑", "Artist - Year - Album"),
    YEAR_ARTIST_ALBUM("[%year%] %artist% - %album%", "[%year%] %artist% - %album%", "[%year%] %artist% - %album%", "[年份] 艺术家 - 专辑", "[Year] Artist - Album"),
    ARTIST_ALBUM_YEAR("%artist%/%album% (%year%)", "%artist%/%album% (%year%)", "%artist%/%album% (%year%)", "艺术家/专辑 (年份)", "Artist/Album (Year)"),
    YEAR_ALBUM_ARTIST("%year% - %album% - %artist%", "%year% - %album% - %artist%", "%year% - %album% - %artist%", "年份 - 专辑 - 艺术家", "Year - Album - Artist"),
    ALBUM_ARTIST_YEAR("%album% - %artist% [%year%]", "%album% - %artist% [%year%]", "%album% - %artist% [%year%]", "专辑 - 艺术家 [年份]", "Album - Artist [Year]"),
    ARTIST_ALBUM("%artist% - %album%", "%artist% - %album%", "%artist% - %album%", "艺术家 - 专辑", "Artist - Album"),
    ALBUM_YEAR("%album% (%year%)", "%album% (%year%)", "%album% (%year%)", "专辑 (年份)", "Album (Year)"),
    CUSTOM("custom", "自定义模板", "Custom Template", "自定义命名模板", "Custom naming template");
    
    private final String code;
    private final String template;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    DirectoryTemplate(String code, String template, String nameEn, String descriptionZh, String descriptionEn) {
        this.code = code;
        this.template = template;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
    }
    
    @Override
    public String getCode() {
        return code;
    }
    
    @Override
    public String getNameZh() {
        return template;
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
    
    public String getTemplate() {
        return template;
    }
    
    public boolean isCustom() {
        return this == CUSTOM;
    }
    
    public static DirectoryTemplate fromCode(String code) {
        return PluginEnum.fromCode(code, DirectoryTemplate.class, ARTIST_YEAR_ALBUM);
    }
}
