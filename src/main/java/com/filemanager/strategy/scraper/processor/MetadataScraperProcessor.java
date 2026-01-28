package com.filemanager.strategy.scraper.processor;

import com.filemanager.strategy.scraper.cache.MetadataCacheManager;
import com.filemanager.strategy.scraper.config.*;
import com.filemanager.strategy.scraper.model.*;
import com.filemanager.strategy.scraper.source.MetadataSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

/**
 * 元数据刮削处理器
 * 负责执行具体的刮削操作
 */
public class MetadataScraperProcessor {
    private MetadataSource source;
    private MetadataCacheManager cacheManager;
    private LyricsModuleConfig lyricsConfig;
    private CoverModuleConfig coverConfig;
    private AlbumInfoModuleConfig albumInfoConfig;
    
    public MetadataScraperProcessor(MetadataCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    public void setSource(MetadataSource source) {
        this.source = source;
    }
    
    public void setLyricsConfig(LyricsModuleConfig lyricsConfig) {
        this.lyricsConfig = lyricsConfig;
    }
    
    public void setCoverConfig(CoverModuleConfig coverConfig) {
        this.coverConfig = coverConfig;
    }
    
    public void setAlbumInfoConfig(AlbumInfoModuleConfig albumInfoConfig) {
        this.albumInfoConfig = albumInfoConfig;
    }
    
    /**
     * 处理歌词
     * @param artist 艺术家
     * @param title 歌曲标题
     * @param duration 歌曲时长（秒）
     * @param targetFile 目标文件
     * @return 是否成功处理
     */
    public boolean processLyrics(String artist, String title, int duration, File targetFile) {
        if (!lyricsConfig.isEnabled()) {
            return false;
        }
        
        String cacheKey = "lyrics:" + artist + ":" + title;
        LyricsInfo lyrics = null;
        
        if (lyricsConfig.isUseCache()) {
            lyrics = cacheManager.getCachedLyrics(cacheKey);
        }
        
        if (lyrics == null) {
            lyrics = source.searchLyrics(artist, title, duration);
            if (lyrics != null && lyricsConfig.isUseCache()) {
                cacheManager.cacheLyrics(cacheKey, lyrics);
            }
        }
        
        if (lyrics == null) {
            return false;
        }
        
        return saveLyrics(lyrics, targetFile);
    }
    
    /**
     * 处理封面
     * @param artist 艺术家
     * @param album 专辑名称
     * @param targetDir 目标目录
     * @return 是否成功处理
     */
    public boolean processCover(String artist, String album, File targetDir) {
        if (!coverConfig.isEnabled()) {
            return false;
        }
        
        String cacheKey = "cover:" + artist + ":" + album;
        CoverInfo cover = null;
        
        if (coverConfig.isUseCache()) {
            cover = cacheManager.getCachedCover(cacheKey);
        }
        
        if (cover == null) {
            cover = source.searchCover(artist, album);
            if (cover != null && coverConfig.isUseCache()) {
                cacheManager.cacheCover(cacheKey, cover);
            }
        }
        
        if (cover == null) {
            return false;
        }
        
        return saveCover(cover, targetDir);
    }
    
    /**
     * 处理专辑信息
     * @param artist 艺术家
     * @param album 专辑名称
     * @param targetDir 目标目录
     * @return 是否成功处理
     */
    public boolean processAlbumInfo(String artist, String album, File targetDir) {
        if (!albumInfoConfig.isEnabled()) {
            return false;
        }
        
        String cacheKey = "album:" + artist + ":" + album;
        AlbumInfo albumInfo = null;
        
        if (albumInfoConfig.isUseCache()) {
            albumInfo = cacheManager.getCachedAlbumInfo(cacheKey);
        }
        
        if (albumInfo == null) {
            albumInfo = source.searchAlbumInfo(artist, album);
            if (albumInfo != null && albumInfoConfig.isUseCache()) {
                cacheManager.cacheAlbumInfo(cacheKey, albumInfo);
            }
        }
        
        if (albumInfo == null) {
            return false;
        }
        
        return saveAlbumInfo(albumInfo, targetDir);
    }
    
    private boolean saveLyrics(LyricsInfo lyrics, File targetFile) {
        try {
            ModuleConfig.SaveMode saveMode = lyricsConfig.getSaveMode();
            
            if (saveMode == ModuleConfig.SaveMode.EMBEDDED || saveMode == ModuleConfig.SaveMode.BOTH) {
                return saveLyricsEmbedded(lyrics, targetFile);
            }
            
            if (saveMode == ModuleConfig.SaveMode.SEPARATE_FILE || saveMode == ModuleConfig.SaveMode.BOTH) {
                return saveLyricsAsFile(lyrics, targetFile);
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Failed to save lyrics: " + e.getMessage());
            return false;
        }
    }
    
    private boolean saveLyricsEmbedded(LyricsInfo lyrics, File targetFile) {
        return false;
    }
    
    private boolean saveLyricsAsFile(LyricsInfo lyrics, File targetFile) {
        try {
            String baseName = targetFile.getName();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = baseName.substring(0, dotIndex);
            }
            
            File lyricsFile = new File(targetFile.getParentFile(), baseName + ".lrc");
            
            if (lyricsFile.exists()) {
                ModuleConfig.DuplicateMode dupMode = lyricsConfig.getDuplicateMode();
                if (dupMode == ModuleConfig.DuplicateMode.SKIP) {
                    return false;
                }
            }
            
            Files.write(lyricsFile.toPath(), lyrics.getContent().getBytes());
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save lyrics as file: " + e.getMessage());
            return false;
        }
    }
    
    private boolean saveCover(CoverInfo cover, File targetDir) {
        try {
            File coverFile = new File(targetDir, "cover.jpg");
            
            if (coverFile.exists()) {
                ModuleConfig.DuplicateMode dupMode = coverConfig.getDuplicateMode();
                if (dupMode == ModuleConfig.DuplicateMode.SKIP) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save cover: " + e.getMessage());
            return false;
        }
    }
    
    private boolean saveAlbumInfo(AlbumInfo albumInfo, File targetDir) {
        try {
            File infoFile = new File(targetDir, "AlbumInfo.txt");
            
            if (infoFile.exists()) {
                ModuleConfig.DuplicateMode dupMode = albumInfoConfig.getDuplicateMode();
                if (dupMode == ModuleConfig.DuplicateMode.SKIP) {
                    return false;
                }
            }
            
            StringBuilder content = new StringBuilder();
            content.append("专辑名称: ").append(albumInfo.getName()).append("\n");
            content.append("艺术家: ").append(albumInfo.getArtist()).append("\n");
            
            if (albumInfo.getYear() != null) {
                content.append("发行年份: ").append(albumInfo.getYear()).append("\n");
            }
            
            if (albumInfo.getGenre() != null) {
                content.append("流派: ").append(albumInfo.getGenre()).append("\n");
            }
            
            if (albumInfoConfig.isIncludeDescription() && albumInfo.getDescription() != null) {
                content.append("\n专辑简介:\n").append(albumInfo.getDescription()).append("\n");
            }
            
            if (albumInfoConfig.isIncludeCopyright() && albumInfo.getCopyright() != null) {
                content.append("\n版权信息:\n").append(albumInfo.getCopyright()).append("\n");
            }
            
            if (albumInfoConfig.isIncludeTrackList() && !albumInfo.getTracks().isEmpty()) {
                content.append("\n曲目列表:\n");
                for (int i = 0; i < albumInfo.getTracks().size(); i++) {
                    TrackInfo track = albumInfo.getTracks().get(i);
                    content.append(String.format("%d. %s - %s\n", 
                            i + 1, track.getTitle(), track.getArtist()));
                }
            }
            
            Files.write(infoFile.toPath(), content.toString().getBytes());
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save album info: " + e.getMessage());
            return false;
        }
    }
}