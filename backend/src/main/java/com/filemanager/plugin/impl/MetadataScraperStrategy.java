package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;

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
        addConfigField("source", "数据源", "select", (Object) "本地推断 (仅生成清单)", 
            "元数据数据源", true, 
            Arrays.asList("本地推断 (仅生成清单)", "网易云音乐 (中文歌曲) (不完善)", 
                      "咪咕音乐 (版权歌曲) (不完善)", "MusicBrainz (开源数据库)", 
                      "iTunes (苹果音乐)", "Last.fm (全球音乐平台) (不完善)", 
                      "Discogs (音乐数据库) (不完善)"));
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
        setConfigValue(config, "source", (Object) "本地推断 (仅生成清单)");
        setConfigValue(config, "threads", (Object) 4);
        setConfigValue(config, "lyricsEnabled", (Object) true);
        setConfigValue(config, "coverEnabled", (Object) true);
        setConfigValue(config, "albumInfoEnabled", (Object) true);
        setConfigValue(config, "maxRequests", (Object) 10);
        setConfigValue(config, "periodMs", (Object) 1000);
    }
}