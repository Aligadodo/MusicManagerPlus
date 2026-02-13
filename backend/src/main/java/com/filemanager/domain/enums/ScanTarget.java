package com.filemanager.domain.enums;

public enum ScanTarget {
    FILES_ONLY("仅文件", "只处理文件，不处理文件夹"),
    FOLDERS_ONLY("仅文件夹", "只处理文件夹，不处理文件"),
    ALL("全部", "处理文件和文件夹");
    
    private final String displayName;
    private final String description;
    
    ScanTarget(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
