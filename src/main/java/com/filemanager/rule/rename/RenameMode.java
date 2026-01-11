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

public enum RenameMode {
    ONLY_FILENAME("仅文件名"), ONLY_EXTENSION("仅扩展名"), ALL("全部内容");
    
    private final String description;
    
    RenameMode(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}