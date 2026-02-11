package com.filemanager.plugin.demo;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Demo插件：展示如何实现和注册插件扩展
 * 此插件仅作为示例，展示插件开发的基本结构和方法
 */
public class DemoPlugin implements IPlugin {
    private static final String PLUGIN_ID = "demo-plugin";
    private static final String PLUGIN_NAME = "Demo插件";
    private static final String PLUGIN_DESCRIPTION = "插件开发示例，展示如何实现和注册插件扩展";
    private static final String PLUGIN_VERSION = "1.0.0";

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return PLUGIN_DESCRIPTION;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        // 示例配置：包含一个字符串参数和一个布尔参数
        HashMap<String, Object> configValues = new HashMap<>();
        configValues.put("greeting", "Hello, Plugin!");
        configValues.put("enabled", true);
        configValues.put("threshold", 0.8);
        
        return new PluginConfigDTO(configValues);
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        // 定义插件参数，用于前端配置界面
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        // 字符串参数
        parameters.add(new PluginParameterDTO(
                "greeting",
                "问候语",
                "插件的问候语",
                "text",
                "Hello, Plugin!",
                true
        ));
        
        // 布尔参数
        parameters.add(new PluginParameterDTO(
                "enabled",
                "是否启用",
                "是否启用插件功能",
                "boolean",
                true,
                true
        ));
        
        // 数字参数
        parameters.add(new PluginParameterDTO(
                "threshold",
                "阈值",
                "示例阈值参数",
                "number",
                0.8,
                false
        ));
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        // 示例前置条件：仅处理文件
        return Collections.emptyList();
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        // 执行插件逻辑
        return processFiles(filePaths, config, context, true);
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        // 预览插件执行结果
        return processFiles(filePaths, config, context, false);
    }

    /**
     * 处理文件的核心逻辑
     * @param filePaths 文件路径列表
     * @param config 插件配置
     * @param context 执行上下文
     * @param execute 是否实际执行
     * @return 变更记录列表
     */
    private List<ChangeRecord> processFiles(List<String> filePaths, PluginConfigDTO config, ExecutionContext context, boolean execute) {
        List<ChangeRecord> changeRecords = new ArrayList<>();
        
        // 获取配置值
        String greeting = config.getConfigValues().getOrDefault("greeting", "Hello, Plugin!").toString();
        boolean enabled = Boolean.parseBoolean(config.getConfigValues().getOrDefault("enabled", true).toString());
        double threshold = Double.parseDouble(config.getConfigValues().getOrDefault("threshold", 0.8).toString());
        
        // 处理每个文件
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.exists()) {
                // 创建变更记录
                ChangeRecord record = new ChangeRecord();
                record.setOriginalName(file.getName());
                record.setNewName(file.getName());
                record.setFilePath(file.getPath());
                record.setChanged(true);
                record.setStatus(ChangeRecord.ExecStatus.PENDING);
                
                // 添加额外参数
                HashMap<String, Object> extraParams = new HashMap<>();
                extraParams.put("demo_greeting", greeting);
                extraParams.put("demo_enabled", String.valueOf(enabled));
                extraParams.put("demo_threshold", String.valueOf(threshold));
                record.setExtraParams(extraParams);
                
                changeRecords.add(record);
            }
        }
        
        return changeRecords;
    }
}