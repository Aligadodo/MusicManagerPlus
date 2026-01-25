/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-25
 */
package com.filemanager.strategy.cleanup;

/**
 * 清理模式枚举
 */
public enum CleanupMode {
    DEDUP_FILES("同目录下的文件去重"),     // 文件去重
    DEDUP_FOLDERS("文件夹去重"),   // 文件夹去重
    REMOVE_EMPTY_DIRS("空目录清理"), // 空目录清理
    DIRECT_CLEANUP("直接清理"),   // 直接清理模式
    MERGE_SAME_NAME_PARENT_CHILD("同名父子文件夹合并"), // 同名父子文件夹合并模式
    MERGE_NESTED_FOLDERS("嵌套文件夹合并"); // 嵌套文件夹合并模式

    private final String desc;

    CleanupMode(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}
