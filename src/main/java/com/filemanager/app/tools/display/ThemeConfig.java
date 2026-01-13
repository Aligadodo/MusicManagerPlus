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

    @Override
    public void saveConfig(Properties props) {
        // 模板信息
        props.setProperty("ui.template.name", this.getTemplateName());
        props.setProperty("ui.template.description", this.getTemplateDescription());
        
        // 背景设置
        props.setProperty("ui.bg.image", this.getBgImagePath());
        props.setProperty("ui.bg.color", this.getBgColor());
        
        // 主题色
        props.setProperty("ui.accent.color", this.getAccentColor());
        props.setProperty("ui.accent.light.color", this.getAccentLightColor());
        props.setProperty("ui.accent.dark.color", this.getAccentDarkColor());
        
        // 文本颜色分级
        props.setProperty("ui.text.primary", this.getTextPrimaryColor());
        props.setProperty("ui.text.secondary", this.getTextSecondaryColor());
        props.setProperty("ui.text.tertiary", this.getTextTertiaryColor());
        props.setProperty("ui.text.disabled", this.getTextDisabledColor());
        
        // 透明度和背景模式
        props.setProperty("ui.glass.opacity", String.valueOf(this.getGlassOpacity()));
        props.setProperty("ui.dark.bg", String.valueOf(this.isDarkBackground()));
        
        // 基础样式设置
        props.setProperty("ui.corner.radius", String.valueOf(this.getCornerRadius()));
        props.setProperty("ui.border.color", this.getBorderColor());
        props.setProperty("ui.border.width", String.valueOf(this.getBorderWidth()));
        
        // 面板样式分组
        props.setProperty("ui.panel.bg.color", this.getPanelBgColor());
        props.setProperty("ui.panel.border.color", this.getPanelBorderColor());
        props.setProperty("ui.panel.title.color", this.getPanelTitleColor());
        props.setProperty("ui.panel.hover.color", this.getPanelHoverColor());
        
        // 按钮样式分级
        props.setProperty("ui.button.primary.bg", this.getButtonPrimaryBgColor());
        props.setProperty("ui.button.primary.text", this.getButtonPrimaryTextColor());
        props.setProperty("ui.button.primary.border", this.getButtonPrimaryBorderColor());
        props.setProperty("ui.button.primary.hover", this.getButtonPrimaryHoverColor());
        props.setProperty("ui.button.primary.pressed", this.getButtonPrimaryPressedColor());
        
        props.setProperty("ui.button.secondary.bg", this.getButtonSecondaryBgColor());
        props.setProperty("ui.button.secondary.text", this.getButtonSecondaryTextColor());
        props.setProperty("ui.button.secondary.border", this.getButtonSecondaryBorderColor());
        props.setProperty("ui.button.secondary.hover", this.getButtonSecondaryHoverColor());
        props.setProperty("ui.button.secondary.pressed", this.getButtonSecondaryPressedColor());
        
        props.setProperty("ui.button.success.bg", this.getButtonSuccessBgColor());
        props.setProperty("ui.button.success.text", this.getButtonSuccessTextColor());
        props.setProperty("ui.button.success.border", this.getButtonSuccessBorderColor());
        props.setProperty("ui.button.success.hover", this.getButtonSuccessHoverColor());
        
        props.setProperty("ui.button.warning.bg", this.getButtonWarningBgColor());
        props.setProperty("ui.button.warning.text", this.getButtonWarningTextColor());
        props.setProperty("ui.button.warning.border", this.getButtonWarningBorderColor());
        props.setProperty("ui.button.warning.hover", this.getButtonWarningHoverColor());
        
        props.setProperty("ui.button.error.bg", this.getButtonErrorBgColor());
        props.setProperty("ui.button.error.text", this.getButtonErrorTextColor());
        props.setProperty("ui.button.error.border", this.getButtonErrorBorderColor());
        props.setProperty("ui.button.error.hover", this.getButtonErrorHoverColor());
        
        // 按钮尺寸设置
        props.setProperty("ui.button.height", String.valueOf(this.getButtonHeight()));
        props.setProperty("ui.button.large.size", String.valueOf(this.getButtonLargeSize()));
        props.setProperty("ui.button.small.size", String.valueOf(this.getButtonSmallSize()));
        props.setProperty("ui.button.icon.size", String.valueOf(this.getButtonIconSize()));
        
        // 列表样式分组
        props.setProperty("ui.list.bg", this.getListBgColor());
        props.setProperty("ui.list.row.even", this.getListRowEvenBgColor());
        props.setProperty("ui.list.row.odd", this.getListRowOddBgColor());
        props.setProperty("ui.list.row.selected.bg", this.getListRowSelectedBgColor());
        props.setProperty("ui.list.row.selected.text", this.getListRowSelectedTextColor());
        props.setProperty("ui.list.row.hover", this.getListRowHoverBgColor());
        props.setProperty("ui.list.border", this.getListBorderColor());
        props.setProperty("ui.list.header.bg", this.getListHeaderBgColor());
        props.setProperty("ui.list.header.text", this.getListHeaderTextColor());
        
        // 状态颜色
        props.setProperty("ui.hover.color", this.getHoverColor());
        props.setProperty("ui.selected.color", this.getSelectedColor());
        props.setProperty("ui.disabled.color", this.getDisabledColor());
        
        // 进度条和状态指示颜色
        props.setProperty("ui.progress.bar.color", this.getProgressBarColor());
        props.setProperty("ui.success.color", this.getSuccessColor());
        props.setProperty("ui.warning.color", this.getWarningColor());
        props.setProperty("ui.error.color", this.getErrorColor());
        props.setProperty("ui.info.color", this.getInfoColor());
        
        // 字体设置
        props.setProperty("ui.font.family", this.getFontFamily());
        props.setProperty("ui.font.size", String.valueOf(this.getFontSize()));
        
        // 字体样式分级
        props.setProperty("ui.font.title.family", this.getTitleFontFamily());
        props.setProperty("ui.font.title.size", String.valueOf(this.getTitleFontSize()));
        props.setProperty("ui.font.title.color", this.getTitleFontColor());
        
        props.setProperty("ui.font.subtitle.family", this.getSubtitleFontFamily());
        props.setProperty("ui.font.subtitle.size", String.valueOf(this.getSubtitleFontSize()));
        props.setProperty("ui.font.subtitle.color", this.getSubtitleFontColor());
        
        props.setProperty("ui.font.button.family", this.getButtonFontFamily());
        props.setProperty("ui.font.button.size", String.valueOf(this.getButtonFontSize()));
        
        props.setProperty("ui.font.description.family", this.getDescriptionFontFamily());
        props.setProperty("ui.font.description.size", String.valueOf(this.getDescriptionFontSize()));
        props.setProperty("ui.font.description.color", this.getDescriptionFontColor());
        
        props.setProperty("ui.font.log.family", this.getLogFontFamily());
        props.setProperty("ui.font.log.size", String.valueOf(this.getLogFontSize()));
        props.setProperty("ui.font.log.color", this.getLogFontColor());
        
        props.setProperty("ui.font.runtime.info.family", this.getRuntimeInfoFontFamily());
        props.setProperty("ui.font.runtime.info.size", String.valueOf(this.getRuntimeInfoFontSize()));
        props.setProperty("ui.font.runtime.info.color", this.getRuntimeInfoFontColor());
        
        // 间距设置
        props.setProperty("ui.spacing.small", String.valueOf(this.getSmallSpacing()));
        props.setProperty("ui.spacing.medium", String.valueOf(this.getMediumSpacing()));
        props.setProperty("ui.spacing.large", String.valueOf(this.getLargeSpacing()));
        props.setProperty("ui.spacing.extra.large", String.valueOf(this.getExtraLargeSpacing()));
    }

    @Override
    public void loadConfig(Properties props) {
        // 模板信息
        String templateName = props.getProperty("ui.template.name");
        if (templateName != null) {
            this.setTemplateName(templateName);
        }
        String templateDesc = props.getProperty("ui.template.description");
        if (templateDesc != null) {
            this.setTemplateDescription(templateDesc);
        }
        
        // 背景设置
        String bgPath = props.getProperty("ui.bg.image");
        if (bgPath != null) {
            this.setBgImagePath(bgPath);
        }
        if (props.containsKey("ui.bg.color")) {
            this.setBgColor(props.getProperty("ui.bg.color"));
        }
        
        // 主题色
        if (props.containsKey("ui.accent.color")) {
            this.setAccentColor(props.getProperty("ui.accent.color"));
        }
        if (props.containsKey("ui.accent.light.color")) {
            this.setAccentLightColor(props.getProperty("ui.accent.light.color"));
        }
        if (props.containsKey("ui.accent.dark.color")) {
            this.setAccentDarkColor(props.getProperty("ui.accent.dark.color"));
        }
        
        // 文本颜色分级
        if (props.containsKey("ui.text.primary")) {
            this.setTextPrimaryColor(props.getProperty("ui.text.primary"));
        }
        if (props.containsKey("ui.text.secondary")) {
            this.setTextSecondaryColor(props.getProperty("ui.text.secondary"));
        }
        if (props.containsKey("ui.text.tertiary")) {
            this.setTextTertiaryColor(props.getProperty("ui.text.tertiary"));
        }
        if (props.containsKey("ui.text.disabled")) {
            this.setTextDisabledColor(props.getProperty("ui.text.disabled"));
        }
        
        // 透明度和背景模式
        if (props.containsKey("ui.glass.opacity")) {
            this.setGlassOpacity(Double.parseDouble(props.getProperty("ui.glass.opacity")));
        }
        if (props.containsKey("ui.dark.bg")) {
            this.setDarkBackground(Boolean.parseBoolean(props.getProperty("ui.dark.bg")));
        }
        
        // 基础样式设置
        if (props.containsKey("ui.corner.radius")) {
            this.setCornerRadius(Double.parseDouble(props.getProperty("ui.corner.radius")));
        }
        if (props.containsKey("ui.border.color")) {
            this.setBorderColor(props.getProperty("ui.border.color"));
        }
        if (props.containsKey("ui.border.width")) {
            this.setBorderWidth(Double.parseDouble(props.getProperty("ui.border.width")));
        }
        
        // 面板样式分组
        if (props.containsKey("ui.panel.bg.color")) {
            this.setPanelBgColor(props.getProperty("ui.panel.bg.color"));
        }
        if (props.containsKey("ui.panel.border.color")) {
            this.setPanelBorderColor(props.getProperty("ui.panel.border.color"));
        }
        if (props.containsKey("ui.panel.title.color")) {
            this.setPanelTitleColor(props.getProperty("ui.panel.title.color"));
        }
        if (props.containsKey("ui.panel.hover.color")) {
            this.setPanelHoverColor(props.getProperty("ui.panel.hover.color"));
        }
        
        // 按钮样式分级
        if (props.containsKey("ui.button.primary.bg")) {
            this.setButtonPrimaryBgColor(props.getProperty("ui.button.primary.bg"));
        }
        if (props.containsKey("ui.button.primary.text")) {
            this.setButtonPrimaryTextColor(props.getProperty("ui.button.primary.text"));
        }
        if (props.containsKey("ui.button.primary.border")) {
            this.setButtonPrimaryBorderColor(props.getProperty("ui.button.primary.border"));
        }
        if (props.containsKey("ui.button.primary.hover")) {
            this.setButtonPrimaryHoverColor(props.getProperty("ui.button.primary.hover"));
        }
        if (props.containsKey("ui.button.primary.pressed")) {
            this.setButtonPrimaryPressedColor(props.getProperty("ui.button.primary.pressed"));
        }
        
        if (props.containsKey("ui.button.secondary.bg")) {
            this.setButtonSecondaryBgColor(props.getProperty("ui.button.secondary.bg"));
        }
        if (props.containsKey("ui.button.secondary.text")) {
            this.setButtonSecondaryTextColor(props.getProperty("ui.button.secondary.text"));
        }
        if (props.containsKey("ui.button.secondary.border")) {
            this.setButtonSecondaryBorderColor(props.getProperty("ui.button.secondary.border"));
        }
        if (props.containsKey("ui.button.secondary.hover")) {
            this.setButtonSecondaryHoverColor(props.getProperty("ui.button.secondary.hover"));
        }
        if (props.containsKey("ui.button.secondary.pressed")) {
            this.setButtonSecondaryPressedColor(props.getProperty("ui.button.secondary.pressed"));
        }
        
        if (props.containsKey("ui.button.success.bg")) {
            this.setButtonSuccessBgColor(props.getProperty("ui.button.success.bg"));
        }
        if (props.containsKey("ui.button.success.text")) {
            this.setButtonSuccessTextColor(props.getProperty("ui.button.success.text"));
        }
        if (props.containsKey("ui.button.success.border")) {
            this.setButtonSuccessBorderColor(props.getProperty("ui.button.success.border"));
        }
        if (props.containsKey("ui.button.success.hover")) {
            this.setButtonSuccessHoverColor(props.getProperty("ui.button.success.hover"));
        }
        
        if (props.containsKey("ui.button.warning.bg")) {
            this.setButtonWarningBgColor(props.getProperty("ui.button.warning.bg"));
        }
        if (props.containsKey("ui.button.warning.text")) {
            this.setButtonWarningTextColor(props.getProperty("ui.button.warning.text"));
        }
        if (props.containsKey("ui.button.warning.border")) {
            this.setButtonWarningBorderColor(props.getProperty("ui.button.warning.border"));
        }
        if (props.containsKey("ui.button.warning.hover")) {
            this.setButtonWarningHoverColor(props.getProperty("ui.button.warning.hover"));
        }
        
        if (props.containsKey("ui.button.error.bg")) {
            this.setButtonErrorBgColor(props.getProperty("ui.button.error.bg"));
        }
        if (props.containsKey("ui.button.error.text")) {
            this.setButtonErrorTextColor(props.getProperty("ui.button.error.text"));
        }
        if (props.containsKey("ui.button.error.border")) {
            this.setButtonErrorBorderColor(props.getProperty("ui.button.error.border"));
        }
        if (props.containsKey("ui.button.error.hover")) {
            this.setButtonErrorHoverColor(props.getProperty("ui.button.error.hover"));
        }
        
        // 按钮尺寸设置
        if (props.containsKey("ui.button.height")) {
            this.setButtonHeight(Double.parseDouble(props.getProperty("ui.button.height")));
        }
        if (props.containsKey("ui.button.large.size")) {
            this.setButtonLargeSize(Double.parseDouble(props.getProperty("ui.button.large.size")));
        }
        if (props.containsKey("ui.button.small.size")) {
            this.setButtonSmallSize(Double.parseDouble(props.getProperty("ui.button.small.size")));
        }
        if (props.containsKey("ui.button.icon.size")) {
            this.setButtonIconSize(Double.parseDouble(props.getProperty("ui.button.icon.size")));
        }
        
        // 列表样式分组
        if (props.containsKey("ui.list.bg")) {
            this.setListBgColor(props.getProperty("ui.list.bg"));
        }
        if (props.containsKey("ui.list.row.even")) {
            this.setListRowEvenBgColor(props.getProperty("ui.list.row.even"));
        }
        if (props.containsKey("ui.list.row.odd")) {
            this.setListRowOddBgColor(props.getProperty("ui.list.row.odd"));
        }
        if (props.containsKey("ui.list.row.selected.bg")) {
            this.setListRowSelectedBgColor(props.getProperty("ui.list.row.selected.bg"));
        }
        if (props.containsKey("ui.list.row.selected.text")) {
            this.setListRowSelectedTextColor(props.getProperty("ui.list.row.selected.text"));
        }
        if (props.containsKey("ui.list.row.hover")) {
            this.setListRowHoverBgColor(props.getProperty("ui.list.row.hover"));
        }
        if (props.containsKey("ui.list.border")) {
            this.setListBorderColor(props.getProperty("ui.list.border"));
        }
        if (props.containsKey("ui.list.header.bg")) {
            this.setListHeaderBgColor(props.getProperty("ui.list.header.bg"));
        }
        if (props.containsKey("ui.list.header.text")) {
            this.setListHeaderTextColor(props.getProperty("ui.list.header.text"));
        }
        
        // 状态颜色
        if (props.containsKey("ui.hover.color")) {
            this.setHoverColor(props.getProperty("ui.hover.color"));
        }
        if (props.containsKey("ui.selected.color")) {
            this.setSelectedColor(props.getProperty("ui.selected.color"));
        }
        if (props.containsKey("ui.disabled.color")) {
            this.setDisabledColor(props.getProperty("ui.disabled.color"));
        }
        
        // 进度条和状态指示颜色
        if (props.containsKey("ui.progress.bar.color")) {
            this.setProgressBarColor(props.getProperty("ui.progress.bar.color"));
        }
        if (props.containsKey("ui.success.color")) {
            this.setSuccessColor(props.getProperty("ui.success.color"));
        }
        if (props.containsKey("ui.warning.color")) {
            this.setWarningColor(props.getProperty("ui.warning.color"));
        }
        if (props.containsKey("ui.error.color")) {
            this.setErrorColor(props.getProperty("ui.error.color"));
        }
        if (props.containsKey("ui.info.color")) {
            this.setInfoColor(props.getProperty("ui.info.color"));
        }
        
        // 字体设置
        if (props.containsKey("ui.font.family")) {
            this.setFontFamily(props.getProperty("ui.font.family"));
        }
        if (props.containsKey("ui.font.size")) {
            this.setFontSize(Double.parseDouble(props.getProperty("ui.font.size")));
        }
        
        // 字体样式分级
        if (props.containsKey("ui.font.title.family")) {
            this.setTitleFontFamily(props.getProperty("ui.font.title.family"));
        }
        if (props.containsKey("ui.font.title.size")) {
            this.setTitleFontSize(Double.parseDouble(props.getProperty("ui.font.title.size")));
        }
        if (props.containsKey("ui.font.title.color")) {
            this.setTitleFontColor(props.getProperty("ui.font.title.color"));
        }
        
        if (props.containsKey("ui.font.subtitle.family")) {
            this.setSubtitleFontFamily(props.getProperty("ui.font.subtitle.family"));
        }
        if (props.containsKey("ui.font.subtitle.size")) {
            this.setSubtitleFontSize(Double.parseDouble(props.getProperty("ui.font.subtitle.size")));
        }
        if (props.containsKey("ui.font.subtitle.color")) {
            this.setSubtitleFontColor(props.getProperty("ui.font.subtitle.color"));
        }
        
        if (props.containsKey("ui.font.button.family")) {
            this.setButtonFontFamily(props.getProperty("ui.font.button.family"));
        }
        if (props.containsKey("ui.font.button.size")) {
            this.setButtonFontSize(Double.parseDouble(props.getProperty("ui.font.button.size")));
        }
        
        if (props.containsKey("ui.font.description.family")) {
            this.setDescriptionFontFamily(props.getProperty("ui.font.description.family"));
        }
        if (props.containsKey("ui.font.description.size")) {
            this.setDescriptionFontSize(Double.parseDouble(props.getProperty("ui.font.description.size")));
        }
        if (props.containsKey("ui.font.description.color")) {
            this.setDescriptionFontColor(props.getProperty("ui.font.description.color"));
        }
        
        if (props.containsKey("ui.font.log.family")) {
            this.setLogFontFamily(props.getProperty("ui.font.log.family"));
        }
        if (props.containsKey("ui.font.log.size")) {
            this.setLogFontSize(Double.parseDouble(props.getProperty("ui.font.log.size")));
        }
        if (props.containsKey("ui.font.log.color")) {
            this.setLogFontColor(props.getProperty("ui.font.log.color"));
        }
        
        if (props.containsKey("ui.font.runtime.info.family")) {
            this.setRuntimeInfoFontFamily(props.getProperty("ui.font.runtime.info.family"));
        }
        if (props.containsKey("ui.font.runtime.info.size")) {
            this.setRuntimeInfoFontSize(Double.parseDouble(props.getProperty("ui.font.runtime.info.size")));
        }
        if (props.containsKey("ui.font.runtime.info.color")) {
            this.setRuntimeInfoFontColor(props.getProperty("ui.font.runtime.info.color"));
        }
        
        // 间距设置
        if (props.containsKey("ui.spacing.small")) {
            this.setSmallSpacing(Double.parseDouble(props.getProperty("ui.spacing.small")));
        }
        if (props.containsKey("ui.spacing.medium")) {
            this.setMediumSpacing(Double.parseDouble(props.getProperty("ui.spacing.medium")));
        }
        if (props.containsKey("ui.spacing.large")) {
            this.setLargeSpacing(Double.parseDouble(props.getProperty("ui.spacing.large")));
        }
        if (props.containsKey("ui.spacing.extra.large")) {
            this.setExtraLargeSpacing(Double.parseDouble(props.getProperty("ui.spacing.extra.large")));
        }
    }
}