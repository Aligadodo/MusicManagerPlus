package com.filemanager.plugin.enums.common;

import com.filemanager.plugin.enums.PluginEnum;

public enum CrossDriveMode implements PluginEnum {
    
    MOVE("move", "移动 (Move)", "Move", "移动文件到目标位置", "Move files to target location", "移动"),
    COPY("copy", "复制 (Copy)", "Copy", "复制文件到目标位置", "Copy files to target location", "复制");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    private final String actionType;
    
    CrossDriveMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn, String actionType) {
        this.code = code;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
        this.actionType = actionType;
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
    
    public String getActionType() {
        return actionType;
    }
    
    public boolean isMove() {
        return this == MOVE;
    }
    
    public boolean isCopy() {
        return this == COPY;
    }
    
    public static CrossDriveMode fromCode(String code) {
        return PluginEnum.fromCode(code, CrossDriveMode.class, MOVE);
    }
}