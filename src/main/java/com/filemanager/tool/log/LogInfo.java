/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.tool.log;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogInfo {
    private LogType type;

    private String message;

    public LogInfo(LogType type, String message) {
        this.type = type;
        this.message = message;
    }
    
    public LogType getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
}

