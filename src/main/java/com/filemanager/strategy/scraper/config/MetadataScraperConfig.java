/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.strategy.scraper.config;

import com.filemanager.strategy.scraper.model.AlbumInfo;
import com.filemanager.strategy.scraper.model.CoverInfo;
import com.filemanager.strategy.scraper.model.LyricsInfo;
import com.filemanager.strategy.scraper.source.MetadataSource;
import com.filemanager.strategy.scraper.source.impl.*;
import com.filemanager.strategy.scraper.ui.AlbumInfoConfigUI;
import com.filemanager.strategy.scraper.ui.ModuleConfigUI;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Spinner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 元数据刮削策略配置类
 * 负责管理元数据刮削策略的UI组件和配置参数
 */
public class MetadataScraperConfig {
    private final JFXComboBox<String> cbSource;
    private final Spinner<Integer> spThreads;
    
    private final ModuleConfigUI lyricsConfigUI;
    private final ModuleConfigUI coverConfigUI;
    private final AlbumInfoConfigUI albumInfoConfigUI;
    
    private final Map<String, MetadataSource> sources;
    
    private String source;
    private int threads;
    
    private LyricsModuleConfig lyricsConfig;
    private CoverModuleConfig coverConfig;
    private AlbumInfoModuleConfig albumInfoConfig;

    public MetadataScraperConfig() {
        sources = new LinkedHashMap<>();
        sources.put("本地推断 (仅生成清单)", new LocalInferenceSource());
        sources.put("网易云音乐 (中文歌曲) (不完善)", new NeteaseMusicSource());
        sources.put("咪咕音乐 (版权歌曲) (不完善)", new MiguMusicSource());
        sources.put("MusicBrainz (开源数据库)", new MusicBrainzSource());
        sources.put("iTunes (苹果音乐)", new ITunesSource());
        sources.put("Last.fm (全球音乐平台) (不完善)", new LastFmSource());
        sources.put("Discogs (音乐数据库) (不完善)", new DiscogsSource());
        
        cbSource = new JFXComboBox<>(FXCollections.observableArrayList(sources.keySet()));
        cbSource.getSelectionModel().select(0);
        
        updateSourceTooltip();
        
        cbSource.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateSourceTooltip();
        });

        spThreads = new Spinner<>(1, 8, 2);
        
        ArrayList<String> threadsTooltipLines = new ArrayList<>();
        threadsTooltipLines.add("参数名称：并发线程数");
        threadsTooltipLines.add("参数用途：设置同时处理的文件数量");
        threadsTooltipLines.add("示例：");
        threadsTooltipLines.add("- 1：单线程处理，速度慢但稳定");
        threadsTooltipLines.add("- 4：四线程处理，速度较快");
        threadsTooltipLines.add("- 8：八线程处理，速度最快但可能占用较多资源");
        com.filemanager.app.tools.display.FloatingTooltip.bindToNode(spThreads, "并发线程数设置", threadsTooltipLines);

        lyricsConfigUI = new ModuleConfigUI(new LyricsModuleConfig());
        coverConfigUI = new ModuleConfigUI(new CoverModuleConfig());
        albumInfoConfigUI = new AlbumInfoConfigUI(new AlbumInfoModuleConfig());
    }

    private void updateSourceTooltip() {
        String selectedSource = cbSource.getValue();
        MetadataSource source = sources.get(selectedSource);
        
        ArrayList<String> tooltipLines = new ArrayList<>();
        tooltipLines.add("参数名称：元数据来源");
        tooltipLines.add("参数用途：选择从哪个平台获取元数据");
        tooltipLines.add("示例：");
        tooltipLines.add("- 本地推断：仅根据文件名推断，不联网");
        tooltipLines.add("- 网易云音乐：适合中文歌曲");
        tooltipLines.add("- MusicBrainz：开源数据库，数据全面");
        
        if (source != null) {
            tooltipLines.add("当前选择：" + source.getSourceName());
            tooltipLines.add("描述：" + source.getSourceDescription());
        }
        
        com.filemanager.app.tools.display.FloatingTooltip.bindToNode(cbSource, "元数据来源选择", tooltipLines);
    }

    public Node getConfigNode() {
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));
        vbox.getChildren().addAll(
                new javafx.scene.control.Label("元数据来源:"),
                cbSource,
                new javafx.scene.control.Label("并发线程数:"),
                spThreads,
                lyricsConfigUI,
                coverConfigUI,
                albumInfoConfigUI
        );
        return vbox;
    }

    public void captureParams() {
        source = cbSource.getValue();
        threads = spThreads.getValue();
        
        lyricsConfigUI.captureParams();
        coverConfigUI.captureParams();
        albumInfoConfigUI.captureParams();
        
        ModuleConfig lyricsModuleConfig = lyricsConfigUI.getConfig();
        ModuleConfig coverModuleConfig = coverConfigUI.getConfig();
        ModuleConfig albumModuleConfig = albumInfoConfigUI.getConfig();
        
        lyricsConfig = (LyricsModuleConfig) lyricsModuleConfig;
        coverConfig = (CoverModuleConfig) coverModuleConfig;
        albumInfoConfig = (AlbumInfoModuleConfig) albumModuleConfig;
    }

    public void saveConfig(Properties props) {
        props.setProperty("mss_source", cbSource.getValue());
        props.setProperty("mss_threads", String.valueOf(spThreads.getValue()));
        
        lyricsConfigUI.captureParams();
        coverConfigUI.captureParams();
        albumInfoConfigUI.captureParams();
        
        props.setProperty("mss_lyrics_enabled", String.valueOf(lyricsConfig.isEnabled()));
        props.setProperty("mss_lyrics_save_mode", lyricsConfig.getSaveMode().name());
        props.setProperty("mss_lyrics_duplicate", lyricsConfig.getDuplicateMode().name());
        props.setProperty("mss_lyrics_cache", String.valueOf(lyricsConfig.isUseCache()));
        
        props.setProperty("mss_cover_enabled", String.valueOf(coverConfig.isEnabled()));
        props.setProperty("mss_cover_save_mode", coverConfig.getSaveMode().name());
        props.setProperty("mss_cover_duplicate", coverConfig.getDuplicateMode().name());
        props.setProperty("mss_cover_cache", String.valueOf(coverConfig.isUseCache()));
        
        props.setProperty("mss_album_enabled", String.valueOf(albumInfoConfig.isEnabled()));
        props.setProperty("mss_album_save_mode", albumInfoConfig.getSaveMode().name());
        props.setProperty("mss_album_duplicate", albumInfoConfig.getDuplicateMode().name());
        props.setProperty("mss_album_cache", String.valueOf(albumInfoConfig.isUseCache()));
    }

    public void loadConfig(Properties props) {
        if (props.containsKey("mss_source")) {
            cbSource.setValue(props.getProperty("mss_source"));
        }
        if (props.containsKey("mss_threads")) {
            spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("mss_threads")));
        }
        
        if (props.containsKey("mss_lyrics_enabled"))
            lyricsConfig.setEnabled(Boolean.parseBoolean(props.getProperty("mss_lyrics_enabled")));
        if (props.containsKey("mss_lyrics_save_mode"))
            lyricsConfig.setSaveMode(com.filemanager.strategy.scraper.config.ModuleConfig.SaveMode.valueOf(props.getProperty("mss_lyrics_save_mode")));
        if (props.containsKey("mss_lyrics_duplicate"))
            lyricsConfig.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.valueOf(props.getProperty("mss_lyrics_duplicate")));
        if (props.containsKey("mss_lyrics_cache"))
            lyricsConfig.setUseCache(Boolean.parseBoolean(props.getProperty("mss_lyrics_cache")));
        
        if (props.containsKey("mss_cover_enabled"))
            coverConfig.setEnabled(Boolean.parseBoolean(props.getProperty("mss_cover_enabled")));
        if (props.containsKey("mss_cover_save_mode"))
            coverConfig.setSaveMode(com.filemanager.strategy.scraper.config.ModuleConfig.SaveMode.valueOf(props.getProperty("mss_cover_save_mode")));
        if (props.containsKey("mss_cover_duplicate"))
            coverConfig.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.valueOf(props.getProperty("mss_cover_duplicate")));
        if (props.containsKey("mss_cover_cache"))
            coverConfig.setUseCache(Boolean.parseBoolean(props.getProperty("mss_cover_cache")));
        
        if (props.containsKey("mss_album_enabled"))
            albumInfoConfig.setEnabled(Boolean.parseBoolean(props.getProperty("mss_album_enabled")));
        if (props.containsKey("mss_album_save_mode"))
            albumInfoConfig.setSaveMode(com.filemanager.strategy.scraper.config.ModuleConfig.SaveMode.valueOf(props.getProperty("mss_album_save_mode")));
        if (props.containsKey("mss_album_duplicate"))
            albumInfoConfig.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.valueOf(props.getProperty("mss_album_duplicate")));
        if (props.containsKey("mss_album_cache"))
            albumInfoConfig.setUseCache(Boolean.parseBoolean(props.getProperty("mss_album_cache")));
        
        lyricsConfigUI.loadFromConfig(lyricsConfig);
        coverConfigUI.loadFromConfig(coverConfig);
        albumInfoConfigUI.loadFromConfig(albumInfoConfig);
    }

    public String getSource() {
        return source;
    }

    public int getThreads() {
        return threads;
    }

    public LyricsModuleConfig getLyricsConfig() {
        return lyricsConfig;
    }

    public CoverModuleConfig getCoverConfig() {
        return coverConfig;
    }

    public AlbumInfoModuleConfig getAlbumInfoConfig() {
        return albumInfoConfig;
    }

    public Map<String, MetadataSource> getSources() {
        return sources;
    }
}
