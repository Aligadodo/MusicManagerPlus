package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.enums.CrossDriveMode;
import com.filemanager.plugin.impl.enums.ProcessScope;
import java.io.File;
import java.util.ArrayList;

public class AdvancedRenameStrategy extends AbstractConfigurableStrategy {

    public AdvancedRenameStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "advanced-rename";
    }

    @Override
    public String getName() {
        return "高级重命名策略";
    }

    @Override
    public String getDescription() {
        return "基于规则的高级文件重命名功能，支持多种条件和操作";
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
        addEnumConfigField("crossDriveMode", "跨盘动作", "select", (Object) CrossDriveMode.MOVE.getCode(), 
            "跨盘操作时的动作", false, 
            getCrossDriveModeOptions());
        addEnumConfigField("processScope", "处理范围", "select", (Object) ProcessScope.ALL.getCode(), 
            "处理的文件类型范围", false, 
            getProcessScopeOptions());
        addConfigField("rules", "重命名规则", "list", (Object) new ArrayList<>(), 
            "重命名规则列表", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "crossDriveMode", (Object) CrossDriveMode.MOVE.getCode());
        setConfigValue(config, "processScope", (Object) ProcessScope.ALL.getCode());
        setConfigValue(config, "rules", (Object) new ArrayList<>());
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        File sourceFile = new File(filePath);
        String newName = generateNewName(sourceFile, config, context);
        
        ChangeRecord record = createChangeRecord(filePath, newName, "PENDING");
        record.setOperationType("RENAME");
        record.setReason("高级重命名");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String newName = generateNewName(sourceFile, config, context);
            File targetFile = new File(newName);
            
            if (targetFile.exists()) {
                context.logWarn("Target file already exists: " + newName);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            sourceFile.renameTo(targetFile);
            
            context.logInfo("Renamed file: " + filePath + " -> " + newName);
            ChangeRecord record = createChangeRecord(filePath, newName, "SUCCESS");
            record.setOperationType("RENAME");
            record.setReason("高级重命名");
            return record;
        } catch (Exception e) {
            context.logError("Error renaming file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String generateNewName(File sourceFile, StrategyConfigDTO config, ExecutionContext context) {
        String crossDriveMode = getConfigValue(config, "crossDriveMode", "move");
        String processScope = getConfigValue(config, "processScope", "all");
        
        String newName = sourceFile.getName();
        
        return sourceFile.getParent() + File.separator + newName;
    }
    
    private java.util.List<EnumOptionDTO> getCrossDriveModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (CrossDriveMode mode : CrossDriveMode.values()) {
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
    
    private java.util.List<EnumOptionDTO> getProcessScopeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (ProcessScope scope : ProcessScope.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(scope.getCode());
            option.setLabel(scope.getNameZh());
            option.setNameEn(scope.getNameEn());
            option.setDescriptionZh(scope.getDescriptionZh());
            option.setDescriptionEn(scope.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}