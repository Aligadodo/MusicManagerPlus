/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.base;

/**
 * 日志提供者接口
 * 定义日志与反馈相关的方法
 */
public interface ILoggingProvider {
    /**
     * 记录日志信息
     * @param s 日志内容
     */
    void log(String s);

    /**
     * 记录错误日志
     * @param s 错误日志内容
     */
    void logError(String s);
}