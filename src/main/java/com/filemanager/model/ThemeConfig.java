package com.filemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 界面主题配置模型
 * 存储颜色、透明度、圆角等外观参数
 */
@Data
@NoArgsConstructor
public class ThemeConfig implements Cloneable {
    
    // 主题色 (默认 Win7 Blue)
    private String accentColor = "#3498db";
    
    // 字体颜色 (默认深灰色，适配浅色玻璃背景)
    private String textColor = "#333333";
    
    // 毛玻璃透明度 (0.0 - 1.0, 值越小越透明)
    private double glassOpacity = 0.65;
    
    // 是否启用深色背景模式
    private boolean isDarkBackground = false;
    
    // 组件圆角半径
    private double cornerRadius = 10.0;

    @Override
    public ThemeConfig clone() {
        try {
            return (ThemeConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            return new ThemeConfig();
        }
    }
}