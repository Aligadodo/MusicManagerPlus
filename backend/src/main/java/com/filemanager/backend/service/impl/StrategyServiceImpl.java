package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.plugin.PluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StrategyServiceImpl implements StrategyService {

    private final Map<String, StrategyConfigDTO> strategyConfigs = new ConcurrentHashMap<>();
    private final Map<String, StrategyInfoDTO> strategies = new ConcurrentHashMap<>();

    @Autowired
    private PluginRegistry pluginRegistry;

    public StrategyServiceImpl() {
        // 初始化内置策略
        initBuiltInStrategies();
    }
    
    @PostConstruct
    private void initPluginStrategies() {
        // 从插件注册表加载策略
        List<com.filemanager.plugin.IPlugin> plugins = pluginRegistry.getAvailablePlugins();
        for (com.filemanager.plugin.IPlugin plugin : plugins) {
            StrategyInfoDTO strategy = new StrategyInfoDTO();
            strategy.setId(plugin.getId());
            strategy.setName(plugin.getName());
            strategy.setDescription(plugin.getDescription());
            strategy.setEnabled(true);
            
            // 转换插件参数为策略配置字段
            List<ConfigFieldDTO> configFields = new ArrayList<>();
            if (plugin.getParameters() != null) {
                for (com.filemanager.domain.dto.PluginParameterDTO param : plugin.getParameters()) {
                    ConfigFieldDTO field = new ConfigFieldDTO(
                        param.getName(),
                        param.getLabel(),
                        param.getType(),
                        param.getDefaultValue(),
                        param.getDescription(),
                        param.isRequired()
                    );
                    configFields.add(field);
                }
            }
            strategy.setConfigFields(configFields);
            strategies.put(strategy.getId(), strategy);
        }
    }

    private void initBuiltInStrategies() {
        // 文件收集策略
        StrategyInfoDTO collectionStrategy = new StrategyInfoDTO();
        collectionStrategy.setId("file-collection");
        collectionStrategy.setName("文件收集策略");
        collectionStrategy.setDescription("根据配置规则收集和整理文件");
        collectionStrategy.setEnabled(true);
        collectionStrategy.setConfigFields(Arrays.asList(
            new ConfigFieldDTO("targetDirectory", "目标目录", "directory", "/tmp/collected", "文件收集的目标目录", true),
            new ConfigFieldDTO("recursive", "递归收集", "boolean", true, "是否递归收集子目录中的文件", false)
        ));
        strategies.put(collectionStrategy.getId(), collectionStrategy);

        // 元数据抓取策略
        StrategyInfoDTO metadataStrategy = new StrategyInfoDTO();
        metadataStrategy.setId("metadata-scraper");
        metadataStrategy.setName("元数据抓取策略");
        metadataStrategy.setDescription("从网络或本地抓取并更新文件的元数据信息");
        metadataStrategy.setEnabled(true);
        metadataStrategy.setConfigFields(Arrays.asList(
            new ConfigFieldDTO("sources", "数据源", "select", "discogs", "元数据数据源", true),
            new ConfigFieldDTO("updateTags", "更新标签", "boolean", true, "是否更新文件标签", false)
        ));
        strategies.put(metadataStrategy.getId(), metadataStrategy);

        // 文件清理策略
        StrategyInfoDTO cleanupStrategy = new StrategyInfoDTO();
        cleanupStrategy.setId("file-cleanup");
        cleanupStrategy.setName("文件清理策略");
        cleanupStrategy.setDescription("根据配置规则清理不需要的文件");
        cleanupStrategy.setEnabled(true);
        cleanupStrategy.setConfigFields(Arrays.asList(
            new ConfigFieldDTO("maxFileAgeDays", "最大文件天数", "number", 30, "超过此天数的文件将被清理", false),
            new ConfigFieldDTO("deleteEmptyDirectories", "删除空目录", "boolean", true, "是否删除空目录", false)
        ));
        strategies.put(cleanupStrategy.getId(), cleanupStrategy);
    }

    @Override
    public List<StrategyInfoDTO> getAvailableStrategies() {
        return new ArrayList<>(strategies.values());
    }

    @Override
    public StrategyInfoDTO getStrategyInfo(String strategyId) {
        return strategies.get(strategyId);
    }

    @Override
    public StrategyConfigDTO getStrategyConfig(String strategyId) {
        StrategyConfigDTO config = strategyConfigs.get(strategyId);
        if (config == null) {
            config = new StrategyConfigDTO();
            // 设置默认配置
            switch (strategyId) {
                case "file-collection":
                    config.setValue("targetDirectory", "/tmp/collected");
                    config.setValue("recursive", true);
                    break;
                case "metadata-scraper":
                    config.setValue("sources", Arrays.asList("discogs", "musicbrainz"));
                    config.setValue("updateTags", true);
                    break;
                case "file-cleanup":
                    config.setValue("maxFileAgeDays", 30);
                    config.setValue("deleteEmptyDirectories", true);
                    break;
            }
            strategyConfigs.put(strategyId, config);
        }
        return config;
    }

    @Override
    public boolean updateStrategyConfig(String strategyId, StrategyConfigDTO config) {
        strategyConfigs.put(strategyId, config);
        return true;
    }

    @Override
    public List<ChangeRecord> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfigDTO config) {
        // 尝试从插件系统获取对应的插件
        com.filemanager.plugin.IPlugin plugin = pluginRegistry.getPlugin(strategyId);
        if (plugin != null) {
            // 转换配置为插件配置
            com.filemanager.domain.dto.PluginConfigDTO pluginConfig = convertToPluginConfig(config);
            return plugin.execute(filePaths, pluginConfig, new com.filemanager.plugin.ExecutionContext());
        }
        
        // 如果没有对应的插件，使用默认实现
        List<ChangeRecord> changes = new ArrayList<>();
        for (String filePath : filePaths) {
            ChangeRecord record = new ChangeRecord();
            record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
            record.setOriginalName(filePath);
            record.setNewName(getTargetPath(filePath, strategyId, config));
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        return changes;
    }

    @Override
    public List<ChangeRecord> executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config) {
        // 尝试从插件系统获取对应的插件
        com.filemanager.plugin.IPlugin plugin = pluginRegistry.getPlugin(strategyId);
        if (plugin != null) {
            // 转换配置为插件配置
            com.filemanager.domain.dto.PluginConfigDTO pluginConfig = convertToPluginConfig(config);
            List<ChangeRecord> changes = plugin.execute(filePaths, pluginConfig, new com.filemanager.plugin.ExecutionContext());
            // 更新执行状态
            for (ChangeRecord record : changes) {
                record.setStatus(ChangeRecord.ExecStatus.SUCCESS);
            }
            return changes;
        }
        
        // 如果没有对应的插件，使用默认实现
        List<ChangeRecord> changes = analyzeFiles(strategyId, filePaths, config);
        for (ChangeRecord record : changes) {
            record.setStatus(ChangeRecord.ExecStatus.SUCCESS);
        }
        return changes;
    }

    private String getTargetPath(String filePath, String strategyId, StrategyConfigDTO config) {
        switch (strategyId) {
            case "file-collection":
                String targetDir = (String) config.getValue("targetDirectory");
                if (targetDir == null) {
                    targetDir = "/tmp/collected";
                }
                String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                return targetDir + "/" + fileName;
            case "metadata-scraper":
                return filePath; // 元数据策略不改变文件名
            case "file-cleanup":
                return null; // 清理策略删除文件
            default:
                return filePath;
        }
    }

    private com.filemanager.domain.dto.PluginConfigDTO convertToPluginConfig(StrategyConfigDTO config) {
        com.filemanager.domain.dto.PluginConfigDTO pluginConfig = new com.filemanager.domain.dto.PluginConfigDTO();
        if (config.getConfigValues() != null) {
            for (Map.Entry<String, Object> entry : config.getConfigValues().entrySet()) {
                pluginConfig.setValue(entry.getKey(), entry.getValue());
            }
        }
        return pluginConfig;
    }
}
