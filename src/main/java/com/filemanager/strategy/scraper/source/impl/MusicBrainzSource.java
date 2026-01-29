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

public class MusicBrainzSource implements MetadataSource {
    
    private static final String API_BASE = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "EchoMusicManager/1.0 (chrse1997@163.com)";
    
    @Override
    public String getSourceName() {
        return "MusicBrainz";
    }
    
    @Override
    public String getSourceDescription() {
        return "MusicBrainz是一个开源的音乐元数据数据库，由社区维护。" +
               "提供丰富的歌曲、艺术家、专辑信息，包括发行日期、ISRC码等。" +
               "数据质量高，更新及时，是全球最大的开源音乐数据库之一。" +
               "支持歌词搜索（通过关联的LyricWiki），封面图片（通过Cover Art Archive）。" +
               "无需API密钥，但请求频率有限制（每秒1次请求）。";
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
            String query = String.format("artist:\"%s\" AND recording:\"%s\"", 
                artist, title);
            String searchUrl = API_BASE + "/recording/?query=" + URLEncoder.encode(query, "UTF-8") + 
                "&fmt=json&limit=5";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> recordings = (List<Map<String, Object>>) searchJson.get("recordings");
                
                if (recordings != null && !recordings.isEmpty()) {
                    for (Map<String, Object> recording : recordings) {
                        String recordingId = (String) recording.get("id");
                        
                        String lyricsUrl = API_BASE + "/recording/" + recordingId + "?inc=lyrics-rels&fmt=json";
                        String lyricsResult = httpGet(lyricsUrl);
                        
                        if (lyricsResult != null) {
                            Map<String, Object> lyricsJson = parseJson(lyricsResult);
                            List<Map<String, Object>> relations = (List<Map<String, Object>>) lyricsJson.get("relations");
                            
                            if (relations != null) {
                                for (Map<String, Object> relation : relations) {
                                    String type = (String) relation.get("type");
                                    if ("lyrics".equals(type)) {
                                        Map<String, Object> url = (Map<String, Object>) relation.get("url");
                                        String lyricsResource = (String) url.get("resource");
                                        
                                        if (lyricsResource != null && lyricsResource.contains("lyrics.wikia.com")) {
                                            LyricsInfo info = new LyricsInfo();
                                            info.setSource(getSourceName());
                                            info.setVerified(true);
                                            info.setFormat("LRC");
                                            info.setContent("歌词来源: " + lyricsResource);
                                            return info;
                                        }
                                    }
                                }
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
    public CoverInfo searchCover(String artist, String album) {
        try {
            String query = String.format("artist:\"%s\" AND release:\"%s\"", 
                artist, album);
            String searchUrl = API_BASE + "/release/?query=" + URLEncoder.encode(query, "UTF-8") + 
                "&fmt=json&limit=5";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> releases = (List<Map<String, Object>>) searchJson.get("releases");
                
                if (releases != null && !releases.isEmpty()) {
                    Map<String, Object> release = releases.get(0);
                    String releaseId = (String) release.get("id");
                    
                    String coverUrl = "https://coverartarchive.org/release/" + releaseId + "/front-500";
                    
                    CoverInfo info = new CoverInfo();
                    info.setSource(getSourceName());
                    info.setImageUrl(coverUrl);
                    info.setFormat("JPEG");
                    info.setWidth(500);
                    info.setHeight(500);
                    return info;
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
            String query = String.format("artist:\"%s\" AND release:\"%s\"", 
                artist, album);
            String searchUrl = API_BASE + "/release/?query=" + URLEncoder.encode(query, "UTF-8") + 
                "&fmt=json&limit=1";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> releases = (List<Map<String, Object>>) searchJson.get("releases");
                
                if (releases != null && !releases.isEmpty()) {
                    Map<String, Object> release = releases.get(0);
                    
                    AlbumInfo info = new AlbumInfo();
                    info.setSource(getSourceName());
                    info.setArtist(artist);
                    info.setName(album);
                    
                    String date = (String) release.get("date");
                    if (date != null) {
                        info.setYear(date);
                    }
                    
                    Map<String, Object> artistInfo = (Map<String, Object>) release.get("artist-credit");
                    if (artistInfo != null && !artistInfo.isEmpty()) {
                        Map<String, Object> firstArtist = (Map<String, Object>) ((List<?>) artistInfo).get(0);
                        Map<String, Object> artistObj = (Map<String, Object>) firstArtist.get("artist");
                        if (artistObj != null) {
                            info.setArtist((String) artistObj.get("name"));
                        }
                    }
                    
                    List<Map<String, Object>> media = (List<Map<String, Object>>) release.get("media");
                    if (media != null && !media.isEmpty()) {
                        int totalTracks = 0;
                        for (Map<String, Object> medium : media) {
                            Integer trackCount = (Integer) medium.get("track-count");
                            if (trackCount != null) {
                                totalTracks += trackCount;
                            }
                        }
                        for (int i = 0; i < totalTracks; i++) {
                            TrackInfo track = new TrackInfo();
                            track.setTrackNumber(i + 1);
                            info.addTrack(track);
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
    
    @Override
    public TrackInfo searchTrackInfo(String artist, String title) {
        try {
            String query = String.format("artist:\"%s\" AND recording:\"%s\"", 
                artist, title);
            String searchUrl = API_BASE + "/recording/?query=" + URLEncoder.encode(query, "UTF-8") + 
                "&fmt=json&limit=1";
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> recordings = (List<Map<String, Object>>) searchJson.get("recordings");
                
                if (recordings != null && !recordings.isEmpty()) {
                    Map<String, Object> recording = recordings.get(0);
                    
                    TrackInfo info = new TrackInfo();
                    info.setSource(getSourceName());
                    info.setArtist(artist);
                    info.setTitle(title);
                    
                    String recordingId = (String) recording.get("id");
                    
                    Integer length = (Integer) recording.get("length");
                    if (length != null) {
                        info.setDuration(length / 1000);
                    }
                    
                    List<Map<String, Object>> artistCredit = (List<Map<String, Object>>) recording.get("artist-credit");
                    if (artistCredit != null && !artistCredit.isEmpty()) {
                        Map<String, Object> firstArtist = (Map<String, Object>) artistCredit.get(0);
                        Map<String, Object> artistObj = (Map<String, Object>) firstArtist.get("artist");
                        if (artistObj != null) {
                            info.setArtist((String) artistObj.get("name"));
                        }
                    }
                    
                    List<Map<String, Object>> releases = (List<Map<String, Object>>) recording.get("releases");
                    if (releases != null && !releases.isEmpty()) {
                        Map<String, Object> firstRelease = releases.get(0);
                        String album = (String) firstRelease.get("title");
                        info.setAlbum(album);
                        
                        String year = (String) firstRelease.get("date");
                        if (year != null) {
                            info.setYear(year);
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