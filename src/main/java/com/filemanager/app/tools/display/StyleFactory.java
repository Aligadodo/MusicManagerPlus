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
    
    // 主题配置
    private static ThemeConfig theme;

    public static void initStyleFactory(ThemeConfig theme) {
        StyleFactory.theme = theme;
        ComponentFactory.initComponentFactory(theme);
        ComponentStyleManager.initComponentStyleManager(theme);
    }
    
    // 创建基础按钮
    public static JFXButton createButton(String text) {
        JFXButton btn = new JFXButton(text);
        btn.setButtonType(JFXButton.ButtonType.FLAT);
        return btn;
    }

    public static Label createLabel(String text, int size, boolean bold) {
        return ComponentFactory.createLabel(text, size, bold);
    }

    public static Node createSeparator() {
        return ComponentFactory.createSeparator();
    }

    /**
     * 渐变分割线
     *
     * @param isVertical
     * @return
     */
    public static Node createSeparatorWithChange(boolean isVertical) {
        return ComponentFactory.createSeparatorWithChange(isVertical);
    }

    /**
     * 带提示词的分割线
     *
     * @param desc
     * @return
     */
    public static HBox createSeparatorWithDesc(String desc) {
        return ComponentFactory.createSeparatorWithDesc(desc);
    }

    /**
     * 自动把其他组件排挤到左右两侧
     *
     * @return
     */
    public static Node createSpacer() {
        return ComponentFactory.createSpacer();
    }

    public static Label createHeader(String text) {
        return ComponentFactory.createHeader(text);
    }

    public static Label createChapter(String text) {
        return ComponentFactory.createChapter(text);
    }

    public static Label createDescLabel(String text) {
        return ComponentFactory.createDescLabel(text);
    }

    public static AutoShrinkLabel createParamLabel(String text) {
        return ComponentFactory.createParamLabel(text);
    }

    public static HBox createParamPairLine(String labelText, Node... controls) {
        return ComponentFactory.createParamPairLine(labelText, controls);
    }

    public static Label createInfoLabel(String text, int maxWidth) {
        return ComponentFactory.createInfoLabel(text, maxWidth);
    }

    public static TextArea createTextArea() {
        return ComponentFactory.createTextArea();
    }

    /**
     * 创建主要按钮（一级按钮）
     */
    public static JFXButton createPrimaryButton(String text, Runnable action) {
        return ComponentFactory.createPrimaryButton(text, action);
    }
    
    /**
     * 创建次要按钮（二级按钮）
     */
    public static JFXButton createSecondaryButton(String text, Runnable action) {
        return ComponentFactory.createSecondaryButton(text, action);
    }
    
    /**
     * 创建成功按钮
     */
    public static JFXButton createSuccessButton(String text, Runnable action) {
        return ComponentFactory.createSuccessButton(text, action);
    }
    
    /**
     * 创建警告按钮
     */
    public static JFXButton createWarningButton(String text, Runnable action) {
        return ComponentFactory.createWarningButton(text, action);
    }
    
    /**
     * 创建错误按钮
     */
    public static JFXButton createErrorButton(String text, Runnable action) {
        return ComponentFactory.createErrorButton(text, action);
    }
    
    /**
     * 创建自定义样式的按钮
     */
    public static JFXButton createActionButton(String text, String colorOverride, Runnable action) {
        return ComponentFactory.createActionButton(text, colorOverride, action);
    }
    
    /**
     * 创建大尺寸按钮
     */
    public static JFXButton createLargeActionButton(String text, String colorOverride, Runnable action) {
        return ComponentFactory.createLargeActionButton(text, colorOverride, action);
    }
    
    /**
     * 创建小尺寸按钮
     */
    public static JFXButton createSmallActionButton(String text, String colorOverride, Runnable action) {
        return ComponentFactory.createSmallActionButton(text, colorOverride, action);
    }
    
    /**
     * 创建带样式的按钮
     */
    private static JFXButton createStyledButton(String text, Runnable action, 
                                               String bgColor, String textColor, 
                                               String borderColor, String hoverColor,
                                               double minWidth, double buttonHeight) {
        JFXButton btn = createButton(text);
        
        // 验证颜色格式（只对有效的十六进制颜色值添加#前缀）
        if (bgColor != null) {
            // 如果不是预定义颜色（如white、black）且不是以#开头，添加#前缀
            if (!bgColor.startsWith("#") && !bgColor.matches("^[a-zA-Z]+$") && bgColor.length() <= 6) {
                bgColor = "#" + bgColor;
            }
        }
        if (textColor != null) {
            if (!textColor.startsWith("#") && !textColor.matches("^[a-zA-Z]+$") && textColor.length() <= 6) {
                textColor = "#" + textColor;
            }
        }
        if (borderColor != null) {
            if (!borderColor.startsWith("#") && !borderColor.matches("^[a-zA-Z]+$") && borderColor.length() <= 6) {
                borderColor = "#" + borderColor;
            }
        }
        if (hoverColor != null) {
            if (!hoverColor.startsWith("#") && !hoverColor.matches("^[a-zA-Z]+$") && hoverColor.length() <= 6) {
                hoverColor = "#" + hoverColor;
            }
        }
        
        // 如果边框颜色为空，使用背景色的变体
        if (borderColor == null && bgColor != null) {
            try {
                Color baseColor = Color.web(bgColor);
                if (baseColor.getBrightness() > 0.6) {
                    // 浅色背景，使用深色边框
                    borderColor = toCssColor(baseColor.darker().darker());
                } else {
                    // 深色背景，使用浅色边框
                    borderColor = toCssColor(baseColor.brighter().brighter());
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
        return ComponentFactory.createIconButton(iconText, colorHex, action);
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static VBox createVBox(Node... subNodes) {
        return ComponentFactory.createVBox(subNodes);
    }

    /**
     * 创建透明的竖向容器
     *
     * @return
     */
    public static VBox createVBoxPanel(Node... subNodes) {
        return ComponentFactory.createVBoxPanel(subNodes);
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBox(Node... subNodes) {
        return ComponentFactory.createHBox(subNodes);
    }


    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBoxPanel(Node... subNodes) {
        return ComponentFactory.createHBoxPanel(subNodes);
    }

    public static VBox createSectionHeader(String title, String subtitle) {
        return ComponentFactory.createSectionHeader(title, subtitle);
    }

    public static void forceDarkText(Node node) {
        if (node instanceof Labeled) ((Labeled) node).setTextFill(Color.web(theme.getTextPrimaryColor()));
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) forceDarkText(child);
        }
    }
    
    /**
     * 将Color对象转换为CSS可用的十六进制颜色字符串
     */
    private static String toCssColor(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    // [新增] 通用：创建统一风格的微型图标按钮
    public static JFXButton createSmallIconButton(String text, EventHandler<ActionEvent> handler) {
        return ComponentFactory.createSmallIconButton(text, handler);
    }

    public static TreeTableColumn<ChangeRecord, String> createTreeTableColumn(String text, boolean needToolTip, int prefWidth, int minWidth, int maxWidth) {
        return ComponentFactory.createTreeTableColumn(text, needToolTip, prefWidth, minWidth, maxWidth);
    }
    
    /**
     * 递归更新节点及其子节点的样式
     * 确保所有界面元素都能正确响应样式变化
     */
    public static void updateNodeStyle(Node node) {
        ComponentStyleManager.updateNodeStyle(node);
    }
    
    /**
     * 全面刷新所有组件样式
     * 遍历所有界面元素及其子元素，根据组件类型应用不同的主题样式
     */
    public static void refreshAllComponents(Node node) {
        ComponentStyleManager.refreshAllComponents(node);
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
        
        // 处理树视图
        if (node instanceof TreeView) {
            applyTreeViewStyle((TreeView<?>) node);
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
        
        // 根据标签的类型和重要性应用不同的文本颜色
        String textColor = theme.getTextPrimaryColor();
        
        // 根据字体大小判断文本重要性
        if (currentFont != null) {
            if (currentFont.getSize() <= 12) {
                textColor = theme.getTextSecondaryColor(); // 小字体使用次要文本颜色
            } else if (currentFont.getSize() >= 16) {
                textColor = theme.getTextPrimaryColor(); // 大字体使用主要文本颜色
            }
        }
        
        // 特殊处理禁用状态
        if (!labeled.isDisable()) {
            labeled.setTextFill(Color.web(textColor));
        } else {
            labeled.setTextFill(Color.web(theme.getTextDisabledColor()));
        }
    }
    
    /**
     * 应用VBox样式
     */
    private static void applyVBoxStyle(VBox vbox) {
        // 检查是否是我们创建的面板
        String currentStyle = vbox.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式，使用透明背景以便显示整体背景色
            vbox.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-spacing: %.1f;",
                    theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), vbox.getSpacing()
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
            // 应用面板样式，使用透明背景以便显示整体背景色
            hbox.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-spacing: %.1f;",
                    theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), hbox.getSpacing()
            ));
        }
    }
    
    /**
     * 应用BorderPane样式
     */
    private static void applyBorderPaneStyle(BorderPane borderPane) {
        String currentStyle = borderPane.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式，使用透明背景以便显示整体背景色
            borderPane.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                    theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
            ));
        }
    }
    
    /**
     * 应用GridPane样式
     */
    private static void applyGridPaneStyle(GridPane gridPane) {
        String currentStyle = gridPane.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式，使用透明背景以便显示整体背景色
            gridPane.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                    theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
            ));
        }
    }
    
    /**
     * 应用StackPane样式
     */
    private static void applyStackPaneStyle(StackPane stackPane) {
        String currentStyle = stackPane.getStyle();
        if (currentStyle.contains("-fx-background-color:") && !currentStyle.contains("transparent")) {
            // 应用面板样式，使用透明背景以便显示整体背景色
            stackPane.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                    theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
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
        // 简化的TabPane样式，避免参数不匹配的问题
        String style = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: 45; -fx-tab-max-height: 45; -fx-tab-min-width: 120; -fx-tab-max-width: 220; -fx-padding: 5 0 0 0;\n" +
                ".tab-pane > .tab-header-area {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-padding: 0 10 0 10;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .tab-header-background {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: 0 0 %.1f 0;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                "    -fx-border-radius: %.1f %.1f 0 0;\n" +
                "    -fx-cursor: hand;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:hover {\n" +
                "    -fx-background-color: %s;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:selected {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s %s %s %s;\n" +
                "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-family: %s;\n" +
                "    -fx-font-size: 15px;\n" +
                "    -fx-font-weight: normal;\n" +
                "    -fx-alignment: center;\n" +
                "    -fx-padding: 5 10;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:hover > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-weight: 500;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:selected > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-padding: 5 10;\n" +
                "}\n" +
                ".tab-pane > .tab-content-area {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: %.1f;\n" +
                "    -fx-border-radius: 0 %.1f %.1f %.1f;\n" +
                "}",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                theme.getBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getPanelHoverColor(),
                theme.getPanelBgColor(), theme.getAccentColor(), theme.getAccentColor(), theme.getPanelBgColor(), theme.getAccentColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(),
                theme.getTextSecondaryColor(), theme.getFontFamily(),
                theme.getTextPrimaryColor(),
                theme.getAccentColor(),
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(), theme.getCornerRadius()
        );
        
        tabPane.setStyle(style);
        
        // 更新所有标签页的样式
        for (Tab tab : tabPane.getTabs()) {
            // 移除单独设置的tab样式，因为样式已经在tab-pane的CSS中定义了
            tab.setStyle("");
        }
    }
    
    /**
     * 应用TitledPane样式
     */
    private static void applyTitledPaneStyle(TitledPane titledPane) {
        titledPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                ".titled-pane > .title {\n" +
                "    -fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 14px; -fx-font-weight: bold;\n" +
                "    -fx-padding: 10 15 10 15; -fx-border-width: 0 0 %.1f 0; -fx-border-color: %s;\n" +
                "}\n" +
                ".titled-pane > .title > .arrow-button {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".titled-pane > .title > .arrow-button .arrow {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-effect: none;\n" +
                "}\n" +
                ".titled-pane > .content {\n" +
                "    -fx-background-color: %s; -fx-border-width: 0;\n" +
                "    -fx-background-radius: 0 0 %.1f %.1f;\n" +
                "}",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getPanelBgColor(), theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getBorderWidth(), theme.getBorderColor(),
                theme.getTextPrimaryColor(),
                theme.getPanelBgColor(), theme.getCornerRadius()
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
        ComponentStyleManager.applyListViewStyle(listView);
    }
    
    /**
     * 应用TableView样式
     */
    private static void applyTableViewStyle(TableView<?> tableView) {
        ComponentStyleManager.applyTableViewStyle(tableView);
    }
    
    /**
     * 应用TreeTableView样式
     */
    private static void applyTreeTableViewStyle(TreeTableView<?> treeTableView) {
        ComponentStyleManager.applyTreeTableViewStyle(treeTableView);
    }
    
    /**
     * 应用TreeView样式
     */
    public static void applyTreeViewStyle(TreeView<?> treeView) {
        ComponentStyleManager.applyTreeViewStyle(treeView);
    }
    
    /**
     * 应用文本输入控件样式（公共方法）
     */
    public static void applyTextInputControlStyle(TextInputControl control) {
        ComponentStyleManager.applyTextInputControlStyle(control);
    }
    
    /**
     * 应用TextArea样式
     */
    private static void applyTextAreaStyle(TextArea textArea) {
        ComponentStyleManager.applyTextAreaStyle(textArea);
    }
    
    /**
     * 设置面板的基本样式
     */
    public static void setBasicStyle(Region region) {
        ComponentStyleManager.setBasicStyle(region);
    }
    
    /**
     * 设置菜单和菜单项的样式
     */
    public static void setMenuStyle(MenuBar menuBar) {
        ComponentStyleManager.setMenuStyle(menuBar);
    }
    
    /**
     * 创建带有主题样式的TabPane
     */
    public static JFXTabPane createTabPane() {
        return ComponentFactory.createTabPane();
    }
    
    /**
     * 为Tab添加主题样式
     */
    public static Tab createStyledTab(String text, Node content) {
        return ComponentFactory.createStyledTab(text, content);
    }


    public static HBox createTreeItemMenu(EventHandler<ActionEvent> open, EventHandler<ActionEvent> up, EventHandler<ActionEvent> down, EventHandler<ActionEvent> del) {
        return ComponentFactory.createTreeItemMenu(open, up, down, del);
    }
    
    /**
     * 创建带有主题样式的ListView
     */
    public static <T> ListView<T> createListView() {
        return ComponentFactory.createListView();
    }


    /**
     * 更新列表行选中的样式
     *
     * @param node
     * @param selected
     */
    public static void updateTreeItemStyle(Node node, boolean selected) {
        ComponentStyleManager.updateTreeItemStyle(node, selected);
    }

    public static Button createRefreshButton(EventHandler<ActionEvent> handler) {
        return ComponentFactory.createRefreshButton(handler);
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
        return ComponentFactory.createProgressBar(initialValue, prefWidth);
    }
    
    /**
     * 创建主进度条（占满宽度）
     * @param initialValue 初始进度值
     * @return 配置好的主进度条
     */
    public static ProgressBar createMainProgressBar(double initialValue) {
        return ComponentFactory.createMainProgressBar(initialValue);
    }
    
    /**
     * 创建根路径进度条（固定宽度）
     * @param initialValue 初始进度值
     * @return 配置好的根路径进度条
     */
    public static ProgressBar createRootPathProgressBar(double initialValue) {
        return ComponentFactory.createRootPathProgressBar(initialValue);
    }
}
