package com.filemanager.strategy.scraper.config;

/**
 * 封面模块配置
 */
public class CoverModuleConfig implements ModuleConfig {
    private boolean enabled = true;
    private SaveMode saveMode = SaveMode.SEPARATE_FILE;
    private DuplicateMode duplicateMode = DuplicateMode.SKIP;
    private boolean useCache = true;
    private int preferredSize = 600; // 首选尺寸
    
    @Override
    public String getModuleName() {
        return "封面匹配";
    }
    
    @Override
    public String getModuleDescription() {
        return "搜索并下载专辑封面，支持嵌入到音频文件或保存为独立图片文件";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public SaveMode getSaveMode() {
        return saveMode;
    }
    
    @Override
    public void setSaveMode(SaveMode saveMode) {
        this.saveMode = saveMode;
    }
    
    @Override
    public DuplicateMode getDuplicateMode() {
        return duplicateMode;
    }
    
    @Override
    public void setDuplicateMode(DuplicateMode duplicateMode) {
        this.duplicateMode = duplicateMode;
    }
    
    @Override
    public boolean isUseCache() {
        return useCache;
    }
    
    @Override
    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }
    
    public int getPreferredSize() {
        return preferredSize;
    }
    
    public void setPreferredSize(int preferredSize) {
        this.preferredSize = preferredSize;
    }
}