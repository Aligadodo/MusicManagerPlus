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
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * UI 组件样式工厂
 * 负责生成风格统一的界面元素
 *
 * @author 28667
 */
public class StyleFactory {

    private static ThemeConfig theme = null;
    String baseStyle = "-fx-background-color: transparent; -fx-border-radius: 3; ";
    // 悬停样式
    String hoverStyle = "-fx-background-color: #eee; -fx-border-radius: 3;  ";

    public static void initStyleFactory(ThemeConfig theme) {
        StyleFactory.theme = theme;
    }

    public static Label createLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font(theme.getFontFamily(), bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(theme.getTextColor()));
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
        // HBox 容器实现：[线条] 文字 [线条]
        HBox labelDivider = new HBox(10);
        labelDivider.setAlignment(Pos.CENTER);

        Label label = new Label(desc);
        label.setFont(Font.font(theme.getFontFamily(), FontWeight.NORMAL, 11));
        label.setTextFill(Color.web(theme.getLightTextColor()));

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
        label.setTextFill(Color.web(theme.getTextColor()));
        label.minWidth(30);
        return label;
    }

    public static Label createChapter(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getTitleFontFamily(), FontWeight.BOLD, 16));
        label.setTextFill(Color.web(theme.getTextColor()));
        label.minWidth(30);
        return label;
    }

    public static Label createDescLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getDescriptionFontFamily(), FontWeight.NORMAL, theme.getDescriptionFontSize()));
        label.setTextFill(Color.web(theme.getTextColor()));
        return label;
    }

    public static AutoShrinkLabel createParamLabel(String text) {
        AutoShrinkLabel label = new AutoShrinkLabel(text);
        label.minWidth(70);
        label.maxWidth(70);
        label.setFont(Font.font(theme.getFontFamily(), FontWeight.BOLD, 12));
        label.setTextFill(Color.web(theme.getTextColor()));
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
        l.setTextFill(Color.web(theme.getLightTextColor()));
        l.setMaxWidth(maxWidth);
        l.setWrapText(true);
        return l;
    }

    public static TextArea createTextArea() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-font-family: %s; -fx-font-size: %.1f;",
                theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth(),
                theme.getLogFontFamily(), theme.getLogFontSize()
        ));
        return logArea;
    }

    private static JFXButton createButton(String text) {
        JFXButton btn = new JFXButton(text);
        btn.setFont(Font.font(theme.getButtonFontFamily(), FontWeight.NORMAL, theme.getButtonFontSize()));
        return btn;
    }

    public static JFXButton createActionButton(String text, String colorOverride, Runnable action) {
        return createLargeActionButton(text, colorOverride, action); // 默认使用大按钮
    }
    
    public static JFXButton createLargeActionButton(String text, String colorOverride, Runnable action) {
        return createActionButton(text, colorOverride, action, theme.getLargeButtonSize());
    }
    
    public static JFXButton createSmallActionButton(String text, String colorOverride, Runnable action) {
        return createActionButton(text, colorOverride, action, theme.getSmallButtonSize());
    }
    
    public static JFXButton createActionButton(String text, String colorOverride, Runnable action, double minWidth) {
        JFXButton btn = createButton(text);
        String color = colorOverride != null ? colorOverride : theme.getAccentColor();
        
        // 验证颜色格式
        if (!color.startsWith("#")) {
            color = "#" + color;
        }
        
        // 基础样式
        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: %.1f; -fx-cursor: hand; -fx-padding: %.1f; -fx-border-width: 0;",
                color, theme.getCornerRadius(), theme.getMediumSpacing()
        );
        
        // 悬停样式
        Color baseColor;
        try {
            baseColor = Color.web(color);
        } catch (IllegalArgumentException e) {
            // 如果颜色格式无效，使用默认颜色
            baseColor = Color.web(theme.getAccentColor());
        }
        
        // 更好的悬停效果：不使用brighter()，而是添加边框和轻微的背景调整
        // 对于浅色按钮，添加深色边框；对于深色按钮，添加浅色边框
        Color borderColor;
        if (baseColor.getBrightness() > 0.6) {
            // 浅色背景，使用深色边框
            borderColor = baseColor.darker().darker();
        } else {
            // 深色背景，使用浅色边框
            borderColor = baseColor.brighter().brighter();
        }
        
        // 保持背景色不变或仅轻微调整
        String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: %.1f; -fx-cursor: hand; -fx-padding: %.1f; -fx-border-width: 2; -fx-border-color: %s;",
                color, theme.getCornerRadius(), theme.getMediumSpacing(), borderColor
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
        String textColor = colorHex != null ? colorHex : theme.getTextColor();

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
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth()
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
        p.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth()));
        p.setPadding(new Insets(5, 5, 5, 5));
        p.setSpacing(5);
        return p;
    }

    public static VBox createSectionHeader(String title, String subtitle) {
        VBox v = new VBox(2);
        v.getChildren().addAll(createHeader(title), createInfoLabel(subtitle, 400));
        return v;
    }

    public static void forceDarkText(Node node) {
        if (node instanceof Labeled) ((Labeled) node).setTextFill(Color.web(theme.getTextColor()));
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) forceDarkText(child);
        }
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
        btn.setTextFill(Color.web(theme.getTextColor()));
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
                theme.getBorderColor(), theme.getCornerRadius(), theme.getFontFamily(), theme.getTextColor()
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
                        setTextFill(Color.web(theme.getTextColor()));
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
     * 递归更新节点及其子节点的样式
     * 确保所有界面元素都能正确响应样式变化
     */
    public static void updateNodeStyle(Node node) {
        refreshAllComponents(node);
    }
    
    /**
     * 全面刷新所有组件样式
     * 遍历所有界面元素及其子元素，根据组件类型应用不同的主题样式
     */
    public static void refreshAllComponents(Node node) {
        if (node == null || theme == null) {
            return;
        }
        
        // 更新节点本身的样式
        applyComponentStyle(node);
        
        // 递归更新子节点
        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            for (Node child : parent.getChildrenUnmodifiable()) {
                refreshAllComponents(child);
            }
        }
    }
    
    /**
     * 根据组件类型应用不同的主题样式
     */
    private static void applyComponentStyle(Node node) {
        if (node == null || theme == null) {
            return;
        }
        
        // 处理标签和按钮等可标记组件
        if (node instanceof Labeled) {
            applyLabeledStyle((Labeled) node);
        }
        
        // 处理布局容器组件
        if (node instanceof VBox) {
            applyVBoxStyle((VBox) node);
        } else if (node instanceof HBox) {
            applyHBoxStyle((HBox) node);
        } else if (node instanceof BorderPane) {
            applyBorderPaneStyle((BorderPane) node);
        } else if (node instanceof GridPane) {
            applyGridPaneStyle((GridPane) node);
        } else if (node instanceof StackPane) {
            applyStackPaneStyle((StackPane) node);
        }
        
        // 处理滚动容器
        if (node instanceof ScrollPane) {
            applyScrollPaneStyle((ScrollPane) node);
        }
        
        // 处理标签页容器
        if (node instanceof TabPane) {
            applyTabPaneStyle((TabPane) node);
        }
        
        // 处理标题面板
        if (node instanceof TitledPane) {
            applyTitledPaneStyle((TitledPane) node);
        }
        
        // 处理分隔线
        if (node instanceof Separator) {
            applySeparatorStyle((Separator) node);
        }
        
        // 处理进度条
        if (node instanceof ProgressBar) {
            applyProgressBarStyle((ProgressBar) node);
        }
        
        // 处理列表视图
        if (node instanceof ListView) {
            applyListViewStyle((ListView<?>) node);
        }
        
        // 处理表格视图
        if (node instanceof TableView) {
            applyTableViewStyle((TableView<?>) node);
        }
        
        // 处理树表格视图
        if (node instanceof TreeTableView) {
            applyTreeTableViewStyle((TreeTableView<?>) node);
        }
        
        // 处理文本区域
        if (node instanceof TextArea) {
            applyTextAreaStyle((TextArea) node);
        }
    }
    
    /**
     * 应用标签和按钮等可标记组件的样式
     */
    private static void applyLabeledStyle(Labeled labeled) {
        Font currentFont = labeled.getFont();
        if (currentFont != null) {
            labeled.setFont(Font.font(
                    theme.getFontFamily(),
                    currentFont.getStyle().contains("Bold") ? FontWeight.BOLD : FontWeight.NORMAL,
                    currentFont.getSize()
            ));
        }
        labeled.setTextFill(Color.web(theme.getTextColor()));
    }
    
    /**
     * 应用VBox样式
     */
    private static void applyVBoxStyle(VBox vbox) {
        // 检查是否是我们创建的面板
        String currentStyle = vbox.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式
            vbox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-spacing: %.1f;",
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth(), vbox.getSpacing()
            ));
        }
    }
    
    /**
     * 应用HBox样式
     */
    private static void applyHBoxStyle(HBox hbox) {
        // 检查是否是我们创建的面板
        String currentStyle = hbox.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式
            hbox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-spacing: %.1f;",
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth(), hbox.getSpacing()
            ));
        }
    }
    
    /**
     * 应用BorderPane样式
     */
    private static void applyBorderPaneStyle(BorderPane borderPane) {
        String currentStyle = borderPane.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式
            borderPane.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth()
            ));
        }
    }
    
    /**
     * 应用GridPane样式
     */
    private static void applyGridPaneStyle(GridPane gridPane) {
        String currentStyle = gridPane.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式
            gridPane.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth()
            ));
        }
    }
    
    /**
     * 应用StackPane样式
     */
    private static void applyStackPaneStyle(StackPane stackPane) {
        String currentStyle = stackPane.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式
            stackPane.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth()
            ));
        }
    }
    
    /**
     * 应用ScrollPane样式
     */
    private static void applyScrollPaneStyle(ScrollPane scrollPane) {
        scrollPane.setStyle(String.format(
                "-fx-background-color: transparent; -fx-border-color: transparent;"
        ));
    }
    
    /**
     * 应用TabPane样式
     */
    private static void applyTabPaneStyle(TabPane tabPane) {
        tabPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: 40; -fx-tab-max-height: 40; -fx-text-fill: %s;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getTextColor()
        ));
        
        // 更新所有标签页的样式
        for (Tab tab : tabPane.getTabs()) {
            tab.setStyle(String.format(
                    "-fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 14px;",
                    theme.getTextColor(), theme.getFontFamily()
            ));
        }
    }
    
    /**
     * 应用TitledPane样式
     */
    private static void applyTitledPaneStyle(TitledPane titledPane) {
        titledPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-text-fill: %s; -fx-font-family: %s;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getTextColor(), theme.getFontFamily()
        ));
    }
    
    /**
     * 应用Separator样式
     */
    private static void applySeparatorStyle(Separator separator) {
        separator.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));
    }
    
    /**
     * 应用ProgressBar样式
     */
    private static void applyProgressBarStyle(ProgressBar progressBar) {
        progressBar.setStyle(String.format(
                "-fx-accent: %s; -fx-background-color: transparent; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getProgressBarColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));
    }
    
    /**
     * 应用ListView样式
     */
    private static void applyListViewStyle(ListView<?> listView) {
        // 设置ListView的背景色和边框
        listView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
    }
    
    /**
     * 应用TableView样式
     */
    private static void applyTableViewStyle(TableView<?> tableView) {
        // 设置TableView的背景色和边框
        tableView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
    }
    
    /**
     * 应用TreeTableView样式
     */
    private static void applyTreeTableViewStyle(TreeTableView<?> treeTableView) {
        // 设置TreeTableView的背景色和边框
        treeTableView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
    }
    
    /**
     * 应用TextArea样式
     */
    private static void applyTextAreaStyle(TextArea textArea) {
        // 设置TextArea的背景色、边框和字体
        textArea.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-font-family: 'Consolas'; -fx-font-size: 12px;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getTextColor()
        ));
    }
    
    /**
     * 设置面板的基本样式
     */
    public static void setBasicStyle(Region region) {
        if (region == null || theme == null) {
            return;
        }
        
        region.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));
    }
    
    /**
     * 设置菜单和菜单项的样式
     */
    public static void setMenuStyle(MenuBar menuBar) {
        if (menuBar == null || theme == null) {
            return;
        }
        
        // 设置菜单栏样式
        menuBar.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: " + theme.getTextColor() + ";"
        );
        
        // 设置所有菜单和菜单项的样式
        for (Menu menu : menuBar.getMenus()) {
            setMenuItemStyle(menu);
            for (MenuItem item : menu.getItems()) {
                setMenuItemStyle(item);
            }
        }
    }
    
    /**
     * 设置单个菜单项的样式
     */
    public static void setMenuItemStyle(MenuItem item) {
        if (item == null || theme == null) {
            return;
        }
        
        item.setStyle(
                "-fx-text-fill: " + theme.getTextColor() + "; " +
                "-fx-font-family: " + theme.getFontFamily() + "; " +
                "-fx-font-size: 14px;"
        );
        
        if (item instanceof Menu) {
            Menu menu = (Menu) item;
            for (MenuItem subItem : menu.getItems()) {
                setMenuItemStyle(subItem);
            }
        }
    }
    
    /**
     * 创建带有主题样式的TabPane
     */
    public static JFXTabPane createTabPane() {
        JFXTabPane tabPane = new JFXTabPane();
        tabPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: 40; -fx-tab-max-height: 40; -fx-text-fill: %s;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getTextColor()
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
                theme.getTextColor(), theme.getFontFamily()
        ));
        
        return tab;
    }


    public static HBox createTreeItemMenu(EventHandler<ActionEvent> open, EventHandler<ActionEvent> up, EventHandler<ActionEvent> down, EventHandler<ActionEvent> del) {
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_RIGHT);
        // 策略操作：上移、下移、删除
        // (注：配置详情通过列表选中触发，这里不需要额外按钮，或者可以加一个 '⚙' 指示)
        if (open != null) {
            JFXButton openUp = StyleFactory.createSmallIconButton("📂", open);
            actions.getChildren().add(openUp);
        }
        if (up != null) {
            JFXButton btnUp = StyleFactory.createSmallIconButton("▲", up);
            actions.getChildren().add(btnUp);
        }
        if (down != null) {
            JFXButton btnDown = StyleFactory.createSmallIconButton("▼", down);
            actions.getChildren().add(btnDown);
        }
        if (del != null) {
            JFXButton btnDel = StyleFactory.createSmallIconButton("✕", del);
            btnDel.setTextFill(Color.web("#e74c3c"));
            actions.getChildren().add(btnDel);
        }
        return actions;
    }


    /**
     * 更新列表行选中的样式
     *
     * @param node
     * @param selected
     */
    public static void updateTreeItemStyle(Node node, boolean selected) {
        if (theme == null) {
            return;
        }
        
        if (selected) {
            // 选中样式：使用主题中的选中颜色
            node.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 0 1 0; -fx-text-fill: %s;",
                    theme.getListRowSelectedBgColor(), theme.getBorderColor(), theme.getTextColor()
            ));
        } else {
            // 默认样式：使用主题中的边框颜色
            node.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-border-color: %s; -fx-border-width: 0 0 1 0; -fx-text-fill: %s;",
                    theme.getBorderColor(), theme.getTextColor()
            ));
        }
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

    public static void setBasicStyle(Node node) {
//        node.setStyle(baseStyle);
//        if (node instanceof Labeled) {
//            ((Labeled) node).setTextFill(Color.web(theme.getTextColor()));
//        }
//        if (node instanceof Parent) {
//            for (Node c : ((Parent) node).getChildrenUnmodifiable()) {
//                setBasicStyle(c);
//            }
//        }
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
}
