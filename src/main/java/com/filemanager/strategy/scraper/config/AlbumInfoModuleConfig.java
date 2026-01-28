package com.filemanager.strategy.scraper.config;

/**
 * 专辑信息模块配置
 */
public class AlbumInfoModuleConfig implements ModuleConfig {
    private boolean enabled = true;
    private SaveMode saveMode = SaveMode.SEPARATE_FILE;
    private DuplicateMode duplicateMode = DuplicateMode.SKIP;
    private boolean useCache = true;
    private boolean includeTrackList = true;
    private boolean includeDescription = true;
    private boolean includeCopyright = true;
    
    @Override
    public String getModuleName() {
        return "专辑信息匹配";
    }
    
    @Override
    public String getModuleDescription() {
        return "搜索并下载专辑信息，包括简介、曲目列表等，保存为文本文件";
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
    
    public boolean isIncludeTrackList() {
        return includeTrackList;
    }
    
    public void setIncludeTrackList(boolean includeTrackList) {
        this.includeTrackList = includeTrackList;
    }
    
    public boolean isIncludeDescription() {
        return includeDescription;
    }
    
    public void setIncludeDescription(boolean includeDescription) {
        this.includeDescription = includeDescription;
    }
    
    public boolean isIncludeCopyright() {
        return includeCopyright;
    }
    
    public void setIncludeCopyright(boolean includeCopyright) {
        this.includeCopyright = includeCopyright;
    }
}