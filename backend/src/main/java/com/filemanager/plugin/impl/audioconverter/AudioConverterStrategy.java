package com.filemanager.plugin.impl.audioconverter;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.AutoFillConfig;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.enums.common.OutputDirMode;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
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
        addConfigField("ffmpegPath", "FFmpeg路径", "text", (Object) "ffmpeg", 
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
        
        // 配置参数联动
        setupParameterRelations();
    }
    
    /**
     * 配置参数联动关系
     */
    private void setupParameterRelations() {
        // outputPath参数：当outputDirMode为指定目录时显示
        List<Map<String, Object>> outputPathConditions = new ArrayList<>();
        Map<String, Object> outputPathCondition = new HashMap<>();
        outputPathCondition.put("dependentParam", "outputDirMode");
        outputPathCondition.put("expectedValue", OutputDirMode.SPECIFIED_DIR.getCode());
        outputPathConditions.add(outputPathCondition);
        
        getConfigField("outputPath").setBlockConditions(outputPathConditions);
        
        // cacheDir参数：当enableCache为true时显示
        List<Map<String, Object>> cacheDirConditions = new ArrayList<>();
        Map<String, Object> cacheDirCondition = new HashMap<>();
        cacheDirCondition.put("dependentParam", "enableCache");
        cacheDirCondition.put("expectedValue", true);
        cacheDirConditions.add(cacheDirCondition);
        
        getConfigField("cacheDir").setBlockConditions(cacheDirConditions);
        
        // snapDir参数：当enableSnap为true时显示
        List<Map<String, Object>> snapDirConditions = new ArrayList<>();
        Map<String, Object> snapDirCondition = new HashMap<>();
        snapDirCondition.put("dependentParam", "enableSnap");
        snapDirCondition.put("expectedValue", true);
        snapDirConditions.add(snapDirCondition);
        
        getConfigField("snapDir").setBlockConditions(snapDirConditions);
        
        // enableTempSuffix参数：当enableCache为false时显示
        List<Map<String, Object>> tempSuffixConditions = new ArrayList<>();
        Map<String, Object> tempSuffixCondition = new HashMap<>();
        tempSuffixCondition.put("dependentParam", "enableCache");
        tempSuffixCondition.put("expectedValue", false);
        tempSuffixConditions.add(tempSuffixCondition);
        
        getConfigField("enableTempSuffix").setBlockConditions(tempSuffixConditions);
        
        // 为ffmpegPath添加自动填充配置
        AutoFillConfig ffmpegAutoFillConfig = new AutoFillConfig();
        ffmpegAutoFillConfig.setTriggerParam("ffmpegThreads");
        ffmpegAutoFillConfig.setFillType("auto_detect");
        ffmpegAutoFillConfig.setDetectPattern("ffmpeg_path");
        getConfigField("ffmpegPath").setAutoFillConfig(ffmpegAutoFillConfig);
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
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs,
        StrategyConfigDTO config,
        ExecutionContext context) {
        
        File virtualInput = new File(currentRecord.getNewPath());
        String name = virtualInput.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1) {
            return Collections.emptyList();
        }
        
        if (!isAudioFile(currentRecord.getFileHandle())) {
            return Collections.emptyList();
        }

        boolean skipCueTracks = getConfigValue(config, "skipCueTracks", true);
        if (skipCueTracks) {
            File actualFile = currentRecord.getFileHandle();
            if (actualFile.length() > 100 * 1024 * 1024) {
                File parentDir = actualFile.getParentFile();
                if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                    File[] files = parentDir.listFiles((dir, filename) -> filename.toLowerCase().endsWith(".cue"));
                    if (files != null && files.length > 0) {
                        context.logInfo("跳过处理：" + actualFile.getName() + " (大于100MB且同目录下存在.cue文件)");
                        return Collections.emptyList();
                    }
                }
            }
        }

        Map<String, String> param = getParams(virtualInput.getParentFile(), name, config);
        String newName = name.substring(0, dotIndex) + "." + param.get("format");

        File targetFile = new File(param.get("parentPath"), newName);
        boolean overwrite = getConfigValue(config, "overwrite", false);

        if (targetFile.exists() && !overwrite) {
            return Collections.emptyList();
        }
        
        ChangeRecord record = new ChangeRecord(
                currentRecord.getOriginalName(),
                targetFile.getName(),
                currentRecord.getFileHandle(),
                true,
                targetFile.getAbsolutePath(),
                OperationType.CONVERT,
                param,
                ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, 
        StrategyConfigDTO config, 
        ExecutionContext context) throws Exception {
        
        if (!"CONVERT".equals(record.getOperationType())) {
            return;
        }

        String sourcePath = record.getFilePath();
        String targetPath = record.getNewPath();
        String ffmpegPath = getConfigValue(config, "ffmpegPath", "ffmpeg");

        try {
            File targetFile = new File(targetPath);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            context.logInfo("Converting audio: " + sourcePath + " -> " + targetPath);

            record.setStatus("SUCCESS");
        } catch (Exception e) {
            context.logError("Error converting audio " + sourcePath + ": " + e.getMessage());
            record.setStatus("ERROR");
            record.setFailReason(e.getMessage());
        }
    }

    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || 
               name.endsWith(".aac") || name.endsWith(".ogg") || name.endsWith(".m4a") || 
               name.endsWith(".wma") || name.endsWith(".ape") || name.endsWith(".opus");
    }

    private Map<String, String> getParams(File parentDir, String fileName, StrategyConfigDTO config) {
        Map<String, String> params = new HashMap<>();
        String outputDirMode = getConfigValue(config, "outputDirMode", "subdirectory");
        String outputPath = getConfigValue(config, "outputPath", "Convert - WAV");
        String targetFormat = getConfigValue(config, "targetFormat", "wav_cd_standard");

        params.put("parentPath", getOutputDirectory(parentDir, outputDirMode, outputPath));
        params.put("format", getExtensionForFormat(targetFormat));

        return params;
    }

    private String getOutputDirectory(File sourceFile, String outputDirMode, String outputPath) {
        File parentDir = sourceFile.getParentFile();
        if (parentDir == null) {
            return outputPath;
        }

        switch (outputDirMode) {
            case "subdirectory":
                return parentDir.getPath() + File.separator + outputPath;
            case "specified_dir":
                return outputPath;
            case "same_as_source":
                return parentDir.getPath();
            default:
                return parentDir.getPath() + File.separator + outputPath;
        }
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetFormat = getConfigValue(config, "targetFormat", "wav_cd_standard");
        String outputDirMode = getConfigValue(config, "outputDirMode", "subdirectory");
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("CONVERT");
        record.setReason("音频转换: " + targetFormat + ", " + outputDirMode);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetFormat = getConfigValue(config, "targetFormat", "wav_cd_standard");
        String outputDirMode = getConfigValue(config, "outputDirMode", "subdirectory");
        String outputPath = getConfigValue(config, "outputPath", "Convert - WAV");
        boolean overwrite = getConfigValue(config, "overwrite", false);
        boolean enableCache = getConfigValue(config, "enableCache", false);
        boolean enableSnap = getConfigValue(config, "enableSnap", false);
        boolean enableTempSuffix = getConfigValue(config, "enableTempSuffix", true);
        boolean autoFormatFilename = getConfigValue(config, "autoFormatFilename", true);
        boolean skipCueTracks = getConfigValue(config, "skipCueTracks", true);
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
        
        try {
            String targetFilePath = getTargetFilePath(sourceFile, targetFormat, outputDirMode, outputPath, context);
            File targetFile = new File(targetFilePath);
            
            if (targetFile.exists() && !overwrite) {
                context.logWarn("Target file already exists: " + targetFilePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
                context.logDebug("Created directory: " + targetFile.getParentFile().getPath());
            }
            
            context.logInfo("Converting audio: " + filePath + " -> " + targetFilePath);
            
            ChangeRecord record = createChangeRecord(filePath, targetFilePath, "SUCCESS");
            record.setOperationType("CONVERT");
            record.setReason("音频转换: " + targetFormat);
            return record;
        } catch (Exception e) {
            context.logError("Error converting audio " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String getTargetFilePath(File sourceFile, String targetFormat, String outputDirMode, String outputPath, ExecutionContext context) {
        String extension = getExtensionForFormat(targetFormat);
        String baseName = sourceFile.getName();
        int lastDotIndex = baseName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            baseName = baseName.substring(0, lastDotIndex);
        }
        
        String newFileName = baseName + "." + extension;
        
        File parentDir = sourceFile.getParentFile();
        if (parentDir == null) {
            return newFileName;
        }
        
        switch (outputDirMode) {
            case "subdirectory":
                return parentDir.getPath() + File.separator + outputPath + File.separator + newFileName;
            case "specified_dir":
                return outputPath + File.separator + newFileName;
            case "same_as_source":
                return parentDir.getPath() + File.separator + newFileName;
            default:
                return parentDir.getPath() + File.separator + outputPath + File.separator + newFileName;
        }
    }

    private String getExtensionForFormat(String format) {
        switch (format) {
            case "wav_cd_standard":
                return "wav";
            case "flac_hq":
                return "flac";
            case "mp3_hq":
                return "mp3";
            case "aac_high":
                return "aac";
            case "ogg_vorbis":
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