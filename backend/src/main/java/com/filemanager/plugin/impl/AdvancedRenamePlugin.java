package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高级重命名策略插件
 * 基于规则的高级文件重命名功能，支持多种条件和操作
 */
public class AdvancedRenamePlugin extends AbstractPlugin {

    public AdvancedRenamePlugin() {
        super("advanced-rename", "高级重命名策略", "基于规则的高级文件重命名功能，支持多种条件和操作", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("crossDriveMode", "跨盘动作", "select", "移动 (Move)", "跨盘操作时的动作", false,
            Arrays.asList("移动 (Move)", "复制 (Copy)"));
        addParameter("processScope", "处理范围", "select", "全部处理", "处理的文件类型范围", false,
            Arrays.asList("仅处理文件", "仅处理文件夹", "全部处理"));
        addParameter("rules", "重命名规则", "list", new ArrayList<>(), "重命名规则列表", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("crossDriveMode", "移动 (Move)");
        setDefaultConfigValue("processScope", "全部处理");
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
        String processScope = getConfigValue(config, "processScope", "全部处理");
        String crossDriveMode = getConfigValue(config, "crossDriveMode", "移动 (Move)");
        
        File file = new File(filePath);
        if (!file.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        // 检查处理范围
        if ("仅处理文件".equals(processScope) && file.isDirectory()) {
            context.logDebug("Skipping directory: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if ("仅处理文件夹".equals(processScope) && file.isFile()) {
            context.logDebug("Skipping file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String newName = applyRenameRules(filePath, config, context);
            
            // 如果文件名没有变化，跳过
            if (filePath.equals(newName)) {
                context.logDebug("File name unchanged: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            File newFile = new File(newName);
            
            // 检查是否跨盘操作
            boolean crossDrive = !file.toPath().getRoot().equals(newFile.toPath().getRoot());
            
            if (crossDrive) {
                context.logInfo("Cross-drive operation detected");
                if ("复制 (Copy)".equals(crossDriveMode)) {
                    // 跨盘复制
                    Files.copy(file.toPath(), newFile.toPath());
                    context.logInfo("Copied file: " + filePath + " -> " + newName);
                } else {
                    // 跨盘移动
                    Files.move(file.toPath(), newFile.toPath());
                    context.logInfo("Moved file: " + filePath + " -> " + newName);
                }
            } else {
                // 同盘操作，直接重命名
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

    /**
     * 应用重命名规则
     */
    private String applyRenameRules(String filePath, PluginConfigDTO config, ExecutionContext context) {
        List<Map<String, Object>> rules = getConfigValue(config, "rules", new ArrayList<>());
        
        if (rules == null || rules.isEmpty()) {
            context.logDebug("No rename rules configured");
            return filePath;
        }
        
        File file = new File(filePath);
        String fileName = file.getName();
        String parentPath = file.getParent();
        
        // 遍历所有规则，按优先级执行
        for (Map<String, Object> rule : rules) {
            String ruleName = (String) rule.get("name");
            boolean enabled = rule.get("enabled") != null ? (Boolean) rule.get("enabled") : true;
            
            if (!enabled) {
                context.logDebug("Rule disabled: " + ruleName);
                continue;
            }
            
            // 检查条件
            if (!checkConditions(file, rule, context)) {
                context.logDebug("Rule conditions not met: " + ruleName);
                continue;
            }
            
            // 应用动作
            fileName = applyActions(fileName, rule, context);
            context.logInfo("Applied rule: " + ruleName);
            
            // 第一个匹配的规则生效，后续规则不再执行
            break;
        }
        
        // 构建新路径
        if (parentPath != null) {
            return parentPath + File.separator + fileName;
        }
        return fileName;
    }

    /**
     * 检查条件
     */
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
            
            if (!checkCondition(file, type, operator, value, context)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 检查单个条件
     */
    private boolean checkCondition(File file, String type, String operator, Object value, ExecutionContext context) {
        String fileName = file.getName();
        String filePath = file.getPath();
        
        switch (type) {
            case "文件名匹配":
                return checkStringCondition(fileName, operator, (String) value);
            case "文件路径匹配":
                return checkStringCondition(filePath, operator, (String) value);
            case "文件大小":
                long fileSize = file.length();
                return checkNumberCondition(fileSize, operator, (Number) value);
            case "文件修改日期":
                long lastModified = file.lastModified();
                return checkNumberCondition(lastModified, operator, (Number) value);
            case "文件扩展名":
                String extension = getFileExtension(file);
                return checkStringCondition(extension, operator, (String) value);
            case "正则表达式":
                Pattern pattern = Pattern.compile((String) value);
                Matcher matcher = pattern.matcher(fileName);
                return matcher.matches();
            default:
                context.logWarn("Unknown condition type: " + type);
                return false;
        }
    }

    /**
     * 检查字符串条件
     */
    private boolean checkStringCondition(String text, String operator, String value) {
        switch (operator) {
            case "等于":
                return text.equals(value);
            case "包含":
                return text.contains(value);
            case "开始于":
                return text.startsWith(value);
            case "结束于":
                return text.endsWith(value);
            case "不等于":
                return !text.equals(value);
            case "不包含":
                return !text.contains(value);
            default:
                return false;
        }
    }

    /**
     * 检查数字条件
     */
    private boolean checkNumberCondition(long number, String operator, Number value) {
        long compareValue = value.longValue();
        switch (operator) {
            case "等于":
                return number == compareValue;
            case "大于":
                return number > compareValue;
            case "小于":
                return number < compareValue;
            case "大于等于":
                return number >= compareValue;
            case "小于等于":
                return number <= compareValue;
            default:
                return false;
        }
    }

    /**
     * 应用动作
     */
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
            
            result = applyAction(result, type, value, context);
        }
        
        return result;
    }

    /**
     * 应用单个动作
     */
    private String applyAction(String fileName, String type, Object value, ExecutionContext context) {
        switch (type) {
            case "替换文本":
                @SuppressWarnings("unchecked")
                Map<String, Object> replaceValue = (Map<String, Object>) value;
                String searchText = (String) replaceValue.get("searchText");
                String replaceText = (String) replaceValue.get("replaceText");
                return fileName.replace(searchText, replaceText);
            case "添加前缀":
                return (String) value + fileName;
            case "添加后缀":
                return fileName + (String) value;
            case "删除文本":
                return fileName.replace((String) value, "");
            case "大小写转换":
                String caseType = (String) value;
                switch (caseType) {
                    case "全部大写":
                        return fileName.toUpperCase();
                    case "全部小写":
                        return fileName.toLowerCase();
                    case "首字母大写":
                        if (fileName.isEmpty()) return fileName;
                        return fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
                    case "标题大小写":
                        return toTitleCase(fileName);
                    default:
                        return fileName;
                }
            case "正则替换":
                @SuppressWarnings("unchecked")
                Map<String, Object> regexValue = (Map<String, Object>) value;
                String pattern = (String) regexValue.get("pattern");
                String replacement = (String) regexValue.get("replacement");
                Pattern regex = Pattern.compile(pattern);
                return regex.matcher(fileName).replaceAll(replacement);
            default:
                context.logWarn("Unknown action type: " + type);
                return fileName;
        }
    }

    /**
     * 转换为标题大小写
     */
    private String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
