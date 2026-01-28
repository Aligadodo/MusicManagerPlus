/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy.duplicate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 添加序号策略
 * 对同名文件添加序号以避免冲突
 */
public class AddSequenceStrategy implements DuplicateStrategy {
    private final boolean keepOriginal; // 是否保留原始文件（不加序号）
    private final String suffixFormat; // 序号后缀格式，如 " (%d)"
    
    /**
     * 构造函数
     * @param keepOriginal 是否保留原始文件
     * @param suffixFormat 序号后缀格式
     */
    public AddSequenceStrategy(boolean keepOriginal, String suffixFormat) {
        this.keepOriginal = keepOriginal;
        this.suffixFormat = suffixFormat != null ? suffixFormat : " (%d)";
    }
    
    @Override
    public List<File> processDuplicates(List<File> duplicates) {
        if (duplicates == null || duplicates.size() <= 1) {
            return duplicates;
        }
        
        List<File> result = new ArrayList<>();
        int counter = 1;
        
        for (int i = 0; i < duplicates.size(); i++) {
            File file = duplicates.get(i);
            
            if (keepOriginal && i == 0) {
                // 保留第一个文件作为原始文件
                result.add(file);
            } else {
                // 为其他文件添加序号
                File numberedFile = createNumberedFile(file, counter++);
                result.add(numberedFile);
            }
        }
        
        return result;
    }
    
    @Override
    public String getName() {
        return "添加序号";
    }
    
    @Override
    public String getDescription() {
        return "对同名文件添加序号以避免冲突";
    }
    
    /**
     * 创建带序号的文件
     * @param originalFile 原始文件
     * @param sequence 序号
     * @return 带序号的文件
     */
    private File createNumberedFile(File originalFile, int sequence) {
        String originalName = originalFile.getName();
        String directory = originalFile.getParent();
        
        // 分离文件名和扩展名
        String baseName;
        String extension = "";
        
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalName.length() - 1) {
            baseName = originalName.substring(0, lastDotIndex);
            extension = originalName.substring(lastDotIndex);
        } else {
            baseName = originalName;
        }
        
        // 移除已有的序号
        baseName = removeExistingNumbering(baseName);
        
        // 创建新的文件名
        String newBaseName = baseName + String.format(suffixFormat, sequence);
        String newFileName = newBaseName + extension;
        
        return new File(directory, newFileName);
    }
    
    /**
     * 移除文件名中已有的序号
     * @param baseName 基础文件名
     * @return 移除序号后的基础文件名
     */
    private String removeExistingNumbering(String baseName) {
        // 匹配常见的序号格式，如 " (1)", " [2]", "-3"
        Pattern pattern = Pattern.compile("\\s*[\\(\\[（](\\d+)[\\)\\]）]\\s*$|\\s*-\\s*(\\d+)\\s*$");
        Matcher matcher = pattern.matcher(baseName);
        
        if (matcher.find()) {
            return baseName.substring(0, matcher.start()).trim();
        }
        
        return baseName;
    }
}
