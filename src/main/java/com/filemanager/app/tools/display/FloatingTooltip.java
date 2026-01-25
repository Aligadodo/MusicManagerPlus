/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-26 
 */
package com.filemanager.app.tools.display;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.List;

/**
 * 悬浮提示框组件
 * 支持多行文本、自适应显示、自动换行、低优先级显示
 */
public class FloatingTooltip {
    // 静态变量，控制是否显示提示信息
    private static boolean showTooltips = true;
    
    private final Stage stage;
    private final TextFlow content;
    
    /**
     * 构造函数
     */
    public FloatingTooltip() {
        // 创建非模态窗口
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.NONE); // 非模态，不阻止其他窗口操作
        stage.setAlwaysOnTop(false); // 不总是在最前面
        
        // 创建文本流容器
        content = new TextFlow();
        content.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.6); " + // 降低透明度
            "-fx-text-fill: white; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        content.setMinWidth(100); // 设置最小宽度
        content.setMaxWidth(350); // 设置最大宽度
        content.setPrefWidth(Region.USE_COMPUTED_SIZE); // 使用计算的宽度
        content.setPrefHeight(Region.USE_COMPUTED_SIZE); // 使用计算的高度
        // 设置场景
        javafx.scene.Scene scene = new javafx.scene.Scene(content);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setResizable(true);
    }
    
    /**
     * 设置提示信息
     * @param title 标题
     * @param contentLines 内容行
     */
    public void setContent(String title, List<String> contentLines) {
        // 清空现有内容
        content.getChildren().clear();
        
        // 添加标题
        Text titleText = new Text(title + "\n\n");
        titleText.setFont(Font.font("System", 12));
        titleText.setFill(Color.web("#4CAF50"));
        content.getChildren().add(titleText);
        
        // 添加内容行
        for (String line : contentLines) {
            Text lineText = new Text(line + "\n");
            lineText.setFont(Font.font("System", 11));
            lineText.setFill(Color.WHITE);
            content.getChildren().add(lineText);
        }
    }
    
    /**
     * 设置提示信息
     * @param contentText 内容文本
     */
    public void setContent(String contentText) {
        // 清空现有内容
        content.getChildren().clear();
        
        // 添加内容
        Text text = new Text(contentText);
        text.setFont(Font.font("System", 11));
        text.setFill(Color.WHITE);
        content.getChildren().add(text);
    }
    
    /**
     * 显示提示框
     * @param node 目标节点
     * @param x 鼠标X坐标
     * @param y 鼠标Y坐标
     */
    public void show(Node node, double x, double y) {
        // 如果关闭了提示信息显示，则直接返回
        if (!showTooltips) {
            return;
        }
        
        if (node == null || !node.isVisible()) {
            return;
        }
        
        // 获取节点所在的窗口
        Window window = node.getScene().getWindow();
        if (window == null) {
            return;
        }
        
        // 计算显示位置：窗口中心位置
        double windowX = window.getX();
        double windowY = window.getY();
        double windowWidth = window.getWidth();
        double windowHeight = window.getHeight();
        
        // 确保content大小已计算
        content.applyCss();
        content.layout();
        
        // 强制计算内容的首选大小
        double contentPrefWidth = content.prefWidth(-1);
        double contentPrefHeight = content.prefHeight(contentPrefWidth);
        
        // 确保宽度在最小和最大范围内
        double contentWidth = Math.max(100, Math.min(350, contentPrefWidth));
        double contentHeight = content.prefHeight(contentWidth);
        
        // 更新content的实际大小
        content.setPrefWidth(contentWidth);
        content.setPrefHeight(contentHeight);
        content.layout();
        
        // 计算窗口中心坐标，确保提示框中心与窗口中心对齐
        double windowCenterX = windowX + windowWidth / 2;
        double windowCenterY = windowY + windowHeight / 2;
        double stageWidth = contentWidth + 24; // 加上padding
        double stageHeight = contentHeight + 16; // 加上padding
        double screenX = windowCenterX - stageWidth / 2;
        double screenY = windowCenterY - stageHeight / 2;
        
        // 设置窗口位置和大小
        stage.setX(screenX);
        stage.setY(screenY);
        stage.setWidth(stageWidth);
        stage.setHeight(stageHeight);
        
        // 显示提示框
        stage.show();
    }
    
    /**
     * 隐藏提示框
     */
    public void hide() {
        stage.hide();
    }
    
    /**
     * 检查提示框是否正在显示
     * @return 是否正在显示
     */
    public boolean isShowing() {
        return stage.isShowing();
    }
    
    /**
     * 绑定到节点的鼠标事件
     * @param node 目标节点
     * @param title 标题
     * @param contentLines 内容行
     */
    public static void bindToNode(Node node, String title, List<String> contentLines) {
        FloatingTooltip tooltip = new FloatingTooltip();
        tooltip.setContent(title, contentLines);
        
        // 鼠标进入事件
        node.setOnMouseEntered(event -> {
            tooltip.show(node, event.getX(), event.getY());
        });
        
        // 鼠标移动事件
        node.setOnMouseMoved(event -> {
            if (!tooltip.isShowing()) {
                tooltip.show(node, event.getX(), event.getY());
            }
        });
        
        // 鼠标离开事件
        node.setOnMouseExited(event -> {
            tooltip.hide();
        });
    }
    
    /**
     * 绑定到节点的鼠标事件
     * @param node 目标节点
     * @param contentText 内容文本
     */
    public static void bindToNode(Node node, String contentText) {
        FloatingTooltip tooltip = new FloatingTooltip();
        tooltip.setContent(contentText);
        
        // 鼠标进入事件
        node.setOnMouseEntered(event -> {
            tooltip.show(node, event.getX(), event.getY());
        });
        
        // 鼠标移动事件
        node.setOnMouseMoved(event -> {
            if (!tooltip.isShowing()) {
                tooltip.show(node, event.getX(), event.getY());
            }
        });
        
        // 鼠标离开事件
        node.setOnMouseExited(event -> {
            tooltip.hide();
        });
    }
    
    /**
     * 设置是否显示提示信息
     * @param show 是否显示提示信息
     */
    public static void setShowTooltips(boolean show) {
        showTooltips = show;
    }
    
    /**
     * 获取是否显示提示信息
     * @return 是否显示提示信息
     */
    public static boolean isShowTooltips() {
        return showTooltips;
    }
}