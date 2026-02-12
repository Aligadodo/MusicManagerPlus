package com.filemanager.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemanager.backend.config.ConfigManager;
import com.filemanager.backend.domain.dto.ThemeDTO;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThemeService {

    private final ObjectMapper objectMapper;
    private final ConfigManager configManager;
    
    private static final String THEMES_DIR = "themes";
    private static final String DEFAULT_THEMES_DIR = THEMES_DIR + File.separator + "default";
    private static final String CUSTOM_THEMES_DIR = THEMES_DIR + File.separator + "custom";
    private static final String DEFAULT_THEME_ID = "default";

    public ThemeService(ObjectMapper objectMapper, ConfigManager configManager) {
        this.objectMapper = objectMapper;
        this.configManager = configManager;
        System.out.println("[ThemeService] ThemeService 构造函数开始");
        initThemesDirectories();
        initDefaultThemes();
        System.out.println("[ThemeService] ThemeService 构造函数完成");
    }

    private void initThemesDirectories() {
        try {
            System.out.println("[ThemeService] 初始化主题目录: " + DEFAULT_THEMES_DIR + ", " + CUSTOM_THEMES_DIR);
            Files.createDirectories(Paths.get(DEFAULT_THEMES_DIR));
            Files.createDirectories(Paths.get(CUSTOM_THEMES_DIR));
            System.out.println("[ThemeService] 主题目录初始化完成");
        } catch (IOException e) {
            System.out.println("[ThemeService] 初始化主题目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initDefaultThemes() {
        try {
            System.out.println("[ThemeService] 初始化默认主题");
            // 检查默认主题是否存在且有效
            File defaultThemeFile = new File(DEFAULT_THEMES_DIR + File.separator + DEFAULT_THEME_ID + ".json");
            System.out.println("[ThemeService] 默认主题文件: " + defaultThemeFile.getAbsolutePath() + ", 存在: " + defaultThemeFile.exists());
            if (!defaultThemeFile.exists() || !isThemeFileValid(defaultThemeFile)) {
                System.out.println("[ThemeService] 创建默认主题");
                // 创建默认主题
                ThemeDTO defaultTheme = createDefaultTheme();
                saveTheme(defaultTheme, DEFAULT_THEMES_DIR);
            }

            // 检查是否需要重新初始化主题预设
            checkAndReinitializeThemes();
            System.out.println("[ThemeService] 默认主题初始化完成");
        } catch (Exception e) {
            System.out.println("[ThemeService] 初始化默认主题失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean isThemeFileValid(File themeFile) {
        try {
            ThemeDTO theme = objectMapper.readValue(themeFile, ThemeDTO.class);
            return theme.getId() != null && theme.getName() != null && theme.getConfig() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void checkAndReinitializeThemes() {
        try {
            System.out.println("[ThemeService] 检查是否需要重新初始化主题预设");
            // 检查默认主题目录中的主题数量
            File defaultDir = new File(DEFAULT_THEMES_DIR);
            File[] files = defaultDir.listFiles((d, name) -> name.endsWith(".json"));
            
            System.out.println("[ThemeService] 发现 " + (files != null ? files.length : 0) + " 个主题文件");
            if (files != null) {
                for (File file : files) {
                    System.out.println("[ThemeService] 主题文件: " + file.getName() + ", 有效: " + isThemeFileValid(file));
                }
            }
            
            // 如果只有默认主题，或者主题文件无效，说明需要重新初始化主题预设
            if (files == null || files.length == 1 || !areThemeFilesValid(files)) {
                System.out.println("[ThemeService] 需要重新初始化主题预设");
                // 强制迁移主题预设
                forceMigrateExistingThemes();
            } else {
                System.out.println("[ThemeService] 主题文件已存在且有效，跳过初始化");
            }
        } catch (Exception e) {
            System.out.println("[ThemeService] 检查主题预设失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean areThemeFilesValid(File[] files) {
        for (File file : files) {
            if (!isThemeFileValid(file)) {
                return false;
            }
        }
        return true;
    }
    
    private void forceMigrateExistingThemes() {
        try {
            System.out.println("[ThemeService] 开始强制迁移主题预设");
            List<Map<String, Object>> existingPresets = (List<Map<String, Object>>) configManager.getConfig(ConfigManager.KEY_THEME_PRESETS, Object.class);
            System.out.println("[ThemeService] 从 ConfigManager 读取到 " + (existingPresets != null ? existingPresets.size() : 0) + " 个主题预设");
            if (existingPresets != null && !existingPresets.isEmpty()) {
                for (Map<String, Object> preset : existingPresets) {
                    String name = (String) preset.get("name");
                    String id = name.toLowerCase().replaceAll("\\s+", "-");
                    
                    ThemeDTO theme = new ThemeDTO();
                    theme.setId(id);
                    theme.setName(name);
                    theme.setDescription((String) preset.get("description"));
                    theme.setType("default");
                    theme.setCreatedAt(Instant.now());
                    theme.setUpdatedAt(Instant.now());
                    
                    Object configObj = preset.get("config");
                    if (configObj instanceof Map) {
                        theme.setConfig((Map<String, Object>) configObj);
                    }
                    
                    // 强制保存主题，覆盖现有文件
                    System.out.println("[ThemeService] 保存主题: " + name + " (" + id + ")");
                    saveTheme(theme, DEFAULT_THEMES_DIR);
                }
                System.out.println("[ThemeService] 主题迁移完成");
            } else {
                System.out.println("[ThemeService] 没有找到主题预设，跳过迁移");
            }
        } catch (Exception e) {
            System.out.println("[ThemeService] 主题迁移失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migrateExistingThemes() throws Exception {
        List<Map<String, Object>> existingPresets = (List<Map<String, Object>>) configManager.getConfig(ConfigManager.KEY_THEME_PRESETS, Object.class);
        if (existingPresets != null && !existingPresets.isEmpty()) {
            for (Map<String, Object> preset : existingPresets) {
                String name = (String) preset.get("name");
                String id = name.toLowerCase().replaceAll("\\s+", "-");
                
                ThemeDTO theme = new ThemeDTO();
                theme.setId(id);
                theme.setName(name);
                theme.setDescription((String) preset.get("description"));
                theme.setType("default");
                theme.setCreatedAt(Instant.now());
                theme.setUpdatedAt(Instant.now());
                
                Object configObj = preset.get("config");
                if (configObj instanceof Map) {
                    theme.setConfig((Map<String, Object>) configObj);
                }
                
                File themeFile = new File(DEFAULT_THEMES_DIR + File.separator + id + ".json");
                if (!themeFile.exists()) {
                    saveTheme(theme, DEFAULT_THEMES_DIR);
                }
            }
        }
    }

    private ThemeDTO createDefaultTheme() {
        ThemeDTO theme = new ThemeDTO();
        theme.setId(DEFAULT_THEME_ID);
        theme.setName("默认主题");
        theme.setDescription("系统默认主题");
        theme.setType("default");
        theme.setCreatedAt(Instant.now());
        theme.setUpdatedAt(Instant.now());
        
        Map<String, Object> config = new HashMap<>();
        config.put("theme", "light");
        config.put("bgColor", "#F8F9FA");
        config.put("accentColor", "#3498DB");
        config.put("textPrimaryColor", "#2C3E50");
        config.put("textSecondaryColor", "#7F8C8D");
        config.put("textTertiaryColor", "#95A5A6");
        config.put("textDisabledColor", "#BDC3C7");
        config.put("panelBgColor", "#FFFFFF");
        config.put("listBgColor", "#FFFFFF");
        config.put("listRowEvenBgColor", "#FFFFFF");
        config.put("listRowOddBgColor", "#F5F7FA");
        config.put("listRowSelectedBgColor", "#E3F2FD");
        config.put("listRowSelectedTextColor", "#2196F3");
        config.put("listRowHoverBgColor", "#F0F8FF");
        config.put("listBorderColor", "#E1E4E8");
        config.put("listHeaderBgColor", "#F8F9FA");
        config.put("listHeaderTextColor", "#586069");
        config.put("borderColor", "#E1E4E8");
        config.put("glassOpacity", 0.8);
        config.put("darkBackground", false);
        config.put("fontFamily", "Roboto");
        config.put("fontSize", 14);
        config.put("cornerRadius", 6);
        config.put("borderWidth", 1);
        config.put("buttonLargeSize", 48);
        config.put("buttonSmallSize", 36);
        
        theme.setConfig(config);
        return theme;
    }

    public List<ThemeDTO> getAllThemes() {
        List<ThemeDTO> themes = new ArrayList<>();
        
        // 加载系统预设主题
        themes.addAll(loadThemesFromDirectory(DEFAULT_THEMES_DIR, "default"));
        
        // 加载用户自定义主题
        themes.addAll(loadThemesFromDirectory(CUSTOM_THEMES_DIR, "custom"));
        
        return themes;
    }

    private List<ThemeDTO> loadThemesFromDirectory(String directory, String type) {
        List<ThemeDTO> themes = new ArrayList<>();
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        ThemeDTO theme = objectMapper.readValue(file, ThemeDTO.class);
                        theme.setType(type);
                        themes.add(theme);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return themes;
    }

    public ThemeDTO getThemeById(String id) {
        // 先在自定义主题中查找
        ThemeDTO theme = loadThemeFromFile(CUSTOM_THEMES_DIR, id);
        if (theme != null) {
            return theme;
        }
        
        // 再在系统预设主题中查找
        theme = loadThemeFromFile(DEFAULT_THEMES_DIR, id);
        if (theme != null) {
            return theme;
        }
        
        // 如果找不到，返回默认主题
        return loadThemeFromFile(DEFAULT_THEMES_DIR, DEFAULT_THEME_ID);
    }

    private ThemeDTO loadThemeFromFile(String directory, String id) {
        File file = new File(directory + File.separator + id + ".json");
        if (file.exists()) {
            try {
                return objectMapper.readValue(file, ThemeDTO.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public ThemeDTO createTheme(ThemeDTO theme) {
        theme.setId(generateThemeId(theme.getName()));
        theme.setType("custom");
        theme.setCreatedAt(Instant.now());
        theme.setUpdatedAt(Instant.now());
        
        try {
            saveTheme(theme, CUSTOM_THEMES_DIR);
            return theme;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ThemeDTO updateTheme(String id, ThemeDTO theme) {
        // 检查是否为系统预设主题
        File defaultThemeFile = new File(DEFAULT_THEMES_DIR + File.separator + id + ".json");
        if (defaultThemeFile.exists()) {
            throw new IllegalArgumentException("系统预设主题不可修改");
        }
        
        // 检查自定义主题是否存在
        File customThemeFile = new File(CUSTOM_THEMES_DIR + File.separator + id + ".json");
        if (!customThemeFile.exists()) {
            throw new IllegalArgumentException("主题不存在");
        }
        
        theme.setId(id);
        theme.setType("custom");
        theme.setUpdatedAt(Instant.now());
        
        try {
            saveTheme(theme, CUSTOM_THEMES_DIR);
            return theme;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteTheme(String id) {
        // 检查是否为默认主题
        if (DEFAULT_THEME_ID.equals(id)) {
            throw new IllegalArgumentException("默认主题不可删除");
        }
        
        // 检查是否为系统预设主题
        File defaultThemeFile = new File(DEFAULT_THEMES_DIR + File.separator + id + ".json");
        if (defaultThemeFile.exists()) {
            throw new IllegalArgumentException("系统预设主题不可删除");
        }
        
        // 删除自定义主题
        File customThemeFile = new File(CUSTOM_THEMES_DIR + File.separator + id + ".json");
        if (customThemeFile.exists()) {
            return customThemeFile.delete();
        }
        
        return false;
    }

    public ThemeDTO getDefaultTheme() {
        return loadThemeFromFile(DEFAULT_THEMES_DIR, DEFAULT_THEME_ID);
    }

    public void setDefaultTheme(String themeId) {
        // 检查主题是否存在
        ThemeDTO theme = getThemeById(themeId);
        if (theme == null) {
            throw new IllegalArgumentException("主题不存在");
        }
        
        // 更新配置中的默认主题
        configManager.setConfig("defaultThemeId", themeId);
    }

    private String generateThemeId(String name) {
        String id = name.toLowerCase().replaceAll("\\s+", "-");
        int counter = 1;
        String originalId = id;
        
        // 确保ID唯一
        while (new File(CUSTOM_THEMES_DIR + File.separator + id + ".json").exists() ||
               new File(DEFAULT_THEMES_DIR + File.separator + id + ".json").exists()) {
            id = originalId + "-" + counter++;
        }
        
        return id;
    }

    private void saveTheme(ThemeDTO theme, String directory) throws IOException {
        File file = new File(directory + File.separator + theme.getId() + ".json");
        objectMapper.writeValue(file, theme);
    }
}