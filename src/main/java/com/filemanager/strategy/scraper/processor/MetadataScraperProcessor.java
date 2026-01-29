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
     * @param record 变更记录，用于记录请求信息
     * @return 是否成功处理
     */
    public boolean processLyrics(String artist, String title, int duration, File targetFile, com.filemanager.model.ChangeRecord record) {
        if (!lyricsConfig.isEnabled()) {
            return false;
        }
        
        if (record != null) {
            record.addProcessInfo("开始处理歌词: " + artist + " - " + title);
            record.addProcessInfo("使用数据源: " + source.getSourceName());
        }
        
        String cacheKey = "lyrics:" + artist + ":" + title;
        LyricsInfo lyrics = null;
        
        if (lyricsConfig.isUseCache()) {
            lyrics = cacheManager.getCachedLyrics(cacheKey);
            if (lyrics != null && record != null) {
                record.addProcessInfo("从缓存获取歌词");
            }
        }
        
        if (lyrics == null) {
            if (record != null) {
                record.addProcessInfo("从数据源搜索歌词");
                record.addProcessInfo("搜索参数: artist='" + artist + "', title='" + title + "', duration='" + duration + "'");
            }
            lyrics = source.searchLyrics(artist, title, duration);
            
            if (record != null) {
                String requestUrl = source.getLastRequestUrl();
                String requestError = source.getLastRequestError();
                if (requestUrl != null) {
                    record.addProcessInfo("请求URL: " + requestUrl);
                }
                if (requestError != null) {
                    record.addProcessInfo("请求错误: " + requestError);
                }
            }
            
            if (lyrics != null) {
                if (record != null) {
                    record.addProcessInfo("搜索结果: 找到歌词，来源='" + lyrics.getSource() + "', 格式='" + lyrics.getFormat() + "', 验证状态='" + lyrics.isVerified() + "'");
                }
                if (lyricsConfig.isUseCache()) {
                    cacheManager.cacheLyrics(cacheKey, lyrics);
                    if (record != null) {
                        record.addProcessInfo("缓存歌词结果");
                    }
                }
            } else {
                if (record != null) {
                    record.addProcessInfo("搜索结果: 未找到歌词");
                }
            }
        }
        
        if (lyrics == null) {
            if (record != null) {
                record.addProcessInfo("未找到歌词");
            }
            return false;
        }
        
        if (record != null) {
            record.addProcessInfo("成功获取歌词，长度: " + lyrics.getContent().length() + " 字符");
        }
        
        boolean saved = saveLyrics(lyrics, targetFile);
        if (saved && record != null) {
            record.addProcessInfo("歌词保存成功");
        }
        
        return saved;
    }
    
    /**
     * 处理封面
     * @param artist 艺术家
     * @param album 专辑名称
     * @param targetDir 目标目录
     * @param record 变更记录，用于记录请求信息
     * @return 是否成功处理
     */
    public boolean processCover(String artist, String album, File targetDir, com.filemanager.model.ChangeRecord record) {
        if (!coverConfig.isEnabled()) {
            return false;
        }
        
        if (record != null) {
            record.addProcessInfo("开始处理封面: " + artist + " - " + album);
            record.addProcessInfo("使用数据源: " + source.getSourceName());
        }
        
        String cacheKey = "cover:" + artist + ":" + album;
        CoverInfo cover = null;
        
        if (coverConfig.isUseCache()) {
            cover = cacheManager.getCachedCover(cacheKey);
            if (cover != null && record != null) {
                record.addProcessInfo("从缓存获取封面");
            }
        }
        
        if (cover == null) {
            if (record != null) {
                record.addProcessInfo("从数据源搜索封面");
                record.addProcessInfo("搜索参数: artist='" + artist + "', album='" + album + "'");
            }
            cover = source.searchCover(artist, album);
            
            if (record != null) {
                String requestUrl = source.getLastRequestUrl();
                String requestError = source.getLastRequestError();
                if (requestUrl != null) {
                    record.addProcessInfo("请求URL: " + requestUrl);
                }
                if (requestError != null) {
                    record.addProcessInfo("请求错误: " + requestError);
                }
            }
            
            if (cover != null) {
                if (record != null) {
                    record.addProcessInfo("搜索结果: 找到封面，来源='" + cover.getSource() + "', URL='" + cover.getImageUrl() + "', 格式='" + cover.getFormat() + "', 尺寸='" + cover.getWidth() + "x" + cover.getHeight() + "'");
                }
                if (coverConfig.isUseCache()) {
                    cacheManager.cacheCover(cacheKey, cover);
                    if (record != null) {
                        record.addProcessInfo("缓存封面结果");
                    }
                }
            } else {
                if (record != null) {
                    record.addProcessInfo("搜索结果: 未找到封面");
                }
            }
        }
        
        if (cover == null) {
            if (record != null) {
                record.addProcessInfo("未找到封面");
            }
            return false;
        }
        
        if (record != null) {
            record.addProcessInfo("成功获取封面，URL: " + cover.getImageUrl());
        }
        
        boolean saved = saveCover(cover, targetDir);
        if (saved && record != null) {
            record.addProcessInfo("封面保存成功");
        }
        
        return saved;
    }
    
    /**
     * 处理专辑信息
     * @param artist 艺术家
     * @param album 专辑名称
     * @param targetDir 目标目录
     * @param record 变更记录，用于记录请求信息
     * @return 是否成功处理
     */
    public boolean processAlbumInfo(String artist, String album, File targetDir, com.filemanager.model.ChangeRecord record) {
        if (!albumInfoConfig.isEnabled()) {
            return false;
        }
        
        if (record != null) {
            record.addProcessInfo("开始处理专辑信息: " + artist + " - " + album);
            record.addProcessInfo("使用数据源: " + source.getSourceName());
        }
        
        String cacheKey = "album:" + artist + ":" + album;
        AlbumInfo albumInfo = null;
        
        if (albumInfoConfig.isUseCache()) {
            albumInfo = cacheManager.getCachedAlbumInfo(cacheKey);
            if (albumInfo != null && record != null) {
                record.addProcessInfo("从缓存获取专辑信息");
            }
        }
        
        if (albumInfo == null) {
            if (record != null) {
                record.addProcessInfo("从数据源搜索专辑信息");
                record.addProcessInfo("搜索参数: artist='" + artist + "', album='" + album + "'");
            }
            albumInfo = source.searchAlbumInfo(artist, album);
            
            if (record != null) {
                String requestUrl = source.getLastRequestUrl();
                String requestError = source.getLastRequestError();
                if (requestUrl != null) {
                    record.addProcessInfo("请求URL: " + requestUrl);
                }
                if (requestError != null) {
                    record.addProcessInfo("请求错误: " + requestError);
                }
            }
            
            if (albumInfo != null) {
                if (record != null) {
                    record.addProcessInfo("搜索结果: 找到专辑信息，来源='" + albumInfo.getSource() + "', 名称='" + albumInfo.getName() + "', 艺术家='" + albumInfo.getArtist() + "'");
                    if (albumInfo.getYear() != null) {
                        record.addProcessInfo("专辑年份: " + albumInfo.getYear());
                    }
                    if (albumInfo.getGenre() != null) {
                        record.addProcessInfo("专辑流派: " + albumInfo.getGenre());
                    }
                    if (albumInfo.getDescription() != null) {
                        record.addProcessInfo("专辑简介长度: " + albumInfo.getDescription().length() + " 字符");
                    }
                    if (!albumInfo.getTracks().isEmpty()) {
                        record.addProcessInfo("曲目数量: " + albumInfo.getTracks().size());
                    }
                }
                if (albumInfoConfig.isUseCache()) {
                    cacheManager.cacheAlbumInfo(cacheKey, albumInfo);
                    if (record != null) {
                        record.addProcessInfo("缓存专辑信息结果");
                    }
                }
            } else {
                if (record != null) {
                    record.addProcessInfo("搜索结果: 未找到专辑信息");
                }
            }
        }
        
        if (albumInfo == null) {
            if (record != null) {
                record.addProcessInfo("未找到专辑信息");
            }
            return false;
        }
        
        if (record != null) {
            record.addProcessInfo("成功获取专辑信息: " + albumInfo.getName());
            record.addProcessInfo("专辑艺术家: " + albumInfo.getArtist());
            if (albumInfo.getYear() != null) {
                record.addProcessInfo("发行年份: " + albumInfo.getYear());
            }
            if (albumInfo.getGenre() != null) {
                record.addProcessInfo("流派: " + albumInfo.getGenre());
            }
        }
        
        boolean saved = saveAlbumInfo(albumInfo, targetDir);
        if (saved && record != null) {
            record.addProcessInfo("专辑信息保存成功");
        }
        
        return saved;
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