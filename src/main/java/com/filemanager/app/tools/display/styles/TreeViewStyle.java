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

import javafx.scene.Node;
import javafx.scene.Scene;
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

        // 从CSS文件加载样式
        loadStyleFromCssFile(treeView, "/style/css/tree-view.css");
    }
    
    /**
     * 从CSS文件加载样式
     * @param node 组件节点
     * @param cssFilePath CSS文件路径
     */
    private static void loadStyleFromCssFile(Node node, String cssFilePath) {
        if (node == null || node.getScene() == null) {
            return;
        }
        
        // 获取CSS文件的URL
        java.net.URL cssUrl = TreeViewStyle.class.getResource(cssFilePath);
        if (cssUrl != null) {
            String cssUrlString = cssUrl.toExternalForm();
            Scene scene = node.getScene();
            
            // 检查样式表是否已经加载
            if (!scene.getStylesheets().contains(cssUrlString)) {
                scene.getStylesheets().add(cssUrlString);
            }
        }
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
        String scrollBarStyle = BaseStyleUtils.buildScrollBarStyle(theme);
        // 为所有选择器添加.tree-view前缀
        scrollBarStyle = scrollBarStyle.replaceAll("\\.virtual-flow", ".tree-view .virtual-flow");
        return scrollBarStyle;
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
