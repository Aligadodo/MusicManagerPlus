/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-13 
 */
package com.filemanager.app.tools.display;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Properties;

import com.filemanager.app.base.IAutoReloadAble;

/**
 * 界面主题配置模型
 * 存储颜色、透明度、圆角等外观参数
 * 按组件类型分组，支持不同级别组件的样式设置
 * @author 28667
 */
@Data
@NoArgsConstructor
public class ThemeConfig implements Cloneable, IAutoReloadAble {
    // 模板信息
    private String templateName = "默认主题"; // 模板名称
    private String templateDescription = "默认的界面样式模板";
    
    // 背景设置
    private String bgImagePath = "";
    private String bgColor = "#f5f5f5";
    
    // 主题色
    private String accentColor = "#3498db"; // 主色调
    private String accentLightColor = "#5dade2"; // 主色调亮色
    private String accentDarkColor = "#2980b9"; // 主色调暗色
    
    // 文本颜色分级
    private String textPrimaryColor = "#2c3e50";  // 主要文本颜色
    private String textSecondaryColor = "#546e7a";  // 次要文本颜色
    private String textTertiaryColor = "#78909c";  // 三级文本颜色
    private String textDisabledColor = "#b0bec5";  // 禁用文本颜色
    
    // 毛玻璃透明度 (0.0 - 1.0, 值越小越透明)
    private double glassOpacity = 0.65;
    
    // 是否启用深色背景模式
    private boolean isDarkBackground = false;
    
    // 基础样式设置
    private double cornerRadius = 5.0;
    private String borderColor = "#e0e0e0";
    private double borderWidth = 1.0;
    
    // 面板样式分组
    private String panelBgColor = "#ffffff";
    private String panelBorderColor = "#e0e0e0";
    private String panelTitleColor = "#2c3e50";
    private String panelHoverColor = "#f5f5f5";
    
    // 按钮样式分级
    // 主要按钮
    private String buttonPrimaryBgColor = "#3498db";
    private String buttonPrimaryTextColor = "#ffffff";
    private String buttonPrimaryBorderColor = "#2980b9";
    private String buttonPrimaryHoverColor = "#2980b9";
    private String buttonPrimaryPressedColor = "#21618c";
    
    // 次要按钮
    private String buttonSecondaryBgColor = "#ffffff";
    private String buttonSecondaryTextColor = "#2c3e50";
    private String buttonSecondaryBorderColor = "#e0e0e0";
    private String buttonSecondaryHoverColor = "#f5f5f5";
    private String buttonSecondaryPressedColor = "#e0e0e0";
    
    // 成功按钮
    private String buttonSuccessBgColor = "#27ae60";
    private String buttonSuccessTextColor = "#ffffff";
    private String buttonSuccessBorderColor = "#229954";
    private String buttonSuccessHoverColor = "#229954";
    
    // 警告按钮
    private String buttonWarningBgColor = "#f39c12";
    private String buttonWarningTextColor = "#ffffff";
    private String buttonWarningBorderColor = "#e67e22";
    private String buttonWarningHoverColor = "#e67e22";
    
    // 错误按钮
    private String buttonErrorBgColor = "#e74c3c";
    private String buttonErrorTextColor = "#ffffff";
    private String buttonErrorBorderColor = "#c0392b";
    private String buttonErrorHoverColor = "#c0392b";
    
    // 按钮尺寸设置
    private double buttonHeight = 36.0;
    private double buttonLargeSize = 80.0;
    private double buttonSmallSize = 60.0;
    private double buttonIconSize = 16.0;
    
    // 列表样式分组
    private String listBgColor = "#ffffff";
    private String listRowEvenBgColor = "#ffffff";
    private String listRowOddBgColor = "#f9f9f9";
    private String listRowSelectedBgColor = "#e3f2fd";
    private String listRowSelectedTextColor = "#2c3e50";
    private String listRowHoverBgColor = "#f5f5f5";
    private String listBorderColor = "#e0e0e0";
    private String listHeaderBgColor = "#f5f5f5";
    private String listHeaderTextColor = "#2c3e50";
    
    // 状态颜色
    private String hoverColor = "#f5f5f5";
    private String selectedColor = "#e3f2fd";
    private String disabledColor = "#f5f5f5";
    
    // 进度条和状态指示颜色
    private String progressBarColor = "#27ae60";
    private String successColor = "#27ae60";
    private String warningColor = "#f39c12";
    private String errorColor = "#e74c3c";
    private String infoColor = "#3498db";
    
    // 字体设置
    private String fontFamily = "Segoe UI, Arial, sans-serif";
    private double fontSize = 14.0;
    
    // 字体样式分级
    private String titleFontFamily = "Segoe UI, Arial, sans-serif";
    private double titleFontSize = 18.0;
    private String titleFontColor = "#2c3e50";
    
    private String subtitleFontFamily = "Segoe UI, Arial, sans-serif";
    private double subtitleFontSize = 16.0;
    private String subtitleFontColor = "#546e7a";
    
    private String buttonFontFamily = "Segoe UI, Arial, sans-serif";
    private double buttonFontSize = 14.0;
    
    private String descriptionFontFamily = "Segoe UI, Arial, sans-serif";
    private double descriptionFontSize = 14.0;
    private String descriptionFontColor = "#78909c";
    
    private String logFontFamily = "Courier New, monospace";
    private double logFontSize = 12.0;
    private String logFontColor = "#2c3e50";
    
    private String runtimeInfoFontFamily = "Segoe UI, Arial, sans-serif";
    private double runtimeInfoFontSize = 12.0;
    private String runtimeInfoFontColor = "#78909c";
    
    // 间距设置
    private double smallSpacing = 5.0;
    private double mediumSpacing = 10.0;
    private double largeSpacing = 15.0;
    private double extraLargeSpacing = 20.0;

    @Override
    public ThemeConfig clone() {
        try {
            return (ThemeConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            return new ThemeConfig();
        }
    }

    // 手动添加的getter方法，解决Lombok注解处理器问题

    // 字体相关getter方法
    public String getTitleFontFamily() { return titleFontFamily; }
    public double getTitleFontSize() { return titleFontSize; }
    public String getDescriptionFontFamily() { return descriptionFontFamily; }
    public double getDescriptionFontSize() { return descriptionFontSize; }
    public String getButtonFontFamily() { return buttonFontFamily; }
    public double getButtonFontSize() { return buttonFontSize; }

    // 间距相关getter方法
    public double getSmallSpacing() { return smallSpacing; }
    public double getMediumSpacing() { return mediumSpacing; }
    public double getLargeSpacing() { return largeSpacing; }

    // 按钮样式相关getter方法
    public String getButtonPrimaryBorderColor() { return buttonPrimaryBorderColor; }
    public String getButtonPrimaryHoverColor() { return buttonPrimaryHoverColor; }
    public String getButtonSecondaryHoverColor() { return buttonSecondaryHoverColor; }
    public String getButtonSuccessBgColor() { return buttonSuccessBgColor; }
    public String getButtonSuccessTextColor() { return buttonSuccessTextColor; }
    public String getButtonSuccessBorderColor() { return buttonSuccessBorderColor; }
    public String getButtonSuccessHoverColor() { return buttonSuccessHoverColor; }
    public String getButtonWarningBgColor() { return buttonWarningBgColor; }
    public String getButtonWarningTextColor() { return buttonWarningTextColor; }
    public String getButtonWarningBorderColor() { return buttonWarningBorderColor; }
    public String getButtonWarningHoverColor() { return buttonWarningHoverColor; }
    public String getButtonErrorBgColor() { return buttonErrorBgColor; }
    public String getButtonErrorTextColor() { return buttonErrorTextColor; }
    public String getButtonErrorBorderColor() { return buttonErrorBorderColor; }
    public String getButtonErrorHoverColor() { return buttonErrorHoverColor; }

    public String getListRowSelectedBgColor() {
        return listRowSelectedBgColor;
    }

    // 模板信息相关方法
    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    // 背景设置相关方法
    public String getBgImagePath() {
        return bgImagePath;
    }

    public void setBgImagePath(String bgImagePath) {
        this.bgImagePath = bgImagePath;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    // 主题色相关方法
    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getAccentLightColor() {
        return accentLightColor;
    }

    public void setAccentLightColor(String accentLightColor) {
        this.accentLightColor = accentLightColor;
    }

    public String getAccentDarkColor() {
        return accentDarkColor;
    }

    public void setAccentDarkColor(String accentDarkColor) {
        this.accentDarkColor = accentDarkColor;
    }

    // 文本颜色分级相关方法
    public String getTextPrimaryColor() {
        return textPrimaryColor;
    }

    public void setTextPrimaryColor(String textPrimaryColor) {
        this.textPrimaryColor = textPrimaryColor;
    }

    public String getTextSecondaryColor() {
        return textSecondaryColor;
    }

    public void setTextSecondaryColor(String textSecondaryColor) {
        this.textSecondaryColor = textSecondaryColor;
    }

    public String getTextTertiaryColor() {
        return textTertiaryColor;
    }

    public void setTextTertiaryColor(String textTertiaryColor) {
        this.textTertiaryColor = textTertiaryColor;
    }

    public String getTextDisabledColor() {
        return textDisabledColor;
    }

    public void setTextDisabledColor(String textDisabledColor) {
        this.textDisabledColor = textDisabledColor;
    }

    // 毛玻璃透明度相关方法
    public double getGlassOpacity() {
        return glassOpacity;
    }

    public void setGlassOpacity(double glassOpacity) {
        this.glassOpacity = glassOpacity;
    }

    // 深色背景模式相关方法
    public boolean isDarkBackground() {
        return isDarkBackground;
    }

    public void setDarkBackground(boolean isDarkBackground) {
        this.isDarkBackground = isDarkBackground;
    }

    // 基础样式设置相关方法
    public double getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(double cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public double getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(double borderWidth) {
        this.borderWidth = borderWidth;
    }

    // 面板样式分组相关方法
    public String getPanelBgColor() {
        return panelBgColor;
    }

    public void setPanelBgColor(String panelBgColor) {
        this.panelBgColor = panelBgColor;
    }

    public String getPanelBorderColor() {
        return panelBorderColor;
    }

    public void setPanelBorderColor(String panelBorderColor) {
        this.panelBorderColor = panelBorderColor;
    }

    public String getPanelTitleColor() {
        return panelTitleColor;
    }

    public void setPanelTitleColor(String panelTitleColor) {
        this.panelTitleColor = panelTitleColor;
    }

    public String getPanelHoverColor() {
        return panelHoverColor;
    }

    public void setPanelHoverColor(String panelHoverColor) {
        this.panelHoverColor = panelHoverColor;
    }

    // 字体相关方法
    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public String getLogFontFamily() {
        return logFontFamily;
    }

    public void setLogFontFamily(String logFontFamily) {
        this.logFontFamily = logFontFamily;
    }

    public double getLogFontSize() {
        return logFontSize;
    }

    public void setLogFontSize(double logFontSize) {
        this.logFontSize = logFontSize;
    }

    // 进度条相关方法
    public String getProgressBarColor() {
        return progressBarColor;
    }

    public void setProgressBarColor(String progressBarColor) {
        this.progressBarColor = progressBarColor;
    }

    // 按钮尺寸相关方法
    public double getButtonLargeSize() {
        return buttonLargeSize;
    }

    public void setButtonLargeSize(double buttonLargeSize) {
        this.buttonLargeSize = buttonLargeSize;
    }

    public double getButtonSmallSize() {
        return buttonSmallSize;
    }

    public void setButtonSmallSize(double buttonSmallSize) {
        this.buttonSmallSize = buttonSmallSize;
    }

    // 状态颜色相关方法
    public String getHoverColor() {
        return hoverColor;
    }

    public void setHoverColor(String hoverColor) {
        this.hoverColor = hoverColor;
    }

    // 按钮样式相关方法
    public String getButtonPrimaryBgColor() {
        return buttonPrimaryBgColor;
    }

    public String getButtonPrimaryTextColor() {
        return buttonPrimaryTextColor;
    }

    public String getButtonSecondaryBgColor() {
        return buttonSecondaryBgColor;
    }

    public String getButtonSecondaryTextColor() {
        return buttonSecondaryTextColor;
    }

    public String getButtonSecondaryBorderColor() {
        return buttonSecondaryBorderColor;
    }

    public void setButtonPrimaryBgColor(String buttonPrimaryBgColor) {
        this.buttonPrimaryBgColor = buttonPrimaryBgColor;
    }

    public void setButtonPrimaryTextColor(String buttonPrimaryTextColor) {
        this.buttonPrimaryTextColor = buttonPrimaryTextColor;
    }

    public void setButtonSecondaryBgColor(String buttonSecondaryBgColor) {
        this.buttonSecondaryBgColor = buttonSecondaryBgColor;
    }

    public void setButtonSecondaryTextColor(String buttonSecondaryTextColor) {
        this.buttonSecondaryTextColor = buttonSecondaryTextColor;
    }

    public void setButtonSecondaryBorderColor(String buttonSecondaryBorderColor) {
        this.buttonSecondaryBorderColor = buttonSecondaryBorderColor;
    }

    // 列表样式相关方法
    public String getListBgColor() {
        return listBgColor;
    }

    public String getListRowOddBgColor() {
        return listRowOddBgColor;
    }

    public String getListRowEvenBgColor() {
        return listRowEvenBgColor;
    }

    // 状态颜色和指示颜色相关方法
    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
    }

    public String getDisabledColor() {
        return disabledColor;
    }

    public void setDisabledColor(String disabledColor) {
        this.disabledColor = disabledColor;
    }

    public String getSuccessColor() {
        return successColor;
    }

    public void setSuccessColor(String successColor) {
        this.successColor = successColor;
    }

    public String getWarningColor() {
        return warningColor;
    }

    public void setWarningColor(String warningColor) {
        this.warningColor = warningColor;
    }

    public String getErrorColor() {
        return errorColor;
    }

    public void setErrorColor(String errorColor) {
        this.errorColor = errorColor;
    }

    public String getInfoColor() {
        return infoColor;
    }

    public void setInfoColor(String infoColor) {
        this.infoColor = infoColor;
    }

    @Override
    public void saveConfig(Properties props) {
        // 模板信息
        props.setProperty("ui.template.name", this.templateName);
        props.setProperty("ui.template.description", this.templateDescription);
        
        // 背景设置
        props.setProperty("ui.bg.image", this.bgImagePath);
        props.setProperty("ui.bg.color", this.bgColor);
        
        // 主题色
        props.setProperty("ui.accent.color", this.accentColor);
        props.setProperty("ui.accent.light.color", this.accentLightColor);
        props.setProperty("ui.accent.dark.color", this.accentDarkColor);
        
        // 文本颜色分级
        props.setProperty("ui.text.primary", this.textPrimaryColor);
        props.setProperty("ui.text.secondary", this.textSecondaryColor);
        props.setProperty("ui.text.tertiary", this.textTertiaryColor);
        props.setProperty("ui.text.disabled", this.textDisabledColor);
        
        // 透明度和背景模式
        props.setProperty("ui.glass.opacity", String.valueOf(this.glassOpacity));
        props.setProperty("ui.dark.bg", String.valueOf(this.isDarkBackground));
        
        // 基础样式设置
        props.setProperty("ui.corner.radius", String.valueOf(this.cornerRadius));
        props.setProperty("ui.border.color", this.borderColor);
        props.setProperty("ui.border.width", String.valueOf(this.borderWidth));
        
        // 面板样式分组
        props.setProperty("ui.panel.bg.color", this.panelBgColor);
        props.setProperty("ui.panel.border.color", this.panelBorderColor);
        props.setProperty("ui.panel.title.color", this.panelTitleColor);
        props.setProperty("ui.panel.hover.color", this.panelHoverColor);
        
        // 按钮样式分级
        props.setProperty("ui.button.primary.bg", this.buttonPrimaryBgColor);
        props.setProperty("ui.button.primary.text", this.buttonPrimaryTextColor);
        props.setProperty("ui.button.primary.border", this.buttonPrimaryBorderColor);
        props.setProperty("ui.button.primary.hover", this.buttonPrimaryHoverColor);
        props.setProperty("ui.button.primary.pressed", this.buttonPrimaryPressedColor);
        
        props.setProperty("ui.button.secondary.bg", this.buttonSecondaryBgColor);
        props.setProperty("ui.button.secondary.text", this.buttonSecondaryTextColor);
        props.setProperty("ui.button.secondary.border", this.buttonSecondaryBorderColor);
        props.setProperty("ui.button.secondary.hover", this.buttonSecondaryHoverColor);
        props.setProperty("ui.button.secondary.pressed", this.buttonSecondaryPressedColor);
        
        props.setProperty("ui.button.success.bg", this.buttonSuccessBgColor);
        props.setProperty("ui.button.success.text", this.buttonSuccessTextColor);
        props.setProperty("ui.button.success.border", this.buttonSuccessBorderColor);
        props.setProperty("ui.button.success.hover", this.buttonSuccessHoverColor);
        
        props.setProperty("ui.button.warning.bg", this.buttonWarningBgColor);
        props.setProperty("ui.button.warning.text", this.buttonWarningTextColor);
        props.setProperty("ui.button.warning.border", this.buttonWarningBorderColor);
        props.setProperty("ui.button.warning.hover", this.buttonWarningHoverColor);
        
        props.setProperty("ui.button.error.bg", this.buttonErrorBgColor);
        props.setProperty("ui.button.error.text", this.buttonErrorTextColor);
        props.setProperty("ui.button.error.border", this.buttonErrorBorderColor);
        props.setProperty("ui.button.error.hover", this.buttonErrorHoverColor);
        
        // 按钮尺寸设置
        props.setProperty("ui.button.height", String.valueOf(this.buttonHeight));
        props.setProperty("ui.button.large.size", String.valueOf(this.buttonLargeSize));
        props.setProperty("ui.button.small.size", String.valueOf(this.buttonSmallSize));
        props.setProperty("ui.button.icon.size", String.valueOf(this.buttonIconSize));
        
        // 列表样式分组
        props.setProperty("ui.list.bg", this.listBgColor);
        props.setProperty("ui.list.row.even", this.listRowEvenBgColor);
        props.setProperty("ui.list.row.odd", this.listRowOddBgColor);
        props.setProperty("ui.list.row.selected.bg", this.listRowSelectedBgColor);
        props.setProperty("ui.list.row.selected.text", this.listRowSelectedTextColor);
        props.setProperty("ui.list.row.hover", this.listRowHoverBgColor);
        props.setProperty("ui.list.border", this.listBorderColor);
        props.setProperty("ui.list.header.bg", this.listHeaderBgColor);
        props.setProperty("ui.list.header.text", this.listHeaderTextColor);
        
        // 状态颜色
        props.setProperty("ui.hover.color", this.hoverColor);
        props.setProperty("ui.selected.color", this.selectedColor);
        props.setProperty("ui.disabled.color", this.disabledColor);
        
        // 进度条和状态指示颜色
        props.setProperty("ui.progress.bar.color", this.progressBarColor);
        props.setProperty("ui.success.color", this.successColor);
        props.setProperty("ui.warning.color", this.warningColor);
        props.setProperty("ui.error.color", this.errorColor);
        props.setProperty("ui.info.color", this.infoColor);
        
        // 字体设置
        props.setProperty("ui.font.family", this.fontFamily);
        props.setProperty("ui.font.size", String.valueOf(this.fontSize));
        
        // 字体样式分级
        props.setProperty("ui.font.title.family", this.titleFontFamily);
        props.setProperty("ui.font.title.size", String.valueOf(this.titleFontSize));
        props.setProperty("ui.font.title.color", this.titleFontColor);
        
        props.setProperty("ui.font.subtitle.family", this.subtitleFontFamily);
        props.setProperty("ui.font.subtitle.size", String.valueOf(this.subtitleFontSize));
        props.setProperty("ui.font.subtitle.color", this.subtitleFontColor);
        
        props.setProperty("ui.font.button.family", this.buttonFontFamily);
        props.setProperty("ui.font.button.size", String.valueOf(this.buttonFontSize));
        
        props.setProperty("ui.font.description.family", this.descriptionFontFamily);
        props.setProperty("ui.font.description.size", String.valueOf(this.descriptionFontSize));
        props.setProperty("ui.font.description.color", this.descriptionFontColor);
        
        props.setProperty("ui.font.log.family", this.logFontFamily);
        props.setProperty("ui.font.log.size", String.valueOf(this.logFontSize));
        props.setProperty("ui.font.log.color", this.logFontColor);
        
        props.setProperty("ui.font.runtime.info.family", this.runtimeInfoFontFamily);
        props.setProperty("ui.font.runtime.info.size", String.valueOf(this.runtimeInfoFontSize));
        props.setProperty("ui.font.runtime.info.color", this.runtimeInfoFontColor);
        
        // 间距设置
        props.setProperty("ui.spacing.small", String.valueOf(this.smallSpacing));
        props.setProperty("ui.spacing.medium", String.valueOf(this.mediumSpacing));
        props.setProperty("ui.spacing.large", String.valueOf(this.largeSpacing));
        props.setProperty("ui.spacing.extra.large", String.valueOf(this.extraLargeSpacing));
    }

    @Override
    public void reload() {
        // 主题配置的reload方法，主要用于通知依赖组件更新
        // 这里可以留空，因为ThemeConfig本身是配置源
    }

    /**
     * 验证并格式化颜色值，确保它是有效的十六进制格式
     */
    private String validateAndFormatColor(String colorValue, String defaultValue) {
        if (colorValue == null || colorValue.isEmpty()) {
            return defaultValue;
        }
        
        // 移除可能的透明度后缀
        if (colorValue.contains("#") && colorValue.length() > 7) {
            colorValue = colorValue.substring(0, 7);
        }
        
        // 转换0x开头的颜色值
        if (colorValue.startsWith("0x")) {
            try {
                String hex = colorValue.substring(2);
                if (hex.length() == 8) {
                    hex = hex.substring(0, 6); // 移除透明度部分
                }
                return "#" + hex;
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        // 确保颜色值以#开头
        if (!colorValue.startsWith("#")) {
            return "#" + colorValue;
        }
        
        // 确保颜色值有正确的长度
        if (colorValue.length() != 7) {
            return defaultValue;
        }
        
        return colorValue;
    }

    @Override
    public void loadConfig(Properties props) {
        // 模板信息
        String templateName = props.getProperty("ui.template.name");
        if (templateName != null) {
            this.templateName = templateName;
        }
        String templateDesc = props.getProperty("ui.template.description");
        if (templateDesc != null) {
            this.templateDescription = templateDesc;
        }
        
        // 背景设置
        String bgPath = props.getProperty("ui.bg.image");
        if (bgPath != null) {
            this.bgImagePath = bgPath;
        }
        if (props.containsKey("ui.bg.color")) {
            this.bgColor = validateAndFormatColor(props.getProperty("ui.bg.color"), this.bgColor);
        }
        
        // 主题色
        if (props.containsKey("ui.accent.color")) {
            this.accentColor = validateAndFormatColor(props.getProperty("ui.accent.color"), this.accentColor);
        }
        if (props.containsKey("ui.accent.light.color")) {
            this.accentLightColor = validateAndFormatColor(props.getProperty("ui.accent.light.color"), this.accentLightColor);
        }
        if (props.containsKey("ui.accent.dark.color")) {
            this.accentDarkColor = validateAndFormatColor(props.getProperty("ui.accent.dark.color"), this.accentDarkColor);
        }
        
        // 文本颜色分级
        if (props.containsKey("ui.text.primary")) {
            this.textPrimaryColor = validateAndFormatColor(props.getProperty("ui.text.primary"), this.textPrimaryColor);
        }
        if (props.containsKey("ui.text.secondary")) {
            this.textSecondaryColor = validateAndFormatColor(props.getProperty("ui.text.secondary"), this.textSecondaryColor);
        }
        if (props.containsKey("ui.text.tertiary")) {
            this.textTertiaryColor = validateAndFormatColor(props.getProperty("ui.text.tertiary"), this.textTertiaryColor);
        }
        if (props.containsKey("ui.text.disabled")) {
            this.textDisabledColor = validateAndFormatColor(props.getProperty("ui.text.disabled"), this.textDisabledColor);
        }
        
        // 透明度和背景模式
        if (props.containsKey("ui.glass.opacity")) {
            this.glassOpacity = Double.parseDouble(props.getProperty("ui.glass.opacity"));
        }
        if (props.containsKey("ui.dark.bg")) {
            this.isDarkBackground = Boolean.parseBoolean(props.getProperty("ui.dark.bg"));
        }
        
        // 基础样式设置
        if (props.containsKey("ui.corner.radius")) {
            this.cornerRadius = Double.parseDouble(props.getProperty("ui.corner.radius"));
        }
        if (props.containsKey("ui.border.color")) {
            this.borderColor = validateAndFormatColor(props.getProperty("ui.border.color"), this.borderColor);
        }
        if (props.containsKey("ui.border.width")) {
            this.borderWidth = Double.parseDouble(props.getProperty("ui.border.width"));
        }
        
        // 面板样式分组
        if (props.containsKey("ui.panel.bg.color")) {
            this.panelBgColor = validateAndFormatColor(props.getProperty("ui.panel.bg.color"), this.panelBgColor);
        }
        if (props.containsKey("ui.panel.border.color")) {
            this.panelBorderColor = validateAndFormatColor(props.getProperty("ui.panel.border.color"), this.panelBorderColor);
        }
        if (props.containsKey("ui.panel.title.color")) {
            this.panelTitleColor = validateAndFormatColor(props.getProperty("ui.panel.title.color"), this.panelTitleColor);
        }
        if (props.containsKey("ui.panel.hover.color")) {
            this.panelHoverColor = validateAndFormatColor(props.getProperty("ui.panel.hover.color"), this.panelHoverColor);
        }
        
        // 按钮样式分级
        if (props.containsKey("ui.button.primary.bg")) {
            this.buttonPrimaryBgColor = validateAndFormatColor(props.getProperty("ui.button.primary.bg"), this.buttonPrimaryBgColor);
        }
        if (props.containsKey("ui.button.primary.text")) {
            this.buttonPrimaryTextColor = validateAndFormatColor(props.getProperty("ui.button.primary.text"), this.buttonPrimaryTextColor);
        }
        if (props.containsKey("ui.button.primary.border")) {
            this.buttonPrimaryBorderColor = validateAndFormatColor(props.getProperty("ui.button.primary.border"), this.buttonPrimaryBorderColor);
        }
        if (props.containsKey("ui.button.primary.hover")) {
            this.buttonPrimaryHoverColor = validateAndFormatColor(props.getProperty("ui.button.primary.hover"), this.buttonPrimaryHoverColor);
        }
        if (props.containsKey("ui.button.primary.pressed")) {
            this.buttonPrimaryPressedColor = validateAndFormatColor(props.getProperty("ui.button.primary.pressed"), this.buttonPrimaryPressedColor);
        }
        
        if (props.containsKey("ui.button.secondary.bg")) {
            this.buttonSecondaryBgColor = validateAndFormatColor(props.getProperty("ui.button.secondary.bg"), this.buttonSecondaryBgColor);
        }
        if (props.containsKey("ui.button.secondary.text")) {
            this.buttonSecondaryTextColor = validateAndFormatColor(props.getProperty("ui.button.secondary.text"), this.buttonSecondaryTextColor);
        }
        if (props.containsKey("ui.button.secondary.border")) {
            this.buttonSecondaryBorderColor = validateAndFormatColor(props.getProperty("ui.button.secondary.border"), this.buttonSecondaryBorderColor);
        }
        if (props.containsKey("ui.button.secondary.hover")) {
            this.buttonSecondaryHoverColor = validateAndFormatColor(props.getProperty("ui.button.secondary.hover"), this.buttonSecondaryHoverColor);
        }
        if (props.containsKey("ui.button.secondary.pressed")) {
            this.buttonSecondaryPressedColor = validateAndFormatColor(props.getProperty("ui.button.secondary.pressed"), this.buttonSecondaryPressedColor);
        }
        
        if (props.containsKey("ui.button.success.bg")) {
            this.buttonSuccessBgColor = validateAndFormatColor(props.getProperty("ui.button.success.bg"), this.buttonSuccessBgColor);
        }
        if (props.containsKey("ui.button.success.text")) {
            this.buttonSuccessTextColor = validateAndFormatColor(props.getProperty("ui.button.success.text"), this.buttonSuccessTextColor);
        }
        if (props.containsKey("ui.button.success.border")) {
            this.buttonSuccessBorderColor = validateAndFormatColor(props.getProperty("ui.button.success.border"), this.buttonSuccessBorderColor);
        }
        if (props.containsKey("ui.button.success.hover")) {
            this.buttonSuccessHoverColor = validateAndFormatColor(props.getProperty("ui.button.success.hover"), this.buttonSuccessHoverColor);
        }
        
        if (props.containsKey("ui.button.warning.bg")) {
            this.buttonWarningBgColor = validateAndFormatColor(props.getProperty("ui.button.warning.bg"), this.buttonWarningBgColor);
        }
        if (props.containsKey("ui.button.warning.text")) {
            this.buttonWarningTextColor = validateAndFormatColor(props.getProperty("ui.button.warning.text"), this.buttonWarningTextColor);
        }
        if (props.containsKey("ui.button.warning.border")) {
            this.buttonWarningBorderColor = validateAndFormatColor(props.getProperty("ui.button.warning.border"), this.buttonWarningBorderColor);
        }
        if (props.containsKey("ui.button.warning.hover")) {
            this.buttonWarningHoverColor = validateAndFormatColor(props.getProperty("ui.button.warning.hover"), this.buttonWarningHoverColor);
        }
        
        if (props.containsKey("ui.button.error.bg")) {
            this.buttonErrorBgColor = validateAndFormatColor(props.getProperty("ui.button.error.bg"), this.buttonErrorBgColor);
        }
        if (props.containsKey("ui.button.error.text")) {
            this.buttonErrorTextColor = validateAndFormatColor(props.getProperty("ui.button.error.text"), this.buttonErrorTextColor);
        }
        if (props.containsKey("ui.button.error.border")) {
            this.buttonErrorBorderColor = validateAndFormatColor(props.getProperty("ui.button.error.border"), this.buttonErrorBorderColor);
        }
        if (props.containsKey("ui.button.error.hover")) {
            this.buttonErrorHoverColor = validateAndFormatColor(props.getProperty("ui.button.error.hover"), this.buttonErrorHoverColor);
        }
        
        // 按钮尺寸设置
        if (props.containsKey("ui.button.height")) {
            this.buttonHeight = Double.parseDouble(props.getProperty("ui.button.height"));
        }
        if (props.containsKey("ui.button.large.size")) {
            this.buttonLargeSize = Double.parseDouble(props.getProperty("ui.button.large.size"));
        }
        if (props.containsKey("ui.button.small.size")) {
            this.buttonSmallSize = Double.parseDouble(props.getProperty("ui.button.small.size"));
        }
        if (props.containsKey("ui.button.icon.size")) {
            this.buttonIconSize = Double.parseDouble(props.getProperty("ui.button.icon.size"));
        }
        
        // 列表样式分组
        if (props.containsKey("ui.list.bg")) {
            this.listBgColor = validateAndFormatColor(props.getProperty("ui.list.bg"), this.listBgColor);
        }
        if (props.containsKey("ui.list.row.even")) {
            this.listRowEvenBgColor = validateAndFormatColor(props.getProperty("ui.list.row.even"), this.listRowEvenBgColor);
        }
        if (props.containsKey("ui.list.row.odd")) {
            this.listRowOddBgColor = validateAndFormatColor(props.getProperty("ui.list.row.odd"), this.listRowOddBgColor);
        }
        if (props.containsKey("ui.list.row.selected.bg")) {
            this.listRowSelectedBgColor = validateAndFormatColor(props.getProperty("ui.list.row.selected.bg"), this.listRowSelectedBgColor);
        }
        if (props.containsKey("ui.list.row.selected.text")) {
            this.listRowSelectedTextColor = validateAndFormatColor(props.getProperty("ui.list.row.selected.text"), this.listRowSelectedTextColor);
        }
        if (props.containsKey("ui.list.row.hover")) {
            this.listRowHoverBgColor = validateAndFormatColor(props.getProperty("ui.list.row.hover"), this.listRowHoverBgColor);
        }
        if (props.containsKey("ui.list.border")) {
            this.listBorderColor = validateAndFormatColor(props.getProperty("ui.list.border"), this.listBorderColor);
        }
        if (props.containsKey("ui.list.header.bg")) {
            this.listHeaderBgColor = validateAndFormatColor(props.getProperty("ui.list.header.bg"), this.listHeaderBgColor);
        }
        if (props.containsKey("ui.list.header.text")) {
            this.listHeaderTextColor = validateAndFormatColor(props.getProperty("ui.list.header.text"), this.listHeaderTextColor);
        }
        
        // 状态颜色
        if (props.containsKey("ui.hover.color")) {
            this.hoverColor = validateAndFormatColor(props.getProperty("ui.hover.color"), this.hoverColor);
        }
        if (props.containsKey("ui.selected.color")) {
            this.selectedColor = validateAndFormatColor(props.getProperty("ui.selected.color"), this.selectedColor);
        }
        if (props.containsKey("ui.disabled.color")) {
            this.disabledColor = validateAndFormatColor(props.getProperty("ui.disabled.color"), this.disabledColor);
        }
        
        // 进度条和状态指示颜色
        if (props.containsKey("ui.progress.bar.color")) {
            this.progressBarColor = validateAndFormatColor(props.getProperty("ui.progress.bar.color"), this.progressBarColor);
        }
        if (props.containsKey("ui.success.color")) {
            this.successColor = validateAndFormatColor(props.getProperty("ui.success.color"), this.successColor);
        }
        if (props.containsKey("ui.warning.color")) {
            this.warningColor = validateAndFormatColor(props.getProperty("ui.warning.color"), this.warningColor);
        }
        if (props.containsKey("ui.error.color")) {
            this.errorColor = validateAndFormatColor(props.getProperty("ui.error.color"), this.errorColor);
        }
        if (props.containsKey("ui.info.color")) {
            this.infoColor = validateAndFormatColor(props.getProperty("ui.info.color"), this.infoColor);
        }
        
        // 字体设置
        if (props.containsKey("ui.font.family")) {
            this.fontFamily = props.getProperty("ui.font.family");
        }
        if (props.containsKey("ui.font.size")) {
            this.fontSize = Double.parseDouble(props.getProperty("ui.font.size"));
        }
        
        // 字体样式分级
        if (props.containsKey("ui.font.title.family")) {
            this.titleFontFamily = props.getProperty("ui.font.title.family");
        }
        if (props.containsKey("ui.font.title.size")) {
            this.titleFontSize = Double.parseDouble(props.getProperty("ui.font.title.size"));
        }
        if (props.containsKey("ui.font.title.color")) {
            this.titleFontColor = props.getProperty("ui.font.title.color");
        }
        
        if (props.containsKey("ui.font.subtitle.family")) {
            this.subtitleFontFamily = props.getProperty("ui.font.subtitle.family");
        }
        if (props.containsKey("ui.font.subtitle.size")) {
            this.subtitleFontSize = Double.parseDouble(props.getProperty("ui.font.subtitle.size"));
        }
        if (props.containsKey("ui.font.subtitle.color")) {
            this.subtitleFontColor = props.getProperty("ui.font.subtitle.color");
        }
        
        if (props.containsKey("ui.font.button.family")) {
            this.buttonFontFamily = props.getProperty("ui.font.button.family");
        }
        if (props.containsKey("ui.font.button.size")) {
            this.buttonFontSize = Double.parseDouble(props.getProperty("ui.font.button.size"));
        }
        
        if (props.containsKey("ui.font.description.family")) {
            this.descriptionFontFamily = props.getProperty("ui.font.description.family");
        }
        if (props.containsKey("ui.font.description.size")) {
            this.descriptionFontSize = Double.parseDouble(props.getProperty("ui.font.description.size"));
        }
        if (props.containsKey("ui.font.description.color")) {
            this.descriptionFontColor = props.getProperty("ui.font.description.color");
        }
        
        if (props.containsKey("ui.font.log.family")) {
            this.logFontFamily = props.getProperty("ui.font.log.family");
        }
        if (props.containsKey("ui.font.log.size")) {
            this.logFontSize = Double.parseDouble(props.getProperty("ui.font.log.size"));
        }
        if (props.containsKey("ui.font.log.color")) {
            this.logFontColor = props.getProperty("ui.font.log.color");
        }
        
        if (props.containsKey("ui.font.runtime.info.family")) {
            this.runtimeInfoFontFamily = props.getProperty("ui.font.runtime.info.family");
        }
        if (props.containsKey("ui.font.runtime.info.size")) {
            this.runtimeInfoFontSize = Double.parseDouble(props.getProperty("ui.font.runtime.info.size"));
        }
        if (props.containsKey("ui.font.runtime.info.color")) {
            this.runtimeInfoFontColor = props.getProperty("ui.font.runtime.info.color");
        }
        
        // 间距设置
        if (props.containsKey("ui.spacing.small")) {
            this.smallSpacing = Double.parseDouble(props.getProperty("ui.spacing.small"));
        }
        if (props.containsKey("ui.spacing.medium")) {
            this.mediumSpacing = Double.parseDouble(props.getProperty("ui.spacing.medium"));
        }
        if (props.containsKey("ui.spacing.large")) {
            this.largeSpacing = Double.parseDouble(props.getProperty("ui.spacing.large"));
        }
        if (props.containsKey("ui.spacing.extra.large")) {
            this.extraLargeSpacing = Double.parseDouble(props.getProperty("ui.spacing.extra.large"));
        }
    }
}