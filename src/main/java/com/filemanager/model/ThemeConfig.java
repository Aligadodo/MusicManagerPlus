package com.filemanager.model;

import com.filemanager.base.IAutoReloadAble;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Properties;

/**
 * 界面主题配置模型
 * 存储颜色、透明度、圆角等外观参数
 * @author 28667
 */
@Data
@NoArgsConstructor
public class ThemeConfig implements Cloneable, IAutoReloadAble {
    // 背景图片
    private String bgImagePath = "";
    
    // 主题色 (默认 Win7 Blue)
    private String accentColor = "#3498db";
    
    // 字体颜色 (默认深灰色，适配浅色玻璃背景)
    private String textColor = "#333333";
    
    // 毛玻璃透明度 (0.0 - 1.0, 值越小越透明)
    private double glassOpacity = 0.65;
    
    // 是否启用深色背景模式
    private boolean isDarkBackground = false;
    
    // 组件圆角半径
    private double cornerRadius = 5.0;

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
        props.setProperty("ui.accent.color", this.getAccentColor());
        props.setProperty("ui.text.color", this.getTextColor());
        props.setProperty("ui.glass.opacity", String.valueOf(this.getGlassOpacity()));
        props.setProperty("ui.dark.bg", String.valueOf(this.isDarkBackground()));
        props.setProperty("ui.bg.image", this.getBgImagePath());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("ui.accent.color")) {
            this.setAccentColor(props.getProperty("ui.accent.color"));
        }
        if (props.containsKey("ui.text.color")) {
            this.setTextColor(props.getProperty("ui.text.color"));
        }
        if (props.containsKey("ui.glass.opacity")) {
            this.setGlassOpacity(Double.parseDouble(props.getProperty("ui.glass.opacity")));
        }
        if (props.containsKey("ui.dark.bg")) {
            this.setDarkBackground(Boolean.parseBoolean(props.getProperty("ui.dark.bg")));
        }
        String bgPath = props.getProperty("ui.bg.image");
        if (bgPath != null) {
            this.setBgImagePath(bgPath);
        }
    }
}