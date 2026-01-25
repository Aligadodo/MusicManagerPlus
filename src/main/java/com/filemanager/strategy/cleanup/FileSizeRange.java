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
 * 文件大小范围枚举
 */
public enum FileSizeRange {
    ALL(0, Long.MAX_VALUE, "所有文件"),
    SMALL(0, 1024 * 1024, "小于1MB"),
    MEDIUM(1024 * 1024, 10 * 1024 * 1024, "1MB-10MB"),
    LARGE(10 * 1024 * 1024, 100 * 1024 * 1024, "10MB-100MB"),
    XLARGE(100 * 1024 * 1024, Long.MAX_VALUE, "大于100MB");

    private final long minSize;
    private final long maxSize;
    private final String desc;

    FileSizeRange(long minSize, long maxSize, String desc) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.desc = desc;
    }

    public long getMinSize() {
        return minSize;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isInRange(long size) {
        return size >= minSize && size < maxSize;
    }

    @Override
    public String toString() {
        return desc;
    }
}
