/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.tools.display;

import com.filemanager.model.ChangeRecord;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * UI 组件创建工厂
 * 负责生成风格统一的界面元素
 *
 * @author hrcao
 */
public class ComponentFactory {

    private static ThemeConfig theme = null;

    public static void initComponentFactory(ThemeConfig theme) {
        ComponentFactory.theme = theme;
    }

    public static Label createLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font(theme.getFontFamily(), bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(theme.getTextPrimaryColor()));
        return l;
    }

    public static Node createSeparator() {
        Separator separator = new Separator();
        separator.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));
        return separator;
    }

    /**
     * 渐变分割线
     *
     * @param isVertical
     * @return
     */
    public static Node createSeparatorWithChange(boolean isVertical) {
        String accentColor = theme.getAccentColor();
        if (isVertical) {
            // 水平渐变分割线
            Region hDivider = new Region();
            hDivider.setPrefHeight(1); // 线条粗细
            hDivider.setStyle(
                    String.format("-fx-background-color: linear-gradient(to right, transparent, %s 50%%, transparent);", accentColor)
            );
            return hDivider;
        }
        // 垂直渐变分割线
        Region vDivider = new Region();
        vDivider.setPrefWidth(1);
        vDivider.setStyle(
                String.format("-fx-background-color: linear-gradient(to bottom, transparent, %s 50%%, transparent);", accentColor)
        );
        return vDivider;
    }

    /**
     * 带提示词的分割线
     *
     * @param desc
     * @return
     */
    public static HBox createSeparatorWithDesc(String desc) {
        HBox labelDivider = new HBox(10);
        labelDivider.setAlignment(Pos.CENTER);

        Label label = new Label(desc);
        label.setFont(Font.font(theme.getFontFamily(), FontWeight.NORMAL, 11));
        label.setTextFill(Color.web(theme.getTextSecondaryColor()));

        Region line1 = new Region();
        HBox.setHgrow(line1, Priority.ALWAYS);
        line1.setPrefHeight(1);
        line1.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));

        Region line2 = new Region();
        HBox.setHgrow(line2, Priority.ALWAYS);
        line2.setPrefHeight(1);
        line2.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));

        labelDivider.getChildren().addAll(line1, label, line2);
        return labelDivider;
    }

    /**
     * 自动把其他组件排挤到左右两侧
     *
     * @return
     */
    public static Node createSpacer() {
        Region spacer = new Region();
        spacer.setStyle("-fx-background-color: transparent;");
        spacer.getStyleClass().add("glass-pane");
        // 关键核心：设置其在 HBox 中始终自动扩展
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Label createHeader(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getTitleFontFamily(), FontWeight.BOLD, theme.getTitleFontSize()));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        label.minWidth(30);
        return label;
    }

    public static Label createChapter(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getTitleFontFamily(), FontWeight.BOLD, 16));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        label.minWidth(30);
        return label;
    }

    public static Label createDescLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getDescriptionFontFamily(), FontWeight.NORMAL, theme.getDescriptionFontSize()));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        return label;
    }

    public static AutoShrinkLabel createParamLabel(String text) {
        AutoShrinkLabel label = new AutoShrinkLabel(text);
        label.minWidth(70);
        label.maxWidth(70);
        label.setFont(Font.font(theme.getFontFamily(), FontWeight.BOLD, 12));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        return label;
    }

    public static HBox createParamPairLine(String labelText, Node... controls) {
        HBox hBox = createHBox(createParamLabel(labelText), createSpacer());
        hBox.getChildren().addAll(controls);
        hBox.setSpacing(theme.getSmallSpacing());
        return hBox;
    }

    public static Label createInfoLabel(String text, int maxWidth) {
        Label l = createLabel(text, 10, false);
        l.setTextFill(Color.web(theme.getTextSecondaryColor()));
        l.setMaxWidth(maxWidth);
        l.setWrapText(true);
        return l;
    }

    public static TextArea createTextArea() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPadding(new Insets(10));
        logArea.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                "-fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: %.1f;\n" +
                ".text-area .scroll-pane {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".text-area .scroll-pane .viewport {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".text-area .scroll-pane .content {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".text-area .scroll-bar:vertical {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-background-radius: 0;\n" +
                "}\n" +
                ".text-area .scroll-bar:horizontal {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-background-radius: 0;\n" +
                "}\n" +
                ".text-area .scroll-bar .thumb {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-background-radius: 4;\n" +
                "}\n" +
                ".text-area .scroll-bar .track {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".text-area .scroll-bar .increment-button, .text-area .scroll-bar .decrement-button {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-pref-height: 0;\n" +
                "    -fx-pref-width: 0;\n" +
                "}",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getTextPrimaryColor(), theme.getLogFontFamily(), theme.getLogFontSize(),
                theme.getTextTertiaryColor()
        ));
        return logArea;
    }

    public static VBox createVBoxPanel() {
        VBox panel = new VBox(theme.getMediumSpacing());
        panel.setPadding(new Insets(theme.getLargeSpacing()));
        panel.setStyle(String.format(
                "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));
        return panel;
    }

    public static HBox createHBoxPanel() {
        HBox panel = new HBox(theme.getMediumSpacing());
        panel.setPadding(new Insets(theme.getLargeSpacing()));
        panel.setStyle(String.format(
                "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));
        return panel;
    }

    public static JFXButton createButton(String text, boolean filled) {
        JFXButton btn = new JFXButton(text);
        if (filled) {
            btn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: white; -fx-font-family: %s; -fx-font-size: 14px; -fx-padding: 8 16;",
                    theme.getAccentColor(), theme.getFontFamily()
            ));
        } else {
            btn.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 14px; -fx-padding: 8 16; -fx-border-color: %s; -fx-border-width: 1px;",
                    theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getBorderColor()
            ));
        }
        return btn;
    }

    public static JFXButton createIconButton(String icon, Runnable action) {
        JFXButton btn = new JFXButton(icon);
        btn.setStyle(String.format(
                "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 16px; -fx-padding: 4 8;",
                theme.getTextPrimaryColor(), theme.getFontFamily()
        ));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private static JFXButton createButton(String text) {
        JFXButton btn = new JFXButton(text);
        btn.setFont(Font.font(theme.getButtonFontFamily(), FontWeight.NORMAL, theme.getButtonFontSize()));
        return btn;
    }

    /**
     * 创建主要按钮（一级按钮）
     */
    public static JFXButton createPrimaryButton(String text, Runnable action) {
        return createStyledButton(text, action, 
                theme.getButtonPrimaryBgColor(), theme.getButtonPrimaryTextColor(), 
                theme.getButtonPrimaryBorderColor(), theme.getButtonPrimaryHoverColor(),
                theme.getButtonLargeSize(), 36.0); // 一级按钮高度
    }
    
    /**
     * 创建次要按钮（二级按钮）
     */
    public static JFXButton createSecondaryButton(String text, Runnable action) {
        return createStyledButton(text, action, 
                theme.getButtonSecondaryBgColor(), theme.getButtonSecondaryTextColor(), 
                theme.getButtonSecondaryBorderColor(), theme.getButtonSecondaryHoverColor(),
                theme.getButtonSmallSize(), 28.0); // 二级按钮高度
    }
    
    /**
     * 创建成功按钮
     */
    public static JFXButton createSuccessButton(String text, Runnable action) {
        return createStyledButton(text, action, 
                theme.getButtonSuccessBgColor(), theme.getButtonSuccessTextColor(), 
                theme.getButtonSuccessBorderColor(), theme.getButtonSuccessHoverColor(),
                theme.getButtonSmallSize(), 28.0);
    }
    
    /**
     * 创建警告按钮
     */
    public static JFXButton createWarningButton(String text, Runnable action) {
        return createStyledButton(text, action, 
                theme.getButtonWarningBgColor(), theme.getButtonWarningTextColor(), 
                theme.getButtonWarningBorderColor(), theme.getButtonWarningHoverColor(),
                theme.getButtonSmallSize(), 28.0);
    }
    
    /**
     * 创建错误按钮
     */
    public static JFXButton createErrorButton(String text, Runnable action) {
        return createStyledButton(text, action, 
                theme.getButtonErrorBgColor(), theme.getButtonErrorTextColor(), 
                theme.getButtonErrorBorderColor(), theme.getButtonErrorHoverColor(),
                theme.getButtonSmallSize(), 28.0);
    }
    
    /**
     * 创建自定义样式的按钮
     */
    public static JFXButton createActionButton(String text, String colorOverride, Runnable action) {
        return createStyledButton(text, action, 
                colorOverride != null ? colorOverride : theme.getAccentColor(), "white",
                null, null, theme.getButtonSmallSize(), 28.0);
    }
    
    /**
     * 创建大尺寸按钮
     */
    public static JFXButton createLargeActionButton(String text, String colorOverride, Runnable action) {
        return createStyledButton(text, action, 
                colorOverride != null ? colorOverride : theme.getAccentColor(), "white",
                null, null, theme.getButtonLargeSize(), 36.0);
    }
    
    /**
     * 创建小尺寸按钮
     */
    public static JFXButton createSmallActionButton(String text, String colorOverride, Runnable action) {
        return createStyledButton(text, action, 
                colorOverride != null ? colorOverride : theme.getAccentColor(), "white",
                null, null, theme.getButtonSmallSize(), 24.0);
    }
    
    /**
     * 创建带样式的按钮
     */
    /**
     * 验证并格式化颜色值，确保它是有效的十六进制格式
     */
    private static String validateAndFormatColor(String colorValue) {
        if (colorValue == null || colorValue.isEmpty()) {
            return "#3498db"; // 默认颜色
        }
        
        // 移除可能的透明度后缀
        if (colorValue.contains("#") && colorValue.length() > 7) {
            colorValue = colorValue.substring(0, 7);
        }
        
        // 转换0x开头的颜色值
        if (colorValue.startsWith("0x")) {
            try {
                String hex = colorValue.substring(2);
                if (hex.length() == 8) {
                    hex = hex.substring(0, 6); // 移除透明度部分
                }
                return "#" + hex;
            } catch (Exception e) {
                return "#3498db"; // 默认颜色
            }
        }
        
        // 确保颜色值以#开头
        if (!colorValue.startsWith("#")) {
            return "#" + colorValue;
        }
        
        // 确保颜色值有正确的长度
        if (colorValue.length() != 7) {
            return "#3498db"; // 默认颜色
        }
        
        return colorValue;
    }
    
    private static JFXButton createStyledButton(String text, Runnable action, 
                                               String bgColor, String textColor, 
                                               String borderColor, String hoverColor,
                                               double minWidth, double buttonHeight) {
        JFXButton btn = createButton(text);
        
        // 验证颜色格式
        bgColor = validateAndFormatColor(bgColor);
        textColor = validateAndFormatColor(textColor);
        if (borderColor != null) {
            borderColor = validateAndFormatColor(borderColor);
        }
        if (hoverColor != null) {
            hoverColor = validateAndFormatColor(hoverColor);
        }
        
        // 如果边框颜色为空，使用背景色的变体
        if (borderColor == null && bgColor != null) {
            try {
                Color baseColor = Color.web(bgColor);
                if (baseColor.getBrightness() > 0.6) {
                    // 浅色背景，使用深色边框
                    borderColor = baseColor.darker().darker().toString();
                } else {
                    // 深色背景，使用浅色边框
                    borderColor = baseColor.brighter().brighter().toString();
                }
            } catch (IllegalArgumentException e) {
                borderColor = bgColor;
            }
        }
        
        // 如果悬停颜色为空，使用边框颜色
        if (hoverColor == null) {
            hoverColor = borderColor;
        }
        
        // 基础样式
        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-background-radius: %.1f; " +
                "-fx-cursor: hand; -fx-padding: %.1f; -fx-border-width: %.1f; -fx-border-color: %s; -fx-min-height: %.1f;",
                bgColor, textColor, theme.getCornerRadius(), theme.getSmallSpacing(), 
                theme.getBorderWidth(), borderColor != null ? borderColor : bgColor, buttonHeight
        );
        
        // 悬停样式
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-background-radius: %.1f; " +
                "-fx-cursor: hand; -fx-padding: %.1f; -fx-border-width: %.1f; -fx-border-color: %s; -fx-min-height: %.1f;",
                hoverColor != null ? hoverColor : bgColor, textColor, theme.getCornerRadius(), 
                theme.getSmallSpacing(), theme.getBorderWidth(), borderColor != null ? borderColor : bgColor, buttonHeight
        );
        
        btn.setStyle(baseStyle);
        btn.setMinWidth(minWidth);
        
        // 添加悬停效果
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        
        if (action != null) btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * [新增] 创建行内图标按钮 (如删除、上移下移)
     */
    public static JFXButton createIconButton(String iconText, String colorHex, Runnable action) {
        JFXButton btn = createButton(iconText);
        String textColor = colorHex != null ? colorHex : theme.getTextPrimaryColor();

        // 基础样式
        String baseStyle = String.format(
                "-fx-background-color: transparent; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: %.1f; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getBorderColor(), theme.getCornerRadius(), theme.getSmallSpacing(), textColor
        );
        // 悬停样式
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: %.1f; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getHoverColor(), theme.getBorderColor(), theme.getCornerRadius(), theme.getSmallSpacing(), textColor
        );

        btn.setStyle(baseStyle);

        btn.setOnAction(e -> {
            if (action != null) action.run();
            e.consume(); // 防止事件冒泡选中列表行
        });

        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));

        return btn;
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static VBox createVBox(Node... subNodes) {
        VBox p = new VBox();
        p.setStyle("-fx-background-color: transparent;");
        p.getStyleClass().add("glass-pane");
        for (Node subNode : subNodes) {
            p.getChildren().add(subNode);
        }
        return p;
    }

    /**
     * 创建透明的竖向容器
     *
     * @return
     */
    public static VBox createVBoxPanel(Node... subNodes) {
        VBox p = createVBox(subNodes);
        p.setStyle(String.format(
                "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));
        p.setSpacing(theme.getMediumSpacing());
        return p;
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBox(Node... subNodes) {
        HBox p = new HBox();
        p.setStyle("-fx-background-color: transparent;");
        p.getStyleClass().add("glass-pane");
        for (Node subNode : subNodes) {
            p.getChildren().add(subNode);
        }
        return p;
    }


    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBoxPanel(Node... subNodes) {
        HBox p = createHBox(subNodes);
        p.setStyle(String.format("-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()));
        p.setPadding(new Insets(5, 5, 5, 5));
        p.setSpacing(5);
        return p;
    }

    public static VBox createSectionHeader(String title, String subtitle) {
        VBox v = new VBox(2);
        v.getChildren().addAll(createHeader(title), createInfoLabel(subtitle, 400));
        return v;
    }

    // [新增] 通用：创建统一风格的微型图标按钮
    public static JFXButton createSmallIconButton(String text, EventHandler<ActionEvent> handler) {
        JFXButton btn = createButton(text);
        String baseStyle = String.format(
                "-fx-background-color: transparent; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: %.1f; -fx-font-size: 10px; -fx-font-family: %s;",
                theme.getBorderColor(), theme.getCornerRadius(), theme.getSmallSpacing(), theme.getFontFamily()
        );
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: %.1f; -fx-font-size: 10px; -fx-font-family: %s;",
                theme.getHoverColor(), theme.getBorderColor(), theme.getCornerRadius(), theme.getSmallSpacing(), theme.getFontFamily()
        );
        
        btn.setStyle(baseStyle);
        btn.setTextFill(Color.web(theme.getTextPrimaryColor()));
        btn.setOnAction(e -> {
            handler.handle(e);
            e.consume(); // 防止事件冒泡触发 ListCell 选中
        });
        // Hover 效果
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        return btn;
    }

    public static TreeTableColumn<ChangeRecord, String> createTreeTableColumn(String text, boolean needToolTip, int prefWidth, int minWidth, int maxWidth) {
        TreeTableColumn<ChangeRecord, String> column = new TreeTableColumn<>(text);
        column.setPrefWidth(prefWidth);
        column.setMinWidth(minWidth);
        column.setMaxWidth(maxWidth);
        column.setStyle(String.format(
                "-fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: 2 6 2 6; -fx-font-size: 10px; -fx-font-family: %s; -fx-text-fill: %s;",
                theme.getBorderColor(), theme.getCornerRadius(), theme.getFontFamily(), theme.getTextPrimaryColor()
        ));
        column.setCellFactory(col -> {
            return new TreeTableCell<ChangeRecord, String>() {
                private final Tooltip tooltip = new Tooltip();

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null); // 必须清除，否则空行也会显示上一个内容的悬浮
                    } else {
                        setText(item);
                        setFont(Font.font(theme.getFontFamily(), FontWeight.NORMAL, 12));
                        setTextFill(Color.web(theme.getTextPrimaryColor()));
                        // 设置悬浮内容
                        tooltip.setText("详情内容：\n" + item);
                        // 可选：设置换行宽度，防止详情太长变成一条直线
                        tooltip.setWrapText(true);
                        tooltip.setPrefWidth(300);
                        setTooltip(tooltip);
                    }
                }
            };
        });
        return column;
    }

    /**
     * 创建带有主题样式的TabPane
     */
    public static JFXTabPane createTabPane() {
        JFXTabPane tabPane = new JFXTabPane();
        tabPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: 40; -fx-tab-max-height: 40; -fx-text-fill: %s;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getTextPrimaryColor()
        ));
        
        return tabPane;
    }
    
    /**
     * 为Tab添加主题样式
     */
    public static Tab createStyledTab(String text, Node content) {
        Tab tab = new Tab(text, content);
        tab.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 14px;",
                theme.getTextPrimaryColor(), theme.getFontFamily()
        ));
        
        return tab;
    }

    public static Button createRefreshButton(EventHandler<ActionEvent> handler) {
        // 1. 创建刷新图标的 SVG 路径 (一个圆圈箭头)
        SVGPath refreshIcon = new SVGPath();
        refreshIcon.setContent("M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z");
        refreshIcon.setFill(javafx.scene.paint.Color.WHITE);

        // 2. 创建按钮并设置样式
        Button btn = new Button();
        btn.setGraphic(refreshIcon); // 将 SVG 设置为按钮图标
        btn.setStyle(
                "-fx-background-color: #BDE0FE;" + // 马卡龙蓝
                        "-fx-background-radius: 50;" +      // 圆形边框
                        "-fx-min-width: 20px;" +
                        "-fx-min-height: 20px;" +
                        "-fx-cursor: hand;"
        );

        // 3. 添加旋转动画（点击时触发）
        RotateTransition rt = new RotateTransition(Duration.millis(600), refreshIcon);
        rt.setByAngle(360); // 旋转 360 度
        rt.setCycleCount(1);
        rt.setInterpolator(Interpolator.EASE_BOTH); // 柔和的启动和停止

        btn.setOnAction(e -> {
            handler.handle(e);
            rt.playFromStart();
        });
        return btn;
    }

    /**
     * 创建统一风格的进度条
     * @param initialValue 初始进度值（0.0-1.0，-1.0表示不确定）
     * @param prefWidth 首选宽度
     * @return 配置好的进度条
     */
    public static ProgressBar createProgressBar(double initialValue, double prefWidth) {
        ProgressBar progressBar = new ProgressBar(initialValue);
        progressBar.setPrefHeight(25);
        progressBar.setPrefWidth(prefWidth);
        progressBar.setStyle("-fx-accent: #27ae60;");
        return progressBar;
    }
    
    /**
     * 创建主进度条（占满宽度）
     * @param initialValue 初始进度值
     * @return 配置好的主进度条
     */
    public static ProgressBar createMainProgressBar(double initialValue) {
        ProgressBar progressBar = createProgressBar(initialValue, 10000.0);
        return progressBar;
    }
    
    /**
     * 创建根路径进度条（固定宽度）
     * @param initialValue 初始进度值
     * @return 配置好的根路径进度条
     */
    public static ProgressBar createRootPathProgressBar(double initialValue) {
        return createProgressBar(initialValue, 200);
    }
    
    /**
     * 创建树节点菜单
     */
    public static HBox createTreeItemMenu(EventHandler<ActionEvent> open, EventHandler<ActionEvent> up, EventHandler<ActionEvent> down, EventHandler<ActionEvent> del) {
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_RIGHT);
        // 策略操作：上移、下移、删除
        if (open != null) {
            JFXButton openUp = createSmallIconButton("📂", open);
            actions.getChildren().add(openUp);
        }
        if (up != null) {
            JFXButton btnUp = createSmallIconButton("▲", up);
            actions.getChildren().add(btnUp);
        }
        if (down != null) {
            JFXButton btnDown = createSmallIconButton("▼", down);
            actions.getChildren().add(btnDown);
        }
        if (del != null) {
            JFXButton btnDel = createSmallIconButton("✕", del);
            btnDel.setTextFill(Color.web("#e74c3c"));
            actions.getChildren().add(btnDel);
        }
        return actions;
    }
    
    /**
     * 更新列表行选中的样式
     */
    public static void updateTreeItemStyle(Node node, boolean selected) {
        if (theme == null) {
            return;
        }
        
        if (selected) {
            // 选中样式：使用主题中的选中颜色
            node.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 0 1 0; -fx-text-fill: %s;",
                    theme.getListRowSelectedBgColor(), theme.getBorderColor(), theme.getTextPrimaryColor()
            ));
        } else {
            // 默认样式：使用主题中的边框颜色
            node.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-border-color: %s; -fx-border-width: 0 0 1 0; -fx-text-fill: %s;",
                    theme.getBorderColor(), theme.getTextPrimaryColor()
            ));
        }
    }
}