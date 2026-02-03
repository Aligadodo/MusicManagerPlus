package com.filemanager.plugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件抽象基类
 * 为具体的策略插件提供通用功能
 */
public abstract class AbstractPlugin implements IPlugin {

    protected String id;
    protected String name;
    protected String description;
    protected String version;
    protected List<Map<String, Object>> parameters;
    protected PluginConfigDTO defaultConfig;

    public AbstractPlugin(String id, String name, String description, String version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.parameters = new ArrayList<>();
        this.defaultConfig = new PluginConfigDTO();
        initParameters();
        initDefaultConfig();
    }

    /**
     * 初始化插件参数
     */
    protected abstract void initParameters();

    /**
     * 初始化默认配置
     */
    protected abstract void initDefaultConfig();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<Map<String, Object>> getParameters() {
        return parameters;
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 添加插件参数
     * @param name 参数名称
     * @param label 参数标签
     * @param type 参数类型
     * @param defaultValue 默认值
     * @param description 参数描述
     * @param required 是否必填
     */
    protected void addParameter(String name, String label, String type, Object defaultValue, String description, boolean required) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("name", name);
        parameter.put("label", label);
        parameter.put("type", type);
        parameter.put("defaultValue", defaultValue);
        parameter.put("description", description);
        parameter.put("required", required);
        parameters.add(parameter);
    }

    /**
     * 添加带选项的插件参数
     * @param name 参数名称
     * @param label 参数标签
     * @param type 参数类型
     * @param defaultValue 默认值
     * @param description 参数描述
     * @param required 是否必填
     * @param options 选项列表
     */
    protected void addParameter(String name, String label, String type, Object defaultValue, String description, boolean required, List<String> options) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("name", name);
        parameter.put("label", label);
        parameter.put("type", type);
        parameter.put("defaultValue", defaultValue);
        parameter.put("description", description);
        parameter.put("required", required);
        parameter.put("options", options);
        parameters.add(parameter);
    }

    /**
     * 设置默认配置值
     * @param key 配置键
     * @param value 配置值
     */
    protected void setDefaultConfigValue(String key, Object value) {
        defaultConfig.setValue(key, value);
    }

    /**
     * 从配置中获取值
     * @param config 配置对象
     * @param key 配置键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 配置值
     */
    protected <T> T getConfigValue(PluginConfigDTO config, String key, T defaultValue) {
        if (config != null) {
            Object value = config.getValue(key);
            if (value != null) {
                return (T) value;
            }
        }
        return defaultValue;
    }

    /**
     * 创建变更记录
     * @param originalPath 原始路径
     * @param newPath 新路径
     * @param status 状态
     * @return 变更记录
     */
    protected ChangeRecord createChangeRecord(String originalPath, String newPath, String status) {
        ChangeRecord record = new ChangeRecord();
        record.setId("change-" + System.currentTimeMillis() + "-" + originalPath.hashCode());
        record.setOriginalName(originalPath);
        record.setNewName(newPath);
        record.setFilePath(originalPath);
        record.setChanged(!originalPath.equals(newPath));
        record.setStatus(status);
        return record;
    }

    /**
     * 执行预览操作
     * @param filePaths 文件路径列表
     * @param config 插件配置
     * @param context 执行上下文
     * @return 变更记录列表
     */
    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        context.logInfo("Previewing plugin: " + name);
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            context.updateProgress(i + 1, filePaths.size());
            
            if (context.isCancelled()) {
                context.logInfo("Preview cancelled");
                break;
            }
            
            ChangeRecord record = createPreviewRecord(filePath, config, context);
            if (record != null) {
                changes.add(record);
            }
        }
        
        context.logInfo("Preview completed: " + changes.size() + " changes");
        return changes;
    }

    /**
     * 创建预览记录
     * @param filePath 文件路径
     * @param config 插件配置
     * @param context 执行上下文
     * @return 变更记录
     */
    protected abstract ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context);

    /**
     * 执行插件操作
     * @param filePaths 文件路径列表
     * @param config 插件配置
     * @param context 执行上下文
     * @return 变更记录列表
     */
    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        context.logInfo("Executing plugin: " + name);
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            context.updateProgress(i + 1, filePaths.size());
            
            if (context.isCancelled()) {
                context.logInfo("Execution cancelled");
                break;
            }
            
            try {
                ChangeRecord record = executeForFile(filePath, config, context);
                if (record != null) {
                    changes.add(record);
                }
            } catch (Exception e) {
                context.logError("Error processing file " + filePath + ": " + e.getMessage());
                ChangeRecord errorRecord = createChangeRecord(filePath, filePath, "ERROR");
                changes.add(errorRecord);
            }
        }
        
        context.logInfo("Execution completed: " + changes.size() + " changes");
        return changes;
    }

    /**
     * 执行单个文件操作
     * @param filePath 文件路径
     * @param config 插件配置
     * @param context 执行上下文
     * @return 变更记录
     */
    protected abstract ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context);
}
