package com.filemanager.plugin.impl.filemigrate.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum OperationMode implements PluginEnum {
    
    MOVE("move", "移动 (MOVE)", "Move", "移动文件", "Move files", "移动"),
    COPY("copy", "复制 (COPY)", "Copy", "复制文件", "Copy files", "复制");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    private final String actionType;
    
    OperationMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn, String actionType) {
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
    
    public static OperationMode fromCode(String code) {
        return PluginEnum.fromCode(code, OperationMode.class, MOVE);
    }
}
