package com.filemanager.plugin.impl.albumdirnormalize;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.albumdirnormalize.enums.DirectoryTemplate;

public class AlbumDirNormalizeStrategy extends AbstractConfigurableStrategy {

    public AlbumDirNormalizeStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "album-dir-normalize";
    }

    @Override
    public String getName() {
        return "专辑目录标准化";
    }

    @Override
    public String getDescription() {
        return "根据元数据标准化专辑目录结构，支持多种命名模板。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void initConfigFields() {
        addConfigField("template", "目录命名模板", "select", (Object) DirectoryTemplate.ARTIST_YEAR_ALBUM.getCode(), 
            "专辑目录的命名模板", true, 
            getDirectoryTemplateOptions());
        addConfigField("customTemplate", "自定义模板", "string", (Object) "", 
            "自定义命名模板", false);
        addConfigField("cleanSpecialChars", "清理特殊字符", "boolean", (Object) true, 
            "清理目录名中的特殊字符", false);
        addConfigField("removeYearPrefix", "移除年份前缀", "boolean", (Object) false, 
            "移除目录名中的年份前缀", false);
        addConfigField("useConsensusMetadata", "使用共识元数据", "boolean", (Object) true, 
            "使用多个文件的共识元数据", false);
        addConfigField("preserveOriginalName", "保留原始名称", "boolean", (Object) true, 
            "当无法获取元数据时保留原始目录名", false);
        addConfigField("validateAlbumInfo", "验证专辑信息", "boolean", (Object) true, 
            "验证专辑信息的完整性", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "template", (Object) DirectoryTemplate.ARTIST_YEAR_ALBUM.getCode());
        setConfigValue(config, "customTemplate", (Object) "");
        setConfigValue(config, "cleanSpecialChars", (Object) true);
        setConfigValue(config, "removeYearPrefix", (Object) false);
        setConfigValue(config, "useConsensusMetadata", (Object) true);
        setConfigValue(config, "preserveOriginalName", (Object) true);
        setConfigValue(config, "validateAlbumInfo", (Object) true);
    }
    
    private java.util.List<String> getDirectoryTemplateOptions() {
        return java.util.Arrays.asList(
            DirectoryTemplate.ARTIST_YEAR_ALBUM.getCode(),
            DirectoryTemplate.YEAR_ARTIST_ALBUM.getCode(),
            DirectoryTemplate.ARTIST_ALBUM_YEAR.getCode(),
            DirectoryTemplate.YEAR_ALBUM_ARTIST.getCode(),
            DirectoryTemplate.ALBUM_ARTIST_YEAR.getCode(),
            DirectoryTemplate.ARTIST_ALBUM.getCode(),
            DirectoryTemplate.ALBUM_YEAR.getCode(),
            DirectoryTemplate.CUSTOM.getCode()
        );
    }
}
