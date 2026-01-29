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

public class ITunesSource implements MetadataSource {
    
    private static final String API_BASE = "https://itunes.apple.com/search";
    private static final String USER_AGENT = "EchoMusicManager/1.0";
    
    private String lastRequestUrl;
    private String lastRequestError;
    
    @Override
    public String getSourceName() {
        return "iTunes";
    }
    
    @Override
    public String getSourceDescription() {
        return "iTunes Search API是苹果公司提供的音乐搜索API，无需认证，支持搜索歌曲、专辑、艺术家等信息。" +
               "数据质量高，包含封面图片、预览音频等丰富信息。" +
               "适合作为主要的数据源来获取音乐元数据。";
    }
    
    @Override
    public EnumSet<SourceCapabilities> getCapabilities() {
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
            String searchUrl = API_BASE + "?term=" + URLEncoder.encode(query, "UTF-8") + 
                "&media=music&entity=album&limit=5";
            lastRequestUrl = searchUrl;
            lastRequestError = null;
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchJson.get("results");
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    
                    String artworkUrl100 = (String) firstResult.get("artworkUrl100");
                    if (artworkUrl100 != null && !artworkUrl100.isEmpty()) {
                        CoverInfo info = new CoverInfo();
                        info.setSource(getSourceName());
                        info.setImageUrl(artworkUrl100);
                        info.setFormat("JPEG");
                        info.setWidth(100);
                        info.setHeight(100);
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
            String query = String.format("%s %s", artist, album);
            String searchUrl = API_BASE + "?term=" + URLEncoder.encode(query, "UTF-8") + 
                "&media=music&entity=album&limit=1";
            lastRequestUrl = searchUrl;
            lastRequestError = null;
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchJson.get("results");
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    
                    AlbumInfo info = new AlbumInfo();
                    info.setSource(getSourceName());
                    
                    String collectionName = (String) firstResult.get("collectionName");
                    if (collectionName != null) {
                        info.setName(collectionName);
                    }
                    
                    String artistName = (String) firstResult.get("artistName");
                    if (artistName != null) {
                        info.setArtist(artistName);
                    }
                    
                    String releaseDate = (String) firstResult.get("releaseDate");
                    if (releaseDate != null) {
                        info.setYear(releaseDate.substring(0, 4));
                    }
                    
                    Integer trackCount = (Integer) firstResult.get("trackCount");
                    if (trackCount != null) {
                        for (int i = 0; i < trackCount; i++) {
                            TrackInfo track = new TrackInfo();
                            track.setTrackNumber(i + 1);
                            info.addTrack(track);
                        }
                    }
                    
                    String primaryGenreName = (String) firstResult.get("primaryGenreName");
                    if (primaryGenreName != null) {
                        info.setGenre(primaryGenreName);
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
    
    @Override
    public TrackInfo searchTrackInfo(String artist, String title) {
        try {
            String query = String.format("%s %s", artist, title);
            String searchUrl = API_BASE + "?term=" + URLEncoder.encode(query, "UTF-8") + 
                "&media=music&entity=song&limit=1";
            lastRequestUrl = searchUrl;
            lastRequestError = null;
            
            String searchResult = httpGet(searchUrl);
            if (searchResult != null && !searchResult.isEmpty()) {
                Map<String, Object> searchJson = parseJson(searchResult);
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchJson.get("results");
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> firstResult = results.get(0);
                    
                    TrackInfo info = new TrackInfo();
                    info.setSource(getSourceName());
                    
                    String trackName = (String) firstResult.get("trackName");
                    if (trackName != null) {
                        info.setTitle(trackName);
                    }
                    
                    String artistName = (String) firstResult.get("artistName");
                    if (artistName != null) {
                        info.setArtist(artistName);
                    }
                    
                    String collectionName = (String) firstResult.get("collectionName");
                    if (collectionName != null) {
                        info.setAlbum(collectionName);
                    }
                    
                    Integer trackTimeMillis = (Integer) firstResult.get("trackTimeMillis");
                    if (trackTimeMillis != null) {
                        info.setDuration(trackTimeMillis / 1000);
                    }
                    
                    Integer trackNumber = (Integer) firstResult.get("trackNumber");
                    if (trackNumber != null) {
                        info.setTrackNumber(trackNumber);
                    }
                    
                    String releaseDate = (String) firstResult.get("releaseDate");
                    if (releaseDate != null) {
                        info.setYear(releaseDate.substring(0, 4));
                    }
                    
                    String primaryGenreName = (String) firstResult.get("primaryGenreName");
                    if (primaryGenreName != null) {
                        info.setGenre(primaryGenreName);
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
    
    private String httpGet(String urlString) {
        lastRequestUrl = urlString;
        lastRequestError = null;
        
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
            } else {
                lastRequestError = "HTTP Response Code: " + responseCode;
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorReader.close();
                if (errorResponse.length() > 0) {
                    lastRequestError += ", Error: " + errorResponse.toString();
                }
            }
        } catch (Exception e) {
            lastRequestError = e.getMessage();
            e.printStackTrace();
        }
        return null;
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
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(json, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}
