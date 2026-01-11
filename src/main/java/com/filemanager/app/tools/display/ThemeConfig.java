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

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Properties;

import com.filemanager.app.base.IAutoReloadAble;

/**
 * 界面主题配置模型
 * 存储颜色、透明度、圆角等外观参数
 * @author 28667
 */
@Data
@NoArgsConstructor
public class ThemeConfig implements Cloneable, IAutoReloadAble {
    // 背景设置
    private String bgImagePath = "";
    private String bgColor = "#f5f5f5";
    
    // 主题色 (默认 Win7 Blue)
    private String accentColor = "#3498db";
    
    // 文本颜色
    private String textColor = "#333333";
    private String lightTextColor = "#666666";
    private String disabledTextColor = "#999999";
    
    // 毛玻璃透明度 (0.0 - 1.0, 值越小越透明)
    private double glassOpacity = 0.65;
    
    // 是否启用深色背景模式
    private boolean isDarkBackground = false;
    
    // 组件圆角半径
    private double cornerRadius = 5.0;
    
    // 边框设置
    private String borderColor = "#e0e0e0";
    private double borderWidth = 1.0;
    
    // 面板设置
    private String panelBgColor = "#ffffff";
    private String panelBorderColor = "#e0e0e0";
    
    // 状态颜色
    private String hoverColor = "#f0f0f0";
    private String selectedColor = "#e3f2fd";
    private String disabledColor = "#f5f5f5";
    
    // 进度条颜色
    private String progressBarColor = "#27ae60";
    
    // 状态指示颜色
    private String successColor = "#27ae60";
    private String warningColor = "#f39c12";
    private String errorColor = "#e74c3c";
    private String infoColor = "#3498db";
    
    // 字体设置
    private String fontFamily = "Segoe UI, Arial, sans-serif";
    private double fontSize = 14.0;
    
    // 间距设置
    private double smallSpacing = 5.0;
    private double mediumSpacing = 10.0;
    private double largeSpacing = 15.0;

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
        props.setProperty("ui.bg.image", this.getBgImagePath());
        props.setProperty("ui.bg.color", this.getBgColor());
        props.setProperty("ui.accent.color", this.getAccentColor());
        props.setProperty("ui.text.color", this.getTextColor());
        props.setProperty("ui.light.text.color", this.getLightTextColor());
        props.setProperty("ui.disabled.text.color", this.getDisabledTextColor());
        props.setProperty("ui.glass.opacity", String.valueOf(this.getGlassOpacity()));
        props.setProperty("ui.dark.bg", String.valueOf(this.isDarkBackground()));
        props.setProperty("ui.corner.radius", String.valueOf(this.getCornerRadius()));
        props.setProperty("ui.border.color", this.getBorderColor());
        props.setProperty("ui.border.width", String.valueOf(this.getBorderWidth()));
        props.setProperty("ui.panel.bg.color", this.getPanelBgColor());
        props.setProperty("ui.panel.border.color", this.getPanelBorderColor());
        props.setProperty("ui.hover.color", this.getHoverColor());
        props.setProperty("ui.selected.color", this.getSelectedColor());
        props.setProperty("ui.disabled.color", this.getDisabledColor());
        props.setProperty("ui.progress.bar.color", this.getProgressBarColor());
        props.setProperty("ui.success.color", this.getSuccessColor());
        props.setProperty("ui.warning.color", this.getWarningColor());
        props.setProperty("ui.error.color", this.getErrorColor());
        props.setProperty("ui.info.color", this.getInfoColor());
        props.setProperty("ui.font.family", this.getFontFamily());
        props.setProperty("ui.font.size", String.valueOf(this.getFontSize()));
        props.setProperty("ui.small.spacing", String.valueOf(this.getSmallSpacing()));
        props.setProperty("ui.medium.spacing", String.valueOf(this.getMediumSpacing()));
        props.setProperty("ui.large.spacing", String.valueOf(this.getLargeSpacing()));
    }

    @Override
    public void loadConfig(Properties props) {
        // 背景设置
        String bgPath = props.getProperty("ui.bg.image");
        if (bgPath != null) {
            this.setBgImagePath(bgPath);
        }
        if (props.containsKey("ui.bg.color")) {
            this.setBgColor(props.getProperty("ui.bg.color"));
        }
        
        // 颜色设置
        if (props.containsKey("ui.accent.color")) {
            this.setAccentColor(props.getProperty("ui.accent.color"));
        }
        if (props.containsKey("ui.text.color")) {
            this.setTextColor(props.getProperty("ui.text.color"));
        }
        if (props.containsKey("ui.light.text.color")) {
            this.setLightTextColor(props.getProperty("ui.light.text.color"));
        }
        if (props.containsKey("ui.disabled.text.color")) {
            this.setDisabledTextColor(props.getProperty("ui.disabled.text.color"));
        }
        
        // 透明度和背景模式
        if (props.containsKey("ui.glass.opacity")) {
            this.setGlassOpacity(Double.parseDouble(props.getProperty("ui.glass.opacity")));
        }
        if (props.containsKey("ui.dark.bg")) {
            this.setDarkBackground(Boolean.parseBoolean(props.getProperty("ui.dark.bg")));
        }
        
        // 圆角和边框
        if (props.containsKey("ui.corner.radius")) {
            this.setCornerRadius(Double.parseDouble(props.getProperty("ui.corner.radius")));
        }
        if (props.containsKey("ui.border.color")) {
            this.setBorderColor(props.getProperty("ui.border.color"));
        }
        if (props.containsKey("ui.border.width")) {
            this.setBorderWidth(Double.parseDouble(props.getProperty("ui.border.width")));
        }
        
        // 面板设置
        if (props.containsKey("ui.panel.bg.color")) {
            this.setPanelBgColor(props.getProperty("ui.panel.bg.color"));
        }
        if (props.containsKey("ui.panel.border.color")) {
            this.setPanelBorderColor(props.getProperty("ui.panel.border.color"));
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
        
        // 字体和间距
        if (props.containsKey("ui.font.family")) {
            this.setFontFamily(props.getProperty("ui.font.family"));
        }
        if (props.containsKey("ui.font.size")) {
            this.setFontSize(Double.parseDouble(props.getProperty("ui.font.size")));
        }
        if (props.containsKey("ui.small.spacing")) {
            this.setSmallSpacing(Double.parseDouble(props.getProperty("ui.small.spacing")));
        }
        if (props.containsKey("ui.medium.spacing")) {
            this.setMediumSpacing(Double.parseDouble(props.getProperty("ui.medium.spacing")));
        }
        if (props.containsKey("ui.large.spacing")) {
            this.setLargeSpacing(Double.parseDouble(props.getProperty("ui.large.spacing")));
        }
    }
}