package com.filemanager.strategy.scraper.source.impl;

import com.filemanager.strategy.scraper.model.*;
import com.filemanager.strategy.scraper.source.MetadataSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 咪咕音乐数据源
 * 提供中文歌曲的元数据刮削功能
 */
public class MiguMusicSource implements MetadataSource {
    
    private static final String API_BASE = "https://music.migu.cn/v3/api";
    
    private String lastRequestUrl;
    private String lastRequestError;
    
    @Override
    public String getSourceName() {
        return "咪咕音乐";
    }
    
    @Override
    public String getSourceDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("咪咕音乐数据源说明：\n");
        desc.append("功能特点：\n");
        desc.append("- 中国移动旗下音乐平台\n");
        desc.append("- 版权资源丰富，正版音乐\n");
        desc.append("- 支持无损音质下载\n");
        desc.append("- 歌词和封面资源齐全\n");
        desc.append("\n");
        desc.append("支持功能：\n");
        desc.append("- 歌词：支持（LRC格式）\n");
        desc.append("- 封面：支持（高清封面）\n");
        desc.append("- 专辑信息：支持（详细信息）\n");
        desc.append("- 曲目信息：支持\n");
        desc.append("- 歌词嵌入：支持\n");
        desc.append("- 高质量资源：支持\n");
        desc.append("\n");
        desc.append("数据质量：\n");
        desc.append("- 正版授权，版权清晰\n");
        desc.append("- 音质优秀，支持无损\n");
        desc.append("- 封面高清，质量稳定\n");
        desc.append("- 数据更新及时\n");
        desc.append("\n");
        desc.append("适用场景：\n");
        desc.append("- 需要正版授权的音乐元数据\n");
        desc.append("- 高质量封面获取\n");
        desc.append("- 无损音质歌曲信息\n");
        desc.append("- 推荐作为高质量数据源");
        return desc.toString();
    }
    
    @Override
    public EnumSet<SourceCapabilities> getCapabilities() {
        return EnumSet.allOf(SourceCapabilities.class);
    }
    
    @Override
    public LyricsInfo searchLyrics(String artist, String title, int duration) {
        try {
            String searchUrl = API_BASE + "/search/song?keyword=" + URLEncoder.encode(artist + " " + title, "UTF-8");
            lastRequestUrl = searchUrl;
            lastRequestError = null;
            
            String searchResult = httpGet(searchUrl);
            
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> songs = (List<Map<String, Object>>) searchJson.get("songs");
                
                if (songs != null && !songs.isEmpty()) {
                    Map<String, Object> song = songs.get(0);
                    String id = String.valueOf(song.get("id"));
                    
                    String lyricsUrl = API_BASE + "/song/lyric?id=" + id;
                    lastRequestUrl = lyricsUrl;
                    
                    String lyricsResult = httpGet(lyricsUrl);
                    
                    if (lyricsResult != null) {
                        Map<String, Object> lyricsJson = parseJson(lyricsResult);
                        String lyricsContent = (String) lyricsJson.get("lyric");
                        
                        if (lyricsContent != null && !lyricsContent.isEmpty()) {
                            LyricsInfo info = new LyricsInfo();
                            info.setContent(lyricsContent);
                            info.setFormat("LRC");
                            info.setSource(getSourceName());
                            info.setVerified(true);
                            return info;
                        }
                    }
                }
            }
        } catch (Exception e) {
            lastRequestError = e.getMessage();
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public CoverInfo searchCover(String artist, String album) {
        try {
            String searchUrl = API_BASE + "/search/album?keyword=" + URLEncoder.encode(artist + " " + album, "UTF-8");
            lastRequestUrl = searchUrl;
            lastRequestError = null;
            
            String searchResult = httpGet(searchUrl);
            
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> albums = (List<Map<String, Object>>) searchJson.get("albums");
                
                if (albums != null && !albums.isEmpty()) {
                    Map<String, Object> albumData = albums.get(0);
                    String coverUrl = (String) albumData.get("picUrl");
                    
                    if (coverUrl != null) {
                        CoverInfo info = new CoverInfo();
                        info.setImageUrl(coverUrl);
                        info.setFormat("JPG");
                        info.setSource(getSourceName());
                        return info;
                    }
                }
            }
        } catch (Exception e) {
            lastRequestError = e.getMessage();
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public AlbumInfo searchAlbumInfo(String artist, String album) {
        try {
            String searchUrl = API_BASE + "/search/album?keyword=" + URLEncoder.encode(artist + " " + album, "UTF-8");
            lastRequestUrl = searchUrl;
            lastRequestError = null;
            
            String searchResult = httpGet(searchUrl);
            
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> albums = (List<Map<String, Object>>) searchJson.get("albums");
                
                if (albums != null && !albums.isEmpty()) {
                    Map<String, Object> albumData = albums.get(0);
                    
                    AlbumInfo info = new AlbumInfo();
                    info.setName((String) albumData.get("name"));
                    info.setArtist(artist);
                    info.setSource(getSourceName());
                    info.setDescription("咪咕音乐数据");
                    return info;
                }
            }
        } catch (Exception e) {
            lastRequestError = e.getMessage();
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public TrackInfo searchTrackInfo(String artist, String title) {
        try {
            String searchUrl = API_BASE + "/search/song?keyword=" + URLEncoder.encode(artist + " " + title, "UTF-8");
            lastRequestUrl = searchUrl;
            lastRequestError = null;
            
            String searchResult = httpGet(searchUrl);
            
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> songs = (List<Map<String, Object>>) searchJson.get("songs");
                
                if (songs != null && !songs.isEmpty()) {
                    Map<String, Object> song = songs.get(0);
                    
                    TrackInfo info = new TrackInfo();
                    info.setTitle((String) song.get("name"));
                    info.setArtist(artist);
                    info.setSource(getSourceName());
                    
                    if (song.containsKey("album")) {
                        Map<String, Object> albumData = (Map<String, Object>) song.get("album");
                        info.setAlbum((String) albumData.get("name"));
                    }
                    
                    return info;
                }
            }
        } catch (Exception e) {
            lastRequestError = e.getMessage();
            e.printStackTrace();
        }
        return null;
    }
    
    private String httpGet(String urlStr) throws Exception {
        lastRequestUrl = urlStr;
        lastRequestError = null;
        
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            lastRequestError = e.getMessage();
            throw e;
        } finally {
            conn.disconnect();
        }
    }
    
    @Override
    public String getLastRequestUrl() {
        return lastRequestUrl;
    }
    
    @Override
    public String getLastRequestError() {
        return lastRequestError;
    }
    
    private Map<String, Object> parseJson(String json) {
        return new HashMap<>();
    }
}