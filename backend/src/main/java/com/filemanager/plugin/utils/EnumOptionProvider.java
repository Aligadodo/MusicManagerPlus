package com.filemanager.plugin.utils;

import com.filemanager.domain.dto.EnumOptionDTO;

import java.util.List;

/**
 * 枚举选项提供者
 * 负责根据策略ID和字段名返回对应的枚举选项
 */
public class EnumOptionProvider {

    /**
     * 根据策略ID和字段名返回对应的枚举选项
     * 处理不同策略使用相同字段名但不同枚举类型的情况
     *
     * @param strategyId 策略ID
     * @param fieldName 字段名
     * @return 枚举选项列表
     */
    public static List<EnumOptionDTO> getEnumOptionsForField(String strategyId, String fieldName) {
        // AudioConverterStrategy 和 CueSplitterStrategy 的 targetFormat
        if (isAudioOrCueStrategy(strategyId) && "targetFormat".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.audioconverter.enums.AudioFormat.class);
        }
        
        // FileTypeFixStrategy 的 targetFormat
        if ("file-type-fix".equals(strategyId) && "targetFormat".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.filetypefix.enums.TargetFormat.class);
        }
        
        // AudioConverterStrategy 和 CueSplitterStrategy 的 outputDirMode
        if (isAudioOrCueStrategy(strategyId) && "outputDirMode".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode.class);
        }
        
        // FileMigrateStrategy 的 outputDirMode
        if ("file-migrate".equals(strategyId) && "outputDirMode".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode.class);
        }
        
        // AudioConverterStrategy 和 CueSplitterStrategy 的 sampleRate
        if (isAudioOrCueStrategy(strategyId) && "sampleRate".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.audioconverter.enums.SampleRate.class);
        }
        
        // AudioConverterStrategy 和 CueSplitterStrategy 的 channels
        if (isAudioOrCueStrategy(strategyId) && "channels".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.audioconverter.enums.Channels.class);
        }
        
        // AdvancedRenameStrategy 的 crossDriveMode
        if ("advanced-rename".equals(strategyId) && "crossDriveMode".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.advancedrename.enums.CrossDriveMode.class);
        }
        
        // AdvancedRenameStrategy 的 processScope
        if ("advanced-rename".equals(strategyId) && "processScope".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.advancedrename.enums.ProcessScope.class);
        }
        
        // AlbumDirNormalizeStrategy 的 template
        if ("album-dir-normalize".equals(strategyId) && "template".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.albumdirnormalize.enums.DirectoryTemplate.class);
        }
        
        // FileCleanupStrategy 的 mode
        if ("file-cleanup".equals(strategyId) && "mode".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.filecleanup.enums.CleanupMode.class);
        }
        
        // FileCleanupStrategy 的 method
        if ("file-cleanup".equals(strategyId) && "method".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.filecleanup.enums.DeleteMethod.class);
        }
        
        // FileCleanupStrategy 的 sizeRange
        if ("file-cleanup".equals(strategyId) && "sizeRange".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.filecleanup.enums.FileSizeRange.class);
        }
        
        // FileMigrateStrategy 的 operationMode
        if ("file-migrate".equals(strategyId) && "operationMode".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.filemigrate.enums.OperationMode.class);
        }
        
        // FileMigrateStrategy 的 scope
        if ("file-migrate".equals(strategyId) && "scope".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.filemigrate.enums.ScopeMode.class);
        }
        
        // FileUnzipStrategy 的 engine
        if ("file-unzip".equals(strategyId) && "engine".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.fileunzip.enums.UnzipEngine.class);
        }
        
        // FileUnzipStrategy 的 outputMode
        if ("file-unzip".equals(strategyId) && "outputMode".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.fileunzip.enums.OutputMode.class);
        }
        
        // MetadataScraperStrategy 的 source
        if ("metadata-scraper".equals(strategyId) && "source".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.metadatascraper.enums.DataSource.class);
        }
        
        // CueSplitterStrategy 的 afterSplitAction
        if ("cue-splitter".equals(strategyId) && "afterSplitAction".equals(fieldName)) {
            return EnumConverter.convertEnumToDTOs(com.filemanager.plugin.impl.cuesplitter.enums.AfterSplitAction.class);
        }
        
        return null;
    }

    /**
     * 检查是否是音频转换或CUE分轨策略
     *
     * @param strategyId 策略ID
     * @return 是否是音频转换或CUE分轨策略
     */
    private static boolean isAudioOrCueStrategy(String strategyId) {
        return "audio-converter".equals(strategyId) || "cue-splitter".equals(strategyId);
    }
}
