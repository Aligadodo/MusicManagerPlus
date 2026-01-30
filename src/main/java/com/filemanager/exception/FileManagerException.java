/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.exception;

public class FileManagerException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public FileManagerException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public FileManagerException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public FileManagerException(String message) {
        this(ErrorCode.UNKNOWN_ERROR, message);
    }
    
    public FileManagerException(String message, Throwable cause) {
        this(ErrorCode.UNKNOWN_ERROR, message, cause);
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public enum ErrorCode {
        UNKNOWN_ERROR(0, "未知错误"),
        FILE_NOT_FOUND(1001, "文件不存在"),
        FILE_READ_ERROR(1002, "文件读取错误"),
        FILE_WRITE_ERROR(1003, "文件写入错误"),
        FILE_PARSE_ERROR(1004, "文件解析错误"),
        DIRECTORY_NOT_FOUND(2001, "目录不存在"),
        DIRECTORY_CREATE_ERROR(2002, "目录创建错误"),
        CONFIG_LOAD_ERROR(3001, "配置加载错误"),
        CONFIG_SAVE_ERROR(3002, "配置保存错误"),
        METADATA_PARSE_ERROR(4001, "元数据解析错误"),
        METADATA_WRITE_ERROR(4002, "元数据写入错误"),
        AUDIO_CONVERSION_ERROR(5001, "音频转换错误"),
        AUDIO_TAG_ERROR(5002, "音频标签错误"),
        NETWORK_ERROR(6001, "网络错误"),
        API_ERROR(6002, "API调用错误"),
        VALIDATION_ERROR(7001, "验证错误"),
        OPERATION_CANCELLED(8001, "操作已取消");
        
        private final int code;
        private final String description;
        
        ErrorCode(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
