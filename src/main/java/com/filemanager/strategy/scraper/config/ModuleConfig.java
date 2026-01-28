package com.filemanager.strategy.scraper.config;

/**
 * 模块配置接口
 * 用于定义各个刮削模块的配置选项
 */
public interface ModuleConfig {
    
    /**
     * 获取模块名称
     * @return 模块名称
     */
    String getModuleName();
    
    /**
     * 获取模块描述
     * @return 模块描述
     */
    String getModuleDescription();
    
    /**
     * 是否启用该模块
     * @return 是否启用
     */
    boolean isEnabled();
    
    /**
     * 设置是否启用该模块
     * @param enabled 是否启用
     */
    void setEnabled(boolean enabled);
    
    /**
     * 获取保存方式
     * @return 保存方式
     */
    SaveMode getSaveMode();
    
    /**
     * 设置保存方式
     * @param saveMode 保存方式
     */
    void setSaveMode(SaveMode saveMode);
    
    /**
     * 获取重复处理方式
     * @return 重复处理方式
     */
    DuplicateMode getDuplicateMode();
    
    /**
     * 设置重复处理方式
     * @param duplicateMode 重复处理方式
     */
    void setDuplicateMode(DuplicateMode duplicateMode);
    
    /**
     * 是否使用缓存
     * @return 是否使用缓存
     */
    boolean isUseCache();
    
    /**
     * 设置是否使用缓存
     * @param useCache 是否使用缓存
     */
    void setUseCache(boolean useCache);
    
    /**
     * 保存方式枚举
     */
    enum SaveMode {
        EMBEDDED,      // 嵌入到文件（如歌词嵌入音频）
        SEPARATE_FILE, // 保存为独立文件
        BOTH           // 同时嵌入和保存文件
    }
    
    /**
     * 重复处理方式枚举
     */
    enum DuplicateMode {
        SKIP,       // 跳过，不处理
        OVERWRITE,  // 覆盖已有文件
        RENAME      // 重命名新文件
    }
}