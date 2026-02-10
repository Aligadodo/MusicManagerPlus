package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AudioConverterStrategy extends AbstractConfigurableStrategy {

    public AudioConverterStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "audio-converter";
    }

    @Override
    public String getName() {
        return "音频格式转换";
    }

    @Override
    public String getDescription() {
        return "高品质音频转换。支持参数微调、乱码修复及智能覆盖检测等。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("targetFormat", "目标格式", "select", (Object) AudioFormat.WAV_CD_STANDARD.getCode(), 
            "转换后的音频文件格式", true, 
            getAudioFormatOptions());
        addEnumConfigField("outputDirMode", "输出目录模式", "select", (Object) OutputDirMode.SUBDIRECTORY.getCode(), 
            "输出目录模式", true, 
            getOutputDirModeOptions());
        addConfigField("outputPath", "输出路径", "directory", (Object) "Convert - WAV", 
            "转换后文件的输出路径", true);
        addEnumConfigField("sampleRate", "采样率", "select", (Object) SampleRate.SR_44100.getCode(), 
            "转换后的音频采样率", false, 
            getSampleRateOptions());
        addEnumConfigField("channels", "声道数", "select", (Object) Channels.STEREO.getCode(), 
            "转换后的音频声道数", false, 
            getChannelsOptions());
        addConfigField("overwrite", "强制覆盖", "boolean", (Object) false, 
            "是否覆盖已存在的目标文件", false);
        addConfigField("ffmpegThreads", "FFmpeg线程数", "number", (Object) 4, 
            "FFmpeg的线程数", false);
        addConfigField("ffmpegPath", "FFmpeg路径", "string", (Object) "ffmpeg", 
            "FFmpeg可执行文件的路径", false);
        addConfigField("enableCache", "启用临时文件缓存", "boolean", (Object) false, 
            "启用临时文件缓存以缓解IO瓶颈", false);
        addConfigField("cacheDir", "缓存目录", "directory", (Object) "", 
            "临时文件缓存目录路径", false);
        addConfigField("enableSnap", "启用镜像路径暂存", "boolean", (Object) false, 
            "启用镜像路径暂存（需要手动移动文件）", false);
        addConfigField("snapDir", "镜像存储目录", "directory", (Object) "", 
            "镜像存储目录路径", false);
        addConfigField("enableTempSuffix", "启用.temp文件后缀", "boolean", (Object) true, 
            "启用.temp文件后缀（文件缓存启用时不生效）", false);
        addConfigField("forceFilenameMeta", "忽略原始文件标签", "boolean", (Object) false, 
            "忽略原始文件标签，强制用文件名重构元数据", false);
        addConfigField("autoFormatFilename", "自动格式化目标文件名", "boolean", (Object) true, 
            "自动将目标文件名转换为简体中文并去除首尾空格", false);
        addConfigField("skipCueTracks", "智能跳过处理", "boolean", (Object) true, 
            "当音频文件大于100MB且同目录下有.cue文件时，跳过处理", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetFormat", (Object) AudioFormat.WAV_CD_STANDARD.getCode());
        setConfigValue(config, "outputDirMode", (Object) OutputDirMode.SUBDIRECTORY.getCode());
        setConfigValue(config, "outputPath", (Object) "Convert - WAV");
        setConfigValue(config, "sampleRate", (Object) SampleRate.SR_44100.getCode());
        setConfigValue(config, "channels", (Object) Channels.STEREO.getCode());
        setConfigValue(config, "overwrite", (Object) false);
        setConfigValue(config, "ffmpegThreads", (Object) 4);
        setConfigValue(config, "ffmpegPath", (Object) "ffmpeg");
        setConfigValue(config, "enableCache", (Object) false);
        setConfigValue(config, "cacheDir", (Object) "");
        setConfigValue(config, "enableSnap", (Object) false);
        setConfigValue(config, "snapDir", (Object) "");
        setConfigValue(config, "enableTempSuffix", (Object) true);
        setConfigValue(config, "forceFilenameMeta", (Object) false);
        setConfigValue(config, "autoFormatFilename", (Object) true);
        setConfigValue(config, "skipCueTracks", (Object) true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetPath = getTargetPath(filePath, config, context);
        ChangeRecord record = createChangeRecord(filePath, targetPath, "PENDING");
        record.setOperationType("CONVERT");
        record.setReason("音频格式转换");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
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
        
        if (!isAudioFile(sourceFile)) {
            context.logDebug("Not an audio file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (skipCueTracks && shouldSkipCueTrack(sourceFile, context)) {
            context.logInfo("Skipping CD mirror file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String targetPath = getTargetPath(filePath, config, context);
            File targetFile = new File(targetPath);
            
            if (autoFormatFilename) {
                String formattedName = formatFilename(targetFile.getName());
                targetFile = new File(targetFile.getParent(), formattedName);
                targetPath = targetFile.getPath();
            }
            
            if (targetFile.exists() && !overwrite) {
                context.logWarn("Target file already exists: " + targetPath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
                context.logDebug("Created directory: " + targetFile.getParentFile().getPath());
            }
            
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
            
            boolean success = convertAudio(sourceFile, actualTargetFile, config, context);
            
            if (success) {
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

    private boolean shouldSkipCueTrack(File file, ExecutionContext context) {
        if (file.length() <= 100 * 1024 * 1024) {
            return false;
        }
        
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

    private String getTargetPath(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String outputDirMode = getConfigValue(config, "outputDirMode", "子目录");
        String outputPath = getConfigValue(config, "outputPath", "Convert - WAV");
        String targetFormat = getConfigValue(config, "targetFormat", "WAV (CD标准)");
        
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        String extension = getFileExtension(targetFormat);
        String targetFileName = changeExtension(fileName, extension);
        
        switch (outputDirMode) {
            case "subdirectory":
                File sourceDir = sourceFile.getParentFile();
                if (sourceDir != null) {
                    return sourceDir.getPath() + File.separator + outputPath + File.separator + targetFileName;
                }
                return outputPath + File.separator + targetFileName;
            case "specified_dir":
                return outputPath + File.separator + targetFileName;
            case "root_dir":
                File rootPath = sourceFile;
                while (rootPath.getParent() != null) {
                    rootPath = rootPath.getParentFile();
                }
                return rootPath.getPath() + File.separator + outputPath + File.separator + targetFileName;
            default:
                return outputPath + File.separator + targetFileName;
        }
    }

    private String getFileExtension(String format) {
        switch (format) {
            case "wav_cd_standard":
            case "wav":
                return "wav";
            case "flac":
                return "flac";
            case "mp3":
                return "mp3";
            case "alac":
                return "m4a";
            case "aac":
                return "aac";
            case "ogg":
                return "ogg";
            default:
                return "wav";
        }
    }

    private String changeExtension(String fileName, String newExtension) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex + 1) + newExtension;
        }
        return fileName + "." + newExtension;
    }

    private String formatFilename(String filename) {
        String formatted = filename.trim();
        return formatted;
    }

    private boolean convertAudio(File sourceFile, File targetFile, StrategyConfigDTO config, ExecutionContext context) {
        String ffmpegPath = getConfigValue(config, "ffmpegPath", "ffmpeg");
        String targetFormat = getConfigValue(config, "targetFormat", "wav_cd_standard");
        String sampleRate = getConfigValue(config, "sampleRate", "sr_44100");
        String channels = getConfigValue(config, "channels", "stereo");
        int ffmpegThreads = getConfigValue(config, "ffmpegThreads", 4);
        
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(sourceFile.getPath());
            
            if (!"original".equals(sampleRate)) {
                command.add("-ar");
                command.add(getSampleRateValue(sampleRate));
            }
            
            if (!"original".equals(channels)) {
                command.add("-ac");
                command.add(String.valueOf(getChannelCount(channels)));
            }
            
            command.add("-threads");
            command.add(String.valueOf(ffmpegThreads));
            
            command.add("-f");
            command.add(getFFmpegFormat(targetFormat));
            
            command.add(targetFile.getPath());
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                context.logDebug(line);
            }
            
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

    private String getSampleRateValue(String sampleRateCode) {
        switch (sampleRateCode) {
            case "sr_44100":
                return "44100";
            case "sr_48000":
                return "48000";
            case "sr_88200":
                return "88200";
            case "sr_96000":
                return "96000";
            case "sr_192000":
                return "192000";
            default:
                return "44100";
        }
    }

    private int getChannelCount(String channels) {
        switch (channels) {
            case "mono":
                return 1;
            case "stereo":
                return 2;
            case "5.1":
                return 6;
            default:
                return 2;
        }
    }

    private String getFFmpegFormat(String format) {
        switch (format) {
            case "wav_cd_standard":
            case "wav":
                return "wav";
            case "flac":
                return "flac";
            case "mp3":
                return "mp3";
            case "alac":
                return "ipod";
            case "aac":
                return "aac";
            case "ogg":
                return "ogg";
            default:
                return "wav";
        }
    }
    
    private java.util.List<EnumOptionDTO> getAudioFormatOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (AudioFormat format : AudioFormat.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(format.getCode());
            option.setLabel(format.getNameZh());
            option.setNameEn(format.getNameEn());
            option.setDescriptionZh(format.getDescriptionZh());
            option.setDescriptionEn(format.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
    
    private java.util.List<EnumOptionDTO> getOutputDirModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (OutputDirMode mode : OutputDirMode.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(mode.getCode());
            option.setLabel(mode.getNameZh());
            option.setNameEn(mode.getNameEn());
            option.setDescriptionZh(mode.getDescriptionZh());
            option.setDescriptionEn(mode.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
    
    private java.util.List<EnumOptionDTO> getSampleRateOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (SampleRate rate : SampleRate.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(rate.getCode());
            option.setLabel(rate.getNameZh());
            option.setNameEn(rate.getNameEn());
            option.setDescriptionZh(rate.getDescriptionZh());
            option.setDescriptionEn(rate.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
    
    private java.util.List<EnumOptionDTO> getChannelsOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (Channels channels : Channels.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(channels.getCode());
            option.setLabel(channels.getNameZh());
            option.setNameEn(channels.getNameEn());
            option.setDescriptionZh(channels.getDescriptionZh());
            option.setDescriptionEn(channels.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}