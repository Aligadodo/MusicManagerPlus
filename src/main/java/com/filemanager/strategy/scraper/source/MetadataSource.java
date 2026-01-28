package com.filemanager.strategy.scraper.source;

import com.filemanager.strategy.scraper.model.LyricsInfo;
import com.filemanager.strategy.scraper.model.CoverInfo;
import com.filemanager.strategy.scraper.model.AlbumInfo;
import com.filemanager.strategy.scraper.model.TrackInfo;

import java.util.EnumSet;

/**
 * 元数据数据源接口
 * 所有数据源都需要实现此接口，提供歌词、封面、专辑信息等刮削功能
 */
public interface MetadataSource {
    
    /**
     * 获取数据源名称
     * @return 数据源名称
     */
    String getSourceName();
    
    /**
     * 获取数据源描述
     * @return 数据源详细描述，用于UI显示
     */
    String getSourceDescription();
    
    /**
     * 获取数据源支持的功能
     * @return 支持的功能集合
     */
    EnumSet<SourceCapabilities> getCapabilities();
    
    /**
     * 搜索歌词
     * @param artist 艺术家
     * @param title 歌曲标题
     * @param duration 歌曲时长（秒）
     * @return 歌词信息，如果未找到返回null
     */
    LyricsInfo searchLyrics(String artist, String title, int duration);
    
    /**
     * 搜索封面
     * @param artist 艺术家
     * @param album 专辑名称
     * @return 封面信息，如果未找到返回null
     */
    CoverInfo searchCover(String artist, String album);
    
    /**
     * 搜索专辑信息
     * @param artist 艺术家
     * @param album 专辑名称
     * @return 专辑信息，如果未找到返回null
     */
    AlbumInfo searchAlbumInfo(String artist, String album);
    
    /**
     * 搜索曲目信息
     * @param artist 艺术家
     * @param title 歌曲标题
     * @return 曲目信息，如果未找到返回null
     */
    TrackInfo searchTrackInfo(String artist, String title);
    
    /**
     * 数据源能力枚举
     */
    enum SourceCapabilities {
        LYRICS,      // 支持歌词搜索
        COVER,        // 支持封面搜索
        ALBUM_INFO,   // 支持专辑信息搜索
        TRACK_INFO,   // 支持曲目信息搜索
        EMBED_LYRICS, // 支持歌词嵌入
        HIGH_QUALITY  // 支持高质量资源
    }
}