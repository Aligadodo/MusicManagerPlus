package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 音频格式转换策略插件
 * 高品质音频转换。支持参数微调、乱码修复及智能覆盖检测等。
 */
public class AudioConverterPlugin extends AbstractPlugin {

    public AudioConverterPlugin() {
        super("audio-converter", "音频格式转换", "高品质音频转换。支持参数微调、乱码修复及智能覆盖检测等。", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("targetFormat", "目标格式", "select", "WAV (CD标准)", "转换后的音频文件格式", true,
            Arrays.asList("WAV (CD标准)", "FLAC", "WAV", "MP3", "ALAC", "AAC", "OGG"));
        addParameter("outputDirMode", "输出目录模式", "select", "子目录", "输出目录模式", true,
            Arrays.asList("子目录", "指定目录", "根目录"));
        addParameter("outputPath", "输出路径", "directory", "Convert - WAV", "转换后文件的输出路径", true);
        addParameter("sampleRate", "采样率", "select", "44100", "转换后的音频采样率", false,
            Arrays.asList("保持原样 (Original)", "44100", "48000", "88200", "96000", "192000"));
        addParameter("channels", "声道数", "select", "2 (Stereo)", "转换后的音频声道数", false,
            Arrays.asList("保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)"));
        addParameter("overwrite", "强制覆盖", "boolean", false, "是否覆盖已存在的目标文件", false);
        addParameter("ffmpegThreads", "FFmpeg线程数", "number", 4, "FFmpeg的线程数", false);
        addParameter("ffmpegPath", "FFmpeg路径", "string", "ffmpeg", "FFmpeg可执行文件的路径", false);
        addParameter("enableCache", "启用临时文件缓存", "boolean", false, "启用临时文件缓存以缓解IO瓶颈", false);
        addParameter("cacheDir", "缓存目录", "directory", "", "临时文件缓存目录路径", false);
        addParameter("enableSnap", "启用镜像路径暂存", "boolean", false, "启用镜像路径暂存（需要手动移动文件）", false);
        addParameter("snapDir", "镜像存储目录", "directory", "", "镜像存储目录路径", false);
        addParameter("enableTempSuffix", "启用.temp文件后缀", "boolean", true, "启用.temp文件后缀（文件缓存启用时不生效）", false);
        addParameter("forceFilenameMeta", "忽略原始文件标签", "boolean", false, "忽略原始文件标签，强制用文件名重构元数据", false);
        addParameter("autoFormatFilename", "自动格式化目标文件名", "boolean", true, "自动将目标文件名转换为简体中文并去除首尾空格", false);
        addParameter("skipCueTracks", "智能跳过处理", "boolean", true, "当音频文件大于100MB且同目录下有.cue文件时，跳过处理", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("targetFormat", "WAV (CD标准)");
        setDefaultConfigValue("outputDirMode", "子目录");
        setDefaultConfigValue("outputPath", "Convert - WAV");
        setDefaultConfigValue("sampleRate", "44100");
        setDefaultConfigValue("channels", "2 (Stereo)");
        setDefaultConfigValue("overwrite", false);
        setDefaultConfigValue("ffmpegThreads", 4);
        setDefaultConfigValue("ffmpegPath", "ffmpeg");
        setDefaultConfigValue("enableCache", false);
        setDefaultConfigValue("cacheDir", "");
        setDefaultConfigValue("enableSnap", false);
        setDefaultConfigValue("snapDir", "");
        setDefaultConfigValue("enableTempSuffix", true);
        setDefaultConfigValue("forceFilenameMeta", false);
        setDefaultConfigValue("autoFormatFilename", true);
        setDefaultConfigValue("skipCueTracks", true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String targetPath = getTargetPath(filePath, config, context);
        ChangeRecord record = createChangeRecord(filePath, targetPath, "PENDING");
        record.setOperationType("CONVERT");
        record.setReason("音频格式转换");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        boolean skipCueTracks = getConfigValue(config, "skipCueTracks", true);
        boolean autoFormatFilename = getConfigValue(config, "autoFormatFilename", true);
        boolean overwrite = getConfigValue(config, "overwrite", false);
        boolean enableCache = getConfigValue(config, "enableCache", false);
        boolean enableSnap = getConfigValue(config, "enableSnap", false);
        boolean enableTempSuffix = getConfigValue(config, "enableTempSuffix", true);
        String ffmpegPath = getConfigValue(config, "ffmpegPath", "ffmpeg");
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        // 检查是否为音频文件
        if (!isAudioFile(sourceFile)) {
            context.logDebug("Not an audio file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        // 智能跳过检查
        if (skipCueTracks && shouldSkipCueTrack(sourceFile, context)) {
            context.logInfo("Skipping CD mirror file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String targetPath = getTargetPath(filePath, config, context);
            File targetFile = new File(targetPath);
            
            // 格式化目标文件名
            if (autoFormatFilename) {
                String formattedName = formatFilename(targetFile.getName());
                targetFile = new File(targetFile.getParent(), formattedName);
                targetPath = targetFile.getPath();
            }
            
            // 检查目标文件是否存在
            if (targetFile.exists() && !overwrite) {
                context.logWarn("Target file already exists: " + targetPath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            // 创建目标目录
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
                context.logDebug("Created directory: " + targetFile.getParentFile().getPath());
            }
            
            // 确定实际输出文件路径
            String actualOutputPath = targetPath;
            if (enableCache) {
                String cacheDir = getConfigValue(config, "cacheDir", "");
                actualOutputPath = cacheDir + File.separator + targetFile.getName();
            } else if (enableSnap) {
                String snapDir = getConfigValue(config, "snapDir", "");
                actualOutputPath = snapDir + File.separator + targetFile.getName();
            } else if (enableTempSuffix) {
                actualOutputPath = targetPath + ".temp";
            }
            
            File actualTargetFile = new File(actualOutputPath);
            
            // 执行音频转换
            boolean success = convertAudio(sourceFile, actualTargetFile, config, context);
            
            if (success) {
                // 如果使用了临时文件后缀，重命名为最终文件名
                if (enableTempSuffix && actualTargetFile.getName().endsWith(".temp")) {
                    File finalFile = new File(actualOutputPath.replace(".temp", ""));
                    Files.move(actualTargetFile.toPath(), finalFile.toPath());
                    actualOutputPath = finalFile.getPath();
                }
                
                context.logInfo("Audio conversion successful: " + filePath + " -> " + actualOutputPath);
                ChangeRecord record = createChangeRecord(filePath, actualOutputPath, "SUCCESS");
                record.setOperationType("CONVERT");
                record.setReason("音频格式转换");
                return record;
            } else {
                context.logError("Audio conversion failed: " + filePath);
                return createChangeRecord(filePath, filePath, "ERROR");
            }
        } catch (Exception e) {
            context.logError("Error converting audio " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    /**
     * 检查是否应该跳过CUE音轨
     */
    private boolean shouldSkipCueTrack(File file, ExecutionContext context) {
        // 检查文件大小是否大于100MB
        if (file.length() <= 100 * 1024 * 1024) {
            return false;
        }
        
        // 检查同目录下是否存在.cue文件
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            File[] cueFiles = parentDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".cue"));
            if (cueFiles != null && cueFiles.length > 0) {
                context.logDebug("Found CUE file in directory: " + parentDir.getPath());
                return true;
            }
        }
        
        return false;
    }

    /**
     * 检查是否为音频文件
     */
    private boolean isAudioFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".flac") || 
               fileName.endsWith(".wav") || fileName.endsWith(".aac") ||
               fileName.endsWith(".ogg") || fileName.endsWith(".wma") ||
               fileName.endsWith(".m4a") || fileName.endsWith(".ape") ||
               fileName.endsWith(".m4p") || fileName.endsWith(".mp4");
    }

    /**
     * 获取目标路径
     */
    private String getTargetPath(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String outputDirMode = getConfigValue(config, "outputDirMode", "子目录");
        String outputPath = getConfigValue(config, "outputPath", "Convert - WAV");
        String targetFormat = getConfigValue(config, "targetFormat", "WAV (CD标准)");
        
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        String extension = getFileExtension(targetFormat);
        String targetFileName = changeExtension(fileName, extension);
        
        switch (outputDirMode) {
            case "子目录":
                File sourceDir = sourceFile.getParentFile();
                if (sourceDir != null) {
                    return sourceDir.getPath() + File.separator + outputPath + File.separator + targetFileName;
                }
                return outputPath + File.separator + targetFileName;
            case "指定目录":
                return outputPath + File.separator + targetFileName;
            case "根目录":
                File rootPath = sourceFile;
                while (rootPath.getParent() != null) {
                    rootPath = rootPath.getParentFile();
                }
                return rootPath.getPath() + File.separator + outputPath + File.separator + targetFileName;
            default:
                return outputPath + File.separator + targetFileName;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String format) {
        switch (format) {
            case "WAV (CD标准)":
            case "WAV":
                return "wav";
            case "FLAC":
                return "flac";
            case "MP3":
                return "mp3";
            case "ALAC":
                return "m4a";
            case "AAC":
                return "aac";
            case "OGG":
                return "ogg";
            default:
                return "wav";
        }
    }

    /**
     * 更改文件扩展名
     */
    private String changeExtension(String fileName, String newExtension) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex + 1) + newExtension;
        }
        return fileName + "." + newExtension;
    }

    /**
     * 格式化文件名
     */
    private String formatFilename(String filename) {
        // 去除首尾空格
        String formatted = filename.trim();
        
        // 转换为简体中文（这里简化处理，实际需要使用转换库）
        // 可以使用opencc4j等库进行繁简转换
        
        return formatted;
    }

    /**
     * 执行音频转换
     */
    private boolean convertAudio(File sourceFile, File targetFile, PluginConfigDTO config, ExecutionContext context) {
        String ffmpegPath = getConfigValue(config, "ffmpegPath", "ffmpeg");
        String targetFormat = getConfigValue(config, "targetFormat", "WAV (CD标准)");
        String sampleRate = getConfigValue(config, "sampleRate", "44100");
        String channels = getConfigValue(config, "channels", "2 (Stereo)");
        int ffmpegThreads = getConfigValue(config, "ffmpegThreads", 4);
        boolean forceFilenameMeta = getConfigValue(config, "forceFilenameMeta", false);
        
        try {
            // 构建FFmpeg命令
            List<String> command = new java.util.ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(sourceFile.getPath());
            
            // 添加采样率参数
            if (!"保持原样 (Original)".equals(sampleRate)) {
                command.add("-ar");
                command.add(sampleRate);
            }
            
            // 添加声道数参数
            if (!"保持原样 (Original)".equals(channels)) {
                command.add("-ac");
                command.add(String.valueOf(getChannelCount(channels)));
            }
            
            // 添加线程数参数
            command.add("-threads");
            command.add(String.valueOf(ffmpegThreads));
            
            // 添加输出格式参数
            command.add("-f");
            command.add(getFFmpegFormat(targetFormat));
            
            // 添加输出文件
            command.add(targetFile.getPath());
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                context.logDebug(line);
            }
            
            // 等待进程完成
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                context.logInfo("FFmpeg conversion completed successfully");
                return true;
            } else {
                context.logError("FFmpeg conversion failed with exit code: " + exitCode);
                return false;
            }
        } catch (Exception e) {
            context.logError("Error executing FFmpeg: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取声道数
     */
    private int getChannelCount(String channels) {
        switch (channels) {
            case "1 (Mono)":
                return 1;
            case "2 (Stereo)":
                return 2;
            case "6 (5.1)":
                return 6;
            default:
                return 2;
        }
    }

    /**
     * 获取FFmpeg格式
     */
    private String getFFmpegFormat(String format) {
        switch (format) {
            case "WAV (CD标准)":
            case "WAV":
                return "wav";
            case "FLAC":
                return "flac";
            case "MP3":
                return "mp3";
            case "ALAC":
                return "ipod";
            case "AAC":
                return "aac";
            case "OGG":
                return "ogg";
            default:
                return "wav";
        }
    }
}
