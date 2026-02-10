package com.filemanager.plugin.utils;

import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.plugin.enums.PluginEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举选项提供者
 * 负责根据策略ID和字段名返回对应的枚举选项
 * 使用反射动态加载枚举类，避免硬编码
 */
public class EnumOptionProvider {

    private static final Map<String, String> STRATEGY_FIELD_ENUM_MAP = new HashMap<>();

    static {
        // 初始化策略字段到枚举类的映射
        // 格式: "strategyId:fieldName" -> "enumClassName"
        
        // AudioConverterStrategy 和 CueSplitterStrategy
        STRATEGY_FIELD_ENUM_MAP.put("audio-converter:targetFormat", "com.filemanager.plugin.impl.audioconverter.enums.AudioFormat");
        STRATEGY_FIELD_ENUM_MAP.put("audio-converter:outputDirMode", "com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode");
        STRATEGY_FIELD_ENUM_MAP.put("audio-converter:sampleRate", "com.filemanager.plugin.impl.audioconverter.enums.SampleRate");
        STRATEGY_FIELD_ENUM_MAP.put("audio-converter:channels", "com.filemanager.plugin.impl.audioconverter.enums.Channels");
        
        STRATEGY_FIELD_ENUM_MAP.put("cue-splitter:targetFormat", "com.filemanager.plugin.impl.audioconverter.enums.AudioFormat");
        STRATEGY_FIELD_ENUM_MAP.put("cue-splitter:outputDirMode", "com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode");
        STRATEGY_FIELD_ENUM_MAP.put("cue-splitter:sampleRate", "com.filemanager.plugin.impl.audioconverter.enums.SampleRate");
        STRATEGY_FIELD_ENUM_MAP.put("cue-splitter:channels", "com.filemanager.plugin.impl.audioconverter.enums.Channels");
        STRATEGY_FIELD_ENUM_MAP.put("cue-splitter:afterSplitAction", "com.filemanager.plugin.impl.cuesplitter.enums.AfterSplitAction");
        
        // FileTypeFixStrategy
        STRATEGY_FIELD_ENUM_MAP.put("file-type-fix:targetFormat", "com.filemanager.plugin.impl.filetypefix.enums.TargetFormat");
        
        // AdvancedRenameStrategy
        STRATEGY_FIELD_ENUM_MAP.put("advanced-rename:crossDriveMode", "com.filemanager.plugin.impl.advancedrename.enums.CrossDriveMode");
        STRATEGY_FIELD_ENUM_MAP.put("advanced-rename:processScope", "com.filemanager.plugin.impl.advancedrename.enums.ProcessScope");
        
        // AlbumDirNormalizeStrategy
        STRATEGY_FIELD_ENUM_MAP.put("album-dir-normalize:template", "com.filemanager.plugin.impl.albumdirnormalize.enums.DirectoryTemplate");
        
        // FileCleanupStrategy
        STRATEGY_FIELD_ENUM_MAP.put("file-cleanup:mode", "com.filemanager.plugin.impl.filecleanup.enums.CleanupMode");
        STRATEGY_FIELD_ENUM_MAP.put("file-cleanup:method", "com.filemanager.plugin.impl.filecleanup.enums.DeleteMethod");
        STRATEGY_FIELD_ENUM_MAP.put("file-cleanup:sizeRange", "com.filemanager.plugin.impl.filecleanup.enums.FileSizeRange");
        
        // FileMigrateStrategy
        STRATEGY_FIELD_ENUM_MAP.put("file-migrate:operationMode", "com.filemanager.plugin.impl.filemigrate.enums.OperationMode");
        STRATEGY_FIELD_ENUM_MAP.put("file-migrate:scope", "com.filemanager.plugin.impl.filemigrate.enums.ScopeMode");
        STRATEGY_FIELD_ENUM_MAP.put("file-migrate:outputDirMode", "com.filemanager.plugin.impl.audioconverter.enums.OutputDirMode");
        
        // FileUnzipStrategy
        STRATEGY_FIELD_ENUM_MAP.put("file-unzip:engine", "com.filemanager.plugin.impl.fileunzip.enums.UnzipEngine");
        STRATEGY_FIELD_ENUM_MAP.put("file-unzip:outputMode", "com.filemanager.plugin.impl.fileunzip.enums.OutputMode");
        
        // MetadataScraperStrategy
        STRATEGY_FIELD_ENUM_MAP.put("metadata-scraper:source", "com.filemanager.plugin.impl.metadatascraper.enums.DataSource");
        
        // CueFileRenameStrategy
        STRATEGY_FIELD_ENUM_MAP.put("cue-file-rename:renameMode", "com.filemanager.plugin.impl.enums.RenameMode");
        
        // NcmIntegratedStrategy
        STRATEGY_FIELD_ENUM_MAP.put("ncm-integrated:operationMode", "com.filemanager.plugin.impl.enums.NcmOperationMode");
        STRATEGY_FIELD_ENUM_MAP.put("ncm-integrated:outputFormat", "com.filemanager.plugin.impl.enums.NcmOutputFormat");
    }

    /**
     * 根据策略ID和字段名返回对应的枚举选项
     * 处理不同策略使用相同字段名但不同枚举类型的情况
     *
     * @param strategyId 策略ID
     * @param fieldName 字段名
     * @return 枚举选项列表
     */
    public static List<EnumOptionDTO> getEnumOptionsForField(String strategyId, String fieldName) {
        if (strategyId == null || fieldName == null) {
            return null;
        }
        
        String key = strategyId + ":" + fieldName;
        String enumClassName = STRATEGY_FIELD_ENUM_MAP.get(key);
        
        if (enumClassName == null) {
            return null;
        }
        
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            if (PluginEnum.class.isAssignableFrom(enumClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends PluginEnum> pluginEnumClass = (Class<? extends PluginEnum>) enumClass;
                return PluginEnum.getEnumOptions(pluginEnumClass);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Enum class not found: " + enumClassName);
        } catch (Exception e) {
            System.err.println("Error loading enum options for " + key + ": " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 注册新的策略字段枚举映射
     * 用于动态添加新的枚举映射
     *
     * @param strategyId 策略ID
     * @param fieldName 字段名
     * @param enumClassName 枚举类全限定名
     */
    public static void registerEnumMapping(String strategyId, String fieldName, String enumClassName) {
        if (strategyId != null && fieldName != null && enumClassName != null) {
            STRATEGY_FIELD_ENUM_MAP.put(strategyId + ":" + fieldName, enumClassName);
        }
    }
}
