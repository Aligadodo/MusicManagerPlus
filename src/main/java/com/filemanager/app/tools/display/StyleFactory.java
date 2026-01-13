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
        
        // 验证颜色格式
        if (bgColor != null && !bgColor.startsWith("#")) {
            bgColor = "#" + bgColor;
        }
        if (textColor != null && !textColor.startsWith("#")) {
            textColor = "#" + textColor;
        }
        if (borderColor != null && !borderColor.startsWith("#")) {
            borderColor = "#" + borderColor;
        }
        if (hoverColor != null && !hoverColor.startsWith("#")) {
            hoverColor = "#" + hoverColor;
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
            // 应用面板样式
            vbox.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-spacing: %.1f;",
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), vbox.getSpacing()
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
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), hbox.getSpacing()
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
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
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
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
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
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
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
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getTextPrimaryColor()
        ));
        
        // 更新所有标签页的样式
        for (Tab tab : tabPane.getTabs()) {
            tab.setStyle(String.format(
                    "-fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 14px;",
                    theme.getTextPrimaryColor(), theme.getFontFamily()
            ));
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
        // 设置TextArea的背景色、边框和字体，与createTextArea保持一致
        textArea.setStyle(String.format(
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
        // 确保文本区域有内边距
        textArea.setPadding(new Insets(10));
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
