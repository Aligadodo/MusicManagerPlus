package com.filemanager.plugin.impl.metadatascraper;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.metadatascraper.enums.DataSource;

public class MetadataScraperStrategy extends AbstractConfigurableStrategy {

    public MetadataScraperStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "metadata-scraper";
    }

    @Override
    public String getName() {
        return "元数据抓取";
    }

    @Override
    public String getDescription() {
        return "从网络或本地抓取并更新文件的元数据信息";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("source", "数据源", "select", (Object) DataSource.LOCAL_INFERENCE.getCode(), 
            "元数据数据源", true, 
            getDataSourceOptions());
        addConfigField("threads", "线程数", "number", (Object) 4, 
            "并发抓取的线程数", false);
        addConfigField("lyricsEnabled", "启用歌词模块", "boolean", (Object) true, 
            "是否启用歌词抓取", false);
        addConfigField("coverEnabled", "启用封面模块", "boolean", (Object) true, 
            "是否启用封面抓取", false);
        addConfigField("albumInfoEnabled", "启用专辑信息模块", "boolean", (Object) true, 
            "是否启用专辑信息抓取", false);
        addConfigField("maxRequests", "最大请求数", "number", (Object) 10, 
            "单位时间内的最大请求数", false);
        addConfigField("periodMs", "时间周期", "number", (Object) 1000, 
            "限流的时间周期（毫秒）", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "source", (Object) DataSource.LOCAL_INFERENCE.getCode());
        setConfigValue(config, "threads", (Object) 4);
        setConfigValue(config, "lyricsEnabled", (Object) true);
        setConfigValue(config, "coverEnabled", (Object) true);
        setConfigValue(config, "albumInfoEnabled", (Object) true);
        setConfigValue(config, "maxRequests", (Object) 10);
        setConfigValue(config, "periodMs", (Object) 1000);
    }
    
    private java.util.List<EnumOptionDTO> getDataSourceOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (DataSource source : DataSource.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(source.getCode());
            option.setLabel(source.getNameZh());
            option.setNameEn(source.getNameEn());
            option.setDescriptionZh(source.getDescriptionZh());
            option.setDescriptionEn(source.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}
