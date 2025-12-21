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
}

