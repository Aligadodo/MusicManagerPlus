package com.filemanager.backend.service;

import com.filemanager.domain.dto.AutoFillConfig;
import com.filemanager.domain.dto.ConfigFieldDTO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数关系管理服务
 * 用于处理参数之间的各种关系，包括主子关系、互斥关系、联动关系等
 */
@Service
public class ParameterRelationService {

    /**
     * 根据参数关系过滤需要显示的参数
     * @param allFields 所有参数列表
     * @param currentValues 当前参数值
     * @return 需要显示的参数列表
     */
    public List<ConfigFieldDTO> filterVisibleFields(List<ConfigFieldDTO> allFields, Map<String, Object> currentValues) {
        List<ConfigFieldDTO> visibleFields = new ArrayList<>();
        
        for (ConfigFieldDTO field : allFields) {
            if (isFieldVisible(field, currentValues)) {
                visibleFields.add(field);
            }
        }
        
        return visibleFields;
    }

    /**
     * 判断参数是否可见
     * @param field 参数字段
     * @param currentValues 当前参数值
     * @return 是否可见
     */
    private boolean isFieldVisible(ConfigFieldDTO field, Map<String, Object> currentValues) {
        // 检查阻止条件
        if (field.getBlockConditions() != null && !field.getBlockConditions().isEmpty()) {
            for (Map<String, Object> condition : field.getBlockConditions()) {
                if (isConditionMet(condition, currentValues)) {
                    return false;
                }
            }
        }
        
        // 检查依赖条件
        if (field.getDependsOn() != null && field.getDependsValue() != null) {
            Object dependentValue = currentValues.get(field.getDependsOn());
            if (dependentValue == null || !dependentValue.equals(field.getDependsValue())) {
                return false;
            }
        }
        
        // 检查父参数是否可见（对于子参数）
        if (field.getDependsOn() != null) {
            ConfigFieldDTO parentField = findFieldByName(allFieldsCache, field.getDependsOn());
            if (parentField != null && !isFieldVisible(parentField, currentValues)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 处理互斥关系
     * 当某个参数值变化时，自动取消同一互斥组中其他参数的值
     * @param changedParamName 变化的参数名
     * @param changedParamValue 变化的参数值
     * @param allFields 所有参数列表
     * @param currentValues 当前参数值
     * @return 更新后的参数值
     */
    public Map<String, Object> handleExclusiveRelation(
            String changedParamName, 
            Object changedParamValue, 
            List<ConfigFieldDTO> allFields, 
            Map<String, Object> currentValues) {
        
        ConfigFieldDTO changedField = findFieldByName(allFields, changedParamName);
        if (changedField == null || changedField.getExclusiveGroup() == null) {
            return currentValues;
        }
        
        String exclusiveGroup = changedField.getExclusiveGroup();
        Map<String, Object> updatedValues = new HashMap<>(currentValues);
        
        // 找到同一互斥组的所有参数
        List<ConfigFieldDTO> exclusiveFields = allFields.stream()
                .filter(f -> exclusiveGroup.equals(f.getExclusiveGroup()))
                .collect(Collectors.toList());
        
        // 取消其他参数的值
        for (ConfigFieldDTO field : exclusiveFields) {
            if (!field.getName().equals(changedParamName)) {
                // 根据参数类型设置默认值
                if ("boolean".equals(field.getType())) {
                    updatedValues.put(field.getName(), false);
                } else if ("select".equals(field.getType()) || "enum".equals(field.getType())) {
                    updatedValues.put(field.getName(), null);
                } else {
                    updatedValues.put(field.getName(), field.getDefaultValue());
                }
            }
        }
        
        return updatedValues;
    }

    /**
     * 处理自动填充关系
     * 当依赖参数值变化时，自动填充当前参数的值
     * @param changedParamName 变化的参数名
     * @param changedParamValue 变化的参数值
     * @param allFields 所有参数列表
     * @param currentValues 当前参数值
     * @return 更新后的参数值
     */
    public Map<String, Object> handleAutoFill(
            String changedParamName, 
            Object changedParamValue, 
            List<ConfigFieldDTO> allFields, 
            Map<String, Object> currentValues) {
        
        Map<String, Object> updatedValues = new HashMap<>(currentValues);
        
        // 遍历所有参数，查找需要自动填充的参数
        for (ConfigFieldDTO field : allFields) {
            AutoFillConfig autoFillConfig = field.getAutoFillConfig();
            if (autoFillConfig == null) {
                continue;
            }
            
            // 检查是否触发自动填充
            if (changedParamName.equals(autoFillConfig.getTriggerParam()) &&
                (autoFillConfig.getTriggerValue() == null || 
                 autoFillConfig.getTriggerValue().equals(String.valueOf(changedParamValue)))) {
                
                // 根据填充类型执行自动填充
                Object fillValue = executeAutoFill(autoFillConfig, changedParamValue, currentValues);
                if (fillValue != null) {
                    updatedValues.put(field.getName(), fillValue);
                }
            }
        }
        
        return updatedValues;
    }

    /**
     * 执行自动填充逻辑
     * @param autoFillConfig 自动填充配置
     * @param triggerValue 触发值
     * @param currentValues 当前参数值
     * @return 填充值
     */
    private Object executeAutoFill(AutoFillConfig autoFillConfig, Object triggerValue, Map<String, Object> currentValues) {
        String fillType = autoFillConfig.getFillType();
        
        if ("fixed_value".equals(fillType)) {
            return autoFillConfig.getFillValue();
        } else if ("auto_detect".equals(fillType)) {
            return autoDetectValue(autoFillConfig, triggerValue, currentValues);
        } else if ("expression".equals(fillType)) {
            return evaluateExpression(autoFillConfig.getFillValue(), currentValues);
        }
        
        return null;
    }

    /**
     * 自动检测值
     * @param autoFillConfig 自动填充配置
     * @param triggerValue 触发值
     * @param currentValues 当前参数值
     * @return 检测到的值
     */
    private Object autoDetectValue(AutoFillConfig autoFillConfig, Object triggerValue, Map<String, Object> currentValues) {
        String detectPattern = autoFillConfig.getDetectPattern();
        
        // 如果没有指定检测模式，根据触发值自动选择
        if (detectPattern == null && triggerValue != null) {
            String triggerValueStr = String.valueOf(triggerValue);
            if (triggerValueStr.contains("7zip") || triggerValueStr.contains("7z")) {
                detectPattern = "7zip_path";
            } else if (triggerValueStr.contains("bandizip") || triggerValueStr.contains("bz")) {
                detectPattern = "bandizip_path";
            } else if (triggerValueStr.contains("ffmpeg")) {
                detectPattern = "ffmpeg_path";
            }
        }
        
        if (detectPattern == null) {
            return null;
        }
        
        // 根据检测模式执行检测逻辑
        if ("7zip_path".equals(detectPattern)) {
            return detect7zipPath();
        } else if ("bandizip_path".equals(detectPattern)) {
            return detectBandizipPath();
        } else if ("ffmpeg_path".equals(detectPattern)) {
            return detectFfmpegPath();
        }
        
        return null;
    }

    /**
     * 检测7zip路径
     * @return 7zip路径
     */
    private String detect7zipPath() {
        List<String> paths = Arrays.asList(
            System.getProperty("user.dir") + "\\tools\\7z.exe",
            System.getProperty("user.dir") + "\\tools\\7-Zip\\7z.exe",
            "C:\\Program Files\\7-Zip\\7z.exe",
            "C:\\Program Files (x86)\\7-Zip\\7z.exe"
        );
        
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return path;
            }
        }
        
        return null;
    }

    /**
     * 检测Bandizip路径
     * @return Bandizip路径
     */
    private String detectBandizipPath() {
        List<String> paths = Arrays.asList(
            System.getProperty("user.dir") + "\\tools\\bz.exe",
            System.getProperty("user.dir") + "\\tools\\bc.exe",
            System.getProperty("user.dir") + "\\tools\\Bandizip\\bz.exe",
            System.getProperty("user.dir") + "\\tools\\Bandizip\\bc.exe",
            "C:\\Program Files\\Bandizip\\bz.exe",
            "C:\\Program Files\\Bandizip\\bc.exe"
        );
        
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return path;
            }
        }
        
        return null;
    }

    /**
     * 检测ffmpeg路径
     * @return ffmpeg路径
     */
    private String detectFfmpegPath() {
        List<String> paths = Arrays.asList(
            System.getProperty("user.dir") + "\\tools\\ffmpeg.exe",
            System.getProperty("user.dir") + "\\tools\\ffmpeg\\ffmpeg.exe",
            "C:\\Program Files\\ffmpeg\\ffmpeg.exe",
            "C:\\Program Files (x86)\\ffmpeg\\ffmpeg.exe"
        );
        
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return path;
            }
        }
        
        return null;
    }

    /**
     * 评估表达式
     * @param expression 表达式
     * @param currentValues 当前参数值
     * @return 评估结果
     */
    private Object evaluateExpression(String expression, Map<String, Object> currentValues) {
        // 简单实现：替换表达式中的参数占位符
        String result = expression;
        for (Map.Entry<String, Object> entry : currentValues.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * 判断条件是否满足
     * @param condition 条件
     * @param currentValues 当前参数值
     * @return 是否满足
     */
    private boolean isConditionMet(Map<String, Object> condition, Map<String, Object> currentValues) {
        String dependentParam = (String) condition.get("dependentParam");
        Object expectedValue = condition.get("expectedValue");
        
        if (dependentParam == null || expectedValue == null) {
            return false;
        }
        
        Object actualValue = currentValues.get(dependentParam);
        return expectedValue.equals(actualValue);
    }

    /**
     * 根据名称查找参数
     * @param fields 参数列表
     * @param name 参数名
     * @return 参数字段
     */
    public ConfigFieldDTO findFieldByName(List<ConfigFieldDTO> fields, String name) {
        return fields.stream()
                .filter(f -> name.equals(f.getName()))
                .findFirst()
                .orElse(null);
    }

    // 缓存所有参数，用于子参数的可见性判断
    private List<ConfigFieldDTO> allFieldsCache;
    
    public void setAllFieldsCache(List<ConfigFieldDTO> fields) {
        this.allFieldsCache = fields;
    }
}