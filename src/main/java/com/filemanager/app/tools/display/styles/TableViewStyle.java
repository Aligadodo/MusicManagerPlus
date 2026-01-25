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
import javafx.scene.control.TableView;

/**
 * TableView 样式管理类
 * 负责管理和应用 TableView 及其子组件的样式
 *
 * @author hrcao
 */
public class TableViewStyle {

    /**
     * 应用 TableView 的完整样式
     *
     * @param tableView TableView 组件
     * @param theme     主题配置
     */
    public static void applyStyle(TableView<?> tableView, ThemeConfig theme) {
        if (tableView == null || theme == null) {
            return;
        }

        // 构建各个模块的样式
        String baseStyle = buildBaseStyle(theme);
        String headerStyle = buildHeaderStyle(theme);
        String rowStyle = buildRowStyle(theme);
        String cellStyle = buildCellStyle(theme);
        String scrollBarStyle = buildScrollBarStyle(theme);

        // 拼接所有样式
        String completeStyle = String.join("\n", baseStyle, headerStyle, rowStyle, cellStyle, scrollBarStyle);

        // 应用样式
        tableView.setStyle(completeStyle);
    }

    /**
     * 构建 TableView 的基础样式
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
     * 构建表头样式
     *
     * @param theme 主题配置
     * @return 表头样式字符串
     */
    private static String buildHeaderStyle(ThemeConfig theme) {
        String headerBgColor = theme.getTableHeaderBgColorWithOpacity();
        double cornerRadius = theme.getCornerRadius();

        StringBuilder styleBuilder = new StringBuilder();

        // 表头背景样式
        styleBuilder.append(".table-view ").append(BaseStyleUtils.buildHeaderBackgroundStyle(theme, headerBgColor, cornerRadius)).append("\n");

        // 列头样式
        styleBuilder.append(".table-view ").append(BaseStyleUtils.buildColumnHeaderStyle(theme)).append("\n");

        return styleBuilder.toString();
    }

    /**
     * 构建行样式
     *
     * @param theme 主题配置
     * @return 行样式字符串
     */
    private static String buildRowStyle(ThemeConfig theme) {
        String selectedBgColor = theme.getListRowSelectedBgColorWithOpacity(0.8);
        String hoverBgColor = theme.getListRowHoverBgColorWithOpacity(0.6);

        return String.format(
                ".table-view .table-row-cell {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-border-color: transparent;\n" +
                "    -fx-border-width: 1 0 0 0;\n" +
                "}\n" +
                ".table-view .table-row-cell:filled {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".table-view .table-row-cell:filled:selected {\n" +
                "    -fx-background-color: %s;\n" +
                "}\n" +
                ".table-view .table-row-cell:filled:hover {\n" +
                "    -fx-background-color: %s;\n" +
                "}",
                selectedBgColor,
                hoverBgColor
        );
    }

    /**
     * 构建单元格样式
     *
     * @param theme 主题配置
     * @return 单元格样式字符串
     */
    private static String buildCellStyle(ThemeConfig theme) {
        return String.format(
                ".table-view .table-cell {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-family: '%s';\n" +
                "    -fx-padding: 10 10;\n" +
                "    -fx-border-color: transparent;\n" +
                "}",
                theme.getTextPrimaryColor(), theme.getFontFamily()
        );
    }

    /**
     * 构建滚动条样式
     *
     * @param theme 主题配置
     * @return 滚动条样式字符串
     */
    private static String buildScrollBarStyle(ThemeConfig theme) {
        return ".table-view " + BaseStyleUtils.buildScrollBarStyle(theme);
    }
}
