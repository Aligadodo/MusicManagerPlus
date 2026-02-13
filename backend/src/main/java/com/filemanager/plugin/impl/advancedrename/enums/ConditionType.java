package com.filemanager.plugin.impl.advancedrename.enums;

public enum ConditionType {
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    REGEX_MATCH,
    FILE_SIZE_GT,
    FILE_SIZE_LT,
    PARENT_DIR_IS,
    PATH_CONTAINS,
    PATH_NOT_CONTAINS,
    EXT_IN,
    EXT_NOT_IN,
    IS_DIRECTORY,
    IS_FILE;

    public String getCode() {
        return name().toLowerCase();
    }
}