/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.rule.rename;

public enum RenameActionType {
    REPLACE_TEXT("文本替换"), REPLACE_REGEX("正则替换"), PREPEND("前缀添加"), APPEND("后缀添加"),
    TO_LOWER("转小写"), TO_UPPER("转大写"), TRIM("去前后空格"), ADD_LETTER_PREFIX("首字母前缀"),
    CLEAN_NOISE("智能清理"), BATCH_REMOVE("批量移除"), CUT_PREFIX("截取前N位"), CUT_SUFFIX("截取后N位"), KEEP_PREFIX("保留前N位"),
    KEEP_SUFFIX("保留后N位"), REMOVE_PREFIX("移除前缀"), REMOVE_SUFFIX("移除后缀"), TRADITIONAL_TO_SIMPLIFIED("繁体转简体");

    private final String d;

    RenameActionType(String d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return d;
    }
}