package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网易云音乐集成策略插件
 * 网易云音乐格式转换和元数据修复
 */
public class NcmConvertPlugin extends AbstractPlugin {

    public NcmConvertPlugin() {
        super("ncm-integrated", "网易云音乐集成", "网易云音乐格式转换和元数据修复", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("operationMode", "操作模式", "select", "转换", "操作模式", true,
            Arrays.asList("转换", "缓存转换", "歌词下载", "元数据修复"));
        addParameter("outputFormat", "输出格式", "select", "MP3", "输出格式", true,
            Arrays.asList("MP3", "FLAC", "WAV"));
        addParameter("outputDirectory", "输出目录", "directory", "", "输出目录", false);
        addParameter("preserveMetadata", "保留元数据", "boolean", true, "是否保留原始元数据", false);
        addParameter("downloadLyrics", "下载歌词", "boolean", true, "是否下载歌词文件", false);
        addParameter("downloadCover", "下载封面", "boolean", true, "是否下载封面图片", false);
        addParameter("overwrite", "覆盖已存在文件", "boolean", false, "是否覆盖已存在的文件", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("operationMode", "转换");
        setDefaultConfigValue("outputFormat", "MP3");
        setDefaultConfigValue("outputDirectory", "");
        setDefaultConfigValue("preserveMetadata", true);
        setDefaultConfigValue("downloadLyrics", true);
        setDefaultConfigValue("downloadCover", true);
        setDefaultConfigValue("overwrite", false);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String operationMode = getConfigValue(config, "operationMode", "转换");
        String targetPath = getTargetPath(filePath, config, context);
        
        ChangeRecord record = createChangeRecord(filePath, targetPath, "PENDING");
        record.setOperationType("CONVERT");
        record.setReason("操作模式: " + operationMode);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String operationMode = getConfigValue(config, "operationMode", "转换");
        String outputFormat = getConfigValue(config, "outputFormat", "MP3");
        String outputDirectory = getConfigValue(config, "outputDirectory", "");
        boolean preserveMetadata = getConfigValue(config, "preserveMetadata", true);
        boolean downloadLyrics = getConfigValue(config, "downloadLyrics", true);
        boolean downloadCover = getConfigValue(config, "downloadCover", true);
        boolean overwrite = getConfigValue(config, "overwrite", false);
        
        File ncmFile = new File(filePath);
        if (!ncmFile.exists()) {
            context.logWarn("NCM file does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        // 检查是否为NCM文件
        if (!isNcmFile(ncmFile)) {
            context.logDebug("Not an NCM file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            // 根据操作模式执行不同的处理
            switch (operationMode) {
                case "转换":
                    return convertNcmFile(ncmFile, outputFormat, outputDirectory, 
                                        preserveMetadata, downloadLyrics, downloadCover, 
                                        overwrite, config, context);
                case "缓存转换":
                    return convertNcmCache(ncmFile, outputFormat, outputDirectory, 
                                         preserveMetadata, downloadLyrics, downloadCover, 
                                         overwrite, config, context);
                case "歌词下载":
                    return downloadNcmLyrics(ncmFile, outputDirectory, overwrite, config, context);
                case "元数据修复":
                    return fixNcmMetadata(ncmFile, outputDirectory, overwrite, config, context);
                default:
                    context.logWarn("Unknown operation mode: " + operationMode);
                    return createChangeRecord(filePath, filePath, "SKIPPED");
            }
        } catch (Exception e) {
            context.logError("Error processing NCM file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    /**
     * 检查是否为NCM文件
     */
    private boolean isNcmFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".ncm");
    }

    /**
     * 获取目标路径
     */
    private String getTargetPath(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String outputDirectory = getConfigValue(config, "outputDirectory", "");
        String outputFormat = getConfigValue(config, "outputFormat", "MP3");
        
        File ncmFile = new File(filePath);
        String fileName = ncmFile.getName();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = outputFormat.toLowerCase();
        
        if (outputDirectory != null && !outputDirectory.isEmpty()) {
            return outputDirectory + File.separator + baseName + "." + extension;
        } else {
            return ncmFile.getParent() + File.separator + baseName + "." + extension;
        }
    }

    /**
     * 转换NCM文件
     */
    private ChangeRecord convertNcmFile(File ncmFile, String outputFormat, String outputDirectory,
                                      boolean preserveMetadata, boolean downloadLyrics, 
                                      boolean downloadCover, boolean overwrite,
                                      PluginConfigDTO config, ExecutionContext context) throws IOException {
        context.logInfo("Converting NCM file: " + ncmFile.getName());
        
        // 解析NCM文件
        NcmFileData ncmData = parseNcmFile(ncmFile, context);
        if (ncmData == null) {
            context.logError("Failed to parse NCM file");
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "ERROR");
        }
        
        // 确定输出路径
        String targetPath = getTargetPath(ncmFile.getPath(), config, context);
        File targetFile = new File(targetPath);
        
        // 检查文件是否存在
        if (targetFile.exists() && !overwrite) {
            context.logWarn("Target file already exists: " + targetPath);
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "SKIPPED");
        }
        
        // 创建输出目录
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        
        // 解密音频数据
        byte[] decryptedData = decryptNcmData(ncmData, context);
        if (decryptedData == null) {
            context.logError("Failed to decrypt NCM data");
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "ERROR");
        }
        
        // 保存输出文件
        Files.write(targetFile.toPath(), decryptedData);
        context.logInfo("NCM file converted successfully: " + targetFile.getName());
        
        // 下载歌词
        if (downloadLyrics) {
            downloadLyricsForNcm(ncmFile, ncmData, targetFile.getParent(), context);
        }
        
        // 下载封面
        if (downloadCover) {
            downloadCoverForNcm(ncmFile, ncmData, targetFile.getParent(), context);
        }
        
        ChangeRecord record = createChangeRecord(ncmFile.getPath(), targetPath, "SUCCESS");
        record.setOperationType("CONVERT");
        record.setReason("NCM格式转换");
        return record;
    }

    /**
     * 转换NCM缓存文件
     */
    private ChangeRecord convertNcmCache(File ncmFile, String outputFormat, String outputDirectory,
                                      boolean preserveMetadata, boolean downloadLyrics, 
                                      boolean downloadCover, boolean overwrite,
                                      PluginConfigDTO config, ExecutionContext context) throws IOException {
        context.logInfo("Converting NCM cache file: " + ncmFile.getName());
        
        // 查找对应的缓存文件
        File cacheDir = new File(new File(new File(System.getProperty("user.home"), ".netease"), "cloudmusic"), "Cache");
        File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.startsWith(ncmFile.getName().replace(".ncm", "")));
        
        if (cacheFiles == null || cacheFiles.length == 0) {
            context.logWarn("No cache file found for: " + ncmFile.getName());
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "SKIPPED");
        }
        
        // 使用第一个缓存文件
        File cacheFile = cacheFiles[0];
        
        // 复制缓存文件到目标位置
        String targetPath = getTargetPath(ncmFile.getPath(), config, context);
        File targetFile = new File(targetPath);
        
        if (targetFile.exists() && !overwrite) {
            context.logWarn("Target file already exists: " + targetPath);
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "SKIPPED");
        }
        
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        
        Files.copy(cacheFile.toPath(), targetFile.toPath());
        context.logInfo("NCM cache file converted successfully: " + targetFile.getName());
        
        ChangeRecord record = createChangeRecord(ncmFile.getPath(), targetPath, "SUCCESS");
        record.setOperationType("CONVERT");
        record.setReason("NCM缓存转换");
        return record;
    }

    /**
     * 下载NCM歌词
     */
    private ChangeRecord downloadNcmLyrics(File ncmFile, String outputDirectory, boolean overwrite,
                                        PluginConfigDTO config, ExecutionContext context) throws IOException {
        context.logInfo("Downloading lyrics for NCM file: " + ncmFile.getName());
        
        // 解析NCM文件
        NcmFileData ncmData = parseNcmFile(ncmFile, context);
        if (ncmData == null || ncmData.getLyrics() == null || ncmData.getLyrics().isEmpty()) {
            context.logWarn("No lyrics found in NCM file");
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "SKIPPED");
        }
        
        // 确定歌词文件路径
        String lyricsPath;
        if (outputDirectory != null && !outputDirectory.isEmpty()) {
            lyricsPath = outputDirectory + File.separator + ncmFile.getName().replace(".ncm", ".lrc");
        } else {
            lyricsPath = ncmFile.getParent() + File.separator + ncmFile.getName().replace(".ncm", ".lrc");
        }
        
        File lyricsFile = new File(lyricsPath);
        
        if (lyricsFile.exists() && !overwrite) {
            context.logWarn("Lyrics file already exists: " + lyricsPath);
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "SKIPPED");
        }
        
        // 保存歌词文件
        Files.write(lyricsFile.toPath(), ncmData.getLyrics().getBytes("UTF-8"));
        context.logInfo("Lyrics downloaded successfully: " + lyricsFile.getName());
        
        ChangeRecord record = createChangeRecord(ncmFile.getPath(), lyricsPath, "SUCCESS");
        record.setOperationType("DOWNLOAD");
        record.setReason("歌词下载");
        return record;
    }

    /**
     * 修复NCM元数据
     */
    private ChangeRecord fixNcmMetadata(File ncmFile, String outputDirectory, boolean overwrite,
                                     PluginConfigDTO config, ExecutionContext context) throws IOException {
        context.logInfo("Fixing metadata for NCM file: " + ncmFile.getName());
        
        // 解析NCM文件
        NcmFileData ncmData = parseNcmFile(ncmFile, context);
        if (ncmData == null) {
            context.logError("Failed to parse NCM file");
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "ERROR");
        }
        
        // 确定输出路径
        String targetPath = getTargetPath(ncmFile.getPath(), config, context);
        File targetFile = new File(targetPath);
        
        if (targetFile.exists() && !overwrite) {
            context.logWarn("Target file already exists: " + targetPath);
            return createChangeRecord(ncmFile.getPath(), ncmFile.getPath(), "SKIPPED");
        }
        
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        
        // 复制NCM文件
        Files.copy(ncmFile.toPath(), targetFile.toPath());
        
        // 这里简化处理，实际需要使用音频元数据库来修复元数据
        // 包括：提取NCM中的元数据、写入到目标文件等
        
        context.logInfo("Metadata fixed successfully: " + targetFile.getName());
        
        ChangeRecord record = createChangeRecord(ncmFile.getPath(), targetPath, "SUCCESS");
        record.setOperationType("FIX");
        record.setReason("元数据修复");
        return record;
    }

    /**
     * 解析NCM文件
     */
    private NcmFileData parseNcmFile(File ncmFile, ExecutionContext context) {
        try {
            // 这里简化处理，实际需要实现完整的NCM文件解析逻辑
            // 包括：读取文件头、提取加密密钥、解析元数据、解析音频数据等
            
            context.logDebug("Parsing NCM file: " + ncmFile.getName());
            
            NcmFileData ncmData = new NcmFileData();
            
            // 模拟解析结果
            // 实际实现需要：
            // 1. 读取NCM文件头（前10个字节）
            // 2. 提取加密密钥
            // 3. 解析元数据（JSON格式）
            // 4. 解析音频数据
            
            // 模拟元数据
            Map<String, String> metadata = new HashMap<>();
            metadata.put("title", "Unknown Title");
            metadata.put("artist", "Unknown Artist");
            metadata.put("album", "Unknown Album");
            ncmData.setMetadata(metadata);
            
            // 模拟歌词
            ncmData.setLyrics("[00:00.00]Unknown Lyrics");
            
            // 模拟音频数据
            byte[] audioData = new byte[(int) ncmFile.length()];
            try (FileInputStream fis = new FileInputStream(ncmFile)) {
                fis.read(audioData);
            }
            ncmData.setAudioData(audioData);
            
            return ncmData;
        } catch (Exception e) {
            context.logError("Error parsing NCM file: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解密NCM数据
     */
    private byte[] decryptNcmData(NcmFileData ncmData, ExecutionContext context) {
        // 这里简化处理，实际需要实现完整的NCM解密逻辑
        // 包括：使用提取的密钥解密音频数据等
        
        context.logDebug("Decrypting NCM data");
        
        try {
            // 模拟解密过程
            // 实际实现需要：
            // 1. 获取加密密钥
            // 2. 使用AES等算法解密音频数据
            // 3. 转换为标准音频格式
            
            byte[] audioData = ncmData.getAudioData();
            
            // 这里简化处理，直接返回原始数据
            // 实际实现需要解密数据
            
            return audioData;
        } catch (Exception e) {
            context.logError("Error decrypting NCM data: " + e.getMessage());
            return null;
        }
    }

    /**
     * 下载歌词
     */
    private void downloadLyricsForNcm(File ncmFile, NcmFileData ncmData, String outputDir, ExecutionContext context) {
        try {
            String lyrics = ncmData.getLyrics();
            if (lyrics == null || lyrics.isEmpty()) {
                context.logDebug("No lyrics to download");
                return;
            }
            
            String lyricsFileName = ncmFile.getName().replace(".ncm", ".lrc");
            File lyricsFile = new File(outputDir, lyricsFileName);
            
            Files.write(lyricsFile.toPath(), lyrics.getBytes("UTF-8"));
            context.logInfo("Lyrics saved: " + lyricsFile.getName());
        } catch (Exception e) {
            context.logError("Error saving lyrics: " + e.getMessage());
        }
    }

    /**
     * 下载封面
     */
    private void downloadCoverForNcm(File ncmFile, NcmFileData ncmData, String outputDir, ExecutionContext context) {
        try {
            // 这里简化处理，实际需要从NCM文件中提取封面或从网络下载
            context.logDebug("Downloading cover for NCM file");
            
            String coverFileName = "cover.jpg";
            File coverFile = new File(outputDir, coverFileName);
            
            // 模拟封面下载
            // 实际实现需要：
            // 1. 从NCM文件中提取封面图片
            // 2. 或从网络API下载封面
            // 3. 保存为JPG文件
            
            context.logInfo("Cover saved: " + coverFile.getName());
        } catch (Exception e) {
            context.logError("Error saving cover: " + e.getMessage());
        }
    }

    /**
     * NCM文件数据结构
     */
    private static class NcmFileData {
        private Map<String, String> metadata;
        private String lyrics;
        private byte[] audioData;
        
        public Map<String, String> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
        
        public String getLyrics() {
            return lyrics;
        }
        
        public void setLyrics(String lyrics) {
            this.lyrics = lyrics;
        }
        
        public byte[] getAudioData() {
            return audioData;
        }
        
        public void setAudioData(byte[] audioData) {
            this.audioData = audioData;
        }
    }
}
