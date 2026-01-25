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

/**
 * 基础样式工具类
 * 包含通用的样式方法和工具函数
 *
 * @author hrcao
 */
public class BaseStyleUtils {

    /**
     * 获取带透明度的颜色
     *
     * @param color    原始颜色
     * @param opacity  透明度 (0.0-1.0)
     * @return 带透明度的颜色
     */
    public static String getColorWithOpacity(String color, double opacity) {
        if (color == null || !color.startsWith("#") || color.length() != 7) {
            return color;
        }

        int alpha = (int) (opacity * 255);
        String alphaHex = String.format("%02x", alpha);
        return color + alphaHex;
    }

    /**
     * 构建组件的基础样式
     *
     * @param theme        主题配置
     * @param bgColor      背景颜色
     * @param borderColor  边框颜色
     * @param borderWidth  边框宽度
     * @param cornerRadius 圆角半径
     * @return 基础样式字符串
     */
    public static String buildBaseStyle(ThemeConfig theme, String bgColor, String borderColor, double borderWidth, double cornerRadius) {
        return String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;",
                bgColor, borderColor, borderWidth, cornerRadius, cornerRadius
        );
    }

    /**
     * 构建滚动条样式
     *
     * @param theme 主题配置
     * @return 滚动条样式字符串
     */
    public static String buildScrollBarStyle(ThemeConfig theme) {
        return String.format(
                ".virtual-flow .scroll-bar:vertical,\n" +
                ".virtual-flow .scroll-bar:horizontal {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".virtual-flow .scroll-bar .thumb {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-background-radius: 4;\n" +
                "}\n" +
                ".virtual-flow .scroll-bar .track {\n" +
                "    -fx-background-color: transparent;\n" +
                "}",
                theme.getTextTertiaryColor()
        );
    }

    /**
     * 构建表头背景样式
     *
     * @param theme        主题配置
     * @param headerBgColor 表头背景颜色
     * @param cornerRadius 圆角半径
     * @return 表头背景样式字符串
     */
    public static String buildHeaderBackgroundStyle(ThemeConfig theme, String headerBgColor, double cornerRadius) {
        return String.format(
                ".column-header-background {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: 0 0 %.1f 0;\n" +
                "    -fx-background-radius: %.1f %.1f 0 0;\n" +
                "}\n" +
                ".column-header-background .filler {\n" +
                "    -fx-background-color: transparent;\n" +
                "}",
                headerBgColor, theme.getBorderColor(), theme.getBorderWidth(), cornerRadius, cornerRadius
        );
    }

    /**
     * 构建列头样式
     *
     * @param theme 主题配置
     * @return 列头样式字符串
     */
    public static String buildColumnHeaderStyle(ThemeConfig theme) {
        return String.format(
                ".column-header {\n" +
                "    -fx-background-color: transparent;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: 0 %.1f 0 0;\n" +
                "}\n" +
                ".column-header .label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-family: '%s';\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-padding: 12 10;\n" +
                "}",
                theme.getBorderColor(), theme.getBorderWidth(), theme.getTextPrimaryColor(), theme.getFontFamily()
        );
    }
}
