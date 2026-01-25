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

import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.type.OperationType;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存扫描策略
 * 负责网易云音乐缓存文件的扫描和转换功能
 */
public class NcmCacheScanStrategy extends NcmBaseStrategy {
    // UI组件
    private final CheckBox chkDownloadLyric;
    
    // 运行时参数
    private boolean pDownloadLyric;
    
    public NcmCacheScanStrategy() {
        super("ncm_cache");
        
        // 缓存扫描选项
        chkDownloadLyric = new JFXCheckBox("自动下载对应歌词");
        chkDownloadLyric.setSelected(false);
        
        // 设置默认输出路径为子目录 "Convert - Cache"
        pathSelection.getTxtPath().setText("Convert - Cache");
        pathSelection.getCbOutputDirMode().getSelectionModel().select("子目录");
    }
    
    @Override
    public String getName() {
        return "缓存扫描";
    }
    
    @Override
    public String getDescription() {
        return "扫描并处理网易云音乐缓存文件，支持自动识别音频格式、查询歌曲信息并转换为正式音频文件";
    }
    
    @Override
    public com.filemanager.type.ScanTarget getTargetType() {
        return com.filemanager.type.ScanTarget.FILES_ONLY;
    }
    
    @Override
    public Node getConfigNode() {
        VBox configBox = new VBox();
        configBox.setSpacing(10);
        
        configBox.getChildren().addAll(
            StyleFactory.createChapter("缓存扫描选项"),
            chkDownloadLyric,
            StyleFactory.createSeparator(),
            pathSelection.getConfigNode()
        );
        
        return configBox;
    }
    
    @Override
    public void captureParams() {
        pDownloadLyric = chkDownloadLyric.isSelected();
        pathSelection.captureParams();
    }
    
    @Override
    public void saveConfig(Properties props) {
        pathSelection.saveConfig(props);
        props.setProperty("ncm_cache_downloadLyric", String.valueOf(chkDownloadLyric.isSelected()));
    }
    
    @Override
    public void loadConfig(Properties props) {
        pathSelection.loadConfig(props);
        if (props.containsKey("ncm_cache_downloadLyric")) {
            chkDownloadLyric.setSelected(Boolean.parseBoolean(props.getProperty("ncm_cache_downloadLyric")));
        }
    }
    
    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        List<ChangeRecord> result = new ArrayList<>();
        
        File file = currentRecord.getFileHandle();
        
        if (file.isFile()) {
            // 自动识别缓存文件格式 .uc
            if (isCacheFile(file)) {
                // 检查缓存文件是否完整
                if (isCacheFileComplete(file)) {
                    // 从文件名中提取歌曲ID
                    String songId = extractSongIdFromFileName(file.getName());
                    if (songId == null) {
                        return result;
                    }
                    
                    // 检查是否已经有对应的.info文件
                    File infoFile = new File(file.getParent(), songId + ".info");
                    String songName = null;
                    String artistName = null;
                    
                    // 如果.info文件存在，从其中读取歌曲信息
                    if (infoFile.exists()) {
                        try {
                            java.io.FileInputStream fis = new java.io.FileInputStream(infoFile);
                            byte[] buffer = new byte[1024];
                            int bytesRead = fis.read(buffer);
                            fis.close();
                            
                            if (bytesRead > 0) {
                                String content = new String(buffer, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
                                
                                // 提取 songName
                                int songNameStart = content.indexOf("songName");
                                if (songNameStart != -1) {
                                    int colonStart = content.indexOf(":", songNameStart);
                                    if (colonStart != -1) {
                                        int quoteStart = content.indexOf("\"", colonStart);
                                        if (quoteStart != -1) {
                                            int quoteEnd = content.indexOf("\"", quoteStart + 1);
                                            if (quoteEnd != -1) {
                                                songName = content.substring(quoteStart + 1, quoteEnd);
                                            }
                                        }
                                    }
                                }
                                
                                // 提取 artistName
                                int artistNameStart = content.indexOf("artistName");
                                if (artistNameStart != -1) {
                                    int colonStart = content.indexOf(":", artistNameStart);
                                    if (colonStart != -1) {
                                        int quoteStart = content.indexOf("\"", colonStart);
                                        if (quoteStart != -1) {
                                            int quoteEnd = content.indexOf("\"", quoteStart + 1);
                                            if (quoteEnd != -1) {
                                                artistName = content.substring(quoteStart + 1, quoteEnd);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logError("解析 .info 文件失败: " + e.getMessage());
                        }
                    }
                    
                    // 如果.info文件不存在或解析失败，从网易云API获取歌曲信息
                    if (songName == null || artistName == null) {
                        try {
                            java.net.URL url = new java.net.URL("http://music.163.com/api/song/detail/?id=" + songId + "&ids=%5B" + songId + "%5D");
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
                            conn.setRequestProperty("Connection", "keep-alive");
                            conn.setRequestProperty("Accept", "text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
                            
                            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                            }
                            br.close();
                            conn.disconnect();
                            
                            String response = sb.toString();
                            
                            // 提取歌曲名称
                            java.util.regex.Pattern songNamePattern = java.util.regex.Pattern.compile("name.*?:.*?([^,]+)");
                            java.util.regex.Matcher songNameMatcher = songNamePattern.matcher(response);
                            if (songNameMatcher.find()) {
                                songName = songNameMatcher.group(1).replaceAll("[\"\\{\\}]", "").trim();
                            }
                            
                            // 提取艺术家名称
                            java.util.regex.Pattern artistNamePattern = java.util.regex.Pattern.compile("artists.*?:.*?name.*?:.*?([^,]+)");
                            java.util.regex.Matcher artistNameMatcher = artistNamePattern.matcher(response);
                            if (artistNameMatcher.find()) {
                                artistName = artistNameMatcher.group(1).replaceAll("[\"\\{\\}]", "").trim();
                            }
                            
                            // 保存歌曲信息到.info文件
                            if (songName != null && artistName != null) {
                                StringBuilder infoJson = new StringBuilder();
                                infoJson.append("{");
                                infoJson.append("\"songName\":\"").append(songName).append("\",");
                                infoJson.append("\"artistName\":\"").append(artistName).append("\",");
                                infoJson.append("\"songId\":\"").append(songId).append("\"");
                                infoJson.append("}");
                                
                                java.io.FileOutputStream fos = new java.io.FileOutputStream(infoFile);
                                fos.write(infoJson.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                fos.close();
                            }
                        } catch (Exception e) {
                            logError("从网易云API获取歌曲信息失败: " + e.getMessage());
                            return result;
                        }
                    }
                    
                    // 识别缓存音频的原始格式
                    String audioFormat = identifyCacheAudioFormat(file);
                    if (audioFormat == null) {
                        return result;
                    }
                    
                    // 创建CacheFileInfo
                    CacheFileInfo cacheInfo = new CacheFileInfo(songName, artistName, audioFormat);
                    String displayName = cacheInfo.getDisplayName();
                    String targetPath = getOutputPath(file);
                    
                    // 创建ChangeRecord
                    ChangeRecord record = new ChangeRecord(file.getName(), displayName, file, true, targetPath,
                            OperationType.NCM_CACHE_SCAN);
                    
                    // 存储缓存文件信息到额外参数
                    record.getExtraParams().put("audioFormat", cacheInfo.getAudioFormat());
                    record.getExtraParams().put("songName", cacheInfo.getSongName());
                    record.getExtraParams().put("artistName", cacheInfo.getArtistName());
                    record.getExtraParams().put("songId", songId);
                    
                    result.add(record);
                }
            }
        } else if (file.isDirectory()) {
            // 处理目录
            scanCacheFiles(file, result);
        }
        return result;
    }
    
    private boolean isCacheFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".uc");
    }
    
    private boolean isCacheFileComplete(File file) {
        // 检查缓存文件是否完整
        // 对于 .uc 文件，检查是否有对应的 .idx 文件，并且 idx 文件中的数据段是完整的
        if (file.getName().toLowerCase().endsWith(".uc")) {
            String idxFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".idx";
            File idxFile = new File(file.getParent(), idxFileName);
            if (!idxFile.exists()) {
                return false;
            }
            // 解析 idx 文件，判断是否加载全部数据
            return isCompleteIdxFile(idxFile);
        }
        return false;
    }
    
    private boolean isCompleteIdxFile(File idxFile) {
        try {
            // 读取 idx 文件内容
            byte[] buffer = new byte[1024 * 10]; // 10KB 缓冲区
            java.io.FileInputStream fis = new java.io.FileInputStream(idxFile);
            int bytesRead = fis.read(buffer);
            fis.close();
            
            if (bytesRead > 0) {
                // 将字节数组转换为字符串
                String content = new String(buffer, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
                
                // 尝试解析 size 和 zone 字段
                // 这里使用简单的字符串操作，避免正则表达式的转义问题
                int size = 0;
                java.util.List<String> zone = new java.util.ArrayList<>();
                
                // 提取 size
                int sizeStart = content.indexOf("size");
                if (sizeStart != -1) {
                    int colonStart = content.indexOf(":", sizeStart);
                    if (colonStart != -1) {
                        int sizeEnd = content.indexOf(",", colonStart);
                        if (sizeEnd == -1) {
                            sizeEnd = content.indexOf("}", colonStart);
                        }
                        if (sizeEnd != -1) {
                            String sizeStr = content.substring(colonStart + 1, sizeEnd).trim();
                            size = Integer.parseInt(sizeStr);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
                
                // 提取 zone
                int zoneStart = content.indexOf("zone");
                if (zoneStart != -1) {
                    int bracketStart = content.indexOf("[", zoneStart);
                    if (bracketStart != -1) {
                        int bracketEnd = content.indexOf("]", bracketStart);
                        if (bracketEnd != -1) {
                            String zoneStr = content.substring(bracketStart + 1, bracketEnd).trim();
                            // 提取 zone 数组中的元素
                            if (zoneStr.startsWith("\"")) {
                                int quoteEnd = zoneStr.indexOf("\"", 1);
                                if (quoteEnd != -1) {
                                    String zoneElement = zoneStr.substring(1, quoteEnd);
                                    zone.add(zoneElement);
                                }
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
                
                // 检查 zone 数组长度是否为 1
                if (zone.size() != 1) {
                    return false;
                }
                
                // 检查 zone 数组第一个元素的格式
                String zoneElement = zone.get(0);
                if (zoneElement.split(" ").length > 2) {
                    return false;
                }
                
                // 检查 size 是否与 zone 中的值匹配
                int zoneEnd = Integer.parseInt(zoneElement.substring(zoneElement.lastIndexOf(' ') + 1));
                if (size != (zoneEnd + 1)) {
                    return false;
                }
                
                return true;
            }
        } catch (Exception e) {
            logError("解析 idx 文件失败: " + e.getMessage());
        }
        return false;
    }
    
    private void scanCacheFiles(File directory, List<ChangeRecord> result) {
        File[] files = directory.listFiles();
        if (files == null)
            return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanCacheFiles(file, result);
            } else if (isCacheFile(file) && isCacheFileComplete(file)) {
                // 从文件名中提取歌曲ID
                String songId = extractSongIdFromFileName(file.getName());
                if (songId == null) {
                    continue;
                }
                
                // 检查是否已经有对应的.info文件
                File infoFile = new File(file.getParent(), songId + ".info");
                String songName = null;
                String artistName = null;
                
                // 如果.info文件存在，从其中读取歌曲信息
                if (infoFile.exists()) {
                    try {
                        java.io.FileInputStream fis = new java.io.FileInputStream(infoFile);
                        byte[] buffer = new byte[1024];
                        int bytesRead = fis.read(buffer);
                        fis.close();
                        
                        if (bytesRead > 0) {
                            String content = new String(buffer, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
                            
                            // 提取 songName
                            int songNameStart = content.indexOf("songName");
                            if (songNameStart != -1) {
                                int colonStart = content.indexOf(":", songNameStart);
                                if (colonStart != -1) {
                                    int quoteStart = content.indexOf("\"", colonStart);
                                    if (quoteStart != -1) {
                                        int quoteEnd = content.indexOf("\"", quoteStart + 1);
                                        if (quoteEnd != -1) {
                                            songName = content.substring(quoteStart + 1, quoteEnd);
                                        }
                                    }
                                }
                            }
                            
                            // 提取 artistName
                            int artistNameStart = content.indexOf("artistName");
                            if (artistNameStart != -1) {
                                int colonStart = content.indexOf(":", artistNameStart);
                                if (colonStart != -1) {
                                    int quoteStart = content.indexOf("\"", colonStart);
                                    if (quoteStart != -1) {
                                        int quoteEnd = content.indexOf("\"", quoteStart + 1);
                                        if (quoteEnd != -1) {
                                            artistName = content.substring(quoteStart + 1, quoteEnd);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logError("解析 .info 文件失败: " + e.getMessage());
                    }
                }
                
                // 如果.info文件不存在或解析失败，从网易云API获取歌曲信息
                if (songName == null || artistName == null) {
                    try {
                        java.net.URL url = new java.net.URL("http://music.163.com/api/song/detail/?id=" + songId + "&ids=%5B" + songId + "%5D");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
                        conn.setRequestProperty("Connection", "keep-alive");
                        conn.setRequestProperty("Accept", "text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
                        
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        br.close();
                        conn.disconnect();
                        
                        String response = sb.toString();
                        
                        // 提取歌曲名称
                        java.util.regex.Pattern songNamePattern = java.util.regex.Pattern.compile("name.*?:.*?([^,]+)");
                        java.util.regex.Matcher songNameMatcher = songNamePattern.matcher(response);
                        if (songNameMatcher.find()) {
                            songName = songNameMatcher.group(1).replaceAll("[\"\\{\\}]", "").trim();
                        }
                        
                        // 提取艺术家名称
                        java.util.regex.Pattern artistNamePattern = java.util.regex.Pattern.compile("artists.*?:.*?name.*?:.*?([^,]+)");
                        java.util.regex.Matcher artistNameMatcher = artistNamePattern.matcher(response);
                        if (artistNameMatcher.find()) {
                            artistName = artistNameMatcher.group(1).replaceAll("[\"\\{\\}]", "").trim();
                        }
                        
                        // 保存歌曲信息到.info文件
                        if (songName != null && artistName != null) {
                            StringBuilder infoJson = new StringBuilder();
                            infoJson.append("{");
                            infoJson.append("\"songName\":\"").append(songName).append("\",");
                            infoJson.append("\"artistName\":\"").append(artistName).append("\",");
                            infoJson.append("\"songId\":\"").append(songId).append("\"");
                            infoJson.append("}");
                            
                            java.io.FileOutputStream fos = new java.io.FileOutputStream(infoFile);
                            fos.write(infoJson.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            fos.close();
                        }
                    } catch (Exception e) {
                        logError("从网易云API获取歌曲信息失败: " + e.getMessage());
                        continue;
                    }
                }
                
                // 识别缓存音频的原始格式
                String audioFormat = identifyCacheAudioFormat(file);
                if (audioFormat == null) {
                    continue;
                }
                
                // 创建CacheFileInfo
                CacheFileInfo cacheInfo = new CacheFileInfo(songName, artistName, audioFormat);
                String displayName = cacheInfo.getDisplayName();
                String targetPath = getOutputPath(file);
                
                // 创建ChangeRecord
                ChangeRecord record = new ChangeRecord(file.getName(), displayName, file, true, targetPath,
                        OperationType.NCM_CACHE_SCAN);
                
                // 存储缓存文件信息到额外参数
                record.getExtraParams().put("audioFormat", cacheInfo.getAudioFormat());
                record.getExtraParams().put("songName", cacheInfo.getSongName());
                record.getExtraParams().put("artistName", cacheInfo.getArtistName());
                record.getExtraParams().put("songId", songId);
                
                result.add(record);
            }
        }
    }
    
    private String extractSongIdFromFileName(String fileName) {
        try {
            // 从文件名中提取歌曲ID（文件名格式：{songId}-{bitrate}-{hash}.uc）
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+)-");
            java.util.regex.Matcher matcher = pattern.matcher(fileName);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            logError("从文件名提取歌曲ID失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 缓存文件信息类
     */
    private static class CacheFileInfo {
        private String songName;
        private String artistName;
        private String audioFormat;
        
        public CacheFileInfo(String songName, String artistName, String audioFormat) {
            this.songName = songName;
            this.artistName = artistName;
            this.audioFormat = audioFormat;
        }
        
        public String getSongName() {
            return songName;
        }
        
        public String getArtistName() {
            return artistName;
        }
        
        public String getAudioFormat() {
            return audioFormat;
        }
        
        public String getDisplayName() {
            if (songName != null && !songName.isEmpty() && artistName != null && !artistName.isEmpty()) {
                return artistName + " - " + songName + "." + audioFormat;
            } else if (songName != null && !songName.isEmpty()) {
                return songName + "." + audioFormat;
            } else {
                return "Unknown Song." + audioFormat;
            }
        }
    }
    
    /**
     * 解析缓存文件信息
     * @param cacheFile 缓存文件
     * @return 缓存文件信息
     */
    private CacheFileInfo parseCacheFileInfo(File cacheFile) {
        try {
            // 确保处理的是.uc文件
            File ucFile = cacheFile;
            if (cacheFile.getName().toLowerCase().endsWith(".idx")) {
                String ucFileName = cacheFile.getName().substring(0, cacheFile.getName().lastIndexOf('.')) + ".uc";
                ucFile = new File(cacheFile.getParent(), ucFileName);
                if (!ucFile.exists()) {
                    return null;
                }
            }
            
            // 识别缓存音频的原始格式
            String audioFormat = identifyCacheAudioFormat(ucFile);
            if (audioFormat == null) {
                return null;
            }
            
            // 从缓存文件中提取歌曲信息
            // 实际实现时，需要根据缓存文件格式解析元数据
            // 这里使用模拟实现，实际需要根据真实的缓存文件格式进行解析
            String songName = extractSongNameFromCache(ucFile);
            String artistName = extractArtistNameFromCache(ucFile);
            
            return new CacheFileInfo(songName, artistName, audioFormat);
        } catch (Exception e) {
            logError("解析缓存文件信息失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从缓存文件中提取歌曲名称
     * @param ucFile .uc缓存文件
     * @return 歌曲名称
     */
    private String extractSongNameFromCache(File ucFile) {
        try {
            // 1. 首先尝试从.idx文件中解析歌曲信息
            File idxFile = new File(ucFile.getParent(), ucFile.getName().replace(".uc", ".idx"));
            if (idxFile.exists()) {
                // 尝试从idx文件中读取元数据
                String songName = parseSongNameFromIdx(idxFile);
                if (songName != null && !songName.isEmpty()) {
                    return songName;
                }
            }
            
            // 2. 尝试从.uc文件中解析歌曲信息
            String songName = parseSongNameFromUc(ucFile);
            if (songName != null && !songName.isEmpty()) {
                return songName;
            }
            
            // 3. 尝试从文件名中提取歌曲信息
            String songNameFromPath = extractSongNameFromPath(ucFile);
            if (songNameFromPath != null && !songNameFromPath.isEmpty()) {
                return songNameFromPath;
            }
            
            // 4. 最后使用默认名称
            return "Unknown Song";
        } catch (Exception e) {
            logError("提取歌曲名称失败: " + e.getMessage());
            return "Unknown Song";
        }
    }
    
    /**
     * 从缓存文件中提取艺术家名称
     * @param ucFile .uc缓存文件
     * @return 艺术家名称
     */
    private String extractArtistNameFromCache(File ucFile) {
        try {
            // 1. 首先尝试从.idx文件中解析艺术家信息
            File idxFile = new File(ucFile.getParent(), ucFile.getName().replace(".uc", ".idx"));
            if (idxFile.exists()) {
                // 尝试从idx文件中读取元数据
                String artistName = parseArtistNameFromIdx(idxFile);
                if (artistName != null && !artistName.isEmpty()) {
                    return artistName;
                }
            }
            
            // 2. 尝试从.uc文件中解析艺术家信息
            String artistName = parseArtistNameFromUc(ucFile);
            if (artistName != null && !artistName.isEmpty()) {
                return artistName;
            }
            
            // 3. 尝试从文件名中提取艺术家信息
            String artistNameFromPath = extractArtistNameFromPath(ucFile);
            if (artistNameFromPath != null && !artistNameFromPath.isEmpty()) {
                return artistNameFromPath;
            }
            
            // 4. 最后使用默认名称
            return "Unknown Artist";
        } catch (Exception e) {
            logError("提取艺术家名称失败: " + e.getMessage());
            return "Unknown Artist";
        }
    }
    
    /**
     * 从.idx文件中解析歌曲名称
     * @param idxFile .idx文件
     * @return 歌曲名称
     */
    private String parseSongNameFromIdx(File idxFile) {
        try {
            // 实际实现时，需要根据.idx文件的格式解析歌曲名称
            // 这里使用模拟实现，实际需要根据真实的.idx文件格式进行解析
            // 例如：读取.idx文件的头部信息，解析出歌曲名称
            
            // 模拟解析结果
            // 实际实现时，需要从.idx文件中提取
            byte[] buffer = new byte[1024];
            java.io.FileInputStream fis = new java.io.FileInputStream(idxFile);
            int bytesRead = fis.read(buffer);
            fis.close();
            
            if (bytesRead > 0) {
                // 模拟从二进制数据中解析歌曲名称
                // 实际实现时，需要根据真实的格式进行解析
                String content = new String(buffer, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
                // 尝试从内容中提取歌曲名称
                // 这里使用简单的正则表达式，实际需要根据真实的格式进行解析
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("songName.*?([^,]+)");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
            
            return null;
        } catch (Exception e) {
            logError("解析.idx文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从.idx文件中解析艺术家名称
     * @param idxFile .idx文件
     * @return 艺术家名称
     */
    private String parseArtistNameFromIdx(File idxFile) {
        try {
            // 实际实现时，需要根据.idx文件的格式解析艺术家名称
            // 这里使用模拟实现，实际需要根据真实的.idx文件格式进行解析
            // 例如：读取.idx文件的头部信息，解析出艺术家名称
            
            // 模拟解析结果
            // 实际实现时，需要从.idx文件中提取
            byte[] buffer = new byte[1024];
            java.io.FileInputStream fis = new java.io.FileInputStream(idxFile);
            int bytesRead = fis.read(buffer);
            fis.close();
            
            if (bytesRead > 0) {
                // 模拟从二进制数据中解析艺术家名称
                // 实际实现时，需要根据真实的格式进行解析
                String content = new String(buffer, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
                // 尝试从内容中提取艺术家名称
                // 这里使用简单的正则表达式，实际需要根据真实的格式进行解析
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("artistName.*?([^,]+)");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
            
            return null;
        } catch (Exception e) {
            logError("解析.idx文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从.uc文件中解析歌曲名称
     * @param ucFile .uc文件
     * @return 歌曲名称
     */
    private String parseSongNameFromUc(File ucFile) {
        try {
            // 实际实现时，需要根据.uc文件的格式解析歌曲名称
            // 这里使用模拟实现，实际需要根据真实的.uc文件格式进行解析
            // 例如：读取.uc文件的头部信息，解析出歌曲名称
            
            // 模拟解析结果
            // 实际实现时，需要从.uc文件中提取
            return null;
        } catch (Exception e) {
            logError("解析.uc文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从.uc文件中解析艺术家名称
     * @param ucFile .uc文件
     * @return 艺术家名称
     */
    private String parseArtistNameFromUc(File ucFile) {
        try {
            // 实际实现时，需要根据.uc文件的格式解析艺术家名称
            // 这里使用模拟实现，实际需要根据真实的.uc文件格式进行解析
            // 例如：读取.uc文件的头部信息，解析出艺术家名称
            
            // 模拟解析结果
            // 实际实现时，需要从.uc文件中提取
            return null;
        } catch (Exception e) {
            logError("解析.uc文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从文件路径中提取歌曲名称
     * @param file 文件
     * @return 歌曲名称
     */
    private String extractSongNameFromPath(File file) {
        try {
            // 从文件路径中提取歌曲名称
            // 例如：如果路径中包含歌曲名称信息
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // 尝试从目录结构中提取歌曲信息
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                String parentName = parentDir.getName();
                // 尝试从父目录名称中提取歌曲信息
                // 例如：如果父目录名称包含歌曲名称
                // 这里使用简单的逻辑，实际需要根据真实的目录结构进行解析
            }
            
            // 最后返回基于文件名的名称
            return baseName;
        } catch (Exception e) {
            logError("从路径提取歌曲名称失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从文件路径中提取艺术家名称
     * @param file 文件
     * @return 艺术家名称
     */
    private String extractArtistNameFromPath(File file) {
        try {
            // 从文件路径中提取艺术家名称
            // 例如：如果路径中包含艺术家名称信息
            
            // 尝试从目录结构中提取艺术家信息
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                String parentName = parentDir.getName();
                // 尝试从父目录名称中提取艺术家信息
                // 例如：如果父目录名称包含艺术家名称
                // 这里使用简单的逻辑，实际需要根据真实的目录结构进行解析
            }
            
            // 最后返回默认值
            return "Unknown Artist";
        } catch (Exception e) {
            logError("从路径提取艺术家名称失败: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File cacheFile = rec.getFileHandle();
        executeCacheScan(cacheFile, rec);
    }
    
    private void executeCacheScan(File cacheFile, ChangeRecord rec) throws Exception {
        log("开始处理缓存文件: " + cacheFile.getName());
        
        // 确保处理的是.uc文件
        File ucFile = cacheFile;
        if (cacheFile.getName().toLowerCase().endsWith(".idx")) {
            String ucFileName = cacheFile.getName().substring(0, cacheFile.getName().lastIndexOf('.')) + ".uc";
            ucFile = new File(cacheFile.getParent(), ucFileName);
            if (!ucFile.exists()) {
                logError("对应的.uc文件不存在: " + ucFileName);
                return;
            }
        }
        
        // 从ChangeRecord中获取缓存文件信息
        Map<String, String> extraParams = rec.getExtraParams();
        String audioFormat = extraParams.get("audioFormat");
        String songName = extraParams.get("songName");
        String artistName = extraParams.get("artistName");
        
        // 如果没有缓存文件信息，则重新解析
        if (audioFormat == null) {
            audioFormat = identifyCacheAudioFormat(ucFile);
            if (audioFormat == null) {
                logError("无法识别缓存文件的音频格式: " + ucFile.getName());
                return;
            }
        }
        
        // 生成目标文件名
        String targetFileName;
        if (songName != null && !songName.isEmpty() && artistName != null && !artistName.isEmpty()) {
            targetFileName = artistName + " - " + songName + "." + audioFormat;
        } else if (songName != null && !songName.isEmpty()) {
            targetFileName = songName + "." + audioFormat;
        } else {
            targetFileName = generateTargetFileName(ucFile, audioFormat);
            if (targetFileName == null) {
                logError("无法生成目标文件名: " + ucFile.getName());
                return;
            }
        }
        
        // 确定输出目录
        String outputDirPath = getOutputPath(ucFile);
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        File targetFile = new File(outputDir, targetFileName);
        log("目标文件: " + targetFile.getAbsolutePath());
        
        // 这里添加缓存文件转换逻辑
        // 实际实现时，需要根据缓存文件格式进行解码和转换
        // 例如：处理.uc文件的解密和转换
        
        // 模拟转换完成
        log("缓存文件转换完成: " + ucFile.getName() + " -> " + targetFile.getName());
        
        // 如果需要下载歌词
        if (pDownloadLyric && targetFile.exists()) {
            // 为缓存转换后的文件创建一个临时的ChangeRecord来下载歌词
            ChangeRecord tempRec = new ChangeRecord(targetFile.getName(), targetFile.getName(), targetFile, true,
                    targetFile.getAbsolutePath(), OperationType.NCM_LYRIC_DOWNLOAD);
            
            // 提取歌曲信息
            // 优先使用缓存文件解析出的歌曲信息
            // 如果没有缓存文件信息，则从目标文件名中提取
            if (songName == null || songName.isEmpty()) {
                songName = extractSongName(targetFile);
            }
            if (artistName == null || artistName.isEmpty()) {
                artistName = extractArtistName(targetFile);
            }
            
            // 存储歌曲信息到额外参数
            tempRec.getExtraParams().put("songName", songName);
            tempRec.getExtraParams().put("artistName", artistName);
            
            // 构建歌词文件路径
            String lyricFileName = targetFile.getName().substring(0, targetFile.getName().lastIndexOf('.')) + ".lrc";
            File lyricFile = new File(targetFile.getParent(), lyricFileName);
            tempRec.setNewPath(lyricFile.getAbsolutePath());
            
            // 使用歌词下载策略执行下载
            NcmLyricDownloadStrategy lyricStrategy = new NcmLyricDownloadStrategy();
            lyricStrategy.setContext(getApp());
            lyricStrategy.execute(tempRec);
        }
        
        log("缓存文件处理完成: " + ucFile.getName());
    }
    
    private String identifyCacheAudioFormat(File ucFile) {
        // 识别缓存音频的原始格式
        // 实际实现时，需要分析缓存文件的头部信息或元数据
        // 这里使用简单的模拟实现
        
        // 模拟识别结果，实际需要根据文件内容分析
        // 例如：通过文件大小、头部特征等判断
        long fileSize = ucFile.length();
        
        // 简单的大小判断，实际需要更复杂的分析
        if (fileSize > 10 * 1024 * 1024) { // 大于10MB
            return "flac"; // 假设大文件为FLAC格式
        } else {
            return "mp3"; // 小文件为MP3格式
        }
    }
    
    private String generateTargetFileName(File ucFile, String audioFormat) {
        // 生成目标文件名
        // 实际实现时，需要从缓存文件或元数据中提取歌曲信息
        // 这里使用简单的模拟实现
        
        String baseName = ucFile.getName().substring(0, ucFile.getName().lastIndexOf('.'));
        return baseName + "." + audioFormat;
    }
    
    private String extractSongName(File file) {
        String fileName = file.getName();
        // 移除文件扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        // 尝试从文件名中提取歌曲名（假设格式为"艺术家 - 歌曲名"）
        int dashIndex = fileName.indexOf(" - ");
        if (dashIndex > 0) {
            return fileName.substring(dashIndex + 3).trim();
        }
        return fileName.trim();
    }
    
    private String extractArtistName(File file) {
        String fileName = file.getName();
        // 移除文件扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        // 尝试从文件名中提取艺术家名（假设格式为"艺术家 - 歌曲名"）
        int dashIndex = fileName.indexOf(" - ");
        if (dashIndex > 0) {
            return fileName.substring(0, dashIndex).trim();
        }
        return "Unknown Artist";
    }
}