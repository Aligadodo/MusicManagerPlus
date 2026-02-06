package com.filemanager.plugin.impl.filecleanup.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum FileSizeRange implements PluginEnum {
    
    ALL("all", "全部", "All", "所有文件大小", "All file sizes"),
    LESS_THAN_1MB("less_than_1mb", "小于1MB", "Less than 1MB", "小于1MB的文件", "Files less than 1MB"),
    LESS_THAN_10MB("less_than_10mb", "小于10MB", "Less than 10MB", "小于10MB的文件", "Files less than 10MB"),
    LESS_THAN_100MB("less_than_100mb", "小于100MB", "Less than 100MB", "小于100MB的文件", "Files less than 100MB"),
    LESS_THAN_1GB("less_than_1gb", "小于1GB", "Less than 1GB", "小于1GB的文件", "Files less than 1GB"),
    GREATER_THAN_1MB("greater_than_1mb", "大于1MB", "Greater than 1MB", "大于1MB的文件", "Files greater than 1MB"),
    GREATER_THAN_10MB("greater_than_10mb", "大于10MB", "Greater than 10MB", "大于10MB的文件", "Files greater than 10MB"),
    GREATER_THAN_100MB("greater_than_100mb", "大于100MB", "Greater than 100MB", "大于100MB的文件", "Files greater than 100MB"),
    GREATER_THAN_1GB("greater_than_1gb", "大于1GB", "Greater than 1GB", "大于1GB的文件", "Files greater than 1GB");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    FileSizeRange(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
        this.code = code;
        this.nameZh = nameZh;
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
    
    public long getMaxSizeBytes() {
        switch (this) {
            case LESS_THAN_1MB:
                return 1024 * 1024L;
            case LESS_THAN_10MB:
                return 10 * 1024 * 1024L;
            case LESS_THAN_100MB:
                return 100 * 1024 * 1024L;
            case LESS_THAN_1GB:
                return 1024 * 1024 * 1024L;
            default:
                return Long.MAX_VALUE;
        }
    }
    
    public long getMinSizeBytes() {
        switch (this) {
            case GREATER_THAN_1MB:
                return 1024 * 1024L;
            case GREATER_THAN_10MB:
                return 10 * 1024 * 1024L;
            case GREATER_THAN_100MB:
                return 100 * 1024 * 1024L;
            case GREATER_THAN_1GB:
                return 1024 * 1024 * 1024L;
            default:
                return 0;
        }
    }
    
    public boolean isSizeInRange(long fileSizeBytes) {
        return fileSizeBytes >= getMinSizeBytes() && fileSizeBytes <= getMaxSizeBytes();
    }
    
    public static FileSizeRange fromCode(String code) {
        return PluginEnum.fromCode(code, FileSizeRange.class, ALL);
    }
}
