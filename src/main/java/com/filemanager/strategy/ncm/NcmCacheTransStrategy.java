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
import com.filemanager.tool.file.AudioTypeInspector;
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
    private final NcmLyricDownloadStrategy lyricDownloadStrategy;
    
    public NcmCacheTransStrategy() {
        super("ncm_cache");
        
        // 初始化工具类
        idxFileParser = new IdxFileParser();
        neteaseApiClient = new NeteaseApiClient();
        fileNameExtractor = new FileNameExtractor();
        lyricDownloadStrategy = new NcmLyricDownloadStrategy();
        
        // 缓存扫描选项
        chkDownloadLyric = new JFXCheckBox("自动下载对应歌词");
        chkDownloadLyric.setSelected(false);
        
        // 设置默认输出路径为子目录 "Convert - Cache"
        pathSelection.getCbOutputDirMode().getSelectionModel().select("子目录");
        pathSelection.getTxtPath().setText("Convert - Cache"); 
    }
    
    @Override
    public void setContext(com.filemanager.app.base.IAppController app) {
        super.setContext(app);
        lyricDownloadStrategy.setContext(app);
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
        props.setProperty("ncm_cache_download_lyric", String.valueOf(chkDownloadLyric.isSelected()));
    }
    
    @Override
    public void loadConfig(Properties props) {
        pathSelection.loadConfig(props);
        if (props.containsKey("ncm_cache_download_lyric")) {
            chkDownloadLyric.setSelected(Boolean.parseBoolean(props.getProperty("ncm_cache_download_lyric")));
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
                        NeteaseApiClient.SongInfo songInfo = neteaseApiClient.readSongInfoFromFile(infoFile);
                        if (songInfo != null) {
                            songName = songInfo.getSongName();
                            artistName = songInfo.getArtistName();
                        }
                    }
                    
                    // 如果.info文件不存在或解析失败，从网易云API获取歌曲信息
                    if (songName == null || artistName == null) {
                        NeteaseApiClient.SongInfo songInfo = neteaseApiClient.getSongInfo(songId, infoFile);
                        if (songInfo != null) {
                            songName = songInfo.getSongName();
                            artistName = songInfo.getArtistName();
                        }
                        
                        // 如果API获取失败，返回空结果
                        if (songName == null || artistName == null) {
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
        }
        return result;
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
        String songId = extraParams.get("songId");
        
        // 如果没有缓存文件信息，则重新解析
        if (audioFormat == null) {
            audioFormat = identifyCacheAudioFormat(ucFile);
            if (audioFormat == null) {
                logError("无法识别缓存文件的音频格式: " + ucFile.getName());
                return;
            }
        }
        
        // 直接使用ChangeRecord中的目标文件路径
        File targetFile = new File(rec.getNewPath());
        
        // 确保输出目录存在
        File outputDir = targetFile.getParentFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        log("目标文件: " + targetFile.getAbsolutePath());
        
        // 缓存文件转换逻辑
        // 处理.uc文件的解密和转换
        try {
            // 读取UC文件内容
            byte[] ucContent = java.nio.file.Files.readAllBytes(ucFile.toPath());
            
            // 解密UC文件（对每个字节进行异或操作）
            for (int i = 0; i < ucContent.length; i++) {
                ucContent[i] ^= 0xa3;
            }
            
            // 写入目标文件
            java.nio.file.Files.write(targetFile.toPath(), ucContent);
            
            log("缓存文件转换完成: " + ucFile.getName() + " -> " + targetFile.getName());
            
            // 使用AudioTypeInspector检测并修复文件类型
            try {
                AudioTypeInspector.FileTypeCheckResult checkResult = AudioTypeInspector.inspectHard(targetFile);
                if (checkResult.success) {
                    if (checkResult.needsFix) {
                        // 需要修复文件类型
                        String filename = targetFile.getName();
                        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
                        File newFile = new File(targetFile.getParent(), nameWithoutExt + checkResult.suggestedExtension);
                        
                        // 重命名文件
                        if (targetFile.renameTo(newFile)) {
                            log("文件类型修复完成: " + targetFile.getName() + " -> " + newFile.getName());
                            // 更新targetFile为修复后的文件
                            targetFile = newFile;
                            // 更新ChangeRecord中的新路径
                            rec.setNewPath(newFile.getAbsolutePath());
                        } else {
                            logError("文件类型修复失败: 无法重命名文件");
                        }
                    } else {
                        log("文件类型正确，无需修复: " + targetFile.getName());
                    }
                } else {
                    logError("文件类型检测失败: " + checkResult.message);
                }
            } catch (Exception e) {
                logError("文件类型检测和修复失败: " + e.getMessage());
            }
        } catch (Exception e) {
            logError("缓存文件转换失败: " + e.getMessage());
            return;
        }
        
        // 如果需要下载歌词
        if (pDownloadLyric && targetFile.exists()) {
            // 为缓存转换后的文件创建一个临时的ChangeRecord来下载歌词
            ChangeRecord tempRec = new ChangeRecord(targetFile.getName(), targetFile.getName(), targetFile, true,
                    targetFile.getAbsolutePath(), OperationType.NCM_LYRIC_DOWNLOAD);
            
            // 存储歌曲信息到额外参数
            tempRec.getExtraParams().put("songName", songName);
            tempRec.getExtraParams().put("artistName", artistName);
            tempRec.getExtraParams().put("songId", songId);
            
            // 构建歌词文件路径
            String lyricFileName = targetFile.getName().substring(0, targetFile.getName().lastIndexOf('.')) + ".lrc";
            File lyricFile = new File(targetFile.getParent(), lyricFileName);
            tempRec.setNewPath(lyricFile.getAbsolutePath());
            
            // 使用歌词下载策略执行下载
            lyricDownloadStrategy.execute(tempRec);
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
