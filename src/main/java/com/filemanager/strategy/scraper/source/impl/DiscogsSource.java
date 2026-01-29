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

public class DiscogsSource implements MetadataSource {
    
    private static final String API_BASE = "https://api.discogs.com";
    private static final String API_KEY = "YOUR_DISCOGS_KEY";
    private static final String API_SECRET = "YOUR_DISCOGS_SECRET";
    private static final String USER_AGENT = "EchoMusicManager/1.0";
    
    @Override
    public String getSourceName() {
        return "Discogs";
    }
    
    @Override
    public String getSourceDescription() {
        return "Discogs是全球最大的音乐数据库和音乐市场，拥有超过1500万条发行记录。" +
               "提供详细的音乐元数据，包括艺术家、专辑、发行信息、曲目列表等。" +
               "数据质量极高，由社区维护和验证。" +
               "支持高质量封面图片，详细的专辑信息和曲目列表。" +
               "需要API密钥（可在https://www.discogs.com/developers/免费申请），" +
               "认证用户每分钟可请求60次，未认证用户每分钟可请求25次。";
    }
    
    @Override
    public EnumSet<MetadataSource.SourceCapabilities> getCapabilities() {
        return EnumSet.of(
            MetadataSource.SourceCapabilities.COVER,
            MetadataSource.SourceCapabilities.ALBUM_INFO,
            MetadataSource.SourceCapabilities.TRACK_INFO
        );
    }
    
    @Override
    public LyricsInfo searchLyrics(String artist, String title, int duration) {
        return null;
    }
    
    @Override
    public CoverInfo searchCover(String artist, String album) {
        try {
            String query = String.format("%s %s", artist, album);
            String searchUrl = API_BASE + "/database/search?q=" + 
                URLEncoder.encode(query, "UTF-8") +
                "&type=release&per_page=1" +
                "&key=" + API_KEY + "&secret=" + API_SECRET;
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchJson.get("results");
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    String resourceUrl = (String) firstResult.get("resource_url");
                    
                    String releaseResult = httpGet(resourceUrl);
                    if (releaseResult != null) {
                        Map<String, Object> releaseJson = parseJson(releaseResult);
                        List<Map<String, Object>> images = (List<Map<String, Object>>) releaseJson.get("images");
                        
                        if (images != null && !images.isEmpty()) {
                            Map<String, Object> primaryImage = images.get(0);
                            String imageUrl = (String) primaryImage.get("uri");
                            
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                CoverInfo info = new CoverInfo();
                                info.setSource(getSourceName());
                                info.setImageUrl(imageUrl);
                                info.setFormat("JPEG");
                                info.setWidth((Integer) primaryImage.get("width"));
                                info.setHeight((Integer) primaryImage.get("height"));
                                return info;
                            }
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
            String query = String.format("%s %s", artist, album);
            String searchUrl = API_BASE + "/database/search?q=" + 
                URLEncoder.encode(query, "UTF-8") +
                "&type=release&per_page=1" +
                "&key=" + API_KEY + "&secret=" + API_SECRET;
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchJson.get("results");
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    String resourceUrl = (String) firstResult.get("resource_url");
                    
                    String releaseResult = httpGet(resourceUrl);
                    if (releaseResult != null) {
                        Map<String, Object> releaseJson = parseJson(releaseResult);
                        
                        AlbumInfo info = new AlbumInfo();
                        info.setSource(getSourceName());
                        info.setArtist(artist);
                        info.setName(album);
                        
                        String title = (String) releaseJson.get("title");
                        if (title != null) {
                            info.setName(title);
                        }
                        
                        String year = (String) releaseJson.get("year");
                        if (year != null) {
                            info.setYear(year);
                        }
                        
                        List<Map<String, Object>> artists = (List<Map<String, Object>>) releaseJson.get("artists");
                        if (artists != null && !artists.isEmpty()) {
                            info.setArtist((String) artists.get(0).get("name"));
                        }
                        
                        List<Map<String, Object>> tracklist = (List<Map<String, Object>>) releaseJson.get("tracklist");
                        if (tracklist != null) {
                            for (int i = 0; i < tracklist.size(); i++) {
                                TrackInfo track = new TrackInfo();
                                track.setTrackNumber(i + 1);
                                info.addTrack(track);
                            }
                        }
                        
                        String notes = (String) releaseJson.get("notes");
                        if (notes != null && !notes.isEmpty()) {
                            info.setDescription(notes);
                        }
                        
                        List<String> genres = (List<String>) releaseJson.get("genres");
                        if (genres != null) {
                            if (!genres.isEmpty()) {
                                info.setGenre(String.join(", ", genres));
                            }
                        }
                        
                        return info;
                    }
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
            String query = String.format("%s %s", artist, title);
            String searchUrl = API_BASE + "/database/search?q=" + 
                URLEncoder.encode(query, "UTF-8") +
                "&type=master&per_page=1" +
                "&key=" + API_KEY + "&secret=" + API_SECRET;
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchJson.get("results");
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    String resourceUrl = (String) firstResult.get("resource_url");
                    
                    String masterResult = httpGet(resourceUrl);
                    if (masterResult != null) {
                        Map<String, Object> masterJson = parseJson(masterResult);
                        
                        TrackInfo info = new TrackInfo();
                        info.setSource(getSourceName());
                        info.setArtist(artist);
                        info.setTitle(title);
                        
                        String titleStr = (String) masterJson.get("title");
                        if (titleStr != null) {
                            info.setAlbum(titleStr);
                        }
                        
                        String year = (String) masterJson.get("year");
                        if (year != null) {
                            info.setYear(year);
                        }
                        
                        List<Map<String, Object>> artists = (List<Map<String, Object>>) masterJson.get("artists");
                        if (artists != null && !artists.isEmpty()) {
                            info.setArtist((String) artists.get(0).get("name"));
                        }
                        
                        List<Map<String, Object>> tracklist = (List<Map<String, Object>>) masterJson.get("tracklist");
                        if (tracklist != null) {
                            for (int i = 0; i < tracklist.size(); i++) {
                                Map<String, Object> track = tracklist.get(i);
                                String trackTitle = (String) track.get("title");
                                if (trackTitle != null && trackTitle.equals(title)) {
                                    info.setTrackNumber(i + 1);
                                    break;
                                }
                            }
                        }
                        
                        List<String> genres = (List<String>) masterJson.get("genres");
                        if (genres != null) {
                            if (!genres.isEmpty()) {
                                info.setGenre(String.join(", ", genres));
                            }
                        }
                        
                        return info;
                    }
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
            conn.setRequestProperty("User-Agent", USER_AGENT);
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