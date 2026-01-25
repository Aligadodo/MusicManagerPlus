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

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

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

        // 注册主题变更监听器
        ThemeManager.getInstance().addThemeChangeListener(newTheme -> {
            ComponentStyleManager.theme = newTheme;
        });
    }

    /**
     * 统一更新所有组件样式的入口方法
     *
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

        // 检查组件是否标记为主题独立
        if (isThemeIndependent(node)) {
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
        // 跳过MenuBarButton，因为它的style属性已经被绑定了
        if (labeled.getClass().getName().contains("MenuBarButton")) {
            // 只更新文本颜色，不设置完整样式
            String bgColor = extractBackgroundColor(labeled.getParent());
            String textColor = getContrastTextColor(bgColor);
            labeled.setTextFill(javafx.scene.paint.Color.web(textColor));
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

        // 提取背景颜色并计算对比文本颜色
        String bgColor = extractBackgroundColor(labeled.getParent());
        String textColor = getContrastTextColor(bgColor);

        // 设置更新后的样式，确保不会保留*号前缀
        String trimmedStyle = currentStyle.trim();
        if (trimmedStyle.startsWith("*")) {
            trimmedStyle = trimmedStyle.substring(1).trim();
        }
        labeled.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: '%s';",
                trimmedStyle, textColor, theme.getFontFamily()
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
        scrollPane.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;"
        );
    }

    /**
     * 应用TabPane样式
     */
    private static void applyTabPaneStyle(TabPane tabPane) {
        // 检测是否为二级TabPane（通过父容器或其他特征）
        boolean isSecondary = isSecondaryTabPane(tabPane);

        // 根据是否为二级TabPane设置不同的尺寸
        double tabHeight = isSecondary ? 45.0 : 50.0;
        double tabMinWidth = isSecondary ? 80.0 : 120.0;
        double tabMaxWidth = isSecondary ? 180.0 : 220.0;
        double fontSize = isSecondary ? 13.0 : 15.0;

        // 为TabPane相关颜色添加透明度
        String panelBgColor = theme.getPanelBgColor();
        String bgColor = theme.getBgColor();
        String panelHoverColor = theme.getPanelHoverColor();

        // 特殊处理transparent关键字，无论是否带有#前缀或被截断为transp
        if (panelBgColor != null && !"transparent".equalsIgnoreCase(panelBgColor) && !"#transparent".equalsIgnoreCase(panelBgColor) && !"transp".equalsIgnoreCase(panelBgColor) && !"#transp".equalsIgnoreCase(panelBgColor) && panelBgColor.startsWith("#")) {
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            panelBgColor = panelBgColor + alphaHex;
        }

        if (bgColor.startsWith("#")) {
            int alpha = (int) (theme.getGlassOpacity() * 255 * 0.8);
            String alphaHex = String.format("%02x", alpha);
            bgColor = bgColor + alphaHex;
        }

        if (panelHoverColor.startsWith("#")) {
            int alpha = (int) (theme.getGlassOpacity() * 255 * 0.9);
            String alphaHex = String.format("%02x", alpha);
            panelHoverColor = panelHoverColor + alphaHex;
        }

        // 拆分CSS样式为多个部分，提高可维护性

        // 1. 基础样式

        String styleBuilder = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: %.1f; -fx-tab-max-height: %.1f; -fx-tab-min-width: %.1f; -fx-tab-max-width: %.1f; -fx-text-fill: %s;\n",
                panelBgColor, theme.getBorderColor(), theme.getBorderWidth(),
                tabHeight, tabHeight, tabMinWidth, tabMaxWidth, theme.getTextPrimaryColor()
        ) +

                // 2. 表头区域样式
                ".tab-pane > .tab-header-area {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-padding: 0 10 0 10;\n" +
                "}\n" +

                // 3. 表头背景样式
                String.format(
                        ".tab-pane > .tab-header-area > .tab-header-background {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-border-color: %s;\n" +
                                "    -fx-border-width: 0 0 %.1f 0;\n" +
                                "}\n",
                        panelBgColor, theme.getBorderColor(), theme.getBorderWidth()
                ) +

                // 4. 标签基础样式
                String.format(
                        ".tab-pane > .tab-header-area > .headers-region > .tab {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-border-color: %s;\n" +
                                "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                                "    -fx-border-radius: %.1f %.1f 0 0;\n" +
                                "    -fx-cursor: hand;\n" +
                                "}\n",
                        bgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius()
                ) +

                // 5. 悬停标签样式
                String.format(
                        ".tab-pane > .tab-header-area > .headers-region > .tab:hover {\n" +
                                "    -fx-background-color: %s;\n" +
                                "}\n",
                        panelHoverColor
                ) +

                // 6. 选中标签样式
                String.format(
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-border-color: %s %s %s %s;\n" +
                                "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                                "}\n",
                        panelBgColor, theme.getAccentColor(), theme.getAccentColor(), theme.getPanelBgColor(), theme.getAccentColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth()
                ) +

                // 7. 标签文本样式
                String.format(
                        ".tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-label {\n" +
                                "    -fx-text-fill: %s;\n" +
                                "    -fx-font-family: '%s';\n" +
                                "    -fx-font-size: %.1fpx;\n" +
                                "    -fx-font-weight: normal;\n" +
                                "    -fx-alignment: center;\n" +
                                "    -fx-padding: 5 10;\n" +
                                "}\n",
                        theme.getTextSecondaryColor(), theme.getFontFamily(), fontSize
                ) +

                // 8. 悬停标签的文本样式
                String.format(
                        ".tab-pane > .tab-header-area > .headers-region > .tab:hover > .tab-container > .tab-label {\n" +
                                "    -fx-text-fill: %s;\n" +
                                "    -fx-font-weight: 500;\n" +
                                "}\n",
                        theme.getTextPrimaryColor()
                ) +

                // 9. 选中标签的文本样式
                String.format(
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected > .tab-container > .tab-label {\n" +
                                "    -fx-text-fill: %s;\n" +
                                "    -fx-font-weight: bold;\n" +
                                "    -fx-padding: 5 10;\n" +
                                "}\n",
                        theme.getAccentColor()
                ) +

                // 10. 内容区域样式
                String.format(
                        ".tab-pane > .tab-content-area {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-border-color: %s;\n" +
                                "    -fx-border-width: %.1f;\n" +
                                "    -fx-border-radius: 0 %.1f %.1f %.1f;\n" +
                                "}",
                        panelBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(), theme.getCornerRadius()
                );

        // 设置TabPane的样式
        tabPane.setStyle(styleBuilder);

        // 更新所有标签页的样式
        for (Tab tab : tabPane.getTabs()) {
            tab.setStyle(String.format(
                    "-fx-text-fill: %s; -fx-font-family: '%s'; -fx-font-size: %.1fpx;",
                    theme.getTextPrimaryColor(), theme.getFontFamily(), fontSize
            ));
        }
    }

    /**
     * 判断TabPane是否为二级TabPane
     *
     * @param tabPane TabPane对象
     * @return 是否为二级TabPane
     */
    private static boolean isSecondaryTabPane(TabPane tabPane) {
        // 检查父容器是否也是TabPane的内容
        Node parent = tabPane.getParent();
        if (parent != null) {
            // 检查父容器或祖父容器是否是TabContentArea或Tab的内容
            while (parent != null) {
                String className = parent.getClass().getName();
                if (className.contains("TabContentArea") || className.contains("TabPane") && parent != tabPane) {
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
                        "    -fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-font-weight: bold;\n" +
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
    public static void applyListViewStyle(ListView<?> listView) {
        if (listView == null || theme == null) {
            return;
        }
        com.filemanager.app.tools.display.styles.ListViewStyle.applyStyle(listView, theme);
    }

    /**
     * 应用TableView样式
     */
    public static void applyTableViewStyle(TableView<?> tableView) {
        if (tableView == null || theme == null) {
            return;
        }
        com.filemanager.app.tools.display.styles.TableViewStyle.applyStyle(tableView, theme);
    }

    /**
     * 应用TreeTableView样式
     */
    public static void applyTreeTableViewStyle(TreeTableView<?> treeTableView) {
        if (treeTableView == null || theme == null) {
            return;
        }
        com.filemanager.app.tools.display.styles.TreeTableViewStyle.applyStyle(treeTableView, theme);
    }

    /**
     * 应用TreeView样式
     */
    public static void applyTreeViewStyle(TreeView<?> treeView) {
        if (treeView == null || theme == null) {
            return;
        }
        com.filemanager.app.tools.display.styles.TreeViewStyle.applyStyle(treeView, theme);
    }

    /**
     * 应用TextArea样式
     */
    public static void applyTextAreaStyle(TextArea textArea) {
        if (textArea == null || theme == null) {
            return;
        }
        com.filemanager.app.tools.display.styles.TextAreaStyle.applyStyle(textArea, theme);
    }

    /**
     * 应用文本输入控件样式
     */
    public static void applyTextInputControlStyle(TextInputControl control) {
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
        if (currentStyle.contains("-fx-background-color:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-background-color:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-border-color:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-border-color:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-border-radius:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-border-radius:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-background-radius:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-background-radius:[^;]*;", "");
        }

        // 为面板背景添加透明度，实现玻璃效果
        String panelBgColor = theme.getPanelBgColor();
        // 特殊处理transparent关键字，无论是否带有#前缀或被截断为transp
        if (panelBgColor != null && !"transparent".equalsIgnoreCase(panelBgColor) && !"#transparent".equalsIgnoreCase(panelBgColor) && !"transp".equalsIgnoreCase(panelBgColor) && !"#transp".equalsIgnoreCase(panelBgColor) && panelBgColor.startsWith("#")) {
            // 将十六进制颜色转换为带透明度的RGBA颜色
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            panelBgColor = panelBgColor + alphaHex;
        }

        // 提取背景颜色并计算对比文本颜色
        String bgColor = extractBackgroundColor(control.getParent());
        String textColor = getContrastTextColor(bgColor);

        // 设置更新后的样式，确保不会保留*号前缀
        String trimmedStyle = currentStyle.trim();
        if (trimmedStyle.startsWith("*")) {
            trimmedStyle = trimmedStyle.substring(1).trim();
        }
        control.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: '%s'; -fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-background-radius: %.1f;",
                trimmedStyle, textColor, theme.getFontFamily(), panelBgColor, theme.getBorderColor(), theme.getCornerRadius(), theme.getCornerRadius()
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
        if (currentStyle.contains("-fx-border-radius:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-border-radius:[^;]*;", "");
        }
        if (currentStyle.contains("-fx-background-radius:")) {
            currentStyle = currentStyle.replaceAll(".*?-fx-background-radius:[^;]*;", "");
        }

        // 为面板背景添加透明度，实现玻璃效果
        String panelBgColor = theme.getPanelBgColor();
        // 特殊处理transparent关键字，无论是否带有#前缀或被截断为transp
        if (panelBgColor != null && !"transparent".equalsIgnoreCase(panelBgColor) && !"#transparent".equalsIgnoreCase(panelBgColor) && !"transp".equalsIgnoreCase(panelBgColor) && !"#transp".equalsIgnoreCase(panelBgColor) && panelBgColor.startsWith("#")) {
            // 将十六进制颜色转换为带透明度的RGBA颜色
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            panelBgColor = panelBgColor + alphaHex;
        }

        // 提取背景颜色并计算对比文本颜色
        String bgColor = extractBackgroundColor(comboBox.getParent());
        String textColor = getContrastTextColor(bgColor);

        // 设置更新后的样式，确保不会保留*号前缀
        String trimmedStyle = currentStyle.trim();
        if (trimmedStyle.startsWith("*")) {
            trimmedStyle = trimmedStyle.substring(1).trim();
        }
        comboBox.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: '%s'; -fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-background-radius: %.1f;",
                trimmedStyle, textColor, theme.getFontFamily(), panelBgColor, theme.getBorderColor(), theme.getCornerRadius(), theme.getCornerRadius()
        ));
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

        // 提取背景颜色并计算对比文本颜色
        String bgColor = extractBackgroundColor(checkBox.getParent());
        String textColor = getContrastTextColor(bgColor);

        // 设置更新后的样式，确保不会保留*号前缀
        String trimmedStyle = currentStyle.trim();
        if (trimmedStyle.startsWith("*")) {
            trimmedStyle = trimmedStyle.substring(1).trim();
        }
        checkBox.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: '%s';",
                trimmedStyle, textColor, theme.getFontFamily()
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

        // 提取背景颜色并计算对比文本颜色
        String bgColor = extractBackgroundColor(radioButton.getParent());
        String textColor = getContrastTextColor(bgColor);

        // 设置更新后的样式，确保不会保留*号前缀
        String trimmedStyle = currentStyle.trim();
        if (trimmedStyle.startsWith("*")) {
            trimmedStyle = trimmedStyle.substring(1).trim();
        }
        radioButton.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: '%s';",
                trimmedStyle, textColor, theme.getFontFamily()
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

        // 提取背景颜色并计算对比文本颜色
        String bgColor = extractBackgroundColor(toggleButton.getParent());
        String textColor = getContrastTextColor(bgColor);

        // 设置更新后的样式，确保不会保留*号前缀
        String trimmedStyle = currentStyle.trim();
        if (trimmedStyle.startsWith("*")) {
            trimmedStyle = trimmedStyle.substring(1).trim();
        }
        toggleButton.setStyle(String.format(
                "%s -fx-text-fill: %s; -fx-font-family: '%s';",
                trimmedStyle, textColor, theme.getFontFamily()
        ));
    }

    /**
     * 提取父容器的背景颜色
     */
    private static String extractBackgroundColor(Node node) {
        if (node == null) {
            return theme.getBgColor(); // 默认返回主题背景色
        }

        String style = node.getStyle();
        if (style.contains("-fx-background-color:")) {
            // 提取背景颜色值
            int startIndex = style.indexOf("-fx-background-color:") + "-fx-background-color:".length();
            int endIndex = style.indexOf(";" , startIndex);
            if (endIndex > startIndex) {
                return style.substring(startIndex, endIndex).trim();
            }
        }

        // 递归向上查找
        return extractBackgroundColor(node.getParent());
    }

    /**
     * 根据背景颜色计算对比文本颜色
     */
    private static String getContrastTextColor(String bgColor) {
        // 默认返回主题主要文本颜色
        if (bgColor == null || bgColor.isEmpty()) {
            return theme.getTextPrimaryColor();
        }

        // 处理透明背景情况
        if (bgColor.equals("transparent")) {
            return theme.getTextPrimaryColor();
        }

        try {
            // 解析颜色
            Color color = Color.web(bgColor);

            // 计算亮度
            double brightness = color.getBrightness();

            // 根据亮度返回对比文本颜色
            return brightness > 0.5 ? theme.getTextPrimaryColor() : theme.getTextPrimaryColor();
        } catch (Exception e) {
            // 如果颜色解析失败，返回默认文本颜色
            return theme.getTextPrimaryColor();
        }
    }

    /**
     * 检查组件是否标记为主题独立
     */
    private static boolean isThemeIndependent(Node node) {
        String style = node.getStyle();
        return style != null && style.contains("-fx-theme-independent: true");
    }

    /**
     * 为单个组件应用主题样式（外部调用入口）
     */
    public static void applyThemeStyle(Node node) {
        if (node == null || theme == null) {
            return;
        }

        // 根据组件类型应用不同的样式
        if (node instanceof Button) {
            applyLabeledStyle((Button) node);
        } else if (node instanceof Label) {
            applyLabeledStyle((Label) node);
        } else if (node instanceof TextField) {
            applyTextInputControlStyle((TextField) node);
        } else if (node instanceof PasswordField) {
            applyTextInputControlStyle((PasswordField) node);
        } else if (node instanceof ComboBox) {
            applyComboBoxStyle((ComboBox<?>) node);
        } else if (node instanceof CheckBox) {
            applyCheckBoxStyle((CheckBox) node);
        } else if (node instanceof RadioButton) {
            applyRadioButtonStyle((RadioButton) node);
        } else if (node instanceof ToggleButton) {
            applyToggleButtonStyle((ToggleButton) node);
        } else if (node instanceof VBox) {
            applyVBoxStyle((VBox) node);
        } else if (node instanceof HBox) {
            applyHBoxStyle((HBox) node);
        } else if (node instanceof BorderPane) {
            applyBorderPaneStyle((BorderPane) node);
        } else if (node instanceof GridPane) {
            applyGridPaneStyle((GridPane) node);
        } else if (node instanceof StackPane) {
            applyStackPaneStyle((StackPane) node);
        } else if (node instanceof ScrollPane) {
            applyScrollPaneStyle((ScrollPane) node);
        } else if (node instanceof TabPane) {
            applyTabPaneStyle((TabPane) node);
        } else if (node instanceof TitledPane) {
            applyTitledPaneStyle((TitledPane) node);
        } else if (node instanceof Separator) {
            applySeparatorStyle((Separator) node);
        } else if (node instanceof ProgressBar) {
            applyProgressBarStyle((ProgressBar) node);
        } else if (node instanceof ListView) {
            applyListViewStyle((ListView<?>) node);
        } else if (node instanceof TableView) {
            applyTableViewStyle((TableView<?>) node);
        } else if (node instanceof TreeTableView) {
            applyTreeTableViewStyle((TreeTableView<?>) node);
        } else if (node instanceof TreeView) {
            applyTreeViewStyle((TreeView<?>) node);
        } else if (node instanceof TextArea) {
            applyTextAreaStyle((TextArea) node);
        }
    }

    /**
     * 获取当前主题配置
     */
    public static ThemeConfig getTheme() {
        return theme;
    }

    /**
     * 设置面板的基本样式
     */
    public static void setBasicStyle(Region region) {
        if (region == null || theme == null) {
            return;
        }
        region.setStyle(String.format(
                "-fx-background-color: transparent; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));
    }

    /**
     * 设置菜单和菜单项的样式
     */
    public static void setMenuStyle(MenuBar menuBar) {
        if (menuBar == null || theme == null) {
            return;
        }
        menuBar.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 0 %.1f 0; -fx-text-fill: %s;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getTextPrimaryColor()
        ));
    }

    /**
     * 更新树节点样式
     */
    public static void updateTreeItemStyle(Node node, boolean selected) {
        if (node == null || theme == null) {
            return;
        }
        if (selected) {
            node.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s;",
                    theme.getListRowSelectedBgColor(), theme.getListRowSelectedTextColor()
            ));
        } else {
            node.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-text-fill: %s;",
                    theme.getTextPrimaryColor()
            ));
        }
    }

    /**
     * 手动触发主题刷新（当主题配置变更时调用）
     */
    public static void refreshTheme() {
        // 主题配置已在 ThemeManager 的监听器中更新
        // 此方法留作手动触发刷新的入口
    }
}
