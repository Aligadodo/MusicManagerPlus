/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.type;

public enum ExecStatus {
    PENDING("等待中"),
    RUNNING("运行中"),
    SUCCESS("成功"),
    FAILED("失败"),
    ANALYZE_FAILED("分析失败"),
    SKIPPED("已跳过");

    private final String description;

    ExecStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}