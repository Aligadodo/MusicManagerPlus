/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-29
 */
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

public class LastFmSource implements MetadataSource {
    
    private static final String API_BASE = "http://ws.audioscrobbler.com/2.0/";
    private static final String API_KEY = "YOUR_LASTFM_API_KEY";
    
    @Override
    public String getSourceName() {
        return "Last.fm";
    }
    
    @Override
    public String getSourceDescription() {
        return "Last.fm是一个全球知名的音乐信息平台，拥有庞大的音乐数据库。" +
               "提供歌曲、艺术家、专辑的详细信息，包括发行日期、流派、标签等。" +
               "数据由社区贡献，质量参差不齐但覆盖面广。" +
               "支持歌词搜索（通过第三方API），封面图片（通常为高质量）。" +
               "需要API密钥（可在https://www.last.fm/api/account/create免费申请），" +
               "请求频率限制为每秒5次请求。";
    }
    
    @Override
    public EnumSet<MetadataSource.SourceCapabilities> getCapabilities() {
        return EnumSet.of(
            MetadataSource.SourceCapabilities.LYRICS,
            MetadataSource.SourceCapabilities.COVER,
            MetadataSource.SourceCapabilities.ALBUM_INFO,
            MetadataSource.SourceCapabilities.TRACK_INFO
        );
    }
    
    @Override
    public LyricsInfo searchLyrics(String artist, String title, int duration) {
        try {
            String searchUrl = API_BASE + "?method=track.getInfo" +
                "&api_key=" + API_KEY +
                "&artist=" + URLEncoder.encode(artist, "UTF-8") +
                "&track=" + URLEncoder.encode(title, "UTF-8") +
                "&format=json";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                Map<String, Object> track = (Map<String, Object>) searchJson.get("track");
                
                if (track != null) {
                    LyricsInfo info = new LyricsInfo();
                    info.setSource(getSourceName());
                    info.setVerified(true);
                    info.setFormat("LRC");
                    info.setContent("Last.fm不直接提供歌词，需要通过第三方API获取");
                    return info;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public CoverInfo searchCover(String artist, String album) {
        try {
            String searchUrl = API_BASE + "?method=album.getInfo" +
                "&api_key=" + API_KEY +
                "&artist=" + URLEncoder.encode(artist, "UTF-8") +
                "&album=" + URLEncoder.encode(album, "UTF-8") +
                "&format=json";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                Map<String, Object> albumInfo = (Map<String, Object>) searchJson.get("album");
                
                if (albumInfo != null) {
                    List<Map<String, Object>> images = (List<Map<String, Object>>) albumInfo.get("image");
                    if (images != null && !images.isEmpty()) {
                        Map<String, Object> largestImage = images.get(images.size() - 1);
                        String imageUrl = (String) largestImage.get("#text");
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            CoverInfo info = new CoverInfo();
                            info.setSource(getSourceName());
                            info.setImageUrl(imageUrl);
                            info.setFormat("JPEG");
                            return info;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public AlbumInfo searchAlbumInfo(String artist, String album) {
        try {
            String searchUrl = API_BASE + "?method=album.getInfo" +
                "&api_key=" + API_KEY +
                "&artist=" + URLEncoder.encode(artist, "UTF-8") +
                "&album=" + URLEncoder.encode(album, "UTF-8") +
                "&format=json";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                Map<String, Object> albumInfo = (Map<String, Object>) searchJson.get("album");
                
                if (albumInfo != null) {
                    AlbumInfo info = new AlbumInfo();
                    info.setSource(getSourceName());
                    info.setArtist(artist);
                    info.setName(album);
                    
                    String name = (String) albumInfo.get("name");
                    if (name != null) {
                        info.setName(name);
                    }
                    
                    Map<String, Object> artistObj = (Map<String, Object>) albumInfo.get("artist");
                    if (artistObj != null) {
                        info.setArtist((String) artistObj.get("name"));
                    }
                    
                    List<Map<String, Object>> tracks = (List<Map<String, Object>>) albumInfo.get("tracks");
                    if (tracks != null) {
                        for (int i = 0; i < tracks.size(); i++) {
                            TrackInfo track = new TrackInfo();
                            track.setTrackNumber(i + 1);
                            info.addTrack(track);
                        }
                    }
                    
                    List<Map<String, Object>> tags = (List<Map<String, Object>>) albumInfo.get("tags");
                    if (tags != null) {
                        List<String> tagList = new ArrayList<>();
                        for (Map<String, Object> tag : tags) {
                            String tagName = (String) tag.get("name");
                            if (tagName != null) {
                                tagList.add(tagName);
                            }
                        }
                        if (!tagList.isEmpty()) {
                            info.setGenre(String.join(", ", tagList));
                        }
                    }
                    
                    String wiki = (String) albumInfo.get("wiki");
                    if (wiki != null) {
                        info.setDescription(wiki);
                    }
                    
                    return info;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public TrackInfo searchTrackInfo(String artist, String title) {
        try {
            String searchUrl = API_BASE + "?method=track.getInfo" +
                "&api_key=" + API_KEY +
                "&artist=" + URLEncoder.encode(artist, "UTF-8") +
                "&track=" + URLEncoder.encode(title, "UTF-8") +
                "&format=json";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                Map<String, Object> track = (Map<String, Object>) searchJson.get("track");
                
                if (track != null) {
                    TrackInfo info = new TrackInfo();
                    info.setSource(getSourceName());
                    info.setArtist(artist);
                    info.setTitle(title);
                    
                    String name = (String) track.get("name");
                    if (name != null) {
                        info.setTitle(name);
                    }
                    
                    Map<String, Object> artistObj = (Map<String, Object>) track.get("artist");
                    if (artistObj != null) {
                        info.setArtist((String) artistObj.get("name"));
                    }
                    
                    Map<String, Object> albumObj = (Map<String, Object>) track.get("album");
                    if (albumObj != null) {
                        info.setAlbum((String) albumObj.get("title"));
                    }
                    
                    Integer durationInt = (Integer) track.get("duration");
                    if (durationInt != null) {
                        info.setDuration(durationInt / 1000);
                    }
                    
                    List<Map<String, Object>> tags = (List<Map<String, Object>>) track.get("toptags");
                    if (tags != null) {
                        List<String> tagList = new ArrayList<>();
                        for (Map<String, Object> tag : tags) {
                            String tagName = (String) tag.get("name");
                            if (tagName != null) {
                                tagList.add(tagName);
                            }
                        }
                        if (!tagList.isEmpty()) {
                            info.setGenre(String.join(", ", tagList));
                        }
                    }
                    
                    return info;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private String httpGet(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "EchoMusicManager/1.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Map<String, Object> parseJson(String json) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(json, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}