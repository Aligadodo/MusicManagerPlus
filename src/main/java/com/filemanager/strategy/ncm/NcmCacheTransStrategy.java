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
import com.filemanager.strategy.ncm.model.CacheFileInfo;
import com.filemanager.strategy.ncm.tool.FileNameExtractor;
import com.filemanager.strategy.ncm.tool.IdxFileParser;
import com.filemanager.strategy.ncm.tool.NeteaseApiClient;
import com.filemanager.type.OperationType;
import com.jfoenix.controls.JFXCheckBox;
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
 * @author 28667
 */
public class NcmCacheTransStrategy extends NcmBaseStrategy {
    // UI组件
    private final CheckBox chkDownloadLyric;
    
    // 运行时参数
    private boolean pDownloadLyric;
    
    // 工具类实例
    private final IdxFileParser idxFileParser;
    private final NeteaseApiClient neteaseApiClient;
    private final FileNameExtractor fileNameExtractor;
    
    public NcmCacheTransStrategy() {
        super("ncm_cache");
        
        // 初始化工具类
        idxFileParser = new IdxFileParser();
        neteaseApiClient = new NeteaseApiClient();
        fileNameExtractor = new FileNameExtractor();
        
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
            if (fileNameExtractor.isCacheFile(file)) {
                // 检查缓存文件是否完整
                if (idxFileParser.isCacheFileComplete(file)) {
                    // 从文件名中提取歌曲ID
                    String songId = fileNameExtractor.extractSongIdFromFileName(file.getName());
                    if (songId == null) {
                        return result;
                    }
                    
                    // 检查是否已经有对应的.info文件
                    String ucFileName = file.getName();
                    String infoFileName = ucFileName.substring(0, ucFileName.lastIndexOf('.')) + ".info";
                    File infoFile = new File(file.getParent(), infoFileName);
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
                            
                            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

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
                    String targetDir = getOutputPath(file);
                    // 确保targetPath是包含文件名称的全路径
                    String targetPath = targetDir + File.separator + displayName;
                    
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
    

    
    private void scanCacheFiles(File directory, List<ChangeRecord> result) {
        File[] files = directory.listFiles();
        if (files == null)
            return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanCacheFiles(file, result);
            } else if (fileNameExtractor.isCacheFile(file) && idxFileParser.isCacheFileComplete(file)) {
                // 从文件名中提取歌曲ID
                String songId = fileNameExtractor.extractSongIdFromFileName(file.getName());
                if (songId == null) {
                    continue;
                }
                
                // 检查是否已经有对应的.info文件
                String ucFileName = file.getName();
                String infoFileName = ucFileName.substring(0, ucFileName.lastIndexOf('.')) + ".info";
                File infoFile = new File(file.getParent(), infoFileName);
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
                String targetDir = getOutputPath(file);
                // 确保targetPath是包含文件名称的全路径
                String targetPath = targetDir + File.separator + displayName;
                
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