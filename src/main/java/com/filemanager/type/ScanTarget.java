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

public enum ScanTarget {
    FILES_ONLY("仅文件", "只处理文件，不处理文件夹"),
    FOLDERS_ONLY("仅文件夹", "只处理文件夹，不处理文件"),
    ALL("全部", "处理文件和文件夹");
    
    private final String displayName;
    private final String description;
    
    ScanTarget(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}