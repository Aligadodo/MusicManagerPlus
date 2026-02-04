package com.filemanager.plugin.impl.advancedrename;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.advancedrename.enums.CrossDriveMode;
import com.filemanager.plugin.impl.advancedrename.enums.ProcessScope;
import com.filemanager.plugin.impl.advancedrename.utils.RenameActionProcessor;
import com.filemanager.plugin.impl.advancedrename.utils.RenameConditionChecker;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AdvancedRenamePlugin extends AbstractPlugin {

    public AdvancedRenamePlugin() {
        super("advanced-rename", "高级重命名策略", "基于规则的高级文件重命名功能，支持多种条件和操作", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("crossDriveMode", "跨盘动作", "select", CrossDriveMode.MOVE.getCode(), 
            "跨盘操作时的动作", false,
            getCrossDriveModeOptions());
        addParameter("processScope", "处理范围", "select", ProcessScope.ALL.getCode(), 
            "处理的文件类型范围", false,
            getProcessScopeOptions());
        addParameter("rules", "重命名规则", "list", new ArrayList<>(), 
            "重命名规则列表", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("crossDriveMode", CrossDriveMode.MOVE.getCode());
        setDefaultConfigValue("processScope", ProcessScope.ALL.getCode());
        setDefaultConfigValue("rules", new ArrayList<>());
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String newName = applyRenameRules(filePath, config, context);
        ChangeRecord record = createChangeRecord(filePath, newName, "PENDING");
        record.setOperationType("RENAME");
        record.setReason("应用重命名规则");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        Object processScopeObj = config.getValue("processScope");
        String processScopeCode = processScopeObj != null ? processScopeObj.toString() : ProcessScope.ALL.getCode();
        ProcessScope processScope = ProcessScope.fromCode(processScopeCode);
        
        Object crossDriveModeObj = config.getValue("crossDriveMode");
        String crossDriveModeCode = crossDriveModeObj != null ? crossDriveModeObj.toString() : CrossDriveMode.MOVE.getCode();
        CrossDriveMode crossDriveMode = CrossDriveMode.fromCode(crossDriveModeCode);
        
        File file = new File(filePath);
        if (!file.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (!processScope.shouldProcessFiles() && file.isFile()) {
            context.logDebug("Skipping file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (!processScope.shouldProcessDirectories() && file.isDirectory()) {
            context.logDebug("Skipping directory: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String newName = applyRenameRules(filePath, config, context);
            
            if (filePath.equals(newName)) {
                context.logDebug("File name unchanged: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            File newFile = new File(newName);
            
            boolean crossDrive = !file.toPath().getRoot().equals(newFile.toPath().getRoot());
            
            if (crossDrive) {
                context.logInfo("Cross-drive operation detected");
                if (crossDriveMode.isCopy()) {
                    Files.copy(file.toPath(), newFile.toPath());
                    context.logInfo("Copied file: " + filePath + " -> " + newName);
                } else {
                    Files.move(file.toPath(), newFile.toPath());
                    context.logInfo("Moved file: " + filePath + " -> " + newName);
                }
            } else {
                Files.move(file.toPath(), newFile.toPath());
                context.logInfo("Renamed file: " + filePath + " -> " + newName);
            }
            
            ChangeRecord record = createChangeRecord(filePath, newName, "SUCCESS");
            record.setOperationType("RENAME");
            record.setReason("应用重命名规则");
            return record;
        } catch (Exception e) {
            context.logError("Error renaming file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }
    
    private String applyRenameRules(String filePath, PluginConfigDTO config, ExecutionContext context) {
        Object rulesObj = config.getValue("rules");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = rulesObj instanceof List ? (List<Map<String, Object>>) rulesObj : new ArrayList<>();
        
        if (rules.isEmpty()) {
            context.logDebug("No rename rules configured");
            return filePath;
        }
        
        File file = new File(filePath);
        String fileName = file.getName();
        String parentPath = file.getParent();
        
        for (Map<String, Object> rule : rules) {
            String ruleName = (String) rule.get("name");
            Object enabledObj = rule.get("enabled");
            boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : true;
            
            if (!enabled) {
                context.logDebug("Rule disabled: " + ruleName);
                continue;
            }
            
            if (!checkConditions(file, rule, context)) {
                context.logDebug("Rule conditions not met: " + ruleName);
                continue;
            }
            
            fileName = applyActions(fileName, rule, context);
            context.logInfo("Applied rule: " + ruleName);
            break;
        }
        
        if (parentPath != null) {
            return parentPath + File.separator + fileName;
        }
        return fileName;
    }
    
    private boolean checkConditions(File file, Map<String, Object> rule, ExecutionContext context) {
        Object conditionsObj = rule.get("conditions");
        if (conditionsObj == null) {
            return true;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) conditionsObj;
        
        if (conditions.isEmpty()) {
            return true;
        }
        
        for (Map<String, Object> condition : conditions) {
            String type = (String) condition.get("type");
            String operator = (String) condition.get("operator");
            Object value = condition.get("value");
            
            if (!RenameConditionChecker.checkCondition(file, type, operator, value)) {
                return false;
            }
        }
        
        return true;
    }
    
    private String applyActions(String fileName, Map<String, Object> rule, ExecutionContext context) {
        Object actionsObj = rule.get("actions");
        if (actionsObj == null) {
            return fileName;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) actionsObj;
        
        if (actions.isEmpty()) {
            return fileName;
        }
        
        String result = fileName;
        
        for (Map<String, Object> action : actions) {
            String type = (String) action.get("type");
            Object value = action.get("value");
            
            result = RenameActionProcessor.applyAction(result, type, value);
        }
        
        return result;
    }
    
    private List<String> getCrossDriveModeOptions() {
        return Arrays.asList(
            CrossDriveMode.MOVE.getCode(),
            CrossDriveMode.COPY.getCode()
        );
    }
    
    private List<String> getProcessScopeOptions() {
        return Arrays.asList(
            ProcessScope.FILES_ONLY.getCode(),
            ProcessScope.DIRECTORIES_ONLY.getCode(),
            ProcessScope.ALL.getCode()
        );
    }
}