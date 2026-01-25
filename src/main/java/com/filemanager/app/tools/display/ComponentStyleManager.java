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

import javafx.geometry.Insets;
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
        // 使用ThemeConfig的透明度计算方法
        String listBgColor = theme.getListBgColorWithOpacity();
        String selectedBgColor = theme.getListRowSelectedBgColorWithOpacity(0.6); // 60%透明度
        String hoverBgColor = theme.getListRowHoverBgColorWithOpacity(0.5); // 50%透明度

        // 拆分CSS样式为多个部分，提高可维护性

        // 1. 基础样式

        String styleBuilder = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n",
                listBgColor, theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius()
        ) +

                // 2. 单元格基础样式
                String.format(
                        ".list-view .list-cell {\n" +
                                "    -fx-background-color: transparent;\n" +
                                "    -fx-text-fill: %s;\n" +
                                "    -fx-font-family: '%s';\n" +
                                "    -fx-padding: 8 10;\n" +
                                "}\n",
                        theme.getTextPrimaryColor(), theme.getFontFamily()
                ) +

                // 3. 选中单元格样式
                String.format(
                        ".list-view .list-cell:filled:selected {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-text-fill: %s;\n" +
                                "    -fx-border-color: %s;\n" +
                                "    -fx-border-width: 2;\n" +
                                "    -fx-border-radius: %.1f;\n" +
                                "}\n",
                        selectedBgColor, theme.getListRowSelectedTextColor(), theme.getBorderColor(), theme.getCornerRadius()
                ) +

                // 4. 悬停单元格样式
                String.format(
                        ".list-view .list-cell:filled:hover {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-text-fill: %s;\n" +
                                "}\n",
                        theme.getListRowHoverBgColor(), theme.getTextPrimaryColor()
                ) +

                // 5. 滚动条样式
                String.format(
                        ".list-view .virtual-flow .scroll-bar:vertical,\n" +
                                ".list-view .virtual-flow .scroll-bar:horizontal {\n" +
                                "    -fx-background-color: transparent;\n" +
                                "}\n" +
                                ".list-view .virtual-flow .scroll-bar .thumb {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-background-radius: 4;\n" +
                                "}\n" +
                                ".list-view .virtual-flow .scroll-bar .track {\n" +
                                "    -fx-background-color: transparent;\n" +
                                "}",
                        theme.getTextTertiaryColor()
                );

        // 设置ListView的样式
        listView.setStyle(styleBuilder);
    }

    /**
     * 应用TableView样式
     */
    public static void applyTableViewStyle(TableView<?> tableView) {
        // 使用ThemeConfig的透明度计算方法
        String listBgColor = theme.getListBgColorWithOpacity();
        String headerBgColor = theme.getTableHeaderBgColorWithOpacity(); // 特殊处理：使用比毛玻璃透明度高0.1的透明度
        String selectedBgColor = theme.getListRowSelectedBgColorWithOpacity(0.8); // 80%透明度
        String hoverBgColor = theme.getListRowHoverBgColorWithOpacity(0.6); // 60%透明度

        // 确保数值类型正确，避免格式化错误
        double borderWidth = theme.getBorderWidth();
        double cornerRadius = theme.getCornerRadius();

        // 拆分CSS样式为多个部分，提高可维护性

        // 1. 基础样式

        String styleBuilder = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n",
                listBgColor, theme.getListBorderColor(), borderWidth, cornerRadius, cornerRadius
        ) +

                // 2. 表头背景样式
                String.format(
                        ".table-view .column-header-background {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-border-color: %s;\n" +
                                "    -fx-border-width: 0 0 %.1f 0;\n" +
                                "    -fx-background-radius: %.1f %.1f 0 0;\n" +
                                "}\n",
                        headerBgColor, theme.getBorderColor(), borderWidth, cornerRadius, cornerRadius
                ) +

                // 3. 表头填充样式
                ".table-view .column-header-background .filler {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +

                // 4. 列头样式
                String.format(
                        ".table-view .column-header {\n" +
                                "    -fx-background-color: transparent;\n" +
                                "    -fx-border-color: %s;\n" +
                                "    -fx-border-width: 0 %.1f 0 0;\n" +
                                "}\n",
                        theme.getBorderColor(), borderWidth
                ) +

                // 5. 列头标签样式
                String.format(
                        ".table-view .column-header .label {\n" +
                                "    -fx-text-fill: %s;\n" +
                                "    -fx-font-family: '%s';\n" +
                                "    -fx-font-weight: bold;\n" +
                                "    -fx-padding: 12 10;\n" +
                                "}\n",
                        theme.getTextPrimaryColor(), theme.getFontFamily()
                ) +

                // 6. 行单元格样式
                ".table-view .table-row-cell {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-border-color: transparent;\n" +
                "    -fx-border-width: 1 0 0 0;\n" +
                "}\n" +
                ".table-view .table-row-cell:filled {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +

                // 7. 选中行样式
                String.format(
                        ".table-view .table-row-cell:filled:selected {\n" +
                                "    -fx-background-color: %s;\n" +
                                "}\n",
                        selectedBgColor
                ) +

                // 8. 悬停行样式
                String.format(
                        ".table-view .table-row-cell:filled:hover {\n" +
                                "    -fx-background-color: %s;\n" +
                                "}\n",
                        hoverBgColor
                ) +

                // 9. 单元格样式
                String.format(
                        ".table-view .table-cell {\n" +
                                "    -fx-text-fill: %s;\n" +
                                "    -fx-font-family: '%s';\n" +
                                "    -fx-padding: 10 10;\n" +
                                "    -fx-border-color: transparent;\n" +
                                "}\n",
                        theme.getTextPrimaryColor(), theme.getFontFamily()
                ) +

                // 10. 滚动条样式
                String.format(
                        ".table-view .virtual-flow .scroll-bar:vertical,\n" +
                                ".table-view .virtual-flow .scroll-bar:horizontal {\n" +
                                "    -fx-background-color: transparent;\n" +
                                "}\n" +
                                ".table-view .virtual-flow .scroll-bar .thumb {\n" +
                                "    -fx-background-color: %s;\n" +
                                "    -fx-background-radius: 4;\n" +
                                "}\n" +
                                ".table-view .virtual-flow .scroll-bar .track {\n" +
                                "    -fx-background-color: transparent;\n" +
                                "}",
                        theme.getTextTertiaryColor()
                );

        // 设置TableView的样式
        tableView.setStyle(styleBuilder);
    }

    /**
     * 应用TreeTableView样式
     */
    public static void applyTreeTableViewStyle(TreeTableView<?> treeTableView) {
        // 使用ThemeConfig的透明度计算方法
        String listBgColor = theme.getListBgColorWithOpacity();
        String headerBgColor = theme.getTableHeaderBgColorWithOpacity(); // 特殊处理：使用比毛玻璃透明度高0.1的透明度
        String selectedBgColor = theme.getListRowSelectedBgColorWithOpacity(0.8); // 80%透明度
        String hoverBgColor = theme.getListRowHoverBgColorWithOpacity(0.6); // 60%透明度

        // 确保数值类型正确，避免格式化错误
        double borderWidth = theme.getBorderWidth();
        double cornerRadius = theme.getCornerRadius();

        // 设置TreeTableView的完整样式，包括背景、边框、表头、单元格等
        treeTableView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n" +
                        ".tree-table-view .column-header-background {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 0 0 %.1f 0;\n" +
                        "    -fx-background-radius: %.1f %.1f 0 0;\n" +
                        "}\n" +
                        ".tree-table-view .column-header-background .filler {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .column-header {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 0 %.1f 0 0;\n" +
                        "}\n" +
                        ".tree-table-view .column-header .label {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: '%s';\n" +
                        "    -fx-font-weight: bold;\n" +
                        "    -fx-padding: 12 10;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: transparent;\n" +
                        "    -fx-border-width: 1 0 0 0;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:filled {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:filled:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-table-cell-border-color: transparent;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 2;\n" +
                        "    -fx-border-radius: %.1f;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:filled:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-table-cell-border-color: transparent;\n" +
                        "    -fx-border-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:focused {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-table-cell-border-color: transparent;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 2;\n" +
                        "    -fx-border-radius: %.1f;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:focused:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-table-cell-border-color: transparent;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 2;\n" +
                        "    -fx-border-radius: %.1f;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-cell {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: '%s';\n" +
                        "    -fx-padding: 10 10;\n" +
                        "    -fx-border-color: transparent;\n" +
                        "    -fx-alignment: center-left;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-cell:selected {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-cell:focused {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .virtual-flow .scroll-bar:vertical,\n" +
                        ".tree-table-view .virtual-flow .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .virtual-flow .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".tree-table-view .virtual-flow .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .virtual-flow .scroll-bar .increment-button,\n" +
                        ".tree-table-view .virtual-flow .scroll-bar .decrement-button {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-shape: none;\n" +
                        "    -fx-padding: 0;\n" +
                        "}",
                listBgColor, theme.getListBorderColor(), borderWidth, cornerRadius, cornerRadius,
                headerBgColor, theme.getBorderColor(), borderWidth, cornerRadius,
                theme.getBorderColor(), borderWidth,
                theme.getTextPrimaryColor(), theme.getFontFamily(),
                selectedBgColor, theme.getBorderColor(), cornerRadius,
                hoverBgColor,
                selectedBgColor, theme.getBorderColor(), cornerRadius,
                selectedBgColor, theme.getBorderColor(), cornerRadius,
                theme.getTextPrimaryColor(), theme.getFontFamily(),
                theme.getTextPrimaryColor(),
                theme.getTextPrimaryColor(),
                theme.getTextTertiaryColor()
        ));
    }

    /**
     * 应用TreeView样式
     */
    public static void applyTreeViewStyle(TreeView<?> treeView) {
        // 为列表背景添加透明度，实现玻璃效果
        String listBgColor = theme.getListBgColor();

        if (listBgColor.startsWith("#")) {
            // 将十六进制颜色转换为带透明度的RGBA颜色
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            listBgColor = listBgColor + alphaHex;
        }

        // 设置TreeView的完整样式，包括背景、边框、树节点等
        treeView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n" +
                        ".tree-view .tree-cell {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: '%s';\n" +
                        "    -fx-padding: 6 10;\n" +
                        "}\n" +
                        ".tree-view .tree-cell:filled:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "}\n" +
                        ".tree-view .tree-cell:filled:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "}\n" +
                        ".tree-view .virtual-flow .scroll-bar:vertical,\n" +
                        ".tree-view .virtual-flow .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-view .virtual-flow .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".tree-view .virtual-flow .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-view .check-box {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "}\n" +
                        ".tree-view .check-box .box {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: %.1f;\n" +
                        "    -fx-border-radius: 3;\n" +
                        "}\n" +
                        ".tree-view .check-box:selected .mark {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}",
                listBgColor, theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getTextPrimaryColor(), theme.getFontFamily(),
                theme.getListRowSelectedBgColor(), theme.getListRowSelectedTextColor(),
                theme.getListRowHoverBgColor(), theme.getTextPrimaryColor(),
                theme.getTextTertiaryColor(),
                theme.getTextPrimaryColor(),
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                theme.getAccentColor()
        ));
    }

    /**
     * 应用TextArea样式
     */
    public static void applyTextAreaStyle(TextArea textArea) {
        // 设置TextArea的背景色、边框和字体，与createTextArea保持一致
        textArea.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                        "-fx-text-fill: %s; -fx-font-family: '%s'; -fx-font-size: %.1f;\n" +
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
                        "}\n" +
                        ".text-area .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".text-area .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".text-area .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}",
                theme.getListBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getFontSize(),
                theme.getTextTertiaryColor()
        ));
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

    // ==================== 颜色对比度计算工具方法 ====================

    /**
     * 根据背景颜色自动计算合适的文本颜色
     * @param backgroundColor 背景颜色字符串（支持#RRGGBB或#RRGGBBAA格式）
     * @return 适合的文本颜色字符串
     */
    public static String getContrastTextColor(String backgroundColor) {
        if (backgroundColor == null || backgroundColor.isEmpty() || "transparent".equalsIgnoreCase(backgroundColor)) {
            return theme.getTextPrimaryColor();
        }

        try {
            Color bgColor = Color.web(backgroundColor);
            double brightness = calculateColorBrightness(bgColor);
            // 根据亮度返回对比色：亮背景用深色文本，暗背景用浅色文本
            return brightness > 0.5 ? theme.getTextPrimaryColor() : "#ffffff";
        } catch (Exception e) {
            // 如果颜色解析失败，返回默认文本颜色
            return theme.getTextPrimaryColor();
        }
    }

    /**
     * 计算颜色的亮度（0-1，值越大越亮）
     * @param color 颜色对象
     * @return 亮度值
     */
    private static double calculateColorBrightness(Color color) {
        // 使用相对 luminance 公式计算亮度
        // 参考：https://www.w3.org/WAI/GL/wiki/Relative_luminance
        double r = color.getRed();
        double g = color.getGreen();
        double b = color.getBlue();

        // 线性化 RGB 值
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        // 计算相对亮度
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    /**
     * 从组件样式中提取背景颜色
     * @param node 组件节点
     * @return 背景颜色字符串
     */
    private static String extractBackgroundColor(Node node) {
        if (node == null) {
            return null;
        }

        String style = node.getStyle();
        if (style == null || style.isEmpty()) {
            return null;
        }

        // 从样式字符串中提取背景颜色
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("-fx-background-color:\\s*([^;]+);");
        java.util.regex.Matcher matcher = pattern.matcher(style);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 检查组件是否标记为主题独立
     * @param node 组件节点
     * @return 是否为主题独立
     */
    private static boolean isThemeIndependent(Node node) {
        if (node == null) {
            return false;
        }

        // 检查组件的样式是否包含主题独立标记
        String style = node.getStyle();
        return style != null && style.contains("-fx-theme-independent: true");
    }

    /**
     * 为组件添加主题独立标记
     * @param node 组件节点
     */
    public static void markAsThemeIndependent(Node node) {
        if (node == null) {
            return;
        }

        String currentStyle = node.getStyle();
        if (!currentStyle.contains("-fx-theme-independent: true")) {
            node.setStyle(currentStyle + " -fx-theme-independent: true;");
        }
    }

    /**
     * 设置面板的基本样式
     * @param region 区域组件
     */
    public static void setBasicStyle(Region region) {
        if (region == null || theme == null) {
            return;
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

        // 设置面板的基本样式
        region.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;",
                panelBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius()
        ));
    }

    /**
     * 设置菜单和菜单项的样式
     * @param menuBar 菜单栏组件
     */
    public static void setMenuStyle(MenuBar menuBar) {
        if (menuBar == null || theme == null) {
            return;
        }

        // 设置菜单栏的基本样式
        menuBar.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 0 %.1f 0;",
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));

        // 递归更新所有菜单项的样式
        for (Menu menu : menuBar.getMenus()) {
            updateMenuItemStyle(menu);
        }
    }

    /**
     * 更新菜单项的样式
     * @param menuItem 菜单项组件
     */
    private static void updateMenuItemStyle(MenuItem menuItem) {
        if (menuItem == null || theme == null) {
            return;
        }

        // 设置菜单项的样式
        menuItem.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-family: '%s';",
                theme.getTextPrimaryColor(), theme.getFontFamily()
        ));

        // 如果是子菜单，递归更新
        if (menuItem instanceof Menu) {
            Menu menu = (Menu) menuItem;
            for (MenuItem subMenuItem : menu.getItems()) {
                updateMenuItemStyle(subMenuItem);
            }
        }
    }

    /**
     * 更新树节点的样式
     * @param node 节点组件
     * @param selected 是否选中
     */
    public static void updateTreeItemStyle(Node node, boolean selected) {
        if (node == null || theme == null) {
            return;
        }

        // 提取背景颜色并计算对比文本颜色
        String bgColor = extractBackgroundColor(node);
        String textColor = getContrastTextColor(bgColor);

        // 根据选中状态设置不同的样式
        if (selected) {
            node.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: '%s';",
                    theme.getListRowSelectedBgColor(), theme.getListRowSelectedTextColor(), theme.getFontFamily()
            ));
        } else {
            node.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-family: '%s';",
                    textColor, theme.getFontFamily()
            ));
        }
    }
}