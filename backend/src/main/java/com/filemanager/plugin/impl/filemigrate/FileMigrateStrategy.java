package com.filemanager.plugin.impl.filemigrate;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.enums.common.OutputDirMode;
import com.filemanager.plugin.impl.filemigrate.enums.OperationMode;
import com.filemanager.plugin.impl.filemigrate.enums.ScopeMode;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileMigrateStrategy extends AbstractConfigurableStrategy {

    public FileMigrateStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-migrate";
    }

    @Override
    public String getName() {
        return "文件批量归档和移动";
    }

    @Override
    public String getDescription() {
        return "文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择。";
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
        addEnumConfigField("operationMode", "操作模式", "select", (Object) OperationMode.MOVE.getCode(), 
            "文件的操作方式", true, 
            getOperationModeOptions());
        addEnumConfigField("outputDirMode", "输出目录模式", "select", (Object) OutputDirMode.SUBDIRECTORY.getCode(), 
            "输出目录模式", true, 
            getOutputDirModeOptions());
        addConfigField("outputPath", "输出路径", "directory", (Object) "Archive", 
            "目标路径", true);
        addEnumConfigField("scope", "生效范围", "select", (Object) ScopeMode.ALL.getCode(), 
            "文件处理的生效范围", false, 
            getScopeModeOptions());
        addConfigField("depth", "深度值", "number", (Object) 0, 
            "指定生效范围的深度值", false);
        addConfigField("keepLargest", "保留最大文件", "boolean", (Object) true, 
            "去重时保留最大的文件", false);
        addConfigField("keepEarliest", "保留最早文件", "boolean", (Object) true, 
            "去重时保留日期最早的文件", false);
        addConfigField("keepExt", "优先后缀", "string", (Object) "wav", 
            "去重时优先保留的文件后缀", false);
        addConfigField("audioSpecial", "音频特殊处理", "boolean", (Object) true, 
            "去重时对音频文件进行特殊处理", false);
        
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
        
        // depth参数：当scope为指定深度时显示
        List<Map<String, Object>> depthConditions = new ArrayList<>();
        Map<String, Object> depthCondition = new HashMap<>();
        depthCondition.put("dependentParam", "scope");
        depthCondition.put("expectedValue", ScopeMode.SPECIFIED_DEPTH.getCode());
        depthConditions.add(depthCondition);
        
        getConfigField("depth").setBlockConditions(depthConditions);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "operationMode", (Object) OperationMode.MOVE.getCode());
        setConfigValue(config, "outputDirMode", (Object) OutputDirMode.SUBDIRECTORY.getCode());
        setConfigValue(config, "outputPath", (Object) "Archive");
        setConfigValue(config, "scope", (Object) ScopeMode.ALL.getCode());
        setConfigValue(config, "depth", (Object) 0);
        setConfigValue(config, "keepLargest", (Object) true);
        setConfigValue(config, "keepEarliest", (Object) true);
        setConfigValue(config, "keepExt", (Object) "wav");
        setConfigValue(config, "audioSpecial", (Object) true);
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
        
        String operationMode = getConfigValue(config, "operationMode", "move");
        String outputDirMode = getConfigValue(config, "outputDirMode", "subdirectory");
        String outputPath = getConfigValue(config, "outputPath", "Archive");
        
        context.logInfo("分析文件迁移: " + file.getName() + ", 操作模式: " + operationMode);
        
        String targetPath = getTargetPath(file, outputDirMode, outputPath);
        File targetFile = new File(targetPath, file.getName());
        
        if (targetFile.exists()) {
            context.logWarn("目标文件已存在: " + targetFile.getPath());
            return Collections.emptyList();
        }
        
        OperationType opType = OperationType.MOVE;
        if ("copy".equals(operationMode)) {
            opType = OperationType.COPY;
        }
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            targetFile.getName(),
            currentRecord.getFileHandle(),
            true,
            targetFile.getAbsolutePath(),
            opType,
            new HashMap<>(),
            ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File sourceFile = record.getFileHandle();
        File targetFile = new File(record.getNewPath());
        
        if (!sourceFile.exists()) {
            context.logWarn("源文件不存在: " + sourceFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
            context.logDebug("创建目录: " + targetFile.getParentFile().getPath());
        }
        
        OperationType opType = OperationType.valueOf(record.getOperationType());
        
        if (opType == OperationType.COPY) {
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            context.logInfo("复制文件: " + sourceFile.getPath() + " -> " + targetFile.getPath());
        } else {
            Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            context.logInfo("移动文件: " + sourceFile.getPath() + " -> " + targetFile.getPath());
        }
        
        record.setStatus(ExecStatus.SUCCESS.name());
    }

    private String getTargetPath(File sourceFile, String outputDirMode, String outputPath) {
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
    
    private java.util.List<EnumOptionDTO> getOperationModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (OperationMode mode : OperationMode.values()) {
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
    
    private java.util.List<EnumOptionDTO> getScopeModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (ScopeMode mode : ScopeMode.values()) {
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
