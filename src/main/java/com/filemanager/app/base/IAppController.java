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
 * 应用程序控制器接口
 * 定义 View 层与逻辑层交互的契约
 * 通过继承多个子接口实现功能模块化
 *
 * @author 28667
 */
public interface IAppController extends 
        IDataProvider,      // 数据获取与状态相关
        IViewManager,       // 视图管理相关
        IUIElementProvider, // 用户界面控件相关
        IBusinessOperations, // 业务操作相关
        ILoggingProvider,   // 日志与反馈相关
        IConfigManager,     // 配置管理相关
        ITaskManager,       // 任务管理相关
        IAutoReloadAbleProvider // 自动重新加载组件相关
{
    // 接口组合，无需单独定义方法
}