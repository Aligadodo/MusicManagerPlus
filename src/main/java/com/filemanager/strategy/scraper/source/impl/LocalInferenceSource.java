package com.filemanager.strategy.scraper.source.impl;

import com.filemanager.strategy.scraper.model.*;
import com.filemanager.strategy.scraper.source.MetadataSource;

import java.io.File;
import java.util.*;

/**
 * 本地推断数据源
 * 基于文件系统信息推断元数据，不进行网络请求
 */
public class LocalInferenceSource implements MetadataSource {
    
    @Override
    public String getSourceName() {
        return "本地推断";
    }
    
    @Override
    public String getSourceDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("本地推断数据源说明：\n");
        desc.append("功能特点：\n");
        desc.append("- 仅基于本地文件系统信息生成元数据\n");
        desc.append("- 不进行任何网络请求，速度快且稳定\n");
        desc.append("- 支持从CUE文件提取曲目信息\n");
        desc.append("- 支持从文件名推断基本信息\n");
        desc.append("\n");
        desc.append("支持功能：\n");
        desc.append("- 歌词：不支持\n");
        desc.append("- 封面：不支持\n");
        desc.append("- 专辑信息：支持（基于文件列表生成）\n");
        desc.append("- 曲目信息：支持（基于CUE文件或文件名）\n");
        desc.append("\n");
        desc.append("适用场景：\n");
        desc.append("- 离线环境下的元数据整理\n");
        desc.append("- 生成简单的专辑清单文件\n");
        desc.append("- 从CUE文件提取曲目信息\n");
        desc.append("\n");
        desc.append("数据质量：\n");
        desc.append("- 依赖文件名和目录结构的规范性\n");
        desc.append("- 信息可能不完整或不准确\n");
        desc.append("- 建议作为补充数据源使用");
        return desc.toString();
    }
    
    @Override
    public EnumSet<SourceCapabilities> getCapabilities() {
        EnumSet<SourceCapabilities> caps = EnumSet.of(
                SourceCapabilities.ALBUM_INFO,
                SourceCapabilities.TRACK_INFO
        );
        return caps;
    }
    
    @Override
    public LyricsInfo searchLyrics(String artist, String title, int duration) {
        return null; // 本地推断不支持歌词搜索
    }
    
    @Override
    public CoverInfo searchCover(String artist, String album) {
        return null; // 本地推断不支持封面搜索
    }
    
    @Override
    public AlbumInfo searchAlbumInfo(String artist, String album) {
        AlbumInfo info = new AlbumInfo();
        info.setName(album != null ? album : "Unknown Album");
        info.setArtist(artist != null ? artist : "Unknown Artist");
        info.setSource(getSourceName());
        info.setDescription("此专辑信息由本地推断生成，基于文件系统信息。");
        return info;
    }
    
    @Override
    public TrackInfo searchTrackInfo(String artist, String title) {
        TrackInfo info = new TrackInfo();
        info.setTitle(title != null ? title : "Unknown Title");
        info.setArtist(artist != null ? artist : "Unknown Artist");
        info.setSource(getSourceName());
        return info;
    }
}