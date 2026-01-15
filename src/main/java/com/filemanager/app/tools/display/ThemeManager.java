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
        // 从文件加载主题
        loadThemesFromDirectory();
        
        // 如果没有加载到任何主题，创建默认主题作为后备
        if (themePresets.isEmpty()) {
            createDefaultFallbackThemes();
        }
    }
    
    /**
     * 从主题目录加载所有主题
     */
    private void loadThemesFromDirectory() {
        createStyleDirIfNotExists();
        
        // 尝试从多个位置加载主题文件
        boolean loaded = false;
        
        // 1. 首先尝试从src/main/resources目录加载（IDE环境）
        loaded = loadThemesFromPath("src/main/resources/style/themes");
        
        // 2. 如果没有加载到，尝试从classpath加载（打包后的环境）
        if (!loaded) {
            loaded = loadThemesFromClasspath();
        }
        
        // 3. 如果还是没有加载到，创建默认主题目录
        if (!loaded) {
            File themesDir = new File("style/themes");
            themesDir.mkdir();
        }
    }
    
    /**
     * 从指定路径加载主题
     */
    private boolean loadThemesFromPath(String path) {
        File themesDir = new File(path);
        
        // 添加调试信息，输出当前工作目录和主题目录的绝对路径
        System.out.println("Current working directory: " + System.getProperty("user.dir"));
        System.out.println("Themes directory path: " + themesDir.getAbsolutePath());
        System.out.println("Themes directory exists: " + themesDir.exists());
        System.out.println("Themes directory is directory: " + themesDir.isDirectory());
        
        if (!themesDir.exists() || !themesDir.isDirectory()) {
            return false;
        }
        
        // 获取目录下所有JSON文件
        File[] jsonFiles = themesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        
        // 添加调试信息
        if (jsonFiles == null) {
            System.out.println("Error listing JSON files: listFiles returned null");
            return false;
        } else {
            System.out.println("Found " + jsonFiles.length + " JSON files in themes directory");
            for (File file : jsonFiles) {
                System.out.println("Found JSON file: " + file.getName() + ", exists: " + file.exists() + ", readable: " + file.canRead());
            }
        }
        
        if (jsonFiles.length == 0) {
            return false;
        }
        
        // 加载每个主题文件
        for (File file : jsonFiles) {
            try {
                System.out.println("Attempting to load theme from file: " + file.getAbsolutePath());
                ThemeConfig theme = loadThemeFromFile(file);
                if (theme != null) {
                    // 使用文件名作为主题ID（去除.json后缀）
                    String themeId = file.getName().replace(".json", "");
                    themePresets.put(themeId, theme);
                    System.out.println("Successfully loaded theme: " + themeId);
                } else {
                    System.out.println("Failed to load theme from file: " + file.getName() + ", loadThemeFromFile returned null");
                }
            } catch (Exception e) {
                System.err.println("Exception loading theme from file: " + file.getName() + ", error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return !themePresets.isEmpty();
    }
    
    /**
     * 从classpath加载主题
     */
    private boolean loadThemesFromClasspath() {
        try {
            // 获取当前类的ClassLoader
            ClassLoader classLoader = getClass().getClassLoader();
            
            // 尝试获取classpath下的style/themes目录
            java.net.URL resource = classLoader.getResource("style/themes");
            if (resource != null) {
                File themesDir = new File(resource.toURI());
                return loadThemesFromPath(themesDir.getAbsolutePath());
            }
            
            // 如果直接获取目录失败，尝试列出所有主题文件
            java.util.Enumeration<java.net.URL> themeResources = classLoader.getResources("style/themes/");
            while (themeResources.hasMoreElements()) {
                java.net.URL url = themeResources.nextElement();
                if (url.getProtocol().equals("file")) {
                    File themesDir = new File(url.toURI());
                    if (loadThemesFromPath(themesDir.getAbsolutePath())) {
                        return true;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to load themes from classpath: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 创建默认后备主题
     * 当无法从文件加载主题时使用
     */
    private void createDefaultFallbackThemes() {
        // 浅色主题作为默认后备
        ThemeConfig lightTheme = new ThemeConfig();
        lightTheme.setTemplateName("浅色主题（默认）");
        lightTheme.setTemplateDescription("默认的浅色主题");
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
        
        // 深色主题作为第二个后备
        ThemeConfig darkTheme = new ThemeConfig();
        darkTheme.setTemplateName("深色主题（默认）");
        darkTheme.setTemplateDescription("默认的深色主题");
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
        
        // 设置深色主题的列表样式
        darkTheme.setListBgColor("#2c3e50");
        darkTheme.setListRowEvenBgColor("#2c3e50");
        darkTheme.setListRowOddBgColor("#34495e");
        darkTheme.setListRowSelectedBgColor("#4a6fa5");
        darkTheme.setListRowSelectedTextColor("#ecf0f1");
        darkTheme.setListRowHoverBgColor("#34495e");
        darkTheme.setListBorderColor("#2c3e50");
        darkTheme.setListHeaderBgColor("#34495e");
        darkTheme.setListHeaderTextColor("#ecf0f1");
        
        themePresets.put("dark", darkTheme);
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
        // 内置主题是从主题目录加载的主题文件
        // 检查是否存在对应的主题文件
        
        // 1. 首先检查项目根目录（IDE环境）
        File themeFile = new File(String.format("style/themes/%s.json", name));
        if (themeFile.exists()) {
            return true;
        }
        
        // 2. 然后检查classpath（打包后的环境）
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            java.net.URL resource = classLoader.getResource(String.format("style/themes/%s.json", name));
            return resource != null;
        } catch (Exception e) {
            return false;
        }
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
        File styleDir = new File("style/themes");
        if (!styleDir.exists()) {
            styleDir.mkdirs();
        }
        
        File settingDir = new File("setting");
        if (!settingDir.exists()) {
            settingDir.mkdir();
        }
    }
}