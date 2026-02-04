package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;

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
    protected void initConfigFields() {
        addConfigField("targetFormat", "目标格式", "select", (Object) "WAV (CD标准)", 
            "转换后的音频文件格式", true, 
            Arrays.asList("WAV (CD标准)", "FLAC", "WAV", "MP3", "ALAC", "AAC", "OGG"));
        addConfigField("outputDirMode", "输出目录模式", "select", (Object) "子目录", 
            "输出目录模式", true, 
            Arrays.asList("子目录", "指定目录", "根目录"));
        addConfigField("outputPath", "输出路径", "directory", (Object) "Split - WAV", 
            "转换后文件的输出路径", true);
        addConfigField("sampleRate", "采样率", "select", (Object) "44100", 
            "转换后的音频采样率", false, 
            Arrays.asList("保持原样 (Original)", "44100", "48000", "88200", "96000", "192000"));
        addConfigField("channels", "声道数", "select", (Object) "2 (Stereo)", 
            "转换后的音频声道数", false, 
            Arrays.asList("保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)"));
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
        addConfigField("afterSplitAction", "切分后操作", "select", (Object) "什么都不做 (默认)", 
            "切分完成后对原始文件的处理方式", false, 
            Arrays.asList("什么都不做 (默认)", "删除原始文件", "归档原始文件"));
        addConfigField("enableArchive", "启用归档目录", "boolean", (Object) false, 
            "启用时，将原始文件移动到指定的归档目录", false);
        addConfigField("archiveDir", "归档目录路径", "directory", (Object) "", 
            "原始文件的归档目录路径", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetFormat", (Object) "WAV (CD标准)");
        setConfigValue(config, "outputDirMode", (Object) "子目录");
        setConfigValue(config, "outputPath", (Object) "Split - WAV");
        setConfigValue(config, "sampleRate", (Object) "44100");
        setConfigValue(config, "channels", (Object) "2 (Stereo)");
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
        setConfigValue(config, "afterSplitAction", (Object) "什么都不做 (默认)");
        setConfigValue(config, "enableArchive", (Object) false);
        setConfigValue(config, "archiveDir", (Object) "");
    }
}