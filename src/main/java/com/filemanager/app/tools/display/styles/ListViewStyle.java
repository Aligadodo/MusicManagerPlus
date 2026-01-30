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
import javafx.scene.control.ListView;

/**
 * ListView 样式管理类
 * 负责管理和应用 ListView 及其子组件的样式
 *
 * @author hrcao
 */
public class ListViewStyle {

    /**
     * 应用 ListView 的完整样式
     *
     * @param listView ListView 组件
     * @param theme    主题配置
     */
    public static void applyStyle(ListView<?> listView, ThemeConfig theme) {
        if (listView == null || theme == null) {
            return;
        }

        // 从CSS文件加载样式
        loadStyleFromCssFile(listView, "/style/css/list-view.css");
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
        java.net.URL cssUrl = ListViewStyle.class.getResource(cssFilePath);
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
     * 构建 ListView 的基础样式
     *
     * @param theme 主题配置
     * @return 基础样式字符串
     */
    private static String buildBaseStyle(ThemeConfig theme) {
        // 使用透明背景
        String listBgColor = "transparent";
        return BaseStyleUtils.buildBaseStyle(
                theme,
                listBgColor,
                theme.getListBorderColor(),
                theme.getBorderWidth(),
                theme.getCornerRadius()
        );
    }

    /**
     * 构建列表单元格样式
     *
     * @param theme 主题配置
     * @return 列表单元格样式字符串
     */
    private static String buildCellStyle(ThemeConfig theme) {
        return String.format(
                ".list-view .list-cell {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-family: '%s';\n" +
                "    -fx-padding: 8 10;\n" +
                "}\n" +
                ".list-view .list-cell:filled:selected {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: 2;\n" +
                "    -fx-border-radius: %.1f;\n" +
                "}\n" +
                ".list-view .list-cell:filled:hover {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-text-fill: %s;\n" +
                "}",
                theme.getTextPrimaryColor(), theme.getFontFamily(),
                theme.getListRowSelectedBgColor(), theme.getListRowSelectedTextColor(), theme.getBorderColor(), theme.getCornerRadius(),
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
        return ".list-view " + BaseStyleUtils.buildScrollBarStyle(theme);
    }
}
