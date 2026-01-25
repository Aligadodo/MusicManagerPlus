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
import javafx.scene.control.TextArea;

/**
 * TextArea 样式管理类
 * 负责管理和应用 TextArea 及其子组件的样式
 *
 * @author hrcao
 */
public class TextAreaStyle {

    /**
     * 应用 TextArea 的完整样式
     *
     * @param textArea TextArea 组件
     * @param theme    主题配置
     */
    public static void applyStyle(TextArea textArea, ThemeConfig theme) {
        if (textArea == null || theme == null) {
            return;
        }

        // 构建各个模块的样式
        String baseStyle = buildBaseStyle(theme);
        String textStyle = buildTextStyle(theme);
        String scrollPaneStyle = buildScrollPaneStyle(theme);
        String scrollBarStyle = buildScrollBarStyle(theme);

        // 拼接所有样式
        String completeStyle = String.join("\n", baseStyle, textStyle, scrollPaneStyle, scrollBarStyle);

        // 应用样式
        textArea.setStyle(completeStyle);
    }

    /**
     * 构建 TextArea 的基础样式
     *
     * @param theme 主题配置
     * @return 基础样式字符串
     */
    private static String buildBaseStyle(ThemeConfig theme) {
        return BaseStyleUtils.buildBaseStyle(
                theme,
                theme.getListBgColor(),
                theme.getBorderColor(),
                theme.getBorderWidth(),
                theme.getCornerRadius()
        );
    }

    /**
     * 构建文本样式
     *
     * @param theme 主题配置
     * @return 文本样式字符串
     */
    private static String buildTextStyle(ThemeConfig theme) {
        return String.format(
                "-fx-text-fill: %s; -fx-font-family: '%s'; -fx-font-size: %.1f;",
                theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getFontSize()
        );
    }

    /**
     * 构建滚动面板样式
     *
     * @param theme 主题配置
     * @return 滚动面板样式字符串
     */
    private static String buildScrollPaneStyle(ThemeConfig theme) {
        return ".text-area .scroll-pane {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".text-area .scroll-pane .viewport {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
                ".text-area .scroll-pane .content {\n" +
                "    -fx-background-color: transparent;\n" +
                "}";
    }

    /**
     * 构建滚动条样式
     *
     * @param theme 主题配置
     * @return 滚动条样式字符串
     */
    private static String buildScrollBarStyle(ThemeConfig theme) {
        return String.format(
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
                theme.getTextTertiaryColor()
        );
    }
}
