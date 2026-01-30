/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.util.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一日志记录工具类
 * 提供统一的日志记录接口，支持不同级别的日志输出
 */
public class Logger {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static LogLevel currentLevel = LogLevel.INFO;
    
    private final String className;
    
    private Logger(String className) {
        this.className = className;
    }
    
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }
    
    public static Logger getLogger(String name) {
        return new Logger(name);
    }
    
    public static void setLevel(LogLevel level) {
        currentLevel = level;
    }
    
    public static LogLevel getLevel() {
        return currentLevel;
    }
    
    public void trace(String message) {
        log(LogLevel.TRACE, message, null);
    }
    
    public void trace(String message, Throwable throwable) {
        log(LogLevel.TRACE, message, throwable);
    }
    
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    public void debug(String message, Throwable throwable) {
        log(LogLevel.DEBUG, message, throwable);
    }
    
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    public void info(String message, Throwable throwable) {
        log(LogLevel.INFO, message, throwable);
    }
    
    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }
    
    public void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }
    
    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    private void log(LogLevel level, String message, Throwable throwable) {
        if (level.getValue() < currentLevel.getValue()) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String threadName = Thread.currentThread().getName();
        String levelStr = level.name();
        
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp).append("] ");
        sb.append("[").append(threadName).append("] ");
        sb.append("[").append(levelStr).append("] ");
        sb.append("[").append(className).append("] ");
        sb.append(message);
        
        if (level == LogLevel.ERROR) {
            System.err.println(sb.toString());
        } else {
            System.out.println(sb.toString());
        }
        
        if (throwable != null) {
            if (level == LogLevel.ERROR) {
                throwable.printStackTrace(System.err);
            } else {
                throwable.printStackTrace(System.out);
            }
        }
    }
    
    public enum LogLevel {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4);
        
        private final int value;
        
        LogLevel(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
}
