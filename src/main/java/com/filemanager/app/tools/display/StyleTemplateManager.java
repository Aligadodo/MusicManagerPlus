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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 样式模板管理器
 * 负责管理样式模板的保存、加载、选择和删除功能
 * 支持自定义扩展样式模板，存储在运行时的style目录下
 * @author hrcao
 */
public class StyleTemplateManager {
    private static final StyleTemplateManager INSTANCE = new StyleTemplateManager();
    private static final String STYLE_DIR = "style";
    private static final String TEMPLATE_SUFFIX = ".style.template";
    
    private Map<String, ThemeConfig> templateMap;
    private ThemeConfig currentTemplate;
    
    private StyleTemplateManager() {
        templateMap = new HashMap<>();
        initStyleDir();
        loadAllTemplates();
    }
    
    public static StyleTemplateManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 初始化样式目录
     */
    private void initStyleDir() {
        File styleDir = new File(STYLE_DIR);
        if (!styleDir.exists()) {
            styleDir.mkdir();
        }
    }
    
    /**
     * 加载所有样式模板
     */
    public void loadAllTemplates() {
        templateMap.clear();
        
        // 添加内置模板
        loadBuiltInTemplates();
        
        // 加载自定义模板
        loadCustomTemplates();
    }
    
    /**
     * 加载内置样式模板
     */
    private void loadBuiltInTemplates() {
        // 默认主题
        ThemeConfig defaultTheme = new ThemeConfig();
        defaultTheme.setTemplateName("默认主题");
        defaultTheme.setTemplateDescription("默认的界面样式模板，提供简洁清晰的外观");
        templateMap.put(defaultTheme.getTemplateName(), defaultTheme);
        
        // 深色主题
        ThemeConfig darkTheme = new ThemeConfig();
        darkTheme.setTemplateName("深色主题");
        darkTheme.setTemplateDescription("深色界面，适合夜间使用，减少视觉疲劳");
        darkTheme.setBgColor("#2c3e50");
        darkTheme.setPanelBgColor("#34495e");
        darkTheme.setTextPrimaryColor("#ecf0f1");
        darkTheme.setTextSecondaryColor("#bdc3c7");
        darkTheme.setTextTertiaryColor("#95a5a6");
        darkTheme.setButtonPrimaryBgColor("#3498db");
        darkTheme.setButtonSecondaryBgColor("#34495e");
        darkTheme.setButtonSecondaryTextColor("#ecf0f1");
        darkTheme.setButtonSecondaryBorderColor("#546e7a");
        darkTheme.setBorderColor("#546e7a");
        darkTheme.setDarkBackground(true);
        templateMap.put(darkTheme.getTemplateName(), darkTheme);
        
        // 明亮主题
        ThemeConfig lightTheme = new ThemeConfig();
        lightTheme.setTemplateName("明亮主题");
        lightTheme.setTemplateDescription("明亮清新的界面，提供愉悦的视觉体验");
        lightTheme.setBgColor("#f8f9fa");
        lightTheme.setPanelBgColor("#ffffff");
        lightTheme.setTextPrimaryColor("#212529");
        lightTheme.setTextSecondaryColor("#6c757d");
        lightTheme.setTextTertiaryColor("#adb5bd");
        lightTheme.setButtonPrimaryBgColor("#007bff");
        lightTheme.setButtonSecondaryBgColor("#ffffff");
        lightTheme.setButtonSecondaryTextColor("#212529");
        lightTheme.setButtonSecondaryBorderColor("#ced4da");
        lightTheme.setBorderColor("#ced4da");
        templateMap.put(lightTheme.getTemplateName(), lightTheme);
        
        // 高对比度主题
        ThemeConfig highContrastTheme = new ThemeConfig();
        highContrastTheme.setTemplateName("高对比度主题");
        highContrastTheme.setTemplateDescription("高对比度设计，提高文本可读性，适合有视觉障碍的用户");
        highContrastTheme.setBgColor("#ffffff");
        highContrastTheme.setPanelBgColor("#ffffff");
        highContrastTheme.setTextPrimaryColor("#000000");
        highContrastTheme.setTextSecondaryColor("#000000");
        highContrastTheme.setTextTertiaryColor("#000000");
        highContrastTheme.setButtonPrimaryBgColor("#000000");
        highContrastTheme.setButtonPrimaryTextColor("#ffffff");
        highContrastTheme.setButtonSecondaryBgColor("#ffffff");
        highContrastTheme.setButtonSecondaryTextColor("#000000");
        highContrastTheme.setButtonSecondaryBorderColor("#000000");
        highContrastTheme.setBorderColor("#000000");
        templateMap.put(highContrastTheme.getTemplateName(), highContrastTheme);
    }
    
    /**
     * 加载自定义样式模板
     */
    private void loadCustomTemplates() {
        File styleDir = new File(STYLE_DIR);
        if (!styleDir.exists()) {
            return;
        }
        
        File[] files = styleDir.listFiles((dir, name) -> name.endsWith(TEMPLATE_SUFFIX));
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()), "UTF-8");
                ThemeConfig template = JSON.parseObject(content, ThemeConfig.class);
                if (template != null && template.getTemplateName() != null) {
                    templateMap.put(template.getTemplateName(), template);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 保存样式模板到文件
     * @param template 样式模板
     * @return 是否保存成功
     */
    public boolean saveTemplate(ThemeConfig template) {
        if (template == null || template.getTemplateName() == null) {
            return false;
        }
        
        try {
            String templateName = template.getTemplateName();
            Path filePath = Paths.get(STYLE_DIR, templateName + TEMPLATE_SUFFIX);
            String json = JSON.toJSONString(template, SerializerFeature.PrettyFormat);
            Files.write(filePath, json.getBytes("UTF-8"));
            
            // 更新模板映射
            templateMap.put(templateName, template);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 删除样式模板
     * @param templateName 模板名称
     * @return 是否删除成功
     */
    public boolean deleteTemplate(String templateName) {
        // 不能删除内置模板
        if (isBuiltInTemplate(templateName)) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(STYLE_DIR, templateName + TEMPLATE_SUFFIX);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // 从映射中移除
            templateMap.remove(templateName);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查是否为内置模板
     * @param templateName 模板名称
     * @return 是否为内置模板
     */
    private boolean isBuiltInTemplate(String templateName) {
        return "默认主题".equals(templateName) || "深色主题".equals(templateName) ||
               "明亮主题".equals(templateName) || "高对比度主题".equals(templateName);
    }
    
    /**
     * 获取所有样式模板名称
     * @return 模板名称列表
     */
    public List<String> getAllTemplateNames() {
        return new ArrayList<>(templateMap.keySet());
    }
    
    /**
     * 获取所有样式模板
     * @return 样式模板列表
     */
    public List<ThemeConfig> getAllTemplates() {
        return new ArrayList<>(templateMap.values());
    }
    
    /**
     * 根据名称获取样式模板
     * @param templateName 模板名称
     * @return 样式模板
     */
    public ThemeConfig getTemplate(String templateName) {
        return templateMap.get(templateName);
    }
    
    /**
     * 设置当前样式模板
     * @param templateName 模板名称
     */
    public void setCurrentTemplate(String templateName) {
        currentTemplate = templateMap.get(templateName);
    }
    
    /**
     * 设置当前样式模板
     * @param template 样式模板
     */
    public void setCurrentTemplate(ThemeConfig template) {
        currentTemplate = template;
    }
    
    /**
     * 获取当前样式模板
     * @return 当前样式模板
     */
    public ThemeConfig getCurrentTemplate() {
        if (currentTemplate == null) {
            currentTemplate = templateMap.get("默认主题");
        }
        return currentTemplate;
    }
    
    /**
     * 更新样式模板
     * @param template 样式模板
     */
    public void updateTemplate(ThemeConfig template) {
        if (template != null && template.getTemplateName() != null) {
            templateMap.put(template.getTemplateName(), template);
            
            // 如果是当前模板，更新当前模板
            if (currentTemplate != null && template.getTemplateName().equals(currentTemplate.getTemplateName())) {
                currentTemplate = template;
            }
        }
    }
}
