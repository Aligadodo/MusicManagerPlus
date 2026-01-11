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

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 主题管理器
 * 负责管理主题配置和预设，提供主题切换功能
 */
public class ThemeManager {
    private static final ThemeManager INSTANCE = new ThemeManager();
    
    @Getter
    private ThemeConfig currentTheme;
    
    private final Map<String, ThemeConfig> themePresets;
    private final CopyOnWriteArrayList<Consumer<ThemeConfig>> themeChangeListeners;
    
    private ThemeManager() {
        themePresets = new HashMap<>();
        themeChangeListeners = new CopyOnWriteArrayList<>();
        
        // 初始化内置主题预设
        initBuiltInThemes();
        
        // 默认使用浅色主题
        currentTheme = themePresets.get("light").clone();
    }
    
    /**
     * 获取单例实例
     */
    public static ThemeManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 初始化内置主题
     */
    private void initBuiltInThemes() {
        // 浅色主题
        ThemeConfig lightTheme = new ThemeConfig();
        lightTheme.setBgColor("#f5f5f5");
        lightTheme.setAccentColor("#3498db");
        lightTheme.setTextColor("#333333");
        lightTheme.setLightTextColor("#666666");
        lightTheme.setDisabledTextColor("#999999");
        lightTheme.setGlassOpacity(0.65);
        lightTheme.setDarkBackground(false);
        lightTheme.setCornerRadius(5.0);
        lightTheme.setBorderColor("#e0e0e0");
        lightTheme.setPanelBgColor("#ffffff");
        lightTheme.setPanelBorderColor("#e0e0e0");
        lightTheme.setHoverColor("#f0f0f0");
        lightTheme.setSelectedColor("#e3f2fd");
        lightTheme.setDisabledColor("#f5f5f5");
        lightTheme.setProgressBarColor("#27ae60");
        themePresets.put("light", lightTheme);
        
        // 深色主题
        ThemeConfig darkTheme = new ThemeConfig();
        darkTheme.setBgColor("#2c3e50");
        darkTheme.setAccentColor("#3498db");
        darkTheme.setTextColor("#ecf0f1");
        darkTheme.setLightTextColor("#bdc3c7");
        darkTheme.setDisabledTextColor("#7f8c8d");
        darkTheme.setGlassOpacity(0.85);
        darkTheme.setDarkBackground(true);
        darkTheme.setCornerRadius(5.0);
        darkTheme.setBorderColor("#34495e");
        darkTheme.setPanelBgColor("#34495e");
        darkTheme.setPanelBorderColor("#2c3e50");
        darkTheme.setHoverColor("#34495e");
        darkTheme.setSelectedColor("#4a6fa5");
        darkTheme.setDisabledColor("#2c3e50");
        darkTheme.setProgressBarColor("#27ae60");
        themePresets.put("dark", darkTheme);
        
        // 蓝色主题
        ThemeConfig blueTheme = new ThemeConfig();
        blueTheme.setBgColor("#e3f2fd");
        blueTheme.setAccentColor("#1976d2");
        blueTheme.setTextColor("#212121");
        blueTheme.setLightTextColor("#757575");
        blueTheme.setDisabledTextColor("#bdbdbd");
        blueTheme.setGlassOpacity(0.70);
        blueTheme.setDarkBackground(false);
        blueTheme.setCornerRadius(5.0);
        blueTheme.setBorderColor("#bbdefb");
        blueTheme.setPanelBgColor("#ffffff");
        blueTheme.setPanelBorderColor("#bbdefb");
        blueTheme.setHoverColor("#bbdefb");
        blueTheme.setSelectedColor("#90caf9");
        blueTheme.setDisabledColor("#f5f5f5");
        blueTheme.setProgressBarColor("#1976d2");
        themePresets.put("blue", blueTheme);
        
        // 绿色主题
        ThemeConfig greenTheme = new ThemeConfig();
        greenTheme.setBgColor("#e8f5e9");
        greenTheme.setAccentColor("#2e7d32");
        greenTheme.setTextColor("#212121");
        greenTheme.setLightTextColor("#757575");
        greenTheme.setDisabledTextColor("#bdbdbd");
        greenTheme.setGlassOpacity(0.70);
        greenTheme.setDarkBackground(false);
        greenTheme.setCornerRadius(5.0);
        greenTheme.setBorderColor("#c8e6c9");
        greenTheme.setPanelBgColor("#ffffff");
        greenTheme.setPanelBorderColor("#c8e6c9");
        greenTheme.setHoverColor("#c8e6c9");
        greenTheme.setSelectedColor("#a5d6a7");
        greenTheme.setDisabledColor("#f5f5f5");
        greenTheme.setProgressBarColor("#2e7d32");
        themePresets.put("green", greenTheme);
    }
    
    /**
     * 获取所有主题预设
     */
    public Map<String, ThemeConfig> getThemePresets() {
        return new HashMap<>(themePresets);
    }
    
    /**
     * 切换到预设主题
     */
    public void switchToTheme(String themeName) {
        ThemeConfig preset = themePresets.get(themeName);
        if (preset != null) {
            currentTheme = preset.clone();
            notifyThemeChange();
        }
    }
    
    /**
     * 更新当前主题
     */
    public void updateCurrentTheme(Consumer<ThemeConfig> updater) {
        updater.accept(currentTheme);
        notifyThemeChange();
    }
    
    /**
     * 加载主题配置
     */
    public void loadTheme(Properties props) {
        currentTheme.loadConfig(props);
        notifyThemeChange();
    }
    
    /**
     * 保存主题配置
     */
    public void saveTheme(Properties props) {
        currentTheme.saveConfig(props);
    }
    
    /**
     * 添加主题变更监听器
     */
    public void addThemeChangeListener(Consumer<ThemeConfig> listener) {
        themeChangeListeners.add(listener);
    }
    
    /**
     * 移除主题变更监听器
     */
    public void removeThemeChangeListener(Consumer<ThemeConfig> listener) {
        themeChangeListeners.remove(listener);
    }
    
    /**
     * 通知所有监听器主题已变更
     */
    private void notifyThemeChange() {
        for (Consumer<ThemeConfig> listener : themeChangeListeners) {
            listener.accept(currentTheme.clone());
        }
    }
    
    /**
     * 创建自定义主题
     */
    public void createCustomTheme(String name, ThemeConfig theme) {
        themePresets.put(name, theme.clone());
    }
    
    /**
     * 删除自定义主题
     */
    public void deleteCustomTheme(String name) {
        // 不能删除内置主题
        if (!name.equals("light") && !name.equals("dark") && !name.equals("blue") && !name.equals("green")) {
            themePresets.remove(name);
        }
    }
}