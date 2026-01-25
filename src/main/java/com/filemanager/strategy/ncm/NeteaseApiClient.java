/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-25 
 */
package com.filemanager.strategy.ncm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网易云API客户端
 * 负责与网易云API交互获取歌曲信息
 */
public class NeteaseApiClient {
    
    /**
     * 从网易云API获取歌曲信息
     * @param songId 歌曲ID
     * @param infoFile 保存歌曲信息的文件
     * @return 歌曲信息，包含songName和artistName
     */
    public SongInfo getSongInfo(String songId, File infoFile) {
        try {
            URL url = new URL("http://music.163.com/api/song/detail/?id=" + songId + "&ids=%5B" + songId + "%5D");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Accept", "text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
            
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            conn.disconnect();
            
            String response = sb.toString();
            
            // 提取歌曲名称
            String songName = extractSongName(response);
            
            // 提取艺术家名称
            String artistName = extractArtistName(response);
            
            // 保存歌曲信息到.info文件
            if (songName != null && artistName != null) {
                saveSongInfoToFile(infoFile, songName, artistName, songId);
            }
            
            return new SongInfo(songName, artistName, songId);
        } catch (Exception e) {
            System.err.println("从网易云API获取歌曲信息失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从API响应中提取歌曲名称
     * @param response API响应
     * @return 歌曲名称
     */
    private String extractSongName(String response) {
        Pattern songNamePattern = Pattern.compile("name.*?:.*?([^,]+)");
        Matcher songNameMatcher = songNamePattern.matcher(response);
        if (songNameMatcher.find()) {
            return songNameMatcher.group(1).replaceAll("[\"\\\\{\\\\}]", "").trim();
        }
        return null;
    }
    
    /**
     * 从API响应中提取艺术家名称
     * @param response API响应
     * @return 艺术家名称
     */
    private String extractArtistName(String response) {
        Pattern artistNamePattern = Pattern.compile("artists.*?:.*?name.*?:.*?([^,]+)");
        Matcher artistNameMatcher = artistNamePattern.matcher(response);
        if (artistNameMatcher.find()) {
            return artistNameMatcher.group(1).replaceAll("[\"\\\\{\\\\}]", "").trim();
        }
        return null;
    }
    
    /**
     * 保存歌曲信息到.info文件
     * @param infoFile 保存文件
     * @param songName 歌曲名称
     * @param artistName 艺术家名称
     * @param songId 歌曲ID
     */
    private void saveSongInfoToFile(File infoFile, String songName, String artistName, String songId) {
        try {
            StringBuilder infoJson = new StringBuilder();
            infoJson.append("{");
            infoJson.append("\"songName\":\"").append(songName).append("\",");
            infoJson.append("\"artistName\":\"").append(artistName).append("\",");
            infoJson.append("\"songId\":\"").append(songId).append("\"");
            infoJson.append("}");
            
            FileOutputStream fos = new FileOutputStream(infoFile);
            fos.write(infoJson.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            System.err.println("保存歌曲信息到文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 从.info文件中读取歌曲信息
     * @param infoFile .info文件
     * @return 歌曲信息
     */
    public SongInfo readSongInfoFromFile(File infoFile) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(infoFile);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            fis.close();
            
            if (bytesRead > 0) {
                String content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                
                // 提取 songName
                String songName = extractFieldFromJson(content, "songName");
                
                // 提取 artistName
                String artistName = extractFieldFromJson(content, "artistName");
                
                // 提取 songId
                String songId = extractFieldFromJson(content, "songId");
                
                if (songName != null || artistName != null) {
                    return new SongInfo(songName, artistName, songId);
                }
            }
        } catch (Exception e) {
            System.err.println("解析 .info 文件失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 从JSON字符串中提取字段值
     * @param json JSON字符串
     * @param fieldName 字段名
     * @return 字段值
     */
    private String extractFieldFromJson(String json, String fieldName) {
        int fieldStart = json.indexOf(fieldName);
        if (fieldStart != -1) {
            int colonStart = json.indexOf(":", fieldStart);
            if (colonStart != -1) {
                int quoteStart = json.indexOf("\"", colonStart);
                if (quoteStart != -1) {
                    int quoteEnd = json.indexOf("\"", quoteStart + 1);
                    if (quoteEnd != -1) {
                        return json.substring(quoteStart + 1, quoteEnd);
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 搜索歌曲，获取歌曲ID
     * 
     * @param songName   歌曲名称
     * @param artistName 艺术家名称
     * @return 歌曲ID
     */
    public String searchSong(String songName, String artistName) {
        try {
            String searchUrl = "http://music.163.com/api/search/get/web?csrf_token=";
            String query = songName + (artistName.isEmpty() ? "" : " " + artistName);
            String data = "s=" + URLEncoder.encode(query, "UTF-8") + "&type=1&offset=0&subType=&limit=10";
            
            String response = sendPostRequest(searchUrl, data);
            
            // 解析JSON响应，获取歌曲ID
            int idStart = response.indexOf("\"id\":");
            if (idStart > 0) {
                idStart += 5;
                int idEnd = response.indexOf(",", idStart);
                if (idEnd > idStart) {
                    return response.substring(idStart, idEnd).trim();
                }
            }
        } catch (Exception e) {
            System.err.println("搜索歌曲失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 根据歌曲ID获取歌词
     * 
     * @param songId 歌曲ID
     * @return 歌词内容
     */
    public String getLyricById(String songId) {
        try {
            String lyricUrl = "http://music.163.com/api/song/lyric?id=" + songId + "&lv=1&tv=-1";
            String response = sendGetRequest(lyricUrl);
            
            // 解析JSON响应，获取歌词
            int lrcStart = response.indexOf("\"lyric\":\"");
            if (lrcStart > 0) {
                lrcStart += 9;
                int lrcEnd = response.indexOf("\"}", lrcStart);
                if (lrcEnd > lrcStart) {
                    String lyric = response.substring(lrcStart, lrcEnd);
                    // 解码转义字符
                    lyric = lyric.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"");
                    return lyric;
                }
            }
        } catch (Exception e) {
            System.err.println("获取歌词失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @return 响应内容
     */
    private String sendGetRequest(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        con.setRequestProperty("Referer", "http://music.163.com");
        con.setRequestProperty("Host", "music.163.com");
        
        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("GET请求失败，响应码: " + responseCode);
            return null;
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        return response.toString();
    }
    
    /**
     * 发送POST请求
     * 
     * @param url  请求URL
     * @param data 请求数据
     * @return 响应内容
     */
    private String sendPostRequest(String url, String data) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        con.setRequestProperty("Referer", "http://music.163.com");
        con.setRequestProperty("Host", "music.163.com");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Content-Length", String.valueOf(data.length()));
        
        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(data);
            wr.flush();
        }
        
        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println("POST请求失败，响应码: " + responseCode);
            return null;
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        return response.toString();
    }
    
    /**
     * 歌曲信息类
     */
    public static class SongInfo {
        private final String songName;
        private final String artistName;
        private final String songId;
        
        public SongInfo(String songName, String artistName, String songId) {
            this.songName = songName;
            this.artistName = artistName;
            this.songId = songId;
        }
        
        public String getSongName() {
            return songName;
        }
        
        public String getArtistName() {
            return artistName;
        }
        
        public String getSongId() {
            return songId;
        }
    }
}
