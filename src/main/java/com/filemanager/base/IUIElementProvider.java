package com.filemanager.base;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.control.Spinner;

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
}