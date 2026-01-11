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

import com.filemanager.app.tools.MultiThreadTaskEstimator;
import com.filemanager.type.TaskStatus;
import javafx.concurrent.Task;

/**
 * 任务管理器接口
 * 定义任务管理相关的方法
 */
public interface ITaskManager {
    /**
     * 设置运行UI文本
     * @param msg 运行文本
     */
    void setRunningUI(String msg);
    
    /**
     * 更改执行按钮状态
     * @param enabled 是否启用
     */
    void changeExecuteButton(boolean enabled);
    
    /**
     * 更改预览按钮状态
     * @param enabled 是否启用
     */
    void changePreviewButton(boolean enabled);
    
    /**
     * 更改停止按钮状态
     * @param enabled 是否启用
     */
    void changeStopButton(boolean enabled);
    
    /**
     * 更新进度状态
     * @param status 任务状态
     */
    void updateProgressStatus(TaskStatus status);
    
    /**
     * 绑定进度
     * @param task 任务对象
     */
    void bindProgress(Task<?> task);
    
    /**
     * 更新运行进度
     * @param msg 进度消息
     */
    void updateRunningProgress(String msg);
    
    /**
     * 更新统计信息
     */
    void updateStats();
    
    /**
     * 刷新组合视图
     */
    void refreshComposeView();
    
    /**
     * 获取根路径任务估算器
     * @param rootPath 根路径
     * @return 任务估算器
     */
    MultiThreadTaskEstimator getRootPathEstimator(String rootPath);
}