package com.filemanager.strategy.scraper.config;

/**
 * 歌词模块配置
 */
public class LyricsModuleConfig implements ModuleConfig {
    private boolean enabled = true;
    private SaveMode saveMode = SaveMode.EMBEDDED;
    private DuplicateMode duplicateMode = DuplicateMode.SKIP;
    private boolean useCache = true;
    
    @Override
    public String getModuleName() {
        return "歌词匹配";
    }
    
    @Override
    public String getModuleDescription() {
        return "搜索并下载歌曲歌词，支持嵌入到音频文件或保存为独立LRC文件";
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
}