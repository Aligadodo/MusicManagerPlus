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

import com.filemanager.util.log.Logger;

public class ExceptionHandler {
    
    private static final Logger logger = Logger.getLogger(ExceptionHandler.class);
    
    public static void handleException(Exception e) {
        handleException(e, true);
    }
    
    public static void handleException(Exception e, boolean logError) {
        if (e instanceof FileManagerException) {
            handleFileManagerException((FileManagerException) e, logError);
        } else {
            handleGenericException(e, logError);
        }
    }
    
    private static void handleFileManagerException(FileManagerException e, boolean logError) {
        if (logError) {
            logger.error(String.format("[%d] %s: %s", 
                e.getErrorCode().getCode(), 
                e.getErrorCode().getDescription(), 
                e.getMessage()), e);
        }
    }
    
    private static void handleGenericException(Exception e, boolean logError) {
        if (logError) {
            logger.error("发生异常: " + e.getMessage(), e);
        }
    }
    
    public static void handleException(Exception e, String context) {
        if (e instanceof FileManagerException) {
            handleFileManagerException((FileManagerException) e, context);
        } else {
            handleGenericException(e, context);
        }
    }
    
    private static void handleFileManagerException(FileManagerException e, String context) {
        logger.error(String.format("[%s] [%d] %s: %s", 
            context,
            e.getErrorCode().getCode(), 
            e.getErrorCode().getDescription(), 
            e.getMessage()), e);
    }
    
    private static void handleGenericException(Exception e, String context) {
        logger.error(String.format("[%s] 发生异常: %s", context, e.getMessage()), e);
    }
    
    public static FileManagerException wrapException(Exception e, FileManagerException.ErrorCode errorCode, String message) {
        if (e instanceof FileManagerException) {
            return (FileManagerException) e;
        }
        return new FileManagerException(errorCode, message, e);
    }
    
    public static FileManagerException wrapException(Exception e, String message) {
        return wrapException(e, FileManagerException.ErrorCode.UNKNOWN_ERROR, message);
    }
}
