package com.filemanager.plusv2.type;

public enum ActionType {
    REPLACE_TEXT("文本替换"),
    REPLACE_REGEX("正则替换"),
    PREPEND("前缀添加"),
    APPEND("后缀添加"),
    TO_LOWER("转小写"),
    TO_UPPER("转大写"),
    TRIM("去空格"),
    ADD_LETTER_PREFIX("添加首字母前缀 (W - 王菲)"),
    CLEAN_NOISE("智能清理 (符号/干扰词)"),
    BATCH_REMOVE("批量移除字符/词汇"); // 新增：简单快捷的移除规则

    private String desc;
    ActionType(String desc){ this.desc = desc; }
    @Override public String toString(){ return desc; }
}
