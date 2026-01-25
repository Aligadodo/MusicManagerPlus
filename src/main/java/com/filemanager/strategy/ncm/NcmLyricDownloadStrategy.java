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

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 歌词下载策略
 * 负责为音频文件下载对应的歌词
 */
public class NcmLyricDownloadStrategy extends NcmBaseStrategy {
    // UI组件
    private final CheckBox chkOverwriteExisting;
    private final CheckBox chkPreMatchLyric;
    
    // 运行时参数
    private boolean pOverwriteExisting;
    private boolean pPreMatchLyric;
    
    // API客户端
    private final NeteaseApiClient neteaseApiClient;
    
    public NcmLyricDownloadStrategy() {
        super("ncm_lyric");
        
        // 初始化API客户端
        neteaseApiClient = new NeteaseApiClient();
        
        // 歌词下载选项
        chkOverwriteExisting = new JFXCheckBox("覆盖已存在的歌词文件");
        chkOverwriteExisting.setSelected(false);
        
        // 预匹配歌词选项
        chkPreMatchLyric = new JFXCheckBox("预览阶段先匹配歌词");
        chkPreMatchLyric.setSelected(true);
    }
    
    @Override
    public String getName() {
        return "歌词下载";
    }
    
    @Override
    public Node getConfigNode() {
        VBox configBox = new VBox();
        configBox.setSpacing(10);
        
        configBox.getChildren().addAll(
            StyleFactory.createChapter("歌词下载选项"),
            chkOverwriteExisting,
            chkPreMatchLyric
        );
        
        return configBox;
    }
    
    @Override
    public void captureParams() {
        pOverwriteExisting = chkOverwriteExisting.isSelected();
        pPreMatchLyric = chkPreMatchLyric.isSelected();
        pathSelection.captureParams();
    }
    
    @Override
    public void saveConfig(Properties props) {
        pathSelection.saveConfig(props);
        props.setProperty("ncm_lyric_overwriteExisting", String.valueOf(chkOverwriteExisting.isSelected()));
        props.setProperty("ncm_lyric_preMatchLyric", String.valueOf(chkPreMatchLyric.isSelected()));
    }
    
    @Override
    public void loadConfig(Properties props) {
        pathSelection.loadConfig(props);
        if (props.containsKey("ncm_lyric_overwriteExisting")) {
            chkOverwriteExisting.setSelected(Boolean.parseBoolean(props.getProperty("ncm_lyric_overwriteExisting")));
        }
        if (props.containsKey("ncm_lyric_preMatchLyric")) {
            chkPreMatchLyric.setSelected(Boolean.parseBoolean(props.getProperty("ncm_lyric_preMatchLyric")));
        }
    }
    
    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        List<ChangeRecord> result = new ArrayList<>();
        
        File file = currentRecord.getFileHandle();
        
        if (file.isFile() && isAudioFile(file)) {
            // 构建歌词文件路径
            String lyricFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".lrc";
            File lyricFile = new File(file.getParent(), lyricFileName);
            String lyricFilePath = lyricFile.getAbsolutePath();
            
            // 创建ChangeRecord
            ChangeRecord record = new ChangeRecord(file.getName(), lyricFileName, file, true, lyricFilePath,
                    OperationType.NCM_LYRIC_DOWNLOAD);
            
            // 提取歌曲信息
            String songName = extractSongName(file);
            String artistName = extractArtistName(file);
            
            // 存储歌曲信息到额外参数
            record.getExtraParams().put("songName", songName);
            record.getExtraParams().put("artistName", artistName);
            
            // 根据pPreMatchLyric决定是否在分析阶段预匹配歌词
            if (pPreMatchLyric) {
                // 尝试搜索歌曲，获取歌曲ID
                String songId = null;
                try {
                    songId = searchSong(songName, artistName);
                } catch (Exception e) {
                    logError("搜索歌曲时发生错误: " + e.getMessage());
                }
                
                if (songId != null) {
                    record.getExtraParams().put("songId", songId);
                    log("找到歌曲ID: " + songId + " 对应歌曲: " + songName
                            + (artistName.isEmpty() ? "" : " - " + artistName));
                    
                    result.add(record);
                } else {
                    log("未找到对应歌曲: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
                }
            } else {
                result.add(record);
            }
        }
        
        return result;
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        executeLyricDownload(rec);
    }
    
    private void executeLyricDownload(ChangeRecord rec) throws Exception {
        File audioFile = rec.getFileHandle();
        log("开始为音频文件下载歌词: " + audioFile.getName());
        
        // 从ChangeRecord中获取歌曲信息
        String songName = rec.getExtraParams().get("songName");
        String artistName = rec.getExtraParams().get("artistName");
        String songId = rec.getExtraParams().get("songId");
        
        if (songName == null || songName.isEmpty()) {
            logError("无法获取歌曲名称: " + audioFile.getName());
            return;
        }
        
        String lyricContent = null;
        if (songId != null) {
            // 直接使用歌曲ID获取歌词
            lyricContent = getLyricById(songId);
        } else {
            // 再次尝试搜索并下载歌词
            lyricContent = downloadLyric(songName, artistName);
        }
        
        if (lyricContent != null && !lyricContent.isEmpty()) {
            // 使用ChangeRecord中的新路径作为歌词文件路径
            File lyricFile = new File(rec.getNewPath());
            
            if (!lyricFile.exists() || pOverwriteExisting) {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(lyricFile), StandardCharsets.UTF_8))) {
                    writer.write(lyricContent);
                }
                log("歌词下载完成，已保存为: " + lyricFile.getName());
            } else {
                log("歌词文件已存在，跳过下载: " + lyricFile.getName());
            }
        } else {
            logError("未找到对应歌词: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
        }
    }
    
    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") ||
                name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ogg");
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
        return "";
    }
    
    private String downloadLyric(String songName, String artistName) throws Exception {
        log("尝试下载歌曲歌词: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
        
        // 搜索歌曲，获取歌曲ID
        String songId = searchSong(songName, artistName);
        if (songId == null) {
            logError("未找到对应歌曲: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
            return null;
        }
        
        // 根据歌曲ID获取歌词
        return getLyricById(songId);
    }
    
    /**
     * 搜索歌曲，获取歌曲ID
     * 
     * @param songName   歌曲名称
     * @param artistName 艺术家名称
     * @return 歌曲ID
     */
    private String searchSong(String songName, String artistName) throws Exception {
        return neteaseApiClient.searchSong(songName, artistName);
    }
    
    /**
     * 根据歌曲ID获取歌词
     * 
     * @param songId 歌曲ID
     * @return 歌词内容
     */
    private String getLyricById(String songId) throws Exception {
        return neteaseApiClient.getLyricById(songId);
    }
    

    
    private String generateMockLyric(String songName, String artistName) {
        StringBuilder lyric = new StringBuilder();
        lyric.append("[ti:").append(songName).append("]\n");
        if (!artistName.isEmpty()) {
            lyric.append("[ar:").append(artistName).append("]\n");
        }
        lyric.append("[al:未知专辑]\n");
        lyric.append("[00:00.00]歌词下载功能模拟\n");
        lyric.append("[00:05.00]实际实现时应调用网易云API\n");
        lyric.append("[00:10.00]获取真实歌词内容\n");
        return lyric.toString();
    }
    
    @Override
    public String getDescription() {
        return "为音频文件下载对应的网易云音乐歌词";
    }
    
    @Override
    public com.filemanager.type.ScanTarget getTargetType() {
        return com.filemanager.type.ScanTarget.FILES_ONLY; // 只支持文件
    }
}
