package com.filemanager.plugin.impl.audioconverter;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.audioconverter.converter.FFmpegConverter;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;
import com.filemanager.plugin.impl.audioconverter.utils.AudioFileUtils;
import com.filemanager.plugin.impl.audioconverter.utils.AudioPathUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class AudioConverterPlugin extends AbstractPlugin {

    public AudioConverterPlugin() {
        super("audio-converter", "音频格式转换", "高品质音频转换。支持参数微调、乱码修复及智能覆盖检测等。", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("targetFormat", "目标格式", "select", AudioFormat.WAV_CD_STANDARD.getCode(), 
            "转换后的音频文件格式", true, 
            getAudioFormatOptions());
        addParameter("outputDirMode", "输出目录模式", "select", OutputDirMode.SUBDIRECTORY.getCode(), 
            "输出目录模式", true, 
            getOutputDirModeOptions());
        addParameter("outputPath", "输出路径", "directory", "Convert - WAV", 
            "转换后文件的输出路径", true);
        addParameter("sampleRate", "采样率", "select", SampleRate.SR_44100.getCode(), 
            "转换后的音频采样率", false, 
            getSampleRateOptions());
        addParameter("channels", "声道数", "select", Channels.STEREO.getCode(), 
            "转换后的音频声道数", false, 
            getChannelsOptions());
        addParameter("overwrite", "强制覆盖", "boolean", false, 
            "是否覆盖已存在的目标文件", false);
        addParameter("ffmpegThreads", "FFmpeg线程数", "number", 4, 
            "FFmpeg的线程数", false);
        addParameter("ffmpegPath", "FFmpeg路径", "string", "ffmpeg", 
            "FFmpeg可执行文件的路径", false);
        addParameter("enableCache", "启用临时文件缓存", "boolean", false, 
            "启用临时文件缓存以缓解IO瓶颈", false);
        addParameter("cacheDir", "缓存目录", "directory", "", 
            "临时文件缓存目录路径", false);
        addParameter("enableSnap", "启用镜像路径暂存", "boolean", false, 
            "启用镜像路径暂存（需要手动移动文件）", false);
        addParameter("snapDir", "镜像存储目录", "directory", "", 
            "镜像存储目录路径", false);
        addParameter("enableTempSuffix", "启用.temp文件后缀", "boolean", true, 
            "启用.temp文件后缀（文件缓存启用时不生效）", false);
        addParameter("forceFilenameMeta", "忽略原始文件标签", "boolean", false, 
            "忽略原始文件标签，强制用文件名重构元数据", false);
        addParameter("autoFormatFilename", "自动格式化目标文件名", "boolean", true, 
            "自动将目标文件名转换为简体中文并去除首尾空格", false);
        addParameter("skipCueTracks", "智能跳过处理", "boolean", true, 
            "当音频文件大于100MB且同目录下有.cue文件时，跳过处理", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("targetFormat", AudioFormat.WAV_CD_STANDARD.getCode());
        setDefaultConfigValue("outputDirMode", OutputDirMode.SUBDIRECTORY.getCode());
        setDefaultConfigValue("outputPath", "Convert - WAV");
        setDefaultConfigValue("sampleRate", SampleRate.SR_44100.getCode());
        setDefaultConfigValue("channels", Channels.STEREO.getCode());
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
        String targetPath = AudioPathUtils.getTargetPath(filePath, config);
        ChangeRecord record = createChangeRecord(filePath, targetPath, "PENDING");
        record.setOperationType("CONVERT");
        record.setReason("音频格式转换");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        boolean skipCueTracks = getBooleanConfigValue(config, "skipCueTracks", true);
        boolean autoFormatFilename = getBooleanConfigValue(config, "autoFormatFilename", true);
        boolean overwrite = getBooleanConfigValue(config, "overwrite", false);
        boolean enableCache = getBooleanConfigValue(config, "enableCache", false);
        boolean enableSnap = getBooleanConfigValue(config, "enableSnap", false);
        boolean enableTempSuffix = getBooleanConfigValue(config, "enableTempSuffix", true);
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (!AudioFileUtils.isAudioFile(sourceFile)) {
            context.logDebug("Not an audio file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (skipCueTracks && AudioFileUtils.shouldSkipCueTrack(sourceFile, context)) {
            context.logInfo("Skipping CD mirror file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String targetPath = AudioPathUtils.getTargetPath(filePath, config);
            File targetFile = new File(targetPath);
            
            if (autoFormatFilename) {
                String formattedName = AudioFileUtils.formatFilename(targetFile.getName());
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
            
            String actualOutputPath = determineActualOutputPath(targetPath, config);
            File actualTargetFile = new File(actualOutputPath);
            
            boolean success = FFmpegConverter.convertAudio(sourceFile, actualTargetFile, config, context);
            
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
    
    private boolean getBooleanConfigValue(PluginConfigDTO config, String key, boolean defaultValue) {
        Object value = config.getValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    private String determineActualOutputPath(String targetPath, PluginConfigDTO config) {
        boolean enableCache = getBooleanConfigValue(config, "enableCache", false);
        boolean enableSnap = getBooleanConfigValue(config, "enableSnap", false);
        boolean enableTempSuffix = getBooleanConfigValue(config, "enableTempSuffix", true);
        
        File targetFile = new File(targetPath);
        
        if (enableCache) {
            Object cacheDirObj = config.getValue("cacheDir");
            String cacheDir = cacheDirObj != null ? cacheDirObj.toString() : "";
            return cacheDir + File.separator + targetFile.getName();
        } else if (enableSnap) {
            Object snapDirObj = config.getValue("snapDir");
            String snapDir = snapDirObj != null ? snapDirObj.toString() : "";
            return snapDir + File.separator + targetFile.getName();
        } else if (enableTempSuffix) {
            return targetPath + ".temp";
        }
        
        return targetPath;
    }
    
    private List<String> getAudioFormatOptions() {
        return Arrays.asList(
            AudioFormat.WAV_CD_STANDARD.getCode(),
            AudioFormat.FLAC.getCode(),
            AudioFormat.WAV.getCode(),
            AudioFormat.MP3.getCode(),
            AudioFormat.ALAC.getCode(),
            AudioFormat.AAC.getCode(),
            AudioFormat.OGG.getCode()
        );
    }
    
    private List<String> getOutputDirModeOptions() {
        return Arrays.asList(
            OutputDirMode.SUBDIRECTORY.getCode(),
            OutputDirMode.SPECIFIED_DIR.getCode(),
            OutputDirMode.ROOT_DIR.getCode()
        );
    }
    
    private List<String> getSampleRateOptions() {
        return Arrays.asList(
            SampleRate.ORIGINAL.getCode(),
            SampleRate.SR_44100.getCode(),
            SampleRate.SR_48000.getCode(),
            SampleRate.SR_88200.getCode(),
            SampleRate.SR_96000.getCode(),
            SampleRate.SR_192000.getCode()
        );
    }
    
    private List<String> getChannelsOptions() {
        return Arrays.asList(
            Channels.ORIGINAL.getCode(),
            Channels.MONO.getCode(),
            Channels.STEREO.getCode(),
            Channels.SURROUND_5_1.getCode()
        );
    }
}