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

import com.filemanager.app.tools.display.ThemeConfig;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

/**
 * UI 组件样式管理器
 * 负责统一管理和应用界面元素的样式
 *
 * @author hrcao
 */
public class ComponentStyleManager {

    private static ThemeConfig theme = null;

    public static void initComponentStyleManager(ThemeConfig theme) {
        ComponentStyleManager.theme = theme;
    }

    /**
     * 统一更新所有组件样式的入口方法
     * @param node 需要更新样式的节点
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
        
        // 处理文本输入控件
        if (node instanceof TextInputControl) {
            applyTextInputControlStyle((TextInputControl) node);
        }
        
        // 处理组合框
        if (node instanceof ComboBox) {
            applyComboBoxStyle((ComboBox<?>) node);
        }
        
        // 处理复选框
        if (node instanceof CheckBox) {
            applyCheckBoxStyle((CheckBox) node);
        }
        
        // 处理单选按钮
        if (node instanceof RadioButton) {
            applyRadioButtonStyle((RadioButton) node);
        }
        
        // 处理切换按钮
        if (node instanceof ToggleButton) {
            applyToggleButtonStyle((ToggleButton) node);
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
        // 跳过MenuBarButton，因为它的style属性已经被绑定了
        if (labeled.getClass().getName().contains("MenuBarButton")) {
            // 只更新文本颜色，不设置完整样式
            labeled.setTextFill(javafx.scene.paint.Color.web(theme.getTextPrimaryColor()));
            labeled.setFont(javafx.scene.text.Font.font(theme.getFontFamily(), labeled.getFont().getSize()));
            return;
        }
        
        // 更新所有Labeled组件的文本颜色和字体，确保与主题一致
        String currentStyle = labeled.getStyle();
        
        // 如果当前样式已包含文本颜色或字体设置，移除它们
        if (currentStyle.contains("-fx-text-fill:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-text-fill:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-font-family:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-font-family:[^;]*;", "");
        }
        
        labeled.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: %s;",
                currentStyle.trim(), theme.getTextPrimaryColor(), theme.getFontFamily()
        ));
    }

    /**
     * 应用VBox样式
     */
    private static void applyVBoxStyle(VBox vbox) {
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
        // 检测是否为二级TabPane（通过父容器或其他特征）
        boolean isSecondary = isSecondaryTabPane(tabPane);
        
        // 根据是否为二级TabPane设置不同的尺寸
        double tabHeight = isSecondary ? 35.0 : 45.0;
        double tabMinWidth = isSecondary ? 80.0 : 120.0;
        double tabMaxWidth = isSecondary ? 180.0 : 220.0;
        double fontSize = isSecondary ? 13.0 : 15.0;
        
        tabPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: %.1f; -fx-tab-max-height: %.1f; -fx-tab-min-width: %.1f; -fx-tab-max-width: %.1f; -fx-text-fill: %s;\n" +
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
                "    -fx-font-size: %.1fpx;\n" +
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
                tabHeight, tabHeight, tabMinWidth, tabMaxWidth, theme.getTextPrimaryColor(),
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                theme.getBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getPanelHoverColor(),
                theme.getPanelBgColor(), theme.getAccentColor(), theme.getAccentColor(), theme.getPanelBgColor(), theme.getAccentColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(),
                theme.getTextSecondaryColor(), theme.getFontFamily(), fontSize,
                theme.getTextPrimaryColor(),
                theme.getAccentColor(),
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(), theme.getCornerRadius()
        ));
        
        // 更新所有标签页的样式
        for (Tab tab : tabPane.getTabs()) {
            tab.setStyle(String.format(
                    "-fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: %.1fpx;",
                    theme.getTextPrimaryColor(), theme.getFontFamily(), fontSize
            ));
        }
    }
    
    /**
     * 判断TabPane是否为二级TabPane
     * @param tabPane TabPane对象
     * @return 是否为二级TabPane
     */
    private static boolean isSecondaryTabPane(TabPane tabPane) {
        // 检查父容器是否也是TabPane的内容
        Node parent = tabPane.getParent();
        if (parent != null) {
            // 检查父容器或祖父容器是否是TabContentArea或Tab的内容
            while (parent != null) {
                if (parent.getClass().getName().contains("TabContentArea")) {
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    /**
     * 应用TitledPane样式
     */
    private static void applyTitledPaneStyle(TitledPane titledPane) {
        titledPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                ".titled-pane > .title {\n" +
                "    -fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: %.1fpx; -fx-font-weight: bold;\n" +
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
                theme.getPanelBgColor(), theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getFontSize(), theme.getBorderWidth(), theme.getBorderColor(),
                theme.getTextPrimaryColor(),
                theme.getPanelBgColor(), theme.getCornerRadius(), theme.getCornerRadius()
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
                theme.getListBgColor(), theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
    }

    /**
     * 应用TableView样式
     */
    private static void applyTableViewStyle(TableView<?> tableView) {
        // 设置TableView的背景色和边框
        tableView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f;",
                theme.getListBgColor(), theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
    }

    /**
     * 应用TreeTableView样式
     */
    private static void applyTreeTableViewStyle(TreeTableView<?> treeTableView) {
        // 设置TreeTableView的背景色和边框
        treeTableView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f;",
                theme.getListBgColor(), theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
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
        textArea.setPadding(new Insets(10,0,0,0));
    }
    
    /**
     * 应用文本输入控件样式
     */
    private static void applyTextInputControlStyle(TextInputControl control) {
        // 如果是TextArea，已经有专门的样式处理方法，跳过
        if (control instanceof TextArea) {
            return;
        }
        
        String currentStyle = control.getStyle();
        
        // 如果当前样式已包含文本颜色或字体设置，移除它们
        if (currentStyle.contains("-fx-text-fill:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-text-fill:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-font-family:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-font-family:[^;]*;", "");
        }
        
        control.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: %s; -fx-background-color: %s; -fx-border-color: %s;",
                currentStyle.trim(), theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getPanelBgColor(), theme.getBorderColor()
        ));
    }
    
    /**
     * 应用ComboBox样式
     */
    private static void applyComboBoxStyle(ComboBox<?> comboBox) {
        String currentStyle = comboBox.getStyle();
        
        // 移除现有样式
        if (currentStyle.contains("-fx-text-fill:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-text-fill:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-font-family:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-font-family:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-background-color:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-background-color:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-border-color:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-border-color:[^;]*;", "");
        }
        
        comboBox.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: %s; -fx-background-color: %s; -fx-border-color: %s;",
                currentStyle.trim(), theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getPanelBgColor(), theme.getBorderColor()
        ));
        
        // 更新下拉列表样式
        ContextMenu contextMenu = comboBox.getContextMenu();
        if (contextMenu != null) {
            contextMenu.setStyle(
                    "-fx-background-color: " + theme.getPanelBgColor() + "; " +
                    "-fx-border-color: " + theme.getBorderColor() + ";"
            );
        }
    }
    
    /**
     * 应用CheckBox样式
     */
    private static void applyCheckBoxStyle(CheckBox checkBox) {
        String currentStyle = checkBox.getStyle();
        
        // 移除现有样式
        if (currentStyle.contains("-fx-text-fill:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-text-fill:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-font-family:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-font-family:[^;]*;", "");
        }
        
        checkBox.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: %s;",
                currentStyle.trim(), theme.getTextPrimaryColor(), theme.getFontFamily()
        ));
    }
    
    /**
     * 应用RadioButton样式
     */
    private static void applyRadioButtonStyle(RadioButton radioButton) {
        String currentStyle = radioButton.getStyle();
        
        // 移除现有样式
        if (currentStyle.contains("-fx-text-fill:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-text-fill:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-font-family:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-font-family:[^;]*;", "");
        }
        
        radioButton.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: %s;",
                currentStyle.trim(), theme.getTextPrimaryColor(), theme.getFontFamily()
        ));
    }
    
    /**
     * 应用ToggleButton样式
     */
    private static void applyToggleButtonStyle(ToggleButton toggleButton) {
        String currentStyle = toggleButton.getStyle();
        
        // 移除现有样式
        if (currentStyle.contains("-fx-text-fill:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-text-fill:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-font-family:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-font-family:[^;]*;", "");
        }
        
        toggleButton.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: %s;",
                currentStyle.trim(), theme.getTextPrimaryColor(), theme.getFontFamily()
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
                theme.getPanelBgColor(), theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
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
                "-fx-text-fill: " + theme.getTextPrimaryColor() + ";"
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
    private static void setMenuItemStyle(MenuItem item) {
        if (item == null || theme == null) {
            return;
        }
        
        // 设置菜单项样式
        item.setStyle(
                "-fx-text-fill: " + theme.getTextPrimaryColor() + "; " + 
                "-fx-font-family: " + theme.getFontFamily() + "; " +
                "-fx-font-size: 14px;"
        );
        
        // 确保菜单的label也应用正确的文本颜色
        if (item instanceof Menu) {
            Menu menu = (Menu) item;
            if (menu.getGraphic() instanceof Label) {
                Label label = (Label) menu.getGraphic();
                label.setTextFill(javafx.scene.paint.Color.web(theme.getTextPrimaryColor()));
            }
            // 递归设置子菜单项的样式
            for (MenuItem subItem : menu.getItems()) {
                setMenuItemStyle(subItem);
            }
        }
        
        // 为上下文菜单设置样式
        ContextMenu contextMenu = item.getParentPopup();
        if (contextMenu != null) {
            contextMenu.setStyle(
                    "-fx-background-color: " + theme.getPanelBgColor() + "; " +
                    "-fx-border-color: " + theme.getBorderColor() + "; " +
                    "-fx-border-width: " + theme.getBorderWidth() + "; " +
                    "-fx-border-radius: " + theme.getCornerRadius() + ";"
            );
        }
    }
    
    /**
     * 更新树节点样式
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