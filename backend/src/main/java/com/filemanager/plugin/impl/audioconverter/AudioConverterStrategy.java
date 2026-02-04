package com.filemanager.plugin.impl.audioconverter;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;

import java.util.Arrays;
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
    protected void initConfigFields() {
        addConfigField("targetFormat", "目标格式", "select", (Object) AudioFormat.WAV_CD_STANDARD.getCode(), 
            "转换后的音频文件格式", true, 
            getAudioFormatOptions());
        addConfigField("outputDirMode", "输出目录模式", "select", (Object) OutputDirMode.SUBDIRECTORY.getCode(), 
            "输出目录模式", true, 
            getOutputDirModeOptions());
        addConfigField("outputPath", "输出路径", "directory", (Object) "Convert - WAV", 
            "转换后文件的输出路径", true);
        addConfigField("sampleRate", "采样率", "select", (Object) SampleRate.SR_44100.getCode(), 
            "转换后的音频采样率", false, 
            getSampleRateOptions());
        addConfigField("channels", "声道数", "select", (Object) Channels.STEREO.getCode(), 
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