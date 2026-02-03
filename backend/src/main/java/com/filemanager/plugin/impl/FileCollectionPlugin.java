package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 文件收集策略插件
 * 根据配置规则收集和整理文件
 */
public class FileCollectionPlugin extends AbstractPlugin {

    public FileCollectionPlugin() {
        super("file-collection", "文件收集策略", "根据配置规则收集和整理文件", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("targetDirectory", "目标目录", "directory", "/tmp/collected", "文件收集的目标目录", true);
        addParameter("recursive", "递归收集", "boolean", true, "是否递归收集子目录中的文件", false);
        addParameter("overwrite", "覆盖已存在文件", "boolean", false, "是否覆盖已存在的文件", false);
        addParameter("preserveStructure", "保留目录结构", "boolean", false, "是否保留原始目录结构", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("targetDirectory", "/tmp/collected");
        setDefaultConfigValue("recursive", true);
        setDefaultConfigValue("overwrite", false);
        setDefaultConfigValue("preserveStructure", false);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String targetDirectory = getConfigValue(config, "targetDirectory", "/tmp/collected");
        boolean preserveStructure = getConfigValue(config, "preserveStructure", false);

        String targetPath = getTargetPath(filePath, targetDirectory, preserveStructure);
        return createChangeRecord(filePath, targetPath, "PENDING");
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String targetDirectory = getConfigValue(config, "targetDirectory", "/tmp/collected");
        boolean preserveStructure = getConfigValue(config, "preserveStructure", false);
        boolean overwrite = getConfigValue(config, "overwrite", false);

        String targetPath = getTargetPath(filePath, targetDirectory, preserveStructure);
        
        try {
            File sourceFile = new File(filePath);
            File targetFile = new File(targetPath);
            
            // 创建目标目录
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
                context.logDebug("Created directory: " + targetFile.getParentFile().getPath());
            }
            
            // 复制文件
            if (overwrite || !targetFile.exists()) {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), 
                        StandardCopyOption.REPLACE_EXISTING);
                context.logInfo("Copied file: " + filePath + " -> " + targetPath);
                return createChangeRecord(filePath, targetPath, "SUCCESS");
            } else {
                context.logWarn("File already exists: " + targetPath);
                return createChangeRecord(filePath, targetPath, "SKIPPED");
            }
        } catch (IOException e) {
            context.logError("Error copying file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, targetPath, "ERROR");
        }
    }

    /**
     * 获取目标文件路径
     * @param filePath 原始文件路径
     * @param targetDirectory 目标目录
     * @param preserveStructure 是否保留目录结构
     * @return 目标文件路径
     */
    private String getTargetPath(String filePath, String targetDirectory, boolean preserveStructure) {
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        
        if (preserveStructure) {
            // 保留目录结构
            String relativePath = getRelativePath(sourceFile);
            return targetDirectory + File.separator + relativePath;
        } else {
            // 直接复制到目标目录
            return targetDirectory + File.separator + fileName;
        }
    }

    /**
     * 获取相对路径
     * @param file 文件对象
     * @return 相对路径
     */
    private String getRelativePath(File file) {
        // 简单实现：使用文件的父目录结构
        String parentPath = file.getParent();
        if (parentPath == null) {
            return file.getName();
        }
        
        File parentFile = new File(parentPath);
        String grandParentPath = parentFile.getParent();
        
        if (grandParentPath == null) {
            return parentFile.getName() + File.separator + file.getName();
        }
        
        // 只保留最后两级目录结构
        String parentName = parentFile.getName();
        File grandParentFile = new File(grandParentPath);
        String grandParentName = grandParentFile.getName();
        
        return grandParentName + File.separator + parentName + File.separator + file.getName();
    }
}
