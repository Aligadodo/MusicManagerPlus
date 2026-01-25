/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-25
 */
package com.filemanager.app.tools.display.styles;

import com.filemanager.app.tools.display.ThemeConfig;
import javafx.scene.control.TreeView;

/**
 * TreeView 样式管理类
 * 负责管理和应用 TreeView 及其子组件的样式
 *
 * @author hrcao
 */
public class TreeViewStyle {

    /**
     * 应用 TreeView 的完整样式
     *
     * @param treeView TreeView 组件
     * @param theme    主题配置
     */
    public static void applyStyle(TreeView<?> treeView, ThemeConfig theme) {
        if (treeView == null || theme == null) {
            return;
        }

        // 构建各个模块的样式
        String baseStyle = buildBaseStyle(theme);
        String cellStyle = buildCellStyle(theme);
        String scrollBarStyle = buildScrollBarStyle(theme);
        String checkBoxStyle = buildCheckBoxStyle(theme);

        // 拼接所有样式
        String completeStyle = String.join("\n", baseStyle, cellStyle, scrollBarStyle, checkBoxStyle);

        // 应用样式
        treeView.setStyle(completeStyle);
    }

    /**
     * 构建 TreeView 的基础样式
     *
     * @param theme 主题配置
     * @return 基础样式字符串
     */
    private static String buildBaseStyle(ThemeConfig theme) {
        String listBgColor = theme.getListBgColorWithOpacity();
        return BaseStyleUtils.buildBaseStyle(
                theme,
                listBgColor,
                theme.getListBorderColor(),
                theme.getBorderWidth(),
                theme.getCornerRadius()
        );
    }

    /**
     * 构建树节点样式
     *
     * @param theme 主题配置
     * @return 树节点样式字符串
     */
    private static String buildCellStyle(ThemeConfig theme) {
        return String.format(
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
                "}",
                theme.getTextPrimaryColor(), theme.getFontFamily(),
                theme.getListRowSelectedBgColor(), theme.getListRowSelectedTextColor(),
                theme.getListRowHoverBgColor(), theme.getTextPrimaryColor()
        );
    }

    /**
     * 构建滚动条样式
     *
     * @param theme 主题配置
     * @return 滚动条样式字符串
     */
    private static String buildScrollBarStyle(ThemeConfig theme) {
        return ".tree-view " + BaseStyleUtils.buildScrollBarStyle(theme);
    }

    /**
     * 构建复选框样式
     *
     * @param theme 主题配置
     * @return 复选框样式字符串
     */
    private static String buildCheckBoxStyle(ThemeConfig theme) {
        return String.format(
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
                theme.getTextPrimaryColor(),
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                theme.getAccentColor()
        );
    }
}
