package com.filemanager.strategy.scraper.cache;

import com.filemanager.strategy.scraper.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元数据缓存管理器
 * 用于缓存刮削结果，提高性能和减少网络请求
 */
public class MetadataCacheManager {
    private final Map<String, CachedData> memoryCache;
    private final String cacheDir;
    private final boolean enableDiskCache;
    
    public MetadataCacheManager(boolean enableDiskCache) {
        this.memoryCache = new ConcurrentHashMap<>();
        this.enableDiskCache = enableDiskCache;
        this.cacheDir = System.getProperty("user.home") + File.separator + ".filemanager" + File.separator + "metadata_cache";
        
        if (enableDiskCache) {
            initDiskCache();
        }
    }
    
    private void initDiskCache() {
        try {
            Path cachePath = Paths.get(cacheDir);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize disk cache: " + e.getMessage());
        }
    }
    
    /**
     * 缓存歌词信息
     * @param key 缓存键
     * @param lyrics 歌词信息
     */
    public void cacheLyrics(String key, LyricsInfo lyrics) {
        CachedData data = new CachedData();
        data.type = CacheType.LYRICS;
        data.timestamp = System.currentTimeMillis();
        data.lyrics = lyrics;
        memoryCache.put(key, data);
        
        if (enableDiskCache) {
            saveToDisk(key, data);
        }
    }
    
    /**
     * 获取缓存的歌词信息
     * @param key 缓存键
     * @return 歌词信息，如果不存在返回null
     */
    public LyricsInfo getCachedLyrics(String key) {
        CachedData data = memoryCache.get(key);
        if (data != null && data.type == CacheType.LYRICS) {
            return data.lyrics;
        }
        
        if (enableDiskCache) {
            data = loadFromDisk(key);
            if (data != null && data.type == CacheType.LYRICS) {
                memoryCache.put(key, data);
                return data.lyrics;
            }
        }
        
        return null;
    }
    
    /**
     * 缓存封面信息
     * @param key 缓存键
     * @param cover 封面信息
     */
    public void cacheCover(String key, CoverInfo cover) {
        CachedData data = new CachedData();
        data.type = CacheType.COVER;
        data.timestamp = System.currentTimeMillis();
        data.cover = cover;
        memoryCache.put(key, data);
        
        if (enableDiskCache) {
            saveToDisk(key, data);
        }
    }
    
    /**
     * 获取缓存的封面信息
     * @param key 缓存键
     * @return 封面信息，如果不存在返回null
     */
    public CoverInfo getCachedCover(String key) {
        CachedData data = memoryCache.get(key);
        if (data != null && data.type == CacheType.COVER) {
            return data.cover;
        }
        
        if (enableDiskCache) {
            data = loadFromDisk(key);
            if (data != null && data.type == CacheType.COVER) {
                memoryCache.put(key, data);
                return data.cover;
            }
        }
        
        return null;
    }
    
    /**
     * 缓存专辑信息
     * @param key 缓存键
     * @param album 专辑信息
     */
    public void cacheAlbumInfo(String key, AlbumInfo album) {
        CachedData data = new CachedData();
        data.type = CacheType.ALBUM_INFO;
        data.timestamp = System.currentTimeMillis();
        data.albumInfo = album;
        memoryCache.put(key, data);
        
        if (enableDiskCache) {
            saveToDisk(key, data);
        }
    }
    
    /**
     * 获取缓存的专辑信息
     * @param key 缓存键
     * @return 专辑信息，如果不存在返回null
     */
    public AlbumInfo getCachedAlbumInfo(String key) {
        CachedData data = memoryCache.get(key);
        if (data != null && data.type == CacheType.ALBUM_INFO) {
            return data.albumInfo;
        }
        
        if (enableDiskCache) {
            data = loadFromDisk(key);
            if (data != null && data.type == CacheType.ALBUM_INFO) {
                memoryCache.put(key, data);
                return data.albumInfo;
            }
        }
        
        return null;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        memoryCache.clear();
        
        if (enableDiskCache) {
            try {
                Path cachePath = Paths.get(cacheDir);
                if (Files.exists(cachePath)) {
                    Files.walk(cachePath)
                            .filter(Files::isRegularFile)
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    System.err.println("Failed to delete cache file: " + path);
                                }
                            });
                }
            } catch (IOException e) {
                System.err.println("Failed to clear disk cache: " + e.getMessage());
            }
        }
    }
    
    private void saveToDisk(String key, CachedData data) {
        try {
            String filename = cacheDir + File.separator + sanitizeKey(key) + ".cache";
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(data);
            }
        } catch (IOException e) {
            System.err.println("Failed to save cache to disk: " + e.getMessage());
        }
    }
    
    private CachedData loadFromDisk(String key) {
        try {
            String filename = cacheDir + File.separator + sanitizeKey(key) + ".cache";
            File file = new File(filename);
            if (!file.exists()) {
                return null;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                return (CachedData) ois.readObject();
            }
        } catch (Exception e) {
            System.err.println("Failed to load cache from disk: " + e.getMessage());
            return null;
        }
    }
    
    private String sanitizeKey(String key) {
        return key.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
    
    /**
     * 缓存数据类
     */
    private static class CachedData implements Serializable {
        CacheType type;
        long timestamp;
        LyricsInfo lyrics;
        CoverInfo cover;
        AlbumInfo albumInfo;
        TrackInfo trackInfo;
    }
    
    /**
     * 缓存类型枚举
     */
    private enum CacheType {
        LYRICS,
        COVER,
        ALBUM_INFO,
        TRACK_INFO
    }
}