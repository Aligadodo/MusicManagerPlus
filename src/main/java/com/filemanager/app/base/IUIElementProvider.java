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

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.StackPane;

/**
 * 用户界面元素提供者接口
 * 定义用户界面控件相关的方法
 */
public interface IUIElementProvider {
    /**
     * 获取递归模式选择框
     * @return 递归模式选择框
     */
    JFXComboBox<String> getCbRecursionMode();

    /**
     * 获取递归深度选择器
     * @return 递归深度选择器
     */
    Spinner<Integer> getSpRecursionDepth();

    /**
     * 获取预览线程数选择器
     * @return 预览线程数选择器
     */
    Spinner<Integer> getSpPreviewThreads();

    /**
     * 获取执行线程数选择器
     * @return 执行线程数选择器
     */
    Spinner<Integer> getSpExecutionThreads();

    /**
     * 获取自动运行复选框
     * @return 自动运行复选框
     */
    JFXCheckBox getAutoRun();
    
    /**
     * 获取根容器
     * @return 根容器
     */
    StackPane getRootContainer();
}