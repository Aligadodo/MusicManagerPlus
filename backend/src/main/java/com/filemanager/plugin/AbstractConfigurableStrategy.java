package com.filemanager.plugin;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.enums.ScanTarget;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        return null;
    }

    /**
     * 创建预览记录
     * 子类需要实现此方法来提供预览功能
     * @param filePath 文件路径
     * @param config 配置对象
     * @param context 执行上下文
     * @return 变更记录
     */
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        return null;
    }

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

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        PluginConfigDTO config, 
        ExecutionContext context) {
        
        StrategyConfigDTO strategyConfig = convertToStrategyConfig(config);
        
        // 从context中获取inputRecords和rootDirs
        List<ChangeRecord> inputRecords = context.getInputRecords();
        List<File> rootDirs = context.getRootDirs();
        
        return analyzeWithPreCheck(currentRecord, inputRecords, rootDirs, strategyConfig, context);
    }

    @Override
    public void execute(ChangeRecord record, 
        PluginConfigDTO config, 
        ExecutionContext context) throws Exception {
        
        StrategyConfigDTO strategyConfig = convertToStrategyConfig(config);
        
        // 前置条件检查
        if (!checkPreconditions(record, strategyConfig)) {
            record.setStatus("SKIPPED");
            return;
        }
        
        // 类型检查
        if (ScanTarget.FILES_ONLY == getTargetType() && record.getFileHandle().isDirectory()) {
            record.setStatus("SKIPPED");
            return;
        }
        if (ScanTarget.FOLDERS_ONLY == getTargetType() && record.getFileHandle().isFile()) {
            record.setStatus("SKIPPED");
            return;
        }
        
        // 调用子类的execute方法
        execute(record, strategyConfig, context);
    }

    /**
     * 核心分析方法
     * 
     * @param currentRecord 当前记录
     * @param config 配置对象
     * @param context 执行上下文
     * @return 变更记录列表
     */
    public abstract List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        StrategyConfigDTO config,
        ExecutionContext context);

    /**
     * 核心执行方法
     * 
     * @param record 变更记录
     * @param config 配置对象
     * @param context 执行上下文
     * @throws Exception 执行异常
     */
    public abstract void execute(ChangeRecord record, 
        StrategyConfigDTO config, 
        ExecutionContext context) throws Exception;

    /**
     * 核心分析逻辑 - 带前置检查
     * 
     * @param currentRecord 当前记录
     * @param inputRecords 输入记录列表 [扫描范围内的全量文件]
     * @param rootDirs 根目录列表
     * @param config 配置对象
     * @param context 执行上下文
     * @return 变更记录列表
     */
    public List<ChangeRecord> analyzeWithPreCheck(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs,
        StrategyConfigDTO config,
        ExecutionContext context) {
        
        // 将inputRecords和rootDirs设置到context中
        context.setInputRecords(inputRecords);
        context.setRootDirs(rootDirs);
        
        // 已经变更的文件不支持二次变更
        if (currentRecord.isChanged()) {
            return Collections.emptyList();
        }
        
        // 前置条件检查
        if (!checkPreconditions(currentRecord, config)) {
            return Collections.emptyList();
        }
        
        // 类型检查
        if (ScanTarget.FILES_ONLY == getTargetType() && currentRecord.getFileHandle().isDirectory()) {
            return Collections.emptyList();
        }
        if (ScanTarget.FOLDERS_ONLY == getTargetType() && currentRecord.getFileHandle().isFile()) {
            return Collections.emptyList();
        }
        
        return analyze(currentRecord, config, context);
    }

    /**
     * 前置条件检查
     * 
     * @param record 变更记录
     * @param config 配置对象
     * @return 是否满足前置条件
     */
    protected boolean checkPreconditions(ChangeRecord record, StrategyConfigDTO config) {
        if (config == null || config.getPreconditionGroups() == null || config.getPreconditionGroups().isEmpty()) {
            return true;
        }
        
        File f = record.getFileHandle();
        
        // 只要有一组满足 (组内是AND)，则通过
        for (PreconditionGroupDTO group : config.getPreconditionGroups()) {
            if (testPreconditionGroup(group, f)) {
                return true;
            }
        }
        
        // 所有组都不满足
        return false;
    }

    /**
     * 测试前置条件组
     * 
     * @param group 前置条件组
     * @param f 文件对象
     * @return 是否满足条件组
     */
    protected boolean testPreconditionGroup(PreconditionGroupDTO group, File f) {
        if (group == null || group.getPreconditions() == null || group.getPreconditions().isEmpty()) {
            return true;
        }
        
        // 组内所有条件都要满足 (AND)
        for (PreconditionDTO condition : group.getPreconditions()) {
            if (!testPrecondition(condition, f)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 测试单个前置条件
     * 
     * @param condition 前置条件
     * @param f 文件对象
     * @return 是否满足条件
     */
    protected boolean testPrecondition(PreconditionDTO condition, File f) {
        if (f == null || condition == null) {
            return false;
        }
        
        String name = f.getName();
        String path = f.getAbsolutePath();
        String ext = getExtension(name);
        
        try {
            Object valueObj = condition.getValue();
            String value = valueObj != null ? valueObj.toString() : "";
            
            switch (condition.getOperator()) {
                case CONTAINS:
                    return name.contains(value);
                case NOT_CONTAINS:
                    return !name.contains(value);
                case STARTS_WITH:
                    return name.startsWith(value);
                case ENDS_WITH:
                    return name.endsWith(value);
                case REGEX_MATCH:
                    return name.matches(value);
                case GREATER_THAN:
                    return f.length() > parseSize(value);
                case LESS_THAN:
                    return f.length() < parseSize(value);
                case EQUALS:
                    return name.equals(value);
                case NOT_EQUALS:
                    return !name.equals(value);
                case IN:
                    return checkExtensionList(name, value, true);
                case NOT_IN:
                    return checkExtensionList(name, value, false);
                case IS:
                    return "directory".equalsIgnoreCase(value) && f.isDirectory() || "file".equalsIgnoreCase(value) && f.isFile();
                case IS_NOT:
                    return !("directory".equalsIgnoreCase(value) && f.isDirectory() || "file".equalsIgnoreCase(value) && f.isFile());
                case IS_EMPTY:
                    return f.isDirectory() ? f.list() == null || f.list().length == 0 : f.length() == 0;
                case IS_NOT_EMPTY:
                    return f.isDirectory() ? f.list() != null && f.list().length > 0 : f.length() > 0;
                case HAS_SUBDIRECTORIES:
                    return f.isDirectory() && hasSubdirectories(f);
                case HAS_NO_SUBDIRECTORIES:
                    return f.isDirectory() && !hasSubdirectories(f);
                case DEPTH_GREATER_THAN:
                    return getDepth(f) > Integer.parseInt(value);
                case DEPTH_LESS_THAN:
                    return getDepth(f) < Integer.parseInt(value);
                case FILE_COUNT_GREATER_THAN:
                    return f.isDirectory() && f.list() != null && f.list().length > Integer.parseInt(value);
                case FILE_COUNT_LESS_THAN:
                    return f.isDirectory() && f.list() != null && f.list().length < Integer.parseInt(value);
                case FORMAT_IN:
                    return checkExtensionList(ext, value, true);
                case FORMAT_NOT_IN:
                    return checkExtensionList(ext, value, false);
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    /**
     * 解析文件大小
     */
    private long parseSize(String val) {
        try {
            return (long) (Double.parseDouble(val) * 1024 * 1024);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 检查扩展名列表
     */
    private boolean checkExtensionList(String currentExt, String configStr, boolean matchIfIn) {
        if (configStr == null || configStr.isEmpty()) {
            return false;
        }
        
        String[] exts = configStr.split(",");
        for (String ext : exts) {
            if (ext.trim().equalsIgnoreCase(currentExt)) {
                return matchIfIn;
            }
        }
        
        return !matchIfIn;
    }

    /**
     * 检查目录是否包含子目录
     */
    private boolean hasSubdirectories(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取目录深度
     */
    private int getDepth(File file) {
        int depth = 0;
        File parent = file.getParentFile();
        
        while (parent != null) {
            depth++;
            parent = parent.getParentFile();
        }
        
        return depth;
    }

    /**
     * 获取目标类型
     * 
     * @return 目标类型
     */
    public abstract ScanTarget getTargetType();

    /**
     * IPlugin接口实现：获取默认前置条件组
     * 默认返回空列表，子类可以覆盖此方法提供默认的前置条件
     */
    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }
}
