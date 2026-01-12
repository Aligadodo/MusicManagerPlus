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

import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字体管理器
 * 负责获取系统可用字体和字体管理功能
 */
public class FontManager {
    private static final FontManager INSTANCE = new FontManager();
    
    private final List<String> systemFonts;
    private final List<String> filteredFonts;
    
    private FontManager() {
        // 获取系统所有可用字体
        systemFonts = new ArrayList<>(Font.getFamilies());
        Collections.sort(systemFonts);
        
        // 过滤常用字体，排除一些系统默认的特殊字体
        filteredFonts = systemFonts.stream()
                .filter(font -> !font.contains("@") && !font.startsWith("System"))
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 获取单例实例
     */
    public static FontManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取所有系统字体
     */
    public List<String> getAllSystemFonts() {
        return new ArrayList<>(systemFonts);
    }
    
    /**
     * 获取过滤后的常用字体
     */
    public List<String> getFilteredFonts() {
        return new ArrayList<>(filteredFonts);
    }
    
    /**
     * 检查字体是否可用
     */
    public boolean isFontAvailable(String fontName) {
        return systemFonts.contains(fontName);
    }
    
    /**
     * 获取默认字体列表（包含一些常用字体）
     */
    public List<String> getDefaultFonts() {
        List<String> defaultFonts = new ArrayList<>();
        List<String> popularFonts = Arrays.asList(
                "Segoe UI", "Microsoft YaHei", "SimSun", "SimHei", "KaiTi", 
                "Arial", "Times New Roman", "Courier New", "Georgia", "Verdana"
        );
        
        // 只添加系统中实际存在的常用字体
        for (String font : popularFonts) {
            if (systemFonts.contains(font)) {
                defaultFonts.add(font);
            }
        }
        
        return defaultFonts;
    }
}