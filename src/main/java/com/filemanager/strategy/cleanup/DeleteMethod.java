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
 * 删除方式枚举
 */
public enum DeleteMethod {
    DIRECT_DELETE("直接删除"),       // 直接删除
    PSEUDO_DELETE("伪删除（归档到垃圾箱）"),       // 伪删除（归档到垃圾箱）
    ROLLBACKABLE_DELETE("可回滚删除");  // 可回滚删除

    private final String desc;

    DeleteMethod(String desc) {
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
