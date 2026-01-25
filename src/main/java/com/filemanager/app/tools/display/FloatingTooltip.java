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
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * 悬浮提示框组件
 * 支持多行文本、自适应显示、自动换行、鼠标右上角显示
 */
public class FloatingTooltip {
    private final Popup popup;
    private final TextFlow content;
    private final double widthToHeightRatio = 3.0 / 7.0;
    
    /**
     * 构造函数
     */
    public FloatingTooltip() {
        // 创建弹出窗口
        popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        
        // 创建文本流容器
        content = new TextFlow();
        content.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.8); " +
            "-fx-text-fill: white; " +
            "-fx-padding: 8 12 8 12; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;"
        );
        content.setMaxWidth(300); // 设置最大宽度
        
        // 添加到弹出窗口
        popup.getContent().add(content);
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
        
        // 自适应调整大小
        adjustSize();
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
        
        // 自适应调整大小
        adjustSize();
    }
    
    /**
     * 自适应调整大小
     */
    private void adjustSize() {
        // 测量文本宽度
        content.layout();
        double textWidth = content.getWidth();
        double textHeight = content.getHeight();
        
        // 根据长宽比调整
        if (textWidth > 0 && textHeight > 0) {
            double currentRatio = textWidth / textHeight;
            if (currentRatio > widthToHeightRatio) {
                // 宽度过大，增加高度
                content.setMaxWidth(textWidth * widthToHeightRatio);
            } else {
                // 高度过大，增加宽度
                content.setMaxWidth(textWidth);
            }
        }
        
        // 强制重新布局
        content.layout();
    }
    
    /**
     * 显示提示框
     * @param node 目标节点
     * @param x 鼠标X坐标
     * @param y 鼠标Y坐标
     */
    public void show(Node node, double x, double y) {
        if (node == null || !node.isVisible()) {
            return;
        }
        
        // 获取节点所在的窗口
        Window window = node.getScene().getWindow();
        if (window == null) {
            return;
        }
        
        // 计算显示位置：鼠标上方有距离的位置
        Point2D point = node.localToScene(x, y);
        double sceneX = point.getX();
        double sceneY = point.getY();
        
        // 转换为屏幕坐标
        double screenX = node.getScene().getWindow().getX() + sceneX + 10; // 向右偏移10像素
        double screenY = node.getScene().getWindow().getY() + sceneY - 30; // 向上偏移30像素，避免遮挡鼠标
        
        // 显示提示框
        popup.show(node, screenX, screenY);
    }
    
    /**
     * 隐藏提示框
     */
    public void hide() {
        popup.hide();
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
            if (!tooltip.popup.isShowing()) {
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
            if (!tooltip.popup.isShowing()) {
                tooltip.show(node, event.getX(), event.getY());
            }
        });
        
        // 鼠标离开事件
        node.setOnMouseExited(event -> {
            tooltip.hide();
        });
    }
}