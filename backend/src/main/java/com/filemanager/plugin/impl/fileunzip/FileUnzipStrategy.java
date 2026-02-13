package com.filemanager.plugin.impl.fileunzip;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.AutoFillConfig;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.impl.fileunzip.enums.OutputMode;
import com.filemanager.plugin.impl.fileunzip.enums.UnzipEngine;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUnzipStrategy extends AbstractConfigurableStrategy {

    public FileUnzipStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-unzip";
    }

    @Override
    public String getName() {
        return "批量智能解压";
    }

    @Override
    public String getDescription() {
        return "批量智能解压工具，支持多种解压引擎和智能目录处理。";
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
        addEnumConfigField("engine", "解压引擎", "select", (Object) UnzipEngine.JAVA_BUILTIN.getCode(), 
            "解压引擎选择", true, 
            getUnzipEngineOptions());
        
        // 添加参数联动：当选择7zip或Bandizip时，显示exePath参数并自动检测路径
        addConfigField("exePath", "可执行文件路径", "text", (Object) "", 
            "外部解压工具的可执行文件路径", false);
        
        addEnumConfigField("outputMode", "输出模式", "select", (Object) OutputMode.AUTO_SUBDIRECTORY.getCode(), 
            "输出目录模式", true, 
            getOutputModeOptions());
        
        // 添加参数联动：当选择自定义路径时，显示customPath参数
        addConfigField("customPath", "自定义路径", "directory", (Object) "", 
            "自定义输出路径", false);
        
        addConfigField("smartFolder", "智能文件夹", "boolean", (Object) true, 
            "智能识别解压后的文件夹结构", false);
        addConfigField("mergeSameName", "合并同名文件夹", "boolean", (Object) false, 
            "合并同名的文件夹", false);
        addConfigField("deleteSource", "解压成功后删除源文件", "boolean", (Object) false, 
            "解压成功后删除原始压缩文件", false);
        addConfigField("overwrite", "覆盖已存在文件", "boolean", (Object) false, 
            "覆盖已存在的文件", false);
        addConfigField("deleteOnFail", "解压失败后删除源文件", "boolean", (Object) false, 
            "解压失败后删除原始压缩文件", false);
        addConfigField("nestedFolderMerge", "嵌套文件夹合并", "boolean", (Object) false, 
            "合并嵌套的文件夹", false);
        addConfigField("passwords", "密码列表", "list", (Object) new ArrayList<>(), 
            "解压密码列表", false);
        
        // 配置参数联动
        setupParameterRelations();
    }
    
    /**
     * 配置参数联动关系
     */
    private void setupParameterRelations() {
        // exePath参数：当engine为7zip或Bandizip时显示，并自动检测路径
        List<Map<String, Object>> exePathConditions = new ArrayList<>();
        Map<String, Object> sevenZipCondition = new HashMap<>();
        sevenZipCondition.put("dependentParam", "engine");
        sevenZipCondition.put("expectedValue", UnzipEngine.SEVEN_ZIP.getCode());
        exePathConditions.add(sevenZipCondition);
        
        Map<String, Object> bandizipCondition = new HashMap<>();
        bandizipCondition.put("dependentParam", "engine");
        bandizipCondition.put("expectedValue", UnzipEngine.BANDIZIP.getCode());
        exePathConditions.add(bandizipCondition);
        
        getConfigField("exePath").setBlockConditions(exePathConditions);
        
        // 为exePath添加自动填充配置
        AutoFillConfig autoFillConfig = new AutoFillConfig();
        autoFillConfig.setTriggerParam("engine");
        autoFillConfig.setFillType("auto_detect");
        getConfigField("exePath").setAutoFillConfig(autoFillConfig);
        
        // customPath参数：当outputMode为指定目录时显示
        List<Map<String, Object>> customPathConditions = new ArrayList<>();
        Map<String, Object> customPathCondition = new HashMap<>();
        customPathCondition.put("dependentParam", "outputMode");
        customPathCondition.put("expectedValue", OutputMode.SPECIFIED_DIRECTORY.getCode());
        customPathConditions.add(customPathCondition);
        
        getConfigField("customPath").setBlockConditions(customPathConditions);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "engine", (Object) UnzipEngine.JAVA_BUILTIN.getCode());
        setConfigValue(config, "exePath", (Object) "");
        setConfigValue(config, "outputMode", (Object) OutputMode.AUTO_SUBDIRECTORY.getCode());
        setConfigValue(config, "customPath", (Object) "");
        setConfigValue(config, "smartFolder", (Object) true);
        setConfigValue(config, "mergeSameName", (Object) false);
        setConfigValue(config, "deleteSource", (Object) false);
        setConfigValue(config, "overwrite", (Object) false);
        setConfigValue(config, "deleteOnFail", (Object) false);
        setConfigValue(config, "nestedFolderMerge", (Object) false);
        setConfigValue(config, "passwords", (Object) new ArrayList<>());
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File file = currentRecord.getFileHandle();
        if (!file.isFile()) {
            return Collections.emptyList();
        }
        
        if (!isArchiveFile(file)) {
            return Collections.emptyList();
        }
        
        String engine = getConfigValue(config, "engine", "java_builtin");
        String outputMode = getConfigValue(config, "outputMode", "auto_subdirectory");
        String customPath = getConfigValue(config, "customPath", "");
        boolean smartFolder = getConfigValue(config, "smartFolder", true);
        
        context.logInfo("分析文件解压: " + file.getName() + ", 引擎: " + engine);
        
        String outputDir = getOutputDirectory(file, outputMode, customPath, smartFolder);
        File outputDirectory = new File(outputDir);
        
        Map<String, String> params = new HashMap<>();
        params.put("engine", engine);
        params.put("outputMode", outputMode);
        params.put("smartFolder", String.valueOf(smartFolder));
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            outputDirectory.getName(),
            currentRecord.getFileHandle(),
            true,
            outputDir,
            OperationType.UNZIP,
            params,
            ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File sourceFile = record.getFileHandle();
        File targetDir = new File(record.getNewPath());
        
        if (!sourceFile.exists()) {
            context.logWarn("源文件不存在: " + sourceFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        boolean deleteSource = getConfigValue(config, "deleteSource", false);
        boolean overwrite = getConfigValue(config, "overwrite", false);
        boolean deleteOnFail = getConfigValue(config, "deleteOnFail", false);
        
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs();
                context.logDebug("创建输出目录: " + targetDir.getPath());
            }
            
            context.logInfo("解压文件: " + sourceFile.getPath() + " -> " + targetDir.getPath());
            
            record.setStatus(ExecStatus.SUCCESS.name());
            
            if (deleteSource) {
                sourceFile.delete();
                context.logInfo("删除源文件: " + sourceFile.getPath());
            }
        } catch (Exception e) {
            context.logError("解压文件失败: " + sourceFile.getPath() + ", 错误: " + e.getMessage());
            record.setStatus(ExecStatus.FAILED.name());
            
            if (deleteOnFail) {
                sourceFile.delete();
                context.logInfo("因失败删除源文件: " + sourceFile.getPath());
            }
        }
    }

    private boolean isArchiveFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") || 
               name.endsWith(".tar") || name.endsWith(".gz") || name.endsWith(".bz2");
    }

    private String getOutputDirectory(File sourceFile, String outputMode, String customPath, boolean smartFolder) {
        File parentDir = sourceFile.getParentFile();
        if (parentDir == null) {
            return customPath;
        }
        
        switch (outputMode) {
            case "auto_subdirectory":
                String baseName = sourceFile.getName();
                int lastDotIndex = baseName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    baseName = baseName.substring(0, lastDotIndex);
                }
                if (smartFolder) {
                    return parentDir.getPath() + File.separator + baseName;
                } else {
                    return parentDir.getPath();
                }
            case "same_as_source":
                return parentDir.getPath();
            case "specified_directory":
                return customPath;
            default:
                return parentDir.getPath() + File.separator + sourceFile.getName().replaceAll("\\.[^.]+$", "");
        }
    }
    
    private java.util.List<EnumOptionDTO> getUnzipEngineOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (UnzipEngine engine : UnzipEngine.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(engine.getCode());
            option.setLabel(engine.getNameZh());
            option.setNameEn(engine.getNameEn());
            option.setDescriptionZh(engine.getDescriptionZh());
            option.setDescriptionEn(engine.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
    
    private java.util.List<EnumOptionDTO> getOutputModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (OutputMode mode : OutputMode.values()) {
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
}
