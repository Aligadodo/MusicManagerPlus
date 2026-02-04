package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.plugin.PluginRegistry;
import com.filemanager.plugin.StrategyRegistry;
import com.filemanager.plugin.StrategyConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Service
public class StrategyServiceImpl implements StrategyService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyServiceImpl.class);

    private final Map<String, StrategyConfigDTO> strategyConfigs = new ConcurrentHashMap<>();
    private final String configFilePath = "strategy_configs.json";

    @Autowired
    private PluginRegistry pluginRegistry;

    private StrategyRegistry strategyRegistry;

    public StrategyServiceImpl() {
        // 初始化策略注册器
        this.strategyRegistry = StrategyRegistry.getInstance();
    }
    
    @PostConstruct
    private void initStrategies() {
        // 初始化并注册内置策略
        initBuiltInStrategies();
        
        // 从插件注册表加载策略
        initPluginStrategies();
        
        // 加载保存的策略配置
        loadStrategyConfigs();
    }

    private void initPluginStrategies() {
        // 从插件注册表加载插件并转换为可配置策略
        List<com.filemanager.plugin.IPlugin> plugins = pluginRegistry.getAvailablePlugins();
        for (com.filemanager.plugin.IPlugin plugin : plugins) {
            // 创建插件到StrategyConfigurable的适配器
            PluginToStrategyAdapter adapter = new PluginToStrategyAdapter(plugin);
            strategyRegistry.registerStrategy(adapter);
            logger.info("[Strategy] 加载插件并注册为策略: {} v{}", plugin.getName(), plugin.getVersion());
        }
    }

    /**
     * 插件到StrategyConfigurable的适配器类
     * 使插件能够被当作可配置策略使用
     */
    private static class PluginToStrategyAdapter implements StrategyConfigurable {
        private final com.filemanager.plugin.IPlugin plugin;

        public PluginToStrategyAdapter(com.filemanager.plugin.IPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getId() {
            return plugin.getId();
        }

        @Override
        public String getName() {
            return plugin.getName();
        }

        @Override
        public String getDescription() {
            return plugin.getDescription();
        }

        @Override
        public String getVersion() {
            return plugin.getVersion();
        }

        @Override
        public List<ConfigFieldDTO> getConfigFields() {
            List<ConfigFieldDTO> fields = new ArrayList<>();
            List<Map<String, Object>> parameters = plugin.getParameters();
            if (parameters != null) {
                for (Map<String, Object> param : parameters) {
                    ConfigFieldDTO field = new ConfigFieldDTO();
                    field.setName((String) param.get("name"));
                    field.setLabel((String) param.get("label"));
                    field.setType((String) param.get("type"));
                    field.setDefaultValue(param.get("defaultValue"));
                    field.setDescription((String) param.get("description"));
                    field.setRequired((Boolean) param.get("required"));
                    
                    // 处理选项字段
                    if (param.containsKey("options")) {
                        Object optionsObj = param.get("options");
                        if (optionsObj instanceof List) {
                            field.setOptions((List<String>) optionsObj);
                        }
                    }
                    
                    fields.add(field);
                }
            }
            return fields;
        }

        @Override
        public StrategyConfigDTO initializeDefaultConfig() {
            StrategyConfigDTO config = new StrategyConfigDTO();
            com.filemanager.domain.dto.PluginConfigDTO pluginConfig = plugin.getDefaultConfig();
            if (pluginConfig != null && pluginConfig.getConfigMap() != null) {
                config.setConfigValues(pluginConfig.getConfigMap());
            } else {
                config.setConfigValues(new HashMap<>());
            }
            return config;
        }

        @Override
        public boolean validateConfig(StrategyConfigDTO config) {
            // 简单验证：配置不为null即可
            return config != null;
        }

        @Override
        public <T> T getConfigValue(StrategyConfigDTO config, String key, T defaultValue) {
            if (config == null || config.getConfigValues() == null) {
                return defaultValue;
            }
            Object value = config.getConfigValues().get(key);
            return value != null ? (T) value : defaultValue;
        }

        @Override
        public void setConfigValue(StrategyConfigDTO config, String key, Object value) {
            if (config == null) {
                config = new StrategyConfigDTO();
            }
            if (config.getConfigValues() == null) {
                config.setConfigValues(new HashMap<>());
            }
            config.getConfigValues().put(key, value);
        }
    }

    private void loadStrategyConfigs() {
        try {
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                logger.info("[Strategy] 找到配置文件，开始加载: {}", configFilePath);
                FileReader reader = new FileReader(configFile);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Map<String, Object>> configMap = mapper.readValue(reader, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Map<String, Object>>>() {});
                reader.close();
                
                for (Map.Entry<String, Map<String, Object>> entry : configMap.entrySet()) {
                    String strategyId = entry.getKey();
                    Map<String, Object> configValues = entry.getValue();
                    StrategyConfigDTO config = new StrategyConfigDTO();
                    config.setConfigValues(configValues);
                    strategyConfigs.put(strategyId, config);
                    logger.info("[Strategy] 加载策略配置: {}，配置项数量: {}", strategyId, configValues.size());
                }
                logger.info("[Strategy] 配置加载成功，共加载 {} 个策略配置", configMap.size());
            } else {
                logger.info("[Strategy] 配置文件不存在，使用默认配置: {}", configFilePath);
            }
        } catch (Exception e) {
            logger.error("[Strategy] 配置加载失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveStrategyConfigs() {
        try {
            Map<String, Map<String, Object>> configMap = new HashMap<>();
            for (Map.Entry<String, StrategyConfigDTO> entry : strategyConfigs.entrySet()) {
                String strategyId = entry.getKey();
                StrategyConfigDTO config = entry.getValue();
                if (config.getConfigValues() != null && !config.getConfigValues().isEmpty()) {
                    configMap.put(strategyId, config.getConfigValues());
                }
            }
            
            File configFile = new File(configFilePath);
            FileWriter writer = new FileWriter(configFile);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writeValue(writer, configMap);
            writer.close();
            logger.info("[Strategy] 配置保存成功，共保存 {} 个策略配置", configMap.size());
        } catch (Exception e) {
            logger.error("[Strategy] 配置保存失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initBuiltInStrategies() {
        // 1. AdvancedRenameStrategy - 高级重命名策略
        com.filemanager.plugin.impl.AdvancedRenameStrategy advancedRenameStrategy = new com.filemanager.plugin.impl.AdvancedRenameStrategy();
        strategyRegistry.registerStrategy(advancedRenameStrategy);

        // 2. AudioConverterStrategy - 音频格式转换策略
        com.filemanager.plugin.impl.AudioConverterStrategy audioConverterStrategy = new com.filemanager.plugin.impl.AudioConverterStrategy();
        strategyRegistry.registerStrategy(audioConverterStrategy);

        // 3. FileCleanupStrategy - 文件清理与去重策略
        com.filemanager.plugin.impl.FileCleanupStrategy cleanupStrategy = new com.filemanager.plugin.impl.FileCleanupStrategy();
        strategyRegistry.registerStrategy(cleanupStrategy);

        // 4. MetadataScraperStrategy - 元数据抓取策略
        com.filemanager.plugin.impl.MetadataScraperStrategy metadataScraperStrategy = new com.filemanager.plugin.impl.MetadataScraperStrategy();
        strategyRegistry.registerStrategy(metadataScraperStrategy);

        // 5. CueSplitterStrategy - CUE分轨策略
        com.filemanager.plugin.impl.CueSplitterStrategy cueSplitterStrategy = new com.filemanager.plugin.impl.CueSplitterStrategy();
        strategyRegistry.registerStrategy(cueSplitterStrategy);

        // 6. FileMigrateStrategy - 文件批量归档和移动策略
        com.filemanager.plugin.impl.FileMigrateStrategy fileMigrateStrategy = new com.filemanager.plugin.impl.FileMigrateStrategy();
        strategyRegistry.registerStrategy(fileMigrateStrategy);

        // 7. AlbumDirNormalizeStrategy - 专辑目录标准化策略
        com.filemanager.plugin.impl.AlbumDirNormalizeStrategy albumDirNormalizeStrategy = new com.filemanager.plugin.impl.AlbumDirNormalizeStrategy();
        strategyRegistry.registerStrategy(albumDirNormalizeStrategy);

        // 8. FileUnzipStrategy - 批量智能解压策略
        com.filemanager.plugin.impl.FileUnzipStrategy fileUnzipStrategy = new com.filemanager.plugin.impl.FileUnzipStrategy();
        strategyRegistry.registerStrategy(fileUnzipStrategy);

        // 9. FileCollectionStrategy - 文件收集策略
        com.filemanager.plugin.impl.FileCollectionStrategy fileCollectionStrategy = new com.filemanager.plugin.impl.FileCollectionStrategy();
        strategyRegistry.registerStrategy(fileCollectionStrategy);

        // 10. FileTypeFixStrategy - 文件类型修复策略
        com.filemanager.plugin.impl.FileTypeFixStrategy fileTypeFixStrategy = new com.filemanager.plugin.impl.FileTypeFixStrategy();
        strategyRegistry.registerStrategy(fileTypeFixStrategy);

        // 11. CueFileRenameStrategy - CUE文件重命名策略
        com.filemanager.plugin.impl.CueFileRenameStrategy cueFileRenameStrategy = new com.filemanager.plugin.impl.CueFileRenameStrategy();
        strategyRegistry.registerStrategy(cueFileRenameStrategy);

        // 12. NcmIntegratedStrategy - 网易云音乐集成策略
        com.filemanager.plugin.impl.NcmIntegratedStrategy ncmIntegratedStrategy = new com.filemanager.plugin.impl.NcmIntegratedStrategy();
        strategyRegistry.registerStrategy(ncmIntegratedStrategy);
    }



    @Override
    public List<StrategyInfoDTO> getAvailableStrategies() {
        List<StrategyInfoDTO> strategyInfos = new ArrayList<>();
        List<StrategyConfigurable> strategies = strategyRegistry.getStrategies();
        for (StrategyConfigurable strategy : strategies) {
            StrategyInfoDTO info = new StrategyInfoDTO();
            info.setId(strategy.getId());
            info.setName(strategy.getName());
            info.setDescription(strategy.getDescription());
            info.setEnabled(true);
            info.setConfigFields(strategy.getConfigFields());
            strategyInfos.add(info);
        }
        return strategyInfos;
    }

    @Override
    public StrategyInfoDTO getStrategyInfo(String strategyId) {
        StrategyConfigurable strategy = strategyRegistry.getStrategy(strategyId);
        if (strategy == null) {
            return null;
        }
        
        StrategyInfoDTO info = new StrategyInfoDTO();
        info.setId(strategy.getId());
        info.setName(strategy.getName());
        info.setDescription(strategy.getDescription());
        info.setEnabled(true);
        info.setConfigFields(strategy.getConfigFields());
        return info;
    }

    @Override
    public StrategyConfigDTO getStrategyConfig(String strategyId) {
        logger.info("[Service] 获取策略配置 - strategyId: {}", strategyId);
        StrategyConfigDTO config = strategyConfigs.get(strategyId);
        if (config == null) {
            logger.info("[Service] 策略配置不存在，创建默认配置 - strategyId: {}", strategyId);
            // 尝试从策略注册器获取策略并初始化默认配置
            StrategyConfigurable strategy = strategyRegistry.getStrategy(strategyId);
            if (strategy != null) {
                config = strategy.initializeDefaultConfig();
            } else {
                // 如果策略不存在，创建空配置
                config = new StrategyConfigDTO();
            }
            strategyConfigs.put(strategyId, config);
        }
        logger.info("[Service] 返回策略配置 - strategyId: {}, 配置项数量: {}", strategyId, config.getConfigValues() != null ? config.getConfigValues().size() : 0);
        return config;
    }

    @Override
    public boolean updateStrategyConfig(String strategyId, StrategyConfigDTO config) {
        strategyConfigs.put(strategyId, config);
        saveStrategyConfigs();
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
            record.setStatus("PENDING");
            changes.add(record);
        }
        return changes;
    }

    @Override
    public List<ChangeRecord> executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config) {
        System.out.println("[Strategy] 开始执行策略: " + strategyId);
        System.out.println("[Strategy] 文件数量: " + (filePaths != null ? filePaths.size() : 0));
        System.out.println("[Strategy] 配置项数量: " + (config != null && config.getConfigValues() != null ? config.getConfigValues().size() : 0));
        
        long startTime = System.currentTimeMillis();
        
        // 尝试从插件系统获取对应的插件
        System.out.println("[Strategy] 开始查找插件: " + strategyId);
        com.filemanager.plugin.IPlugin plugin = pluginRegistry.getPlugin(strategyId);
        
        List<ChangeRecord> changes = new ArrayList<>();
        
        if (plugin != null) {
            System.out.println("[Strategy] 找到插件: " + strategyId);
            // 转换配置为插件配置
            System.out.println("[Strategy] 转换配置为插件配置");
            com.filemanager.domain.dto.PluginConfigDTO pluginConfig = convertToPluginConfig(config);
            System.out.println("[Strategy] 配置转换完成，开始执行插件");
            
            try {
                changes = plugin.execute(filePaths, pluginConfig, new com.filemanager.plugin.ExecutionContext());
                System.out.println("[Strategy] 插件执行完成，结果数量: " + (changes != null ? changes.size() : 0));
                
                // 更新执行状态
                if (changes != null) {
                    for (ChangeRecord record : changes) {
                        record.setStatus("SUCCESS");
                    }
                    System.out.println("[Strategy] 执行状态更新完成");
                }
            } catch (Exception e) {
                System.err.println("[Strategy] 插件执行异常: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } else {
            System.out.println("[Strategy] 未找到插件，使用默认实现: " + strategyId);
            // 如果没有对应的插件，使用默认实现
            try {
                changes = analyzeFiles(strategyId, filePaths, config);
                System.out.println("[Strategy] 默认实现执行完成，结果数量: " + (changes != null ? changes.size() : 0));
                
                if (changes != null) {
                    for (ChangeRecord record : changes) {
                        record.setStatus("SUCCESS");
                    }
                    System.out.println("[Strategy] 执行状态更新完成");
                }
            } catch (Exception e) {
                System.err.println("[Strategy] 默认实现执行异常: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("[Strategy] 策略执行完成: " + strategyId);
        System.out.println("[Strategy] 执行时间: " + (endTime - startTime) + "ms");
        System.out.println("[Strategy] 最终结果数量: " + (changes != null ? changes.size() : 0));
        
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
            case "file-migrate":
                String migrateDir = (String) config.getValue("outputPath");
                if (migrateDir == null) {
                    migrateDir = "Archive";
                }
                String migrateFileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                return migrateDir + "/" + migrateFileName;
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
