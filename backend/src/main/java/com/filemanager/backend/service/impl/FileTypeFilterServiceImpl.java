package com.filemanager.backend.service.impl;

import com.filemanager.backend.config.ConfigManager;
import com.filemanager.backend.service.FileTypeFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件类型筛选服务实现
 * 负责管理文件类型的筛选规则和树形结构
 */
@Service
public class FileTypeFilterServiceImpl implements FileTypeFilterService {
    
    private final ConfigManager configManager;
    
    // 文件类型分类
    private static final Map<String, List<String>> FILE_TYPE_CATEGORIES = new HashMap<>();
    
    static {
        // 音频文件
        List<String> audioTypes = new ArrayList<>();
        audioTypes.add("mp3");
        audioTypes.add("wav");
        audioTypes.add("flac");
        audioTypes.add("aac");
        audioTypes.add("ogg");
        audioTypes.add("m4a");
        audioTypes.add("wma");
        audioTypes.add("opus");
        audioTypes.add("ape");
        FILE_TYPE_CATEGORIES.put("audio", audioTypes);
        
        // 视频文件
        List<String> videoTypes = new ArrayList<>();
        videoTypes.add("mp4");
        videoTypes.add("avi");
        videoTypes.add("mkv");
        videoTypes.add("mov");
        videoTypes.add("wmv");
        videoTypes.add("flv");
        videoTypes.add("m4v");
        videoTypes.add("webm");
        FILE_TYPE_CATEGORIES.put("video", videoTypes);
        
        // 图片文件
        List<String> imageTypes = new ArrayList<>();
        imageTypes.add("jpg");
        imageTypes.add("jpeg");
        imageTypes.add("png");
        imageTypes.add("gif");
        imageTypes.add("bmp");
        imageTypes.add("webp");
        imageTypes.add("tiff");
        imageTypes.add("svg");
        FILE_TYPE_CATEGORIES.put("image", imageTypes);
        
        // 文档文件
        List<String> documentTypes = new ArrayList<>();
        documentTypes.add("txt");
        documentTypes.add("doc");
        documentTypes.add("docx");
        documentTypes.add("pdf");
        documentTypes.add("xls");
        documentTypes.add("xlsx");
        documentTypes.add("ppt");
        documentTypes.add("pptx");
        documentTypes.add("md");
        documentTypes.add("rtf");
        FILE_TYPE_CATEGORIES.put("document", documentTypes);
        
        // 压缩文件
        List<String> archiveTypes = new ArrayList<>();
        archiveTypes.add("zip");
        archiveTypes.add("rar");
        archiveTypes.add("7z");
        archiveTypes.add("tar");
        archiveTypes.add("gz");
        archiveTypes.add("bz2");
        archiveTypes.add("iso");
        FILE_TYPE_CATEGORIES.put("archive", archiveTypes);
    }
    
    @Autowired
    public FileTypeFilterServiceImpl(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public Map<String, Object> getFileTypeTree() {
        Map<String, Object> fileTypeTree = configManager.getConfig(ConfigManager.KEY_FILE_TYPE_TREE, Map.class);
        if (fileTypeTree == null) {
            // 返回默认的文件类型树形结构
            return createDefaultFileTypeTree();
        }
        return fileTypeTree;
    }
    
    @Override
    public void setFileTypeTree(Map<String, Object> fileTypeTree) {
        configManager.setConfig(ConfigManager.KEY_FILE_TYPE_TREE, fileTypeTree);
    }
    
    @Override
    public List<String> getCustomFileTypes() {
        List<String> customFileTypes = configManager.getConfig(ConfigManager.KEY_CUSTOM_FILE_TYPES, List.class);
        if (customFileTypes == null) {
            return new ArrayList<>();
        }
        return customFileTypes;
    }
    
    @Override
    public void setCustomFileTypes(List<String> customFileTypes) {
        configManager.setConfig(ConfigManager.KEY_CUSTOM_FILE_TYPES, customFileTypes);
    }
    
    @Override
    public void addCustomFileType(String fileType) {
        if (fileType == null || fileType.trim().isEmpty()) {
            return;
        }
        
        List<String> customFileTypes = getCustomFileTypes();
        if (!customFileTypes.contains(fileType)) {
            customFileTypes.add(fileType);
            setCustomFileTypes(customFileTypes);
        }
    }
    
    @Override
    public void removeCustomFileType(String fileType) {
        if (fileType == null) {
            return;
        }
        
        List<String> customFileTypes = getCustomFileTypes();
        customFileTypes.remove(fileType);
        setCustomFileTypes(customFileTypes);
    }
    
    @Override
    public boolean isFileIncludedByType(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        // 获取文件扩展名
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }
        
        // 检查是否在自定义文件类型中
        List<String> customFileTypes = getCustomFileTypes();
        if (!customFileTypes.isEmpty() && !customFileTypes.contains(extension)) {
            return false;
        }
        
        // 检查文件类型树
        Map<String, Object> fileTypeTree = getFileTypeTree();
        if (fileTypeTree != null && !fileTypeTree.isEmpty()) {
            // 这里可以根据文件类型树的结构进行更复杂的检查
            // 暂时返回true，后续可以扩展
        }
        
        return true;
    }
    
    @Override
    public boolean isAudioFile(String fileName) {
        return isFileTypeInCategory(fileName, "audio");
    }
    
    @Override
    public boolean isVideoFile(String fileName) {
        return isFileTypeInCategory(fileName, "video");
    }
    
    @Override
    public boolean isImageFile(String fileName) {
        return isFileTypeInCategory(fileName, "image");
    }
    
    @Override
    public boolean isDocumentFile(String fileName) {
        return isFileTypeInCategory(fileName, "document");
    }
    
    @Override
    public String getFileCategory(String fileName) {
        if (fileName == null) {
            return "other";
        }
        
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return "other";
        }
        
        for (Map.Entry<String, List<String>> entry : FILE_TYPE_CATEGORIES.entrySet()) {
            if (entry.getValue().contains(extension)) {
                return entry.getKey();
            }
        }
        
        // 检查自定义文件类型
        List<String> customFileTypes = getCustomFileTypes();
        if (customFileTypes.contains(extension)) {
            return "custom";
        }
        
        return "other";
    }
    
    /**
     * 检查文件类型是否在指定分类中
     */
    private boolean isFileTypeInCategory(String fileName, String category) {
        if (fileName == null) {
            return false;
        }
        
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }
        
        List<String> types = FILE_TYPE_CATEGORIES.get(category);
        return types != null && types.contains(extension);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * 创建默认的文件类型树形结构
     */
    private Map<String, Object> createDefaultFileTypeTree() {
        Map<String, Object> tree = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : FILE_TYPE_CATEGORIES.entrySet()) {
            Map<String, Object> category = new HashMap<>();
            category.put("name", entry.getKey());
            category.put("types", entry.getValue());
            category.put("enabled", true);
            tree.put(entry.getKey(), category);
        }
        
        return tree;
    }
}