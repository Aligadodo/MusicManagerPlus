package com.filemanager.rule.rename;

public enum RenameMode {
    ONLY_FILENAME("仅文件名"), ONLY_EXTENSION("仅扩展名"), ALL("全部内容");
    
    private final String description;
    
    RenameMode(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}