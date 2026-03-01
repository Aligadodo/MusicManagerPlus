package com.filemanager.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一配置管理器
 * 负责管理所有配置参数，实现配置的持久化和缓存
 */
@Component
public class ConfigManager {

    private static final String CONFIG_FILE_NAME = "config.json";
    private static String CONFIG_FILE_PATH;
    
    static {
        // 获取项目根目录（backend目录的父目录）
        String backendDir = System.getProperty("user.dir");
        File backendDirFile = new File(backendDir);
        
        // 如果当前目录是backend目录，则使用父目录作为项目根目录
        if (backendDirFile.getName().equals("backend")) {
            CONFIG_FILE_PATH = new File(backendDirFile.getParent(), CONFIG_FILE_NAME).getAbsolutePath();
        } else {
            // 否则使用当前目录下的config.json
            CONFIG_FILE_PATH = new File(backendDir, CONFIG_FILE_NAME).getAbsolutePath();
        }
        
        System.out.println("[ConfigManager] 配置文件路径: " + CONFIG_FILE_PATH);
    }
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 配置缓存
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    // 线程池配置键
    public static final String KEY_PREVIEW_THREADS = "previewThreads";
    public static final String KEY_EXECUTION_THREADS = "executionThreads";
    public static final String KEY_THREAD_POOL_MODE = "threadPoolMode";

    // 运行配置键
    public static final String KEY_AUTO_REFRESH = "autoRefresh";
    public static final String KEY_PREVIEW_LIMIT = "previewLimit";
    public static final String KEY_EXECUTION_LIMIT = "executionLimit";

    // 扫描配置键
    public static final String KEY_RECURSION_MODE = "recursionMode";
    public static final String KEY_RECURSION_DEPTH = "recursionDepth";
    public static final String KEY_MIN_RECURSION_DEPTH = "minRecursionDepth";
    public static final String KEY_MAX_RECURSION_DEPTH = "maxRecursionDepth";

    // 过滤规则键
    public static final String KEY_SCAN_FILTER_LIST = "scanFilterList";

    // 文件类型筛选键
    public static final String KEY_FILE_TYPE_TREE = "fileTypeTree";
    public static final String KEY_CUSTOM_FILE_TYPES = "customFileTypes";

    // 主题配置键
    public static final String KEY_THEME_CONFIG = "themeConfig";
    public static final String KEY_THEME_PRESETS = "themePresets";

    // 默认值
    private static final Map<String, Object> DEFAULT_CONFIG = new HashMap<>();

    static {
        // 线程池配置默认值
        DEFAULT_CONFIG.put(KEY_PREVIEW_THREADS, 4);
        DEFAULT_CONFIG.put(KEY_EXECUTION_THREADS, 8);
        DEFAULT_CONFIG.put(KEY_THREAD_POOL_MODE, "GLOBAL");

        // 运行配置默认值
        DEFAULT_CONFIG.put(KEY_AUTO_REFRESH, true);
        DEFAULT_CONFIG.put(KEY_PREVIEW_LIMIT, 200);
        DEFAULT_CONFIG.put(KEY_EXECUTION_LIMIT, 1000);

        // 扫描配置默认值
        DEFAULT_CONFIG.put(KEY_RECURSION_MODE, "ALL");
        DEFAULT_CONFIG.put(KEY_RECURSION_DEPTH, 3);
        DEFAULT_CONFIG.put(KEY_MIN_RECURSION_DEPTH, 1);
        DEFAULT_CONFIG.put(KEY_MAX_RECURSION_DEPTH, 3);

        // 过滤规则默认值
        DEFAULT_CONFIG.put(KEY_SCAN_FILTER_LIST, new String[]{
            "*Convert*", "*Split*", "*System*", "*trash*", "*Temp*", "*tmp*", "*cache*", "*backup*"
        });

        // 文件类型筛选默认值
        DEFAULT_CONFIG.put(KEY_FILE_TYPE_TREE, new HashMap<>());
        DEFAULT_CONFIG.put(KEY_CUSTOM_FILE_TYPES, new String[]{});

        // 主题配置默认值
        Map<String, Object> defaultThemeConfig = new HashMap<>();
        defaultThemeConfig.put("theme", "light");
        defaultThemeConfig.put("bgColor", "#F8F9FA");
        defaultThemeConfig.put("accentColor", "#3498DB");
        defaultThemeConfig.put("textPrimaryColor", "#2C3E50");
        defaultThemeConfig.put("textSecondaryColor", "#7F8C8D");
        defaultThemeConfig.put("textTertiaryColor", "#95A5A6");
        defaultThemeConfig.put("textDisabledColor", "#BDC3C7");
        defaultThemeConfig.put("panelBgColor", "#FFFFFF");
        defaultThemeConfig.put("listBgColor", "#FFFFFF");
        defaultThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        defaultThemeConfig.put("listRowOddBgColor", "#F5F7FA");
        defaultThemeConfig.put("listRowSelectedBgColor", "#E3F2FD");
        defaultThemeConfig.put("listRowSelectedTextColor", "#2196F3");
        defaultThemeConfig.put("listRowHoverBgColor", "#F0F8FF");
        defaultThemeConfig.put("listBorderColor", "#E1E4E8");
        defaultThemeConfig.put("listHeaderBgColor", "#F8F9FA");
        defaultThemeConfig.put("listHeaderTextColor", "#586069");
        defaultThemeConfig.put("borderColor", "#E1E4E8");
        defaultThemeConfig.put("glassOpacity", 0.8);
        defaultThemeConfig.put("darkBackground", false);
        defaultThemeConfig.put("fontFamily", "Roboto");
        defaultThemeConfig.put("fontSize", 14);
        defaultThemeConfig.put("cornerRadius", 6);
        defaultThemeConfig.put("borderWidth", 1);
        defaultThemeConfig.put("buttonLargeSize", 48);
        defaultThemeConfig.put("buttonSmallSize", 36);
        DEFAULT_CONFIG.put(KEY_THEME_CONFIG, defaultThemeConfig);

        // 主题预设默认值
        List<Map<String, Object>> defaultThemePresets = new ArrayList<>();
        
        // 浅色主题预设
        Map<String, Object> lightPreset = new HashMap<>();
        lightPreset.put("name", "浅色主题");
        lightPreset.put("description", "现代、干净的浅色主题");
        lightPreset.put("config", defaultThemeConfig);
        defaultThemePresets.add(lightPreset);
        
        // 深色主题预设
        Map<String, Object> darkThemeConfig = new HashMap<>(defaultThemeConfig);
        darkThemeConfig.put("theme", "dark");
        darkThemeConfig.put("bgColor", "#1E1E1E");
        darkThemeConfig.put("accentColor", "#4A90E2");
        darkThemeConfig.put("textPrimaryColor", "#E0E0E0");
        darkThemeConfig.put("textSecondaryColor", "#A0A0A0");
        darkThemeConfig.put("textTertiaryColor", "#707070");
        darkThemeConfig.put("textDisabledColor", "#505050");
        darkThemeConfig.put("panelBgColor", "#252526");
        darkThemeConfig.put("listBgColor", "#2D2D30");
        darkThemeConfig.put("listRowEvenBgColor", "#2D2D30");
        darkThemeConfig.put("listRowOddBgColor", "#333333");
        darkThemeConfig.put("listRowSelectedBgColor", "#0E47A1");
        darkThemeConfig.put("listRowSelectedTextColor", "#FFFFFF");
        darkThemeConfig.put("listRowHoverBgColor", "#3C3C3C");
        darkThemeConfig.put("listBorderColor", "#3E3E42");
        darkThemeConfig.put("listHeaderBgColor", "#252526");
        darkThemeConfig.put("listHeaderTextColor", "#CCCCCC");
        darkThemeConfig.put("borderColor", "#3E3E42");
        darkThemeConfig.put("darkBackground", true);
        
        Map<String, Object> darkPreset = new HashMap<>();
        darkPreset.put("name", "深色主题");
        darkPreset.put("description", "优雅、舒适的深色主题");
        darkPreset.put("config", darkThemeConfig);
        defaultThemePresets.add(darkPreset);
        
        // 蓝色主题预设
        Map<String, Object> blueThemeConfig = new HashMap<>(defaultThemeConfig);
        blueThemeConfig.put("theme", "blue");
        blueThemeConfig.put("bgColor", "#F0F4F8");
        blueThemeConfig.put("accentColor", "#1E88E5");
        blueThemeConfig.put("panelBgColor", "#FFFFFF");
        blueThemeConfig.put("listRowSelectedBgColor", "#E3F2FD");
        blueThemeConfig.put("listRowSelectedTextColor", "#1976D2");
        blueThemeConfig.put("listRowHoverBgColor", "#E3F2FD");
        
        Map<String, Object> bluePreset = new HashMap<>();
        bluePreset.put("name", "蓝色主题");
        bluePreset.put("description", "清新、专业的蓝色主题");
        bluePreset.put("config", blueThemeConfig);
        defaultThemePresets.add(bluePreset);
        
        // 绿色主题预设
        Map<String, Object> greenThemeConfig = new HashMap<>(defaultThemeConfig);
        greenThemeConfig.put("theme", "green");
        greenThemeConfig.put("bgColor", "#F1F8E9");
        greenThemeConfig.put("accentColor", "#4CAF50");
        greenThemeConfig.put("panelBgColor", "#FFFFFF");
        greenThemeConfig.put("listRowSelectedBgColor", "#E8F5E8");
        greenThemeConfig.put("listRowSelectedTextColor", "#2E7D32");
        greenThemeConfig.put("listRowHoverBgColor", "#E8F5E8");
        
        Map<String, Object> greenPreset = new HashMap<>();
        greenPreset.put("name", "绿色主题");
        greenPreset.put("description", "自然、健康的绿色主题");
        greenPreset.put("config", greenThemeConfig);
        defaultThemePresets.add(greenPreset);
        
        // 紫色主题预设
        Map<String, Object> purpleThemeConfig = new HashMap<>(defaultThemeConfig);
        purpleThemeConfig.put("theme", "purple");
        purpleThemeConfig.put("bgColor", "#F3E5F5");
        purpleThemeConfig.put("accentColor", "#9C27B0");
        purpleThemeConfig.put("panelBgColor", "#FFFFFF");
        purpleThemeConfig.put("listRowSelectedBgColor", "#F3E5F5");
        purpleThemeConfig.put("listRowSelectedTextColor", "#7B1FA2");
        purpleThemeConfig.put("listRowHoverBgColor", "#F3E5F5");
        
        Map<String, Object> purplePreset = new HashMap<>();
        purplePreset.put("name", "紫色主题");
        purplePreset.put("description", "优雅、神秘的紫色主题");
        purplePreset.put("config", purpleThemeConfig);
        defaultThemePresets.add(purplePreset);
        
        // 橙色主题预设
        Map<String, Object> orangeThemeConfig = new HashMap<>(defaultThemeConfig);
        orangeThemeConfig.put("theme", "orange");
        orangeThemeConfig.put("bgColor", "#FFF3E0");
        orangeThemeConfig.put("accentColor", "#FF9800");
        orangeThemeConfig.put("panelBgColor", "#FFFFFF");
        orangeThemeConfig.put("listRowSelectedBgColor", "#FFF3E0");
        orangeThemeConfig.put("listRowSelectedTextColor", "#E65100");
        orangeThemeConfig.put("listRowHoverBgColor", "#FFF3E0");
        
        Map<String, Object> orangePreset = new HashMap<>();
        orangePreset.put("name", "橙色主题");
        orangePreset.put("description", "活力、温暖的橙色主题");
        orangePreset.put("config", orangeThemeConfig);
        defaultThemePresets.add(orangePreset);
        
        // 梵高星空主题 - 基于梵高的《星空》
        Map<String, Object> vanGoghThemeConfig = new HashMap<>(defaultThemeConfig);
        vanGoghThemeConfig.put("theme", "vangogh");
        vanGoghThemeConfig.put("bgColor", "#0B1026");
        vanGoghThemeConfig.put("accentColor", "#F4D03F");
        vanGoghThemeConfig.put("textPrimaryColor", "#F8F9FA");
        vanGoghThemeConfig.put("textSecondaryColor", "#B0BEC5");
        vanGoghThemeConfig.put("textTertiaryColor", "#78909C");
        vanGoghThemeConfig.put("textDisabledColor", "#546E7A");
        vanGoghThemeConfig.put("panelBgColor", "#1A237E");
        vanGoghThemeConfig.put("listBgColor", "#1A237E");
        vanGoghThemeConfig.put("listRowEvenBgColor", "#1A237E");
        vanGoghThemeConfig.put("listRowOddBgColor", "#283593");
        vanGoghThemeConfig.put("listRowSelectedBgColor", "#F4D03F");
        vanGoghThemeConfig.put("listRowSelectedTextColor", "#0B1026");
        vanGoghThemeConfig.put("listRowHoverBgColor", "#3949AB");
        vanGoghThemeConfig.put("listBorderColor", "#5C6BC0");
        vanGoghThemeConfig.put("listHeaderBgColor", "#1A237E");
        vanGoghThemeConfig.put("listHeaderTextColor", "#E8EAF6");
        vanGoghThemeConfig.put("borderColor", "#5C6BC0");
        vanGoghThemeConfig.put("darkBackground", true);
        
        Map<String, Object> vanGoghPreset = new HashMap<>();
        vanGoghPreset.put("name", "梵高星空");
        vanGoghPreset.put("description", "基于梵高《星空》的深蓝与金色主题");
        vanGoghPreset.put("config", vanGoghThemeConfig);
        defaultThemePresets.add(vanGoghPreset);
        
        // 莫奈睡莲主题 - 基于莫奈的《睡莲》系列
        Map<String, Object> monetThemeConfig = new HashMap<>(defaultThemeConfig);
        monetThemeConfig.put("theme", "monet");
        monetThemeConfig.put("bgColor", "#E8F5E9");
        monetThemeConfig.put("accentColor", "#7B1FA2");
        monetThemeConfig.put("textPrimaryColor", "#1B5E20");
        monetThemeConfig.put("textSecondaryColor", "#4CAF50");
        monetThemeConfig.put("textTertiaryColor", "#81C784");
        monetThemeConfig.put("textDisabledColor", "#A5D6A7");
        monetThemeConfig.put("panelBgColor", "#FFFFFF");
        monetThemeConfig.put("listBgColor", "#FFFFFF");
        monetThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        monetThemeConfig.put("listRowOddBgColor", "#F1F8E9");
        monetThemeConfig.put("listRowSelectedBgColor", "#E1BEE7");
        monetThemeConfig.put("listRowSelectedTextColor", "#4A148C");
        monetThemeConfig.put("listRowHoverBgColor", "#F3E5F5");
        monetThemeConfig.put("listBorderColor", "#C8E6C9");
        monetThemeConfig.put("listHeaderBgColor", "#F1F8E9");
        monetThemeConfig.put("listHeaderTextColor", "#2E7D32");
        monetThemeConfig.put("borderColor", "#C8E6C9");
        
        Map<String, Object> monetPreset = new HashMap<>();
        monetPreset.put("name", "莫奈睡莲");
        monetPreset.put("description", "基于莫奈《睡莲》的柔和绿紫主题");
        monetPreset.put("config", monetThemeConfig);
        defaultThemePresets.add(monetPreset);
        
        // 蒙德里安主题 - 基于蒙德里安的几何抽象风格
        Map<String, Object> mondrianThemeConfig = new HashMap<>(defaultThemeConfig);
        mondrianThemeConfig.put("theme", "mondrian");
        mondrianThemeConfig.put("bgColor", "#FFFFFF");
        mondrianThemeConfig.put("accentColor", "#E53935");
        mondrianThemeConfig.put("textPrimaryColor", "#000000");
        mondrianThemeConfig.put("textSecondaryColor", "#424242");
        mondrianThemeConfig.put("textTertiaryColor", "#757575");
        mondrianThemeConfig.put("textDisabledColor", "#9E9E9E");
        mondrianThemeConfig.put("panelBgColor", "#FAFAFA");
        mondrianThemeConfig.put("listBgColor", "#FFFFFF");
        mondrianThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        mondrianThemeConfig.put("listRowOddBgColor", "#F5F5F5");
        mondrianThemeConfig.put("listRowSelectedBgColor", "#FFEBEE");
        mondrianThemeConfig.put("listRowSelectedTextColor", "#B71C1C");
        mondrianThemeConfig.put("listRowHoverBgColor", "#FFEBEE");
        mondrianThemeConfig.put("listBorderColor", "#E0E0E0");
        mondrianThemeConfig.put("listHeaderBgColor", "#FAFAFA");
        mondrianThemeConfig.put("listHeaderTextColor", "#212121");
        mondrianThemeConfig.put("borderColor", "#000000");
        mondrianThemeConfig.put("borderWidth", 2);
        
        Map<String, Object> mondrianPreset = new HashMap<>();
        mondrianPreset.put("name", "蒙德里安");
        mondrianPreset.put("description", "基于蒙德里安几何抽象的红白黑主题");
        mondrianPreset.put("config", mondrianThemeConfig);
        defaultThemePresets.add(mondrianPreset);
        
        // 毕加索蓝色时期主题 - 基于毕加索的蓝色时期作品
        Map<String, Object> picassoThemeConfig = new HashMap<>(defaultThemeConfig);
        picassoThemeConfig.put("theme", "picasso");
        picassoThemeConfig.put("bgColor", "#E3F2FD");
        picassoThemeConfig.put("accentColor", "#1565C0");
        picassoThemeConfig.put("textPrimaryColor", "#0D47A1");
        picassoThemeConfig.put("textSecondaryColor", "#1976D2");
        picassoThemeConfig.put("textTertiaryColor", "#42A5F5");
        picassoThemeConfig.put("textDisabledColor", "#90CAF9");
        picassoThemeConfig.put("panelBgColor", "#FFFFFF");
        picassoThemeConfig.put("listBgColor", "#FFFFFF");
        picassoThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        picassoThemeConfig.put("listRowOddBgColor", "#E3F2FD");
        picassoThemeConfig.put("listRowSelectedBgColor", "#BBDEFB");
        picassoThemeConfig.put("listRowSelectedTextColor", "#0D47A1");
        picassoThemeConfig.put("listRowHoverBgColor", "#BBDEFB");
        picassoThemeConfig.put("listBorderColor", "#90CAF9");
        picassoThemeConfig.put("listHeaderBgColor", "#E3F2FD");
        picassoThemeConfig.put("listHeaderTextColor", "#1565C0");
        picassoThemeConfig.put("borderColor", "#90CAF9");
        
        Map<String, Object> picassoPreset = new HashMap<>();
        picassoPreset.put("name", "毕加索蓝");
        picassoPreset.put("description", "基于毕加索蓝色时期的深浅蓝主题");
        picassoPreset.put("config", picassoThemeConfig);
        defaultThemePresets.add(picassoPreset);
        
        // 达利超现实主义主题 - 基于达利的超现实主义风格
        Map<String, Object> daliThemeConfig = new HashMap<>(defaultThemeConfig);
        daliThemeConfig.put("theme", "dali");
        daliThemeConfig.put("bgColor", "#F3E5F5");
        daliThemeConfig.put("accentColor", "#FFB300");
        daliThemeConfig.put("textPrimaryColor", "#4A148C");
        daliThemeConfig.put("textSecondaryColor", "#7B1FA2");
        daliThemeConfig.put("textTertiaryColor", "#AB47BC");
        daliThemeConfig.put("textDisabledColor", "#CE93D8");
        daliThemeConfig.put("panelBgColor", "#FFFFFF");
        daliThemeConfig.put("listBgColor", "#FFFFFF");
        daliThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        daliThemeConfig.put("listRowOddBgColor", "#F3E5F5");
        daliThemeConfig.put("listRowSelectedBgColor", "#FFF8E1");
        daliThemeConfig.put("listRowSelectedTextColor", "#FF6F00");
        daliThemeConfig.put("listRowHoverBgColor", "#FFF8E1");
        daliThemeConfig.put("listBorderColor", "#E1BEE7");
        daliThemeConfig.put("listHeaderBgColor", "#F3E5F5");
        daliThemeConfig.put("listHeaderTextColor", "#6A1B9A");
        daliThemeConfig.put("borderColor", "#E1BEE7");
        
        Map<String, Object> daliPreset = new HashMap<>();
        daliPreset.put("name", "达利超现实");
        daliPreset.put("description", "基于达利超现实主义的紫金主题");
        daliPreset.put("config", daliThemeConfig);
        defaultThemePresets.add(daliPreset);
        
        // 康定斯基主题 - 基于康定斯基的抽象艺术
        Map<String, Object> kandinskyThemeConfig = new HashMap<>(defaultThemeConfig);
        kandinskyThemeConfig.put("theme", "kandinsky");
        kandinskyThemeConfig.put("bgColor", "#FFFDE7");
        kandinskyThemeConfig.put("accentColor", "#E91E63");
        kandinskyThemeConfig.put("textPrimaryColor", "#212121");
        kandinskyThemeConfig.put("textSecondaryColor", "#424242");
        kandinskyThemeConfig.put("textTertiaryColor", "#757575");
        kandinskyThemeConfig.put("textDisabledColor", "#9E9E9E");
        kandinskyThemeConfig.put("panelBgColor", "#FFFFFF");
        kandinskyThemeConfig.put("listBgColor", "#FFFFFF");
        kandinskyThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        kandinskyThemeConfig.put("listRowOddBgColor", "#FFFDE7");
        kandinskyThemeConfig.put("listRowSelectedBgColor", "#FCE4EC");
        kandinskyThemeConfig.put("listRowSelectedTextColor", "#880E4F");
        kandinskyThemeConfig.put("listRowHoverBgColor", "#FCE4EC");
        kandinskyThemeConfig.put("listBorderColor", "#FFCDD2");
        kandinskyThemeConfig.put("listHeaderBgColor", "#FFFDE7");
        kandinskyThemeConfig.put("listHeaderTextColor", "#AD1457");
        kandinskyThemeConfig.put("borderColor", "#FFCDD2");
        
        Map<String, Object> kandinskyPreset = new HashMap<>();
        kandinskyPreset.put("name", "康定斯基");
        kandinskyPreset.put("description", "基于康定斯基抽象艺术的鲜艳几何主题");
        kandinskyPreset.put("config", kandinskyThemeConfig);
        defaultThemePresets.add(kandinskyPreset);
        
        // 莫兰迪色系主题 - 基于莫兰迪的灰调色彩
        Map<String, Object> morandiThemeConfig = new HashMap<>(defaultThemeConfig);
        morandiThemeConfig.put("theme", "morandi");
        morandiThemeConfig.put("bgColor", "#F5F5F5");
        morandiThemeConfig.put("accentColor", "#A1887F");
        morandiThemeConfig.put("textPrimaryColor", "#424242");
        morandiThemeConfig.put("textSecondaryColor", "#616161");
        morandiThemeConfig.put("textTertiaryColor", "#757575");
        morandiThemeConfig.put("textDisabledColor", "#9E9E9E");
        morandiThemeConfig.put("panelBgColor", "#FFFFFF");
        morandiThemeConfig.put("listBgColor", "#FFFFFF");
        morandiThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        morandiThemeConfig.put("listRowOddBgColor", "#F5F5F5");
        morandiThemeConfig.put("listRowSelectedBgColor", "#EFEBE9");
        morandiThemeConfig.put("listRowSelectedTextColor", "#5D4037");
        morandiThemeConfig.put("listRowHoverBgColor", "#EFEBE9");
        morandiThemeConfig.put("listBorderColor", "#D7CCC8");
        morandiThemeConfig.put("listHeaderBgColor", "#F5F5F5");
        morandiThemeConfig.put("listHeaderTextColor", "#4E342E");
        morandiThemeConfig.put("borderColor", "#D7CCC8");
        
        Map<String, Object> morandiPreset = new HashMap<>();
        morandiPreset.put("name", "莫兰迪");
        morandiPreset.put("description", "基于莫兰迪灰调的柔和低饱和度主题");
        morandiPreset.put("config", morandiThemeConfig);
        defaultThemePresets.add(morandiPreset);
        
        // 印象派主题 - 基于印象派的光影效果
        Map<String, Object> impressionistThemeConfig = new HashMap<>(defaultThemeConfig);
        impressionistThemeConfig.put("theme", "impressionist");
        impressionistThemeConfig.put("bgColor", "#FFF8E1");
        impressionistThemeConfig.put("accentColor", "#FF7043");
        impressionistThemeConfig.put("textPrimaryColor", "#BF360C");
        impressionistThemeConfig.put("textSecondaryColor", "#E64A19");
        impressionistThemeConfig.put("textTertiaryColor", "#F4511E");
        impressionistThemeConfig.put("textDisabledColor", "#FF8A65");
        impressionistThemeConfig.put("panelBgColor", "#FFFFFF");
        impressionistThemeConfig.put("listBgColor", "#FFFFFF");
        impressionistThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        impressionistThemeConfig.put("listRowOddBgColor", "#FFF8E1");
        impressionistThemeConfig.put("listRowSelectedBgColor", "#FFCCBC");
        impressionistThemeConfig.put("listRowSelectedTextColor", "#BF360C");
        impressionistThemeConfig.put("listRowHoverBgColor", "#FFCCBC");
        impressionistThemeConfig.put("listBorderColor", "#FFAB91");
        impressionistThemeConfig.put("listHeaderBgColor", "#FFF8E1");
        impressionistThemeConfig.put("listHeaderTextColor", "#D84315");
        impressionistThemeConfig.put("borderColor", "#FFAB91");
        
        Map<String, Object> impressionistPreset = new HashMap<>();
        impressionistPreset.put("name", "印象派");
        impressionistPreset.put("description", "基于印象派光影的明亮温暖主题");
        impressionistPreset.put("config", impressionistThemeConfig);
        defaultThemePresets.add(impressionistPreset);
        
        // 包豪斯主题 - 基于包豪斯设计风格
        Map<String, Object> bauhausThemeConfig = new HashMap<>(defaultThemeConfig);
        bauhausThemeConfig.put("theme", "bauhaus");
        bauhausThemeConfig.put("bgColor", "#FAFAFA");
        bauhausThemeConfig.put("accentColor", "#FF5722");
        bauhausThemeConfig.put("textPrimaryColor", "#212121");
        bauhausThemeConfig.put("textSecondaryColor", "#424242");
        bauhausThemeConfig.put("textTertiaryColor", "#616161");
        bauhausThemeConfig.put("textDisabledColor", "#9E9E9E");
        bauhausThemeConfig.put("panelBgColor", "#FFFFFF");
        bauhausThemeConfig.put("listBgColor", "#FFFFFF");
        bauhausThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        bauhausThemeConfig.put("listRowOddBgColor", "#FAFAFA");
        bauhausThemeConfig.put("listRowSelectedBgColor", "#FBE9E7");
        bauhausThemeConfig.put("listRowSelectedTextColor", "#BF360C");
        bauhausThemeConfig.put("listRowHoverBgColor", "#FBE9E7");
        bauhausThemeConfig.put("listBorderColor", "#FFCCBC");
        bauhausThemeConfig.put("listHeaderBgColor", "#FAFAFA");
        bauhausThemeConfig.put("listHeaderTextColor", "#E64A19");
        bauhausThemeConfig.put("borderColor", "#FFCCBC");
        
        Map<String, Object> bauhausPreset = new HashMap<>();
        bauhausPreset.put("name", "包豪斯");
        bauhausPreset.put("description", "基于包豪斯设计的简约功能主义主题");
        bauhausPreset.put("config", bauhausThemeConfig);
        defaultThemePresets.add(bauhausPreset);
        
        // 新艺术运动主题 - 基于新艺术运动风格
        Map<String, Object> artNouveauThemeConfig = new HashMap<>(defaultThemeConfig);
        artNouveauThemeConfig.put("theme", "artnouveau");
        artNouveauThemeConfig.put("bgColor", "#F1F8E9");
        artNouveauThemeConfig.put("accentColor", "#4CAF50");
        artNouveauThemeConfig.put("textPrimaryColor", "#1B5E20");
        artNouveauThemeConfig.put("textSecondaryColor", "#2E7D32");
        artNouveauThemeConfig.put("textTertiaryColor", "#388E3C");
        artNouveauThemeConfig.put("textDisabledColor", "#81C784");
        artNouveauThemeConfig.put("panelBgColor", "#FFFFFF");
        artNouveauThemeConfig.put("listBgColor", "#FFFFFF");
        artNouveauThemeConfig.put("listRowEvenBgColor", "#FFFFFF");
        artNouveauThemeConfig.put("listRowOddBgColor", "#F1F8E9");
        artNouveauThemeConfig.put("listRowSelectedBgColor", "#C8E6C9");
        artNouveauThemeConfig.put("listRowSelectedTextColor", "#1B5E20");
        artNouveauThemeConfig.put("listRowHoverBgColor", "#C8E6C9");
        artNouveauThemeConfig.put("listBorderColor", "#A5D6A7");
        artNouveauThemeConfig.put("listHeaderBgColor", "#F1F8E9");
        artNouveauThemeConfig.put("listHeaderTextColor", "#2E7D32");
        artNouveauThemeConfig.put("borderColor", "#A5D6A7");
        
        Map<String, Object> artNouveauPreset = new HashMap<>();
        artNouveauPreset.put("name", "新艺术");
        artNouveauPreset.put("description", "基于新艺术运动的优雅自然主题");
        artNouveauPreset.put("config", artNouveauThemeConfig);
        defaultThemePresets.add(artNouveauPreset);
        
        DEFAULT_CONFIG.put(KEY_THEME_PRESETS, defaultThemePresets);
    }

    /**
     * 初始化配置
     */
    public void init() {
        loadConfig();
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                FileReader reader = new FileReader(configFile);
                Map<String, Object> config = objectMapper.readValue(reader, Map.class);
                configCache.putAll(config);
                reader.close();
                
                // 检查主题预设是否需要更新
                List<Map<String, Object>> existingPresets = (List<Map<String, Object>>) configCache.get(KEY_THEME_PRESETS);
                List<Map<String, Object>> defaultPresets = (List<Map<String, Object>>) DEFAULT_CONFIG.get(KEY_THEME_PRESETS);
                
                // 如果主题预设不存在或者数量少于默认配置中的主题预设数量，则使用默认配置中的主题预设
                if (existingPresets == null || existingPresets.size() < defaultPresets.size()) {
                    configCache.put(KEY_THEME_PRESETS, defaultPresets);
                    saveConfig();
                }
            } else {
                // 使用默认配置
                configCache.putAll(DEFAULT_CONFIG);
                saveConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 使用默认配置
            configCache.putAll(DEFAULT_CONFIG);
        }
    }

    /**
     * 保存配置
     */
    public void saveConfig() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            FileWriter writer = new FileWriter(configFile);
            objectMapper.writeValue(writer, configCache);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取配置值
     */
    public <T> T getConfig(String key, Class<T> clazz) {
        if (configCache.containsKey(key)) {
            Object value = configCache.get(key);
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
        }
        // 返回默认值
        if (DEFAULT_CONFIG.containsKey(key)) {
            Object value = DEFAULT_CONFIG.get(key);
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
        }
        return null;
    }

    /**
     * 获取配置值，带默认值
     */
    public <T> T getConfig(String key, Class<T> clazz, T defaultValue) {
        T value = getConfig(key, clazz);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置配置值
     */
    public void setConfig(String key, Object value) {
        configCache.put(key, value);
        saveConfig();
    }

    /**
     * 获取所有配置
     */
    public Map<String, Object> getAllConfig() {
        return new HashMap<>(configCache);
    }

    /**
     * 更新多个配置
     */
    public void updateConfig(Map<String, Object> config) {
        configCache.putAll(config);
        saveConfig();
    }

    /**
     * 重置配置到默认值
     */
    public void resetConfig() {
        configCache.clear();
        configCache.putAll(DEFAULT_CONFIG);
        saveConfig();
    }

    /**
     * 验证配置值
     */
    public boolean validateConfig(String key, Object value) {
        switch (key) {
            case KEY_PREVIEW_THREADS:
            case KEY_EXECUTION_THREADS:
                if (value instanceof Integer) {
                    int threads = (Integer) value;
                    return threads >= 1 && threads <= 16;
                }
                return false;
            case KEY_RECURSION_DEPTH:
            case KEY_MIN_RECURSION_DEPTH:
            case KEY_MAX_RECURSION_DEPTH:
                if (value instanceof Integer) {
                    int depth = (Integer) value;
                    return depth >= 1 && depth <= 10;
                }
                return false;
            case KEY_PREVIEW_LIMIT:
            case KEY_EXECUTION_LIMIT:
                if (value instanceof Integer) {
                    int limit = (Integer) value;
                    return limit >= 1 && limit <= 10000;
                }
                return false;
            case KEY_THREAD_POOL_MODE:
                if (value instanceof String) {
                    String mode = (String) value;
                    return "GLOBAL".equals(mode) || "ROOT_PATH".equals(mode);
                }
                return false;
            case KEY_RECURSION_MODE:
                if (value instanceof String) {
                    String mode = (String) value;
                    return "ALL".equals(mode) || "CURRENT".equals(mode) || "SPECIFIC".equals(mode) || "RANGE".equals(mode);
                }
                return false;
            default:
                return true;
        }
    }
}