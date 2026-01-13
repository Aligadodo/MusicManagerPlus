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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

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
        lightTheme.setTextPrimaryColor("#333333");
        lightTheme.setTextSecondaryColor("#666666");
        lightTheme.setTextDisabledColor("#999999");
        lightTheme.setGlassOpacity(0.65);
        lightTheme.setDarkBackground(false);
        lightTheme.setCornerRadius(8.0);
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
        darkTheme.setTextPrimaryColor("#ecf0f1");
        darkTheme.setTextSecondaryColor("#bdc3c7");
        darkTheme.setTextDisabledColor("#7f8c8d");
        darkTheme.setGlassOpacity(0.85);
        darkTheme.setDarkBackground(true);
        darkTheme.setCornerRadius(8.0);
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
        blueTheme.setTextPrimaryColor("#212121");
        blueTheme.setTextSecondaryColor("#757575");
        blueTheme.setTextDisabledColor("#bdbdbd");
        blueTheme.setGlassOpacity(0.70);
        blueTheme.setDarkBackground(false);
        blueTheme.setCornerRadius(8.0);
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
        greenTheme.setTextPrimaryColor("#212121");
        greenTheme.setTextSecondaryColor("#757575");
        greenTheme.setTextDisabledColor("#bdbdbd");
        greenTheme.setGlassOpacity(0.70);
        greenTheme.setDarkBackground(false);
        greenTheme.setCornerRadius(8.0);
        greenTheme.setBorderColor("#c8e6c9");
        greenTheme.setPanelBgColor("#ffffff");
        greenTheme.setPanelBorderColor("#c8e6c9");
        greenTheme.setHoverColor("#c8e6c9");
        greenTheme.setSelectedColor("#a5d6a7");
        greenTheme.setDisabledColor("#f5f5f5");
        greenTheme.setProgressBarColor("#2e7d32");
        themePresets.put("green", greenTheme);
        
        // 网易云音乐 - 深色主题
        ThemeConfig neteaseDarkTheme = new ThemeConfig();
        neteaseDarkTheme.setBgColor("#18191c");
        neteaseDarkTheme.setAccentColor("#e8220e");
        neteaseDarkTheme.setTextPrimaryColor("#ffffff");
        neteaseDarkTheme.setTextSecondaryColor("#b3b3b3");
        neteaseDarkTheme.setTextDisabledColor("#666666");
        neteaseDarkTheme.setGlassOpacity(0.90);
        neteaseDarkTheme.setDarkBackground(true);
        neteaseDarkTheme.setCornerRadius(12.0);
        neteaseDarkTheme.setBorderColor("#2d2d30");
        neteaseDarkTheme.setPanelBgColor("#212224");
        neteaseDarkTheme.setPanelBorderColor("#2d2d30");
        neteaseDarkTheme.setHoverColor("#2d2d30");
        neteaseDarkTheme.setSelectedColor("#3a3a3d");
        neteaseDarkTheme.setDisabledColor("#18191c");
        neteaseDarkTheme.setProgressBarColor("#e8220e");
        themePresets.put("netease_dark", neteaseDarkTheme);
        
        // 网易云音乐 - 浅色主题
        ThemeConfig neteaseLightTheme = new ThemeConfig();
        neteaseLightTheme.setBgColor("#f7f7f7");
        neteaseLightTheme.setAccentColor("#e8220e");
        neteaseLightTheme.setTextPrimaryColor("#333333");
        neteaseLightTheme.setTextSecondaryColor("#666666");
        neteaseLightTheme.setTextDisabledColor("#999999");
        neteaseLightTheme.setGlassOpacity(0.80);
        neteaseLightTheme.setDarkBackground(false);
        neteaseLightTheme.setCornerRadius(12.0);
        neteaseLightTheme.setBorderColor("#e0e0e0");
        neteaseLightTheme.setPanelBgColor("#ffffff");
        neteaseLightTheme.setPanelBorderColor("#e0e0e0");
        neteaseLightTheme.setHoverColor("#f0f0f0");
        neteaseLightTheme.setSelectedColor("#ffebee");
        neteaseLightTheme.setDisabledColor("#f5f5f5");
        neteaseLightTheme.setProgressBarColor("#e8220e");
        themePresets.put("netease_light", neteaseLightTheme);
        
        // 网易云音乐 - 粉色主题
        ThemeConfig neteasePinkTheme = new ThemeConfig();
        neteasePinkTheme.setBgColor("#fce4ec");
        neteasePinkTheme.setAccentColor("#ec407a");
        neteasePinkTheme.setTextPrimaryColor("#333333");
        neteasePinkTheme.setTextSecondaryColor("#757575");
        neteasePinkTheme.setTextDisabledColor("#bdbdbd");
        neteasePinkTheme.setGlassOpacity(0.75);
        neteasePinkTheme.setDarkBackground(false);
        neteasePinkTheme.setCornerRadius(12.0);
        neteasePinkTheme.setBorderColor("#f8bbd0");
        neteasePinkTheme.setPanelBgColor("#ffffff");
        neteasePinkTheme.setPanelBorderColor("#f8bbd0");
        neteasePinkTheme.setHoverColor("#f8bbd0");
        neteasePinkTheme.setSelectedColor("#f48fb1");
        neteasePinkTheme.setDisabledColor("#f5f5f5");
        neteasePinkTheme.setProgressBarColor("#ec407a");
        themePresets.put("netease_pink", neteasePinkTheme);
        
        // 网易云音乐 - 蓝色主题
        ThemeConfig neteaseBlueTheme = new ThemeConfig();
        neteaseBlueTheme.setBgColor("#e3f2fd");
        neteaseBlueTheme.setAccentColor("#2196f3");
        neteaseBlueTheme.setTextPrimaryColor("#333333");
        neteaseBlueTheme.setTextSecondaryColor("#757575");
        neteaseBlueTheme.setTextDisabledColor("#bdbdbd");
        neteaseBlueTheme.setGlassOpacity(0.75);
        neteaseBlueTheme.setDarkBackground(false);
        neteaseBlueTheme.setCornerRadius(12.0);
        neteaseBlueTheme.setBorderColor("#bbdefb");
        neteaseBlueTheme.setPanelBgColor("#ffffff");
        neteaseBlueTheme.setPanelBorderColor("#bbdefb");
        neteaseBlueTheme.setHoverColor("#bbdefb");
        neteaseBlueTheme.setSelectedColor("#90caf9");
        neteaseBlueTheme.setDisabledColor("#f5f5f5");
        neteaseBlueTheme.setProgressBarColor("#2196f3");
        themePresets.put("netease_blue", neteaseBlueTheme);
        
        // 紫色主题
        ThemeConfig purpleTheme = new ThemeConfig();
        purpleTheme.setBgColor("#f3e5f5");
        purpleTheme.setAccentColor("#9c27b0");
        purpleTheme.setTextPrimaryColor("#333333");
        purpleTheme.setTextSecondaryColor("#757575");
        purpleTheme.setTextDisabledColor("#bdbdbd");
        purpleTheme.setGlassOpacity(0.75);
        purpleTheme.setDarkBackground(false);
        purpleTheme.setCornerRadius(10.0);
        purpleTheme.setBorderColor("#e1bee7");
        purpleTheme.setPanelBgColor("#ffffff");
        purpleTheme.setPanelBorderColor("#e1bee7");
        purpleTheme.setHoverColor("#e1bee7");
        purpleTheme.setSelectedColor("#ce93d8");
        purpleTheme.setDisabledColor("#f5f5f5");
        purpleTheme.setProgressBarColor("#9c27b0");
        themePresets.put("purple", purpleTheme);
        
        // 橙色主题
        ThemeConfig orangeTheme = new ThemeConfig();
        orangeTheme.setBgColor("#fff3e0");
        orangeTheme.setAccentColor("#ff9800");
        orangeTheme.setTextPrimaryColor("#333333");
        orangeTheme.setTextSecondaryColor("#757575");
        orangeTheme.setTextDisabledColor("#bdbdbd");
        orangeTheme.setGlassOpacity(0.75);
        orangeTheme.setDarkBackground(false);
        orangeTheme.setCornerRadius(10.0);
        orangeTheme.setBorderColor("#ffe0b2");
        orangeTheme.setPanelBgColor("#ffffff");
        orangeTheme.setPanelBorderColor("#ffe0b2");
        orangeTheme.setHoverColor("#ffe0b2");
        orangeTheme.setSelectedColor("#ffcc80");
        orangeTheme.setDisabledColor("#f5f5f5");
        orangeTheme.setProgressBarColor("#ff9800");
        themePresets.put("orange", orangeTheme);
        
        // 薄荷绿主题
        ThemeConfig mintTheme = new ThemeConfig();
        mintTheme.setBgColor("#e0f7fa");
        mintTheme.setAccentColor("#00bcd4");
        mintTheme.setTextPrimaryColor("#333333");
        mintTheme.setTextSecondaryColor("#757575");
        mintTheme.setTextDisabledColor("#bdbdbd");
        mintTheme.setGlassOpacity(0.75);
        mintTheme.setDarkBackground(false);
        mintTheme.setCornerRadius(10.0);
        mintTheme.setBorderColor("#b2ebf2");
        mintTheme.setPanelBgColor("#ffffff");
        mintTheme.setPanelBorderColor("#b2ebf2");
        mintTheme.setHoverColor("#b2ebf2");
        mintTheme.setSelectedColor("#80deea");
        mintTheme.setDisabledColor("#f5f5f5");
        mintTheme.setProgressBarColor("#00bcd4");
        themePresets.put("mint", mintTheme);
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
     * 获取所有主题预设的名称
     */
    public List<String> getThemePresetNames() {
        return new ArrayList<>(themePresets.keySet());
    }
    
    /**
     * 获取当前主题的名称
     */
    public String getCurrentThemeName() {
        // 查找当前主题对应的预设名称
        for (Map.Entry<String, ThemeConfig> entry : themePresets.entrySet()) {
            if (entry.getValue().equals(currentTheme)) {
                return entry.getKey();
            }
        }
        return "custom";
    }
    
    /**
     * 判断是否为内置主题
     */
    public boolean isBuiltInTheme(String name) {
        // 内置主题包括所有初始加载的主题
        return name.equals("light") || name.equals("dark") || name.equals("blue") || 
               name.equals("green") || name.startsWith("netease_") || 
               name.equals("purple") || name.equals("orange") || name.equals("mint");
    }
    
    /**
     * 删除自定义主题
     */
    public void deleteCustomTheme(String name) {
        // 不能删除内置主题
        if (!isBuiltInTheme(name)) {
            themePresets.remove(name);
        }
    }
    
    /**
     * 保存主题到文件
     */
    public void saveThemeToFile(File file, ThemeConfig theme) {
        // 确保style目录存在
        createStyleDirIfNotExists();
        
        try {
            String json = JSON.toJSONString(theme, SerializerFeature.PrettyFormat);
            Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 从文件加载主题
     */
    public ThemeConfig loadThemeFromFile(File file) {
        try {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return JSON.parseObject(json, ThemeConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 如果style目录不存在则创建
     */
    private void createStyleDirIfNotExists() {
        File styleDir = new File("style");
        if (!styleDir.exists()) {
            styleDir.mkdir();
        }
        
        File settingDir = new File("setting");
        if (!settingDir.exists()) {
            settingDir.mkdir();
        }
    }
}