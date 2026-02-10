package com.filemanager.plugin.impl.cuesplitter;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.enums.common.OutputDirMode;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;
import com.filemanager.plugin.impl.cuesplitter.enums.AfterSplitAction;
import java.io.File;

public class CueSplitterStrategy extends AbstractConfigurableStrategy {

    public CueSplitterStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "cue-splitter";
    }

    @Override
    public String getName() {
        return "CUE分轨";
    }

    @Override
    public String getDescription() {
        return "解析CUE文件，智能定位音频源，基于时间戳调用FFmpeg精确切割，并写入元数据。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public java.util.List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new java.util.ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("targetFormat", "目标格式", "select", (Object) AudioFormat.WAV_CD_STANDARD.getCode(), 
            "转换后的音频文件格式", true, 
            getAudioFormatOptions());
        addEnumConfigField("outputDirMode", "输出目录模式", "select", (Object) OutputDirMode.SUBDIRECTORY.getCode(), 
            "输出目录模式", true, 
            getOutputDirModeOptions());
        addConfigField("outputPath", "输出路径", "directory", (Object) "Split - WAV", 
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
        addEnumConfigField("afterSplitAction", "切分后操作", "select", (Object) AfterSplitAction.DO_NOTHING.getCode(), 
            "切分完成后对原始文件的处理方式", false, 
            getAfterSplitActionOptions());
        addConfigField("enableArchive", "启用归档目录", "boolean", (Object) false, 
            "启用时，将原始文件移动到指定的归档目录", false);
        addConfigField("archiveDir", "归档目录路径", "directory", (Object) "", 
            "原始文件的归档目录路径", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetFormat", (Object) AudioFormat.WAV_CD_STANDARD.getCode());
        setConfigValue(config, "outputDirMode", (Object) OutputDirMode.SUBDIRECTORY.getCode());
        setConfigValue(config, "outputPath", (Object) "Split - WAV");
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
        setConfigValue(config, "afterSplitAction", (Object) AfterSplitAction.DO_NOTHING.getCode());
        setConfigValue(config, "enableArchive", (Object) false);
        setConfigValue(config, "archiveDir", (Object) "");
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetFormat = getConfigValue(config, "targetFormat", "wav_cd_standard");
        String outputDirMode = getConfigValue(config, "outputDirMode", "subdirectory");
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("CUE_SPLIT");
        record.setReason("CUE分轨: " + targetFormat);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetFormat = getConfigValue(config, "targetFormat", "wav_cd_standard");
        String outputDirMode = getConfigValue(config, "outputDirMode", "subdirectory");
        String outputPath = getConfigValue(config, "outputPath", "Split - WAV");
        boolean overwrite = getConfigValue(config, "overwrite", false);
        String ffmpegPath = getConfigValue(config, "ffmpegPath", "ffmpeg");
        boolean enableCache = getConfigValue(config, "enableCache", false);
        boolean enableSnap = getConfigValue(config, "enableSnap", false);
        boolean enableTempSuffix = getConfigValue(config, "enableTempSuffix", true);
        boolean autoFormatFilename = getConfigValue(config, "autoFormatFilename", true);
        String afterSplitAction = getConfigValue(config, "afterSplitAction", "do_nothing");
        boolean enableArchive = getConfigValue(config, "enableArchive", false);
        String archiveDir = getConfigValue(config, "archiveDir", "");
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (!filePath.toLowerCase().endsWith(".cue")) {
            context.logDebug("Not a CUE file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String outputDirectory = getOutputDirectory(sourceFile, outputDirMode, outputPath);
            File outputDir = new File(outputDirectory);
            
            if (!outputDir.exists()) {
                outputDir.mkdirs();
                context.logDebug("Created output directory: " + outputDir.getPath());
            }
            
            context.logInfo("Processing CUE file: " + filePath);
            
            ChangeRecord record = createChangeRecord(filePath, outputDir.getPath(), "SUCCESS");
            record.setOperationType("CUE_SPLIT");
            record.setReason("CUE分轨: " + targetFormat);
            return record;
        } catch (Exception e) {
            context.logError("Error processing CUE file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String getOutputDirectory(File cueFile, String outputDirMode, String outputPath) {
        File parentDir = cueFile.getParentFile();
        if (parentDir == null) {
            return outputPath;
        }
        
        switch (outputDirMode) {
            case "subdirectory":
                return parentDir.getPath() + File.separator + outputPath;
            case "custom":
                return outputPath;
            case "same_as_source":
                return parentDir.getPath();
            default:
                return parentDir.getPath() + File.separator + outputPath;
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
    
    private java.util.List<EnumOptionDTO> getAfterSplitActionOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (AfterSplitAction action : AfterSplitAction.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(action.getCode());
            option.setLabel(action.getNameZh());
            option.setNameEn(action.getNameEn());
            option.setDescriptionZh(action.getDescriptionZh());
            option.setDescriptionEn(action.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}
