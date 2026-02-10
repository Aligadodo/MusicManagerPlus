package com.filemanager.plugin;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.entity.ChangeRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 可配置策略的基础实现类
 * 提供策略配置管理的默认实现
 * 同时实现IPlugin接口，提供execute和preview功能
 */
public abstract class AbstractConfigurableStrategy implements StrategyConfigurable {

    protected List<ConfigFieldDTO> configFields;
    protected String id;
    protected String name;
    protected String description;
    protected String version;

    public AbstractConfigurableStrategy() {
        this.configFields = new ArrayList<>();
        initConfigFields();
    }

    /**
     * 初始化配置字段
     * 子类需要实现此方法来定义自己的配置字段
     */
    protected abstract void initConfigFields();

    /**
     * 初始化默认配置值
     * 子类需要实现此方法来设置默认配置值
     * @param config 配置对象
     */
    protected abstract void initDefaultConfigValues(StrategyConfigDTO config);

    /**
     * 执行单个文件的处理
     * 子类需要实现此方法来提供具体的功能实现
     * @param filePath 文件路径
     * @param config 配置对象
     * @param context 执行上下文
     * @return 变更记录
     */
    protected abstract ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context);

    /**
     * 创建预览记录
     * 子类需要实现此方法来提供预览功能
     * @param filePath 文件路径
     * @param config 配置对象
     * @param context 执行上下文
     * @return 变更记录
     */
    protected abstract ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context);

    @Override
    public List<ConfigFieldDTO> getConfigFields() {
        return configFields;
    }

    /**
     * 根据名称获取配置字段
     * @param name 字段名称
     * @return 配置字段，如果不存在则返回null
     */
    protected ConfigFieldDTO getConfigField(String name) {
        for (ConfigFieldDTO field : configFields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    @Override
    public StrategyConfigDTO initializeDefaultConfig() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        initDefaultConfigValues(config);
        config.setPreconditionGroups(new java.util.ArrayList<>());
        return config;
    }

    @Override
    public boolean validateConfig(StrategyConfigDTO config) {
        if (config == null || config.getConfigValues() == null) {
            return false;
        }

        // 验证必填字段
        for (ConfigFieldDTO field : configFields) {
            if (field.isRequired()) {
                Object value = config.getConfigValues().get(field.getName());
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public <T> T getConfigValue(StrategyConfigDTO config, String key, T defaultValue) {
        if (config == null || config.getConfigValues() == null) {
            return defaultValue;
        }

        Object value = config.getConfigValues().get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    @Override
    public void setConfigValue(StrategyConfigDTO config, String key, Object value) {
        if (config == null) {
            config = new StrategyConfigDTO();
        }

        if (config.getConfigValues() == null) {
            config.setConfigValues(new java.util.HashMap<>());
        }

        config.getConfigValues().put(key, value);
    }

    /**
     * 添加配置字段
     * @param name 字段名称
     * @param label 字段标签
     * @param type 字段类型
     * @param defaultValue 默认值
     * @param description 字段描述
     * @param required 是否必填
     */
    public void addConfigField(String name, String label, String type, Object defaultValue, String description, boolean required) {
        ConfigFieldDTO field = new ConfigFieldDTO(name, label, type, defaultValue, description, required);
        configFields.add(field);
    }

    /**
     * 添加带选项的配置字段
     * @param name 字段名称
     * @param label 字段标签
     * @param type 字段类型
     * @param defaultValue 默认值
     * @param description 字段描述
     * @param required 是否必填
     * @param options 选项列表
     */
    public void addConfigField(String name, String label, String type, Object defaultValue, String description, boolean required, List<String> options) {
        ConfigFieldDTO field = new ConfigFieldDTO(name, label, type, defaultValue, description, required);
        field.setOptions(options);
        configFields.add(field);
    }

    /**
     * 添加带枚举选项的配置字段
     * @param name 字段名称
     * @param label 字段标签
     * @param type 字段类型
     * @param defaultValue 默认值
     * @param description 字段描述
     * @param required 是否必填
     * @param enumOptions 枚举选项列表
     */
    public void addEnumConfigField(String name, String label, String type, Object defaultValue, String description, boolean required, List<com.filemanager.domain.dto.EnumOptionDTO> enumOptions) {
        ConfigFieldDTO field = new ConfigFieldDTO(name, label, type, defaultValue, description, required);
        field.setEnumOptions(enumOptions);
        configFields.add(field);
    }

    /**
     * IPlugin接口实现：获取插件参数
     * 将ConfigFieldDTO转换为PluginParameterDTO
     */
    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        for (ConfigFieldDTO field : configFields) {
            PluginParameterDTO param = new PluginParameterDTO();
            param.setName(field.getName());
            param.setLabel(field.getLabel());
            param.setDescription(field.getDescription());
            param.setType(field.getType());
            param.setDefaultValue(field.getDefaultValue());
            param.setRequired(field.isRequired());
            
            if (field.getOptions() != null) {
                param.setOptions(field.getOptions().toArray(new String[0]));
            }
            
            if (field.getEnumOptions() != null) {
                param.setEnumOptions(field.getEnumOptions());
            }
            
            parameters.add(param);
        }
        return parameters;
    }

    /**
     * IPlugin接口实现：获取默认配置
     * 将StrategyConfigDTO转换为PluginConfigDTO
     */
    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO pluginConfig = new PluginConfigDTO();
        StrategyConfigDTO strategyConfig = initializeDefaultConfig();
        
        if (strategyConfig.getConfigValues() != null) {
            for (Map.Entry<String, Object> entry : strategyConfig.getConfigValues().entrySet()) {
                pluginConfig.setValue(entry.getKey(), entry.getValue());
            }
        }
        
        return pluginConfig;
    }

    /**
     * IPlugin接口实现：预览功能
     * 遍历文件列表，为每个文件创建预览记录
     */
    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        context.logInfo("Previewing strategy: " + name);
        
        StrategyConfigDTO strategyConfig = convertToStrategyConfig(config);
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            context.updateProgress(i + 1, filePaths.size());
            
            if (context.isCancelled()) {
                context.logInfo("Preview cancelled");
                break;
            }
            
            ChangeRecord record = createPreviewRecord(filePath, strategyConfig, context);
            if (record != null) {
                changes.add(record);
            }
        }
        
        context.logInfo("Preview completed: " + changes.size() + " changes");
        return changes;
    }

    /**
     * IPlugin接口实现：执行功能
     * 遍历文件列表，对每个文件执行处理
     */
    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        context.logInfo("Executing strategy: " + name);
        
        StrategyConfigDTO strategyConfig = convertToStrategyConfig(config);
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            context.updateProgress(i + 1, filePaths.size());
            
            if (context.isCancelled()) {
                context.logInfo("Execution cancelled");
                break;
            }
            
            try {
                ChangeRecord record = executeForFile(filePath, strategyConfig, context);
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
     * 将PluginConfigDTO转换为StrategyConfigDTO
     */
    private StrategyConfigDTO convertToStrategyConfig(PluginConfigDTO pluginConfig) {
        StrategyConfigDTO strategyConfig = new StrategyConfigDTO();
        
        if (pluginConfig != null && pluginConfig.getConfigValues() != null) {
            for (Map.Entry<String, Object> entry : pluginConfig.getConfigValues().entrySet()) {
                strategyConfig.setValue(entry.getKey(), entry.getValue());
            }
        }
        
        return strategyConfig;
    }

    /**
     * 创建变更记录
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
}
