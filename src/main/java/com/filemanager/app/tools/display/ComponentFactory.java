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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * UI 组件创建工厂
 * 负责生成风格统一的界面元素
 *
 * @author hrcao
 */
public class ComponentFactory {

    private static ThemeConfig theme = null;

    public static void initComponentFactory(ThemeConfig theme) {
        ComponentFactory.theme = theme;

        // 注册主题变更监听器
        ThemeManager.getInstance().addThemeChangeListener(newTheme -> {
            ComponentFactory.theme = newTheme;
        });
    }

    public static Label createLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font(theme.getFontFamily(), bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(theme.getTextPrimaryColor()));
        return l;
    }

    public static Node createSeparator() {
        Separator separator = new Separator();
        separator.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));
        return separator;
    }

    /**
     * 渐变分割线
     *
     * @param isVertical
     * @return
     */
    public static Node createSeparatorWithChange(boolean isVertical) {
        String accentColor = theme.getAccentColor();
        if (isVertical) {
            // 水平渐变分割线
            Region hDivider = new Region();
            hDivider.setPrefHeight(1); // 线条粗细
            hDivider.setStyle(
                    String.format("-fx-background-color: linear-gradient(to right, transparent, %s 50%%, transparent);", accentColor)
            );
            return hDivider;
        }
        // 垂直渐变分割线
        Region vDivider = new Region();
        vDivider.setPrefWidth(1);
        vDivider.setStyle(
                String.format("-fx-background-color: linear-gradient(to bottom, transparent, %s 50%%, transparent);", accentColor)
        );
        return vDivider;
    }

    /**
     * 带提示词的分割线
     *
     * @param desc
     * @return
     */
    public static HBox createSeparatorWithDesc(String desc) {
        HBox labelDivider = new HBox(10);
        labelDivider.setAlignment(Pos.CENTER);

        Label label = new Label(desc);
        label.setFont(Font.font(theme.getFontFamily(), FontWeight.NORMAL, 11));
        label.setTextFill(Color.web(theme.getTextSecondaryColor()));

        Region line1 = new Region();
        HBox.setHgrow(line1, Priority.ALWAYS);
        line1.setPrefHeight(1);
        line1.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));

        Region line2 = new Region();
        HBox.setHgrow(line2, Priority.ALWAYS);
        line2.setPrefHeight(1);
        line2.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));

        labelDivider.getChildren().addAll(line1, label, line2);
        return labelDivider;
    }

    /**
     * 自动把其他组件排挤到左右两侧
     *
     * @return
     */
    public static Node createSpacer() {
        Region spacer = new Region();
        spacer.setStyle("-fx-background-color: transparent;");
        spacer.getStyleClass().add("glass-pane");
        // 关键核心：设置其在 HBox 中始终自动扩展
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Label createHeader(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getTitleFontFamily(), FontWeight.BOLD, theme.getTitleFontSize()));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        label.minWidth(30);
        return label;
    }

    public static Label createChapter(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getTitleFontFamily(), FontWeight.BOLD, 16));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        label.minWidth(30);
        return label;
    }

    public static Label createDescLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(theme.getDescriptionFontFamily(), FontWeight.NORMAL, theme.getDescriptionFontSize()));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        return label;
    }

    public static AutoShrinkLabel createParamLabel(String text) {
        AutoShrinkLabel label = new AutoShrinkLabel(text);
        label.minWidth(70);
        label.maxWidth(70);
        label.setFont(Font.font(theme.getFontFamily(), FontWeight.BOLD, 12));
        label.setTextFill(Color.web(theme.getTextPrimaryColor()));
        return label;
    }

    public static HBox createParamPairLine(String labelText, Node... controls) {
        HBox hBox = createHBox(createParamLabel(labelText), createSpacer());
        hBox.getChildren().addAll(controls);
        hBox.setSpacing(theme.getSmallSpacing());
        return hBox;
    }

    public static Label createInfoLabel(String text, int maxWidth) {
        Label l = createLabel(text, 10, false);
        l.setTextFill(Color.web(theme.getTextSecondaryColor()));
        l.setMaxWidth(maxWidth);
        l.setWrapText(true);
        return l;
    }

    public static TextArea createTextArea() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPadding(new Insets(10));

        // 使用ThemeConfig中已有的方法获取带透明度的面板背景色
        String bgColor = theme.getPanelBgColorWithOpacity(1.0);

        logArea.setStyle(String.format(
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
                bgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getTextPrimaryColor(), theme.getLogFontFamily(), theme.getLogFontSize(),
                theme.getTextTertiaryColor()
        ));
        return logArea;
    }

    public static VBox createVBoxPanel() {
        VBox panel = new VBox(theme.getMediumSpacing());
        panel.setPadding(new Insets(theme.getLargeSpacing()));

        // 使用ThemeConfig中已有的方法获取带透明度的面板背景色
        String bgColor = theme.getPanelBgColorWithOpacity(1.0);

        panel.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                bgColor, theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
        return panel;
    }

    public static HBox createHBoxPanel() {
        HBox panel = new HBox(theme.getMediumSpacing());
        panel.setPadding(new Insets(theme.getLargeSpacing()));

        // 使用ThemeConfig中已有的方法获取带透明度的面板背景色
        String bgColor = theme.getPanelBgColorWithOpacity(1.0);

        panel.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                bgColor, theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
        return panel;
    }

    public static JFXButton createButton(String text, boolean filled) {
        if (filled) {
            return createStyledButton(text, null,
                    theme.getButtonPrimaryBgColor(), theme.getButtonPrimaryTextColor(),
                    theme.getButtonPrimaryBorderColor(), theme.getButtonPrimaryHoverColor(),
                    theme.getButtonLargeSize(), 36.0);
        } else {
            return createStyledButton(text, null,
                    theme.getButtonSecondaryBgColor(), theme.getButtonSecondaryTextColor(),
                    theme.getButtonSecondaryBorderColor(), theme.getButtonSecondaryHoverColor(),
                    theme.getButtonSmallSize(), 28.0);
        }
    }

    public static JFXButton createIconButton(String icon, Runnable action) {
        JFXButton btn = createButton(icon);

        // 使用更粗的边框宽度
        double borderWidth = Math.max(theme.getBorderWidth(), 1.5);

        // 使用统一的图标按钮样式，确保图标字符可见
        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: '%s'; -fx-font-size: 16px; -fx-padding: 8 12; -fx-cursor: hand; -fx-background-radius: %.1f;" +
                        " -fx-border-width: %.1f; -fx-border-color: %s; -fx-min-height: 30; -fx-min-width: 50; -fx-max-width: Infinity; -fx-alignment: center; -fx-content-display: center;" +
                        " -fx-faint-focus-color: transparent; -fx-focus-color: transparent; -fx-pressed-color: transparent; -fx-armed-color: transparent;",
                theme.getBgColor(), theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getCornerRadius(),
                borderWidth, theme.getBorderColor()
        );

        btn.setStyle(baseStyle);

        // 使用Java代码设置自适应大小，避免CSS中的auto关键字
        btn.setPrefWidth(Region.USE_COMPUTED_SIZE);
        btn.setPrefHeight(Region.USE_COMPUTED_SIZE);
        btn.setMaxHeight(Region.USE_COMPUTED_SIZE);

        // 移除所有样式变化效果
        btn.setOnMouseEntered(null);
        btn.setOnMouseExited(null);
        btn.setOnMousePressed(null);
        btn.setOnMouseReleased(null);

        btn.setOnAction(e -> action.run());
        return btn;
    }

    private static JFXButton createButton(String text) {
        JFXButton btn = new JFXButton(text);
        btn.setFont(Font.font(theme.getButtonFontFamily(), FontWeight.NORMAL, theme.getButtonFontSize()));
        return btn;
    }

    /**
     * 创建主要按钮（一级按钮）
     */
    public static JFXButton createPrimaryButton(String text, Runnable action) {
        return createStyledButton(text, action,
                theme.getButtonPrimaryBgColor(), theme.getButtonPrimaryTextColor(),
                theme.getButtonPrimaryBorderColor(), theme.getButtonPrimaryHoverColor(),
                theme.getButtonLargeSize(), 36.0); // 一级按钮高度
    }

    /**
     * 创建次要按钮（二级按钮）
     */
    public static JFXButton createSecondaryButton(String text, Runnable action) {
        return createStyledButton(text, action,
                theme.getButtonSecondaryBgColor(), theme.getButtonSecondaryTextColor(),
                theme.getButtonSecondaryBorderColor(), theme.getButtonSecondaryHoverColor(),
                theme.getButtonSmallSize(), 28.0); // 二级按钮高度
    }

    /**
     * 创建成功按钮
     */
    public static JFXButton createSuccessButton(String text, Runnable action) {
        return createStyledButton(text, action,
                theme.getButtonSuccessBgColor(), theme.getButtonSuccessTextColor(),
                theme.getButtonSuccessBorderColor(), theme.getButtonSuccessHoverColor(),
                theme.getButtonSmallSize(), 28.0);
    }

    /**
     * 创建警告按钮
     */
    public static JFXButton createWarningButton(String text, Runnable action) {
        return createStyledButton(text, action,
                theme.getButtonWarningBgColor(), theme.getButtonWarningTextColor(),
                theme.getButtonWarningBorderColor(), theme.getButtonWarningHoverColor(),
                theme.getButtonSmallSize(), 28.0);
    }

    /**
     * 创建错误按钮
     */
    public static JFXButton createErrorButton(String text, Runnable action) {
        return createStyledButton(text, action,
                theme.getButtonErrorBgColor(), theme.getButtonErrorTextColor(),
                theme.getButtonErrorBorderColor(), theme.getButtonErrorHoverColor(),
                theme.getButtonSmallSize(), 28.0);
    }

    /**
     * 创建自定义样式的按钮
     */
    public static JFXButton createActionButton(String text, String colorOverride, Runnable action) {
        // 使用主题配置中的按钮文本颜色，而不是硬编码的white
        return createStyledButton(text, action,
                colorOverride != null ? colorOverride : theme.getButtonPrimaryBgColor(), theme.getButtonPrimaryTextColor(),
                null, null, 120.0, 30.0); // 设置固定高度和合适的最小宽度
    }

    /**
     * 创建大尺寸按钮
     */
    public static JFXButton createLargeActionButton(String text, String colorOverride, Runnable action) {
        // 使用主题配置中的按钮文本颜色，而不是硬编码的white
        return createStyledButton(text, action,
                colorOverride != null ? colorOverride : theme.getButtonPrimaryBgColor(), theme.getButtonPrimaryTextColor(),
                null, null, theme.getButtonLargeSize(), theme.getButtonLargeSize());
    }

    /**
     * 创建小尺寸按钮
     */
    public static JFXButton createSmallActionButton(String text, String colorOverride, Runnable action) {
        // 使用主题配置中的按钮文本颜色，而不是硬编码的white
        return createStyledButton(text, action,
                colorOverride != null ? colorOverride : theme.getButtonSecondaryBgColor(), theme.getButtonSecondaryTextColor(),
                null, null, theme.getButtonSmallSize(), theme.getButtonSmallSize());
    }

    /**
     * 创建带样式的按钮
     */
    /**
     * 验证并格式化颜色值，确保它是有效的十六进制格式
     */
    private static String validateAndFormatColor(String colorValue) {
        if (colorValue == null || colorValue.isEmpty()) {
            return "#3498db"; // 默认颜色
        }

        // 转换0x开头的颜色值
        if (colorValue.startsWith("0x")) {
            try {
                String hex = colorValue.substring(2);
                if (hex.length() == 8) {
                    return "#" + hex; // 保留透明度部分
                } else if (hex.length() == 6) {
                    return "#" + hex; // 不包含透明度
                } else {
                    return "#3498db"; // 默认颜色
                }
            } catch (Exception e) {
                return "#3498db"; // 默认颜色
            }
        }

        // 确保颜色值以#开头
        if (!colorValue.startsWith("#")) {
            return "#" + colorValue;
        }

        // 确保颜色值有正确的长度 (7位: #RRGGBB 或 9位: #RRGGBBAA)
        if (colorValue.length() != 7 && colorValue.length() != 9) {
            return "#3498db"; // 默认颜色
        }

        return colorValue;
    }

    private static JFXButton createStyledButton(String text, Runnable action,
                                                String bgColor, String textColor,
                                                String borderColor, String hoverColor,
                                                double minWidth, double buttonHeight) {
        JFXButton btn = createButton(text);

        // 验证颜色格式
        bgColor = validateAndFormatColor(bgColor);
        textColor = validateAndFormatColor(textColor);
        if (borderColor != null) {
            borderColor = validateAndFormatColor(borderColor);
        }
        if (hoverColor != null) {
            hoverColor = validateAndFormatColor(hoverColor);
        }

        // 如果边框颜色为空或与背景色太接近，使用主题边框颜色
        if (borderColor == null || isColorTooClose(bgColor, borderColor)) {
            borderColor = theme.getBorderColor();
        }

        // 如果悬停颜色为空，使用主题悬停颜色
        if (hoverColor == null) {
            hoverColor = theme.getHoverColor();
        }

        // 使用主题的边框宽度
        double borderWidth = theme.getBorderWidth();

        // 增加水平内边距，确保文字有足够空间显示
        double horizontalPadding = Math.max(theme.getSmallSpacing() * 2, 8.0);

        // 基础样式 - 使用传入的按钮高度，设置较小的最小宽度以便按钮能根据文字长度自适应
        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-background-radius: %.1f; " +
                        "-fx-cursor: hand; -fx-padding: %.1f %.1f; -fx-border-width: %.1f; -fx-border-color: %s; " +
                        "-fx-min-height: %.1f; -fx-min-width: %.1f; -fx-max-width: Infinity; -fx-alignment: center; -fx-content-display: center;" +
                        " -fx-faint-focus-color: transparent; -fx-focus-color: transparent; -fx-pressed-color: transparent; -fx-armed-color: transparent;" +
                        " -fx-font-size: 12px;", // 明确设置字体大小，确保文字清晰显示
                bgColor, textColor, theme.getCornerRadius(),
                theme.getSmallSpacing(), horizontalPadding,
                borderWidth, borderColor,
                buttonHeight, Math.max(minWidth * 0.8, 60.0) // 减小最小宽度，允许按钮根据文字自适应
        );

        btn.setStyle(baseStyle);

        // 使用固定高度和自适应宽度
        btn.setPrefWidth(Region.USE_COMPUTED_SIZE);
        btn.setPrefHeight(buttonHeight); // 使用传入的固定高度
        btn.setMaxHeight(buttonHeight); // 限制最大高度
        btn.setWrapText(false); // 不允许文字换行，确保文字在一行完整显示

        // 添加样式变化效果，提供交互反馈
        final String originalStyle = baseStyle;
        final String hoverStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-background-radius: %.1f; " +
                        "-fx-cursor: hand; -fx-padding: %.1f %.1f; -fx-border-width: %.1f; -fx-border-color: %s; " +
                        "-fx-min-height: %.1f; -fx-min-width: %.1f; -fx-max-width: Infinity; -fx-alignment: center; -fx-content-display: center;" +
                        " -fx-faint-focus-color: transparent; -fx-focus-color: transparent; -fx-pressed-color: transparent; -fx-armed-color: transparent;" +
                        " -fx-font-size: 12px;", // 保持字体大小一致
                bgColor, textColor, theme.getCornerRadius(), // 使用原始背景色，避免悬浮时变白
                theme.getSmallSpacing(), horizontalPadding, // 使用相同的水平内边距
                Math.min(borderWidth + 1.0, 3.0), theme.getAccentColor(), // 边框加粗并使用强调色
                buttonHeight, Math.max(minWidth * 0.8, 60.0) // 使用相同的最小宽度
        );

        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) {
                btn.setStyle(hoverStyle);
            }
        });

        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) {
                btn.setStyle(originalStyle);
            }
        });

        if (action != null) btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * 检查两个颜色是否太接近，导致对比度不足
     */
    private static boolean isColorTooClose(String color1, String color2) {
        try {
            Color c1 = Color.web(color1);
            Color c2 = Color.web(color2);

            // 计算RGB分量的差异
            double rDiff = Math.abs(c1.getRed() - c2.getRed());
            double gDiff = Math.abs(c1.getGreen() - c2.getGreen());
            double bDiff = Math.abs(c1.getBlue() - c2.getBlue());

            // 如果RGB分量差异都小于0.1，认为颜色太接近
            return rDiff < 0.1 && gDiff < 0.1 && bDiff < 0.1;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * [新增] 创建行内图标按钮 (如删除、上移下移)
     */
    public static JFXButton createIconButton(String iconText, String colorHex, Runnable action) {
        JFXButton btn = createButton(iconText);
        String textColor = colorHex != null ? colorHex : theme.getTextPrimaryColor();

        // 使用更粗的边框宽度
        double borderWidth = Math.max(theme.getBorderWidth(), 1.5);

        // 使用统一的行内图标按钮样式，增大字体大小
        // 基础样式使用主题背景色，确保文字可见
        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: %.1f; -fx-font-size: 12px; -fx-text-fill: %s; -fx-cursor: hand;" +
                        " -fx-min-height: 28; -fx-min-width: 40.0; -fx-max-width: Infinity; -fx-alignment: center; -fx-content-display: center;" +
                        " -fx-border-width: %.1f;" +
                        " -fx-faint-focus-color: transparent; -fx-focus-color: transparent; -fx-pressed-color: transparent; -fx-armed-color: transparent;",
                theme.getBgColor(), theme.getBorderColor(), theme.getCornerRadius(), theme.getSmallSpacing(), textColor,
                borderWidth
        );

        btn.setStyle(baseStyle);

        // 使用Java代码设置自适应大小，避免CSS中的auto关键字
        btn.setPrefWidth(Region.USE_COMPUTED_SIZE);
        btn.setPrefHeight(Region.USE_COMPUTED_SIZE);
        btn.setMaxHeight(Region.USE_COMPUTED_SIZE);

        btn.setOnAction(e -> {
            if (action != null) action.run();
            e.consume(); // 防止事件冒泡选中列表行
        });

        // 移除所有样式变化效果
        btn.setOnMouseEntered(null);
        btn.setOnMouseExited(null);
        btn.setOnMousePressed(null);
        btn.setOnMouseReleased(null);

        return btn;
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static VBox createVBox(Node... subNodes) {
        VBox p = new VBox();
        p.setStyle("-fx-background-color: transparent;");
        p.getStyleClass().add("glass-pane");
        for (Node subNode : subNodes) {
            p.getChildren().add(subNode);
        }
        return p;
    }

    /**
     * 创建透明的竖向容器
     *
     * @return
     */
    public static VBox createVBoxPanel(Node... subNodes) {
        VBox p = createVBox(subNodes);
        p.setSpacing(theme.getMediumSpacing());
        p.setPadding(new Insets(theme.getLargeSpacing()));

        // 应用玻璃效果透明度
        String bgColor = theme.getPanelBgColor();
        if (bgColor.startsWith("#") && bgColor.length() == 7) {
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            bgColor = bgColor + alphaHex;
        }

        p.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                bgColor, theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));

        return p;
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBox(Node... subNodes) {
        HBox p = new HBox();
        p.setStyle("-fx-background-color: transparent;");
        p.getStyleClass().add("glass-pane");
        for (Node subNode : subNodes) {
            p.getChildren().add(subNode);
        }
        return p;
    }


    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBoxPanel(Node... subNodes) {
        HBox p = createHBox(subNodes);
        p.setPadding(new Insets(5, 5, 5, 5));
        p.setSpacing(5);

        // 应用玻璃效果透明度
        String bgColor = theme.getPanelBgColor();
        if (bgColor.startsWith("#") && bgColor.length() == 7) {
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            bgColor = bgColor + alphaHex;
        }

        p.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                bgColor, theme.getCornerRadius(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));

        return p;
    }

    public static VBox createSectionHeader(String title, String subtitle) {
        VBox v = new VBox(2);
        v.getChildren().addAll(createHeader(title), createInfoLabel(subtitle, 400));
        return v;
    }

    // [新增] 通用：创建统一风格的微型图标按钮
    public static JFXButton createSmallIconButton(String text, EventHandler<ActionEvent> handler) {
        JFXButton btn = createButton(text);

        // 使用更粗的边框宽度
        double borderWidth = Math.max(theme.getBorderWidth(), 1.5);

        // 将背景色设置为半透明，使用主题的玻璃透明度
        String bgColor = theme.getBgColor();
        if (bgColor.startsWith("#") && bgColor.length() == 7) {
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            bgColor = bgColor + alphaHex;
        }

        // 使用半透明边框
        String borderColor = theme.getBorderColor();
        if (borderColor.startsWith("#") && borderColor.length() == 7) {
            int borderAlpha = (int) (0.6 * 255); // 边框透明度60%
            String borderAlphaHex = String.format("%02x", borderAlpha);
            borderColor = borderColor + borderAlphaHex;
        }

        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: %.1f; -fx-font-size: 10px; -fx-font-family: '%s'; -fx-text-fill: %s;" +
                        " -fx-min-height: 22; -fx-min-width: 24.0; -fx-max-width: Infinity; -fx-cursor: hand; -fx-alignment: center; -fx-content-display: center;" +
                        " -fx-border-width: %.1f;" +
                        " -fx-faint-focus-color: transparent; -fx-focus-color: transparent; -fx-pressed-color: transparent; -fx-armed-color: transparent;",
                bgColor, borderColor, theme.getCornerRadius(), theme.getSmallSpacing(), theme.getFontFamily(), theme.getTextPrimaryColor(),
                borderWidth
        );

        btn.setStyle(baseStyle);

        // 使用Java代码设置自适应大小，避免CSS中的auto关键字
        btn.setPrefWidth(Region.USE_COMPUTED_SIZE);
        btn.setPrefHeight(Region.USE_COMPUTED_SIZE);
        btn.setMaxHeight(Region.USE_COMPUTED_SIZE);
        btn.setOnAction(e -> {
            handler.handle(e);
            e.consume(); // 防止事件冒泡触发 ListCell 选中
        });

        // 添加悬停效果
        btn.setOnMouseEntered(e -> {
            // 悬停时增加透明度
            String hoverBgColor = theme.getBgColor();
            if (hoverBgColor.startsWith("#") && hoverBgColor.length() == 7) {
                int hoverAlpha = (int) (Math.min(theme.getGlassOpacity() + 0.1, 1.0) * 255);
                String hoverAlphaHex = String.format("%02x", hoverAlpha);
                hoverBgColor = hoverBgColor + hoverAlphaHex;
            }

            String hoverStyle = String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: %.1f; -fx-font-size: 10px; -fx-font-family: '%s'; -fx-text-fill: %s;" +
                            " -fx-min-height: 22; -fx-min-width: 24.0; -fx-max-width: Infinity; -fx-cursor: hand; -fx-alignment: center; -fx-content-display: center;" +
                            " -fx-border-width: %.1f;" +
                            " -fx-faint-focus-color: transparent; -fx-focus-color: transparent; -fx-pressed-color: transparent; -fx-armed-color: transparent;",
                    hoverBgColor, theme.getAccentColor(), theme.getCornerRadius(), theme.getSmallSpacing(), theme.getFontFamily(), theme.getTextPrimaryColor(),
                    borderWidth
            );
            btn.setStyle(hoverStyle);
        });

        btn.setOnMouseExited(e -> {
            // 恢复原始样式
            btn.setStyle(baseStyle);
        });

        // 移除按压效果，保持简洁
        btn.setOnMousePressed(null);
        btn.setOnMouseReleased(null);
        return btn;
    }

    public static TreeTableColumn<ChangeRecord, String> createTreeTableColumn(String text, boolean needToolTip, int prefWidth, int minWidth, int maxWidth) {
        TreeTableColumn<ChangeRecord, String> column = new TreeTableColumn<>(text);
        column.setPrefWidth(prefWidth);
        column.setMinWidth(minWidth);
        column.setMaxWidth(maxWidth);
        column.setStyle(String.format(
                "-fx-border-color: %s; -fx-border-radius: %.1f; -fx-padding: 2 6 2 6; -fx-font-size: 10px; -fx-font-family: '%s'; -fx-text-fill: %s;",
                theme.getBorderColor(), theme.getCornerRadius(), theme.getFontFamily(), theme.getTextPrimaryColor()
        ));
        column.setCellFactory(col -> {
            return new TreeTableCell<ChangeRecord, String>() {
                private final Tooltip tooltip = new Tooltip();

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null); // 必须清除，否则空行也会显示上一个内容的悬浮
                    } else {
                        setText(item);
                        setFont(Font.font(theme.getFontFamily(), FontWeight.NORMAL, 12));
                        setTextFill(Color.web(theme.getTextPrimaryColor()));
                        // 设置悬浮内容
                        tooltip.setText("详情内容：\n" + item);
                        // 可选：设置换行宽度，防止详情太长变成一条直线
                        tooltip.setWrapText(true);
                        tooltip.setPrefWidth(300);
                        setTooltip(tooltip);
                    }
                }
            };
        });
        return column;
    }

    /**
     * 创建带有主题样式的TabPane
     *
     * @param isSecondary 是否为二级TabPane（尺寸更小）
     */
    public static JFXTabPane createTabPane(boolean isSecondary) {
        JFXTabPane tabPane = new JFXTabPane();

        // 根据是否为二级TabPane设置不同的尺寸
        double tabHeight = isSecondary ? 35.0 : 45.0;
        double tabMinWidth = isSecondary ? 80.0 : 120.0;
        double tabMaxWidth = isSecondary ? 180.0 : 220.0;
        double fontSize = isSecondary ? 13.0 : 15.0;

        tabPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: %.1f; -fx-tab-max-height: %.1f; -fx-tab-min-width: %.1f; -fx-tab-max-width: %.1f; -fx-padding: 5 0 0 0;\n" +
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
                        "    -fx-font-family: '%s';\n" +
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
                tabHeight, tabHeight, tabMinWidth, tabMaxWidth,
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                theme.getBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                theme.getPanelHoverColor(),
                theme.getPanelBgColor(), theme.getAccentColor(), theme.getAccentColor(), theme.getPanelBgColor(), theme.getAccentColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(),
                theme.getTextSecondaryColor(), theme.getFontFamily(), fontSize,
                theme.getTextPrimaryColor(),
                theme.getAccentColor(),
                theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(), theme.getCornerRadius()
        ));

        return tabPane;
    }

    /**
     * 创建一级TabPane（默认尺寸）
     */
    public static JFXTabPane createTabPane() {
        return createTabPane(false);
    }

    /**
     * 为Tab添加主题样式
     */
    public static Tab createStyledTab(String text, Node content) {
        Tab tab = new Tab(text, content);
        tab.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-family: '%s'; -fx-font-size: 14px;",
                theme.getTextPrimaryColor(), theme.getFontFamily()
        ));

        return tab;
    }

    public static Button createRefreshButton(EventHandler<ActionEvent> handler) {
        // 1. 创建刷新图标的 SVG 路径 (一个圆圈箭头)
        SVGPath refreshIcon = new SVGPath();
        refreshIcon.setContent("M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z");
        refreshIcon.setFill(Color.web(theme.getTextPrimaryColor()));

        // 2. 创建按钮并设置样式
        Button btn = new Button();
        btn.setGraphic(refreshIcon); // 将 SVG 设置为按钮图标
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 50; -fx-min-width: 20px; -fx-min-height: 20px; -fx-cursor: hand; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: 50;",
                theme.getButtonPrimaryBgColor(), theme.getBorderColor(), theme.getBorderWidth()
        ));

        // 3. 添加旋转动画（点击时触发）
        RotateTransition rt = new RotateTransition(Duration.millis(600), refreshIcon);
        rt.setByAngle(360); // 旋转 360 度
        rt.setCycleCount(1);
        rt.setInterpolator(Interpolator.EASE_BOTH); // 柔和的启动和停止

        // 4. 添加悬停效果
        btn.setOnMouseEntered(e -> {
            btn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 50; -fx-min-width: 20px; -fx-min-height: 20px; -fx-cursor: hand; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: 50;",
                    theme.getButtonPrimaryHoverColor(), theme.getBorderColor(), theme.getBorderWidth()
            ));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 50; -fx-min-width: 20px; -fx-min-height: 20px; -fx-cursor: hand; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: 50;",
                    theme.getButtonPrimaryBgColor(), theme.getBorderColor(), theme.getBorderWidth()
            ));
        });

        btn.setOnAction(e -> {
            handler.handle(e);
            rt.playFromStart();
        });
        return btn;
    }

    /**
     * 创建统一风格的进度条
     *
     * @param initialValue 初始进度值（0.0-1.0，-1.0表示不确定）
     * @param prefWidth    首选宽度
     * @return 配置好的进度条
     */
    public static ProgressBar createProgressBar(double initialValue, double prefWidth) {
        ProgressBar progressBar = new ProgressBar(initialValue);
        progressBar.setPrefHeight(25);
        progressBar.setPrefWidth(prefWidth);
        progressBar.setStyle("-fx-accent: #27ae60;");
        return progressBar;
    }

    /**
     * 创建主进度条（占满宽度）
     *
     * @param initialValue 初始进度值
     * @return 配置好的主进度条
     */
    public static ProgressBar createMainProgressBar(double initialValue) {
        ProgressBar progressBar = createProgressBar(initialValue, 10000.0);
        return progressBar;
    }

    /**
     * 创建根路径进度条（固定宽度）
     *
     * @param initialValue 初始进度值
     * @return 配置好的根路径进度条
     */
    public static ProgressBar createRootPathProgressBar(double initialValue) {
        return createProgressBar(initialValue, 200);
    }

    /**
     * 创建树节点菜单
     */
    public static HBox createTreeItemMenu(EventHandler<ActionEvent> open, EventHandler<ActionEvent> up, EventHandler<ActionEvent> down, EventHandler<ActionEvent> del) {
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_RIGHT);
        // 策略操作：上移、下移、删除
        if (open != null) {
            JFXButton openUp = createSmallIconButton("📂", open);
            actions.getChildren().add(openUp);
        }
        if (up != null) {
            JFXButton btnUp = createSmallIconButton("▲", up);
            actions.getChildren().add(btnUp);
        }
        if (down != null) {
            JFXButton btnDown = createSmallIconButton("▼", down);
            actions.getChildren().add(btnDown);
        }
        if (del != null) {
            JFXButton btnDel = createSmallIconButton("✕", del);
            btnDel.setTextFill(Color.web("#e74c3c"));
            actions.getChildren().add(btnDel);
        }
        return actions;
    }

    /**
     * 创建带有主题样式的ListView
     */
    public static <T> ListView<T> createListView() {
        ListView<T> listView = new ListView<>();

        // 为面板背景添加透明度，实现玻璃效果
        String panelBgColor = theme.getPanelBgColor();
        // 特殊处理transparent关键字，无论是否带有#前缀或被截断为transp
        if (panelBgColor != null && !"transparent".equalsIgnoreCase(panelBgColor) && !"#transparent".equalsIgnoreCase(panelBgColor) && !"transp".equalsIgnoreCase(panelBgColor) && !"#transp".equalsIgnoreCase(panelBgColor) && panelBgColor.startsWith("#")) {
            // 将十六进制颜色转换为带透明度的RGBA颜色
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            panelBgColor = panelBgColor + alphaHex;
        }

        listView.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;",
                panelBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius()
        ));

        return listView;
    }

    /**
     * 更新列表行选中的样式
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