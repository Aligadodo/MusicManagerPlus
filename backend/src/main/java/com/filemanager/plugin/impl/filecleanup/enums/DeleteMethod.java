package com.filemanager.plugin.impl.filecleanup.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum DeleteMethod implements PluginEnum {
    
    PSEUDO_DELETE("pseudo_delete", "伪删除", "Pseudo Delete", "移动到回收站", "Move to trash"),
    DIRECT_DELETE("direct_delete", "直接删除", "Direct Delete", "永久删除文件", "Permanently delete files"),
    ROLLBACK_DELETE("rollback_delete", "可回滚删除", "Rollback Delete", "可回滚的删除方式", "Rollback deletion method");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    DeleteMethod(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isPseudoDelete() {
        return this == PSEUDO_DELETE;
    }
    
    public boolean isDirectDelete() {
        return this == DIRECT_DELETE;
    }
    
    public boolean isRollbackDelete() {
        return this == ROLLBACK_DELETE;
    }
    
    public boolean requiresTrashPath() {
        return this == PSEUDO_DELETE || this == ROLLBACK_DELETE;
    }
    
    public static DeleteMethod fromCode(String code) {
        return PluginEnum.fromCode(code, DeleteMethod.class, PSEUDO_DELETE);
    }
}
