package com.filemanager.plugin.impl.advancedrename.enums;

public enum RenameActionType {
    REPLACE_TEXT,
    REPLACE_REGEX,
    PREPEND,
    APPEND,
    TO_LOWER,
    TO_UPPER,
    TRIM,
    BATCH_REMOVE,
    CLEAN_NOISE,
    ADD_LETTER_PREFIX,
    CUT_PREFIX,
    CUT_SUFFIX,
    KEEP_PREFIX,
    KEEP_SUFFIX,
    REMOVE_PREFIX,
    REMOVE_SUFFIX,
    TRADITIONAL_TO_SIMPLIFIED,
    ADD_NUMBER_PREFIX;

    public String getCode() {
        return name().toLowerCase();
    }
}