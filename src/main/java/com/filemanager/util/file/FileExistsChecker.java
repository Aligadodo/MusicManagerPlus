package com.filemanager.util.file;

import com.filemanager.util.LanguageUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * 通用文件存在检查工具
 * 支持多种文件命名格式化选项，用于智能检查文件是否存在
 */
public class FileExistsChecker {
    
    /**
     * 文件存在检查参数类
     * 用于配置文件检查时应用的格式化选项
     */
    public static class FileExistsParams {
        private boolean enableCaseInsensitive = false; // 是否忽略大小写
        private boolean enableSimplifiedChinese = false; // 是否转简体中文
        private boolean enableHalfWidth = false; // 是否转半角字符
        private boolean enableUpperCase = false; // 是否转大写
        private boolean enableLowerCase = false; // 是否转小写
        private boolean enableTrim = false; // 是否去除首尾空格
        
        public FileExistsParams() {
        }
        
        public FileExistsParams enableCaseInsensitive() {
            this.enableCaseInsensitive = true;
            return this;
        }
        
        public FileExistsParams enableSimplifiedChinese() {
            this.enableSimplifiedChinese = true;
            return this;
        }
        
        public FileExistsParams enableHalfWidth() {
            this.enableHalfWidth = true;
            return this;
        }
        
        public FileExistsParams enableUpperCase() {
            this.enableUpperCase = true;
            this.enableLowerCase = false;
            return this;
        }
        
        public FileExistsParams enableLowerCase() {
            this.enableLowerCase = true;
            this.enableUpperCase = false;
            return this;
        }
        
        public FileExistsParams enableTrim() {
            this.enableTrim = true;
            return this;
        }
        
        public boolean isEnableCaseInsensitive() {
            return enableCaseInsensitive;
        }
        
        public boolean isEnableSimplifiedChinese() {
            return enableSimplifiedChinese;
        }
        
        public boolean isEnableHalfWidth() {
            return enableHalfWidth;
        }
        
        public boolean isEnableUpperCase() {
            return enableUpperCase;
        }
        
        public boolean isEnableLowerCase() {
            return enableLowerCase;
        }
        
        public boolean isEnableTrim() {
            return enableTrim;
        }
    }
    
    /**
     * 检查目标文件是否存在，支持多种格式化选项
     * 
     * @param parentDir 父目录
     * @param targetFileName 目标文件名
     * @param params 检查参数
     * @return 存在返回true，不存在返回false
     */
    public static boolean checkFileExists(File parentDir, String targetFileName, FileExistsParams params) {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            return false;
        }
        
        // 预处理目标文件名
        String processedTargetName = preprocessFilename(targetFileName, params);
        
        // 遍历目录中的所有文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir.toPath())) {
            for (Path path : stream) {
                File file = path.toFile();
                if (file.isFile()) {
                    // 预处理当前文件名
                    String processedFileName = preprocessFilename(file.getName(), params);
                    
                    // 比较预处理后的文件名
                    if (processedTargetName.equals(processedFileName)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 检查目标文件是否存在，返回匹配的文件对象
     * 
     * @param parentDir 父目录
     * @param targetFileName 目标文件名
     * @param params 检查参数
     * @return 存在返回匹配的文件对象，不存在返回null
     */
    public static File getExistingFile(File parentDir, String targetFileName, FileExistsParams params) {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            return null;
        }
        
        // 预处理目标文件名
        String processedTargetName = preprocessFilename(targetFileName, params);
        
        // 遍历目录中的所有文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir.toPath())) {
            for (Path path : stream) {
                File file = path.toFile();
                if (file.isFile()) {
                    // 预处理当前文件名
                    String processedFileName = preprocessFilename(file.getName(), params);
                    
                    // 比较预处理后的文件名
                    if (processedTargetName.equals(processedFileName)) {
                        return file;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 获取目录中所有文件的预处理名称集合
     * 
     * @param parentDir 父目录
     * @param params 预处理参数
     * @return 预处理后的文件名集合
     */
    public static Set<String> getPreprocessedFileNames(File parentDir, FileExistsParams params) {
        Set<String> result = new HashSet<>();
        
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            return result;
        }
        
        // 遍历目录中的所有文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir.toPath())) {
            for (Path path : stream) {
                File file = path.toFile();
                if (file.isFile()) {
                    // 预处理当前文件名
                    String processedFileName = preprocessFilename(file.getName(), params);
                    result.add(processedFileName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 对文件名进行预处理，应用各种格式化选项
     * 
     * @param filename 原始文件名
     * @param params 预处理参数
     * @return 预处理后的文件名
     */
    private static String preprocessFilename(String filename, FileExistsParams params) {
        if (filename == null) {
            return null;
        }
        
        String processed = filename;
        
        // 去除首尾空格
        if (params.isEnableTrim()) {
            processed = processed.trim();
        }
        
        // 转简体中文
        if (params.isEnableSimplifiedChinese()) {
            processed = LanguageUtil.toSimpleChinese(processed);
        }
        
        // 转半角字符
        if (params.isEnableHalfWidth()) {
            processed = LanguageUtil.toHalfWidth(processed);
        }
        
        // 大小写转换
        if (params.isEnableUpperCase()) {
            processed = processed.toUpperCase();
        } else if (params.isEnableLowerCase()) {
            processed = processed.toLowerCase();
        }
        
        // 如果启用了大小写不敏感但没有指定转换为大写或小写，则默认转换为小写进行比较
        if (params.isEnableCaseInsensitive() && !params.isEnableUpperCase() && !params.isEnableLowerCase()) {
            processed = processed.toLowerCase();
        }
        
        return processed;
    }
}
