package com.filemanager.backend.service;

import java.util.List;
import java.util.Map;

/**
 * 文件类型筛选服务接口
 * 负责管理文件类型的筛选规则和树形结构
 */
public interface FileTypeFilterService {
    
    /**
     * 获取文件类型树形结构
     * @return 文件类型树形结构
     */
    Map<String, Object> getFileTypeTree();
    
    /**
     * 设置文件类型树形结构
     * @param fileTypeTree 文件类型树形结构
     */
    void setFileTypeTree(Map<String, Object> fileTypeTree);
    
    /**
     * 获取自定义文件类型列表
     * @return 自定义文件类型列表
     */
    List<String> getCustomFileTypes();
    
    /**
     * 设置自定义文件类型列表
     * @param customFileTypes 自定义文件类型列表
     */
    void setCustomFileTypes(List<String> customFileTypes);
    
    /**
     * 添加自定义文件类型
     * @param fileType 文件类型
     */
    void addCustomFileType(String fileType);
    
    /**
     * 移除自定义文件类型
     * @param fileType 文件类型
     */
    void removeCustomFileType(String fileType);
    
    /**
     * 检查文件是否符合类型筛选规则
     * @param fileName 文件名
     * @return 是否符合筛选规则
     */
    boolean isFileIncludedByType(String fileName);
    
    /**
     * 检查文件类型是否为音频文件
     * @param fileName 文件名
     * @return 是否为音频文件
     */
    boolean isAudioFile(String fileName);
    
    /**
     * 检查文件类型是否为视频文件
     * @param fileName 文件名
     * @return 是否为视频文件
     */
    boolean isVideoFile(String fileName);
    
    /**
     * 检查文件类型是否为图片文件
     * @param fileName 文件名
     * @return 是否为图片文件
     */
    boolean isImageFile(String fileName);
    
    /**
     * 检查文件类型是否为文档文件
     * @param fileName 文件名
     * @return 是否为文档文件
     */
    boolean isDocumentFile(String fileName);
    
    /**
     * 获取文件的类型分类
     * @param fileName 文件名
     * @return 文件类型分类
     */
    String getFileCategory(String fileName);
}