package com.filemanager.plugin.impl.advancedrename.enums;

public enum RenameMode {
    ONLY_FILENAME,
    ONLY_EXTENSION,
    ALL;

    public String getCode() {
        return name().toLowerCase();
    }
}