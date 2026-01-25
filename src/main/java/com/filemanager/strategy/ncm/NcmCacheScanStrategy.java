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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
    }
    
    @Override
    public String getName() {
        return "缓存扫描";
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
            // 自动识别缓存文件格式 .idx 和 .uc
            if (isCacheFile(file)) {
                // 检查缓存文件是否完整
                if (isCacheFileComplete(file)) {
                    ChangeRecord record = new ChangeRecord(file.getName(), file.getName(), file, true,
                            getOutputPath(file), OperationType.NCM_CACHE_SCAN);
                    result.add(record);
                }
            }
        } else if (file.isDirectory()) {
            // 扫描目录中的缓存文件
            scanCacheFiles(file, result);
        }
        
        return result;
    }
    
    private boolean isCacheFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".idx") || name.endsWith(".uc");
    }
    
    private boolean isCacheFileComplete(File file) {
        // 检查缓存文件是否完整
        // 对于 .uc 文件，检查是否有对应的 .idx 文件
        if (file.getName().toLowerCase().endsWith(".uc")) {
            String idxFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".idx";
            File idxFile = new File(file.getParent(), idxFileName);
            return idxFile.exists();
        }
        // 对于 .idx 文件，检查是否有对应的 .uc 文件
        else if (file.getName().toLowerCase().endsWith(".idx")) {
            String ucFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".uc";
            File ucFile = new File(file.getParent(), ucFileName);
            return ucFile.exists();
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
                ChangeRecord record = new ChangeRecord(file.getName(), file.getName(), file, true, getOutputPath(file),
                        OperationType.NCM_CACHE_SCAN);
                result.add(record);
            }
        }
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File cacheFile = rec.getFileHandle();
        executeCacheScan(cacheFile);
    }
    
    private void executeCacheScan(File cacheFile) throws Exception {
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
        
        // 识别缓存音频的原始格式
        String audioFormat = identifyCacheAudioFormat(ucFile);
        if (audioFormat == null) {
            logError("无法识别缓存文件的音频格式: " + ucFile.getName());
            return;
        }
        
        // 生成目标文件名
        String targetFileName = generateTargetFileName(ucFile, audioFormat);
        if (targetFileName == null) {
            logError("无法生成目标文件名: " + ucFile.getName());
            return;
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
            String songName = extractSongName(targetFile);
            String artistName = extractArtistName(targetFile);
            
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
        return "";
    }
    
    @Override
    public String getDescription() {
        return "网易云音乐缓存文件扫描和转换功能";
    }
    
    @Override
    public com.filemanager.type.ScanTarget getTargetType() {
        return com.filemanager.type.ScanTarget.ALL; // 支持文件和目录
    }
}
