package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.ConfigFieldDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.plugin.StrategyRegistry;
import com.filemanager.plugin.StrategyConfigurable;
import com.filemanager.plugin.util.PreconditionEvaluator;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.enums.common.OutputDirMode;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;
import com.filemanager.plugin.enums.common.CrossDriveMode;
import com.filemanager.plugin.impl.advancedrename.enums.ProcessScope;
import com.filemanager.plugin.impl.albumdirnormalize.enums.DirectoryTemplate;
import com.filemanager.plugin.impl.cuesplitter.enums.AfterSplitAction;
import com.filemanager.plugin.impl.filecleanup.enums.CleanupMode;
import com.filemanager.plugin.impl.filecleanup.enums.DeleteMethod;
import com.filemanager.plugin.impl.filecleanup.enums.FileSizeRange;
import com.filemanager.plugin.impl.filemigrate.enums.OperationMode;
import com.filemanager.plugin.impl.filemigrate.enums.ScopeMode;
import com.filemanager.plugin.impl.filetypefix.enums.TargetFormat;
import com.filemanager.plugin.impl.fileunzip.enums.OutputMode;
import com.filemanager.plugin.impl.fileunzip.enums.UnzipEngine;
import com.filemanager.plugin.impl.metadatascraper.enums.DataSource;
import com.filemanager.plugin.utils.EnumConverter;
import com.filemanager.plugin.utils.EnumOptionProvider;
import com.filemanager.backend.service.ParameterRelationService;
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
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class StrategyServiceImpl implements StrategyService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyServiceImpl.class);
    
    @Autowired
    private ParameterRelationService parameterRelationService;

    private final Map<String, StrategyConfigDTO> strategyConfigs = new ConcurrentHashMap<>();
    private final String configFilePath = "strategy_configs.json";

    private StrategyRegistry strategyRegistry;

    public StrategyServiceImpl() {
        // 初始化策略注册器
        this.strategyRegistry = StrategyRegistry.getInstance();
    }
    
    @PostConstruct
    private void initStrategies() {
        // 初始化并注册内置策略
        initBuiltInStrategies();
        
        // 加载保存的策略配置
        loadStrategyConfigs();
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
                    Map<String, Object> strategyData = entry.getValue();
                    
                    StrategyConfigDTO config = new StrategyConfigDTO();
                    
                    if (strategyData.containsKey("configValues")) {
                        config.setConfigValues((Map<String, Object>) strategyData.get("configValues"));
                    } else {
                        config.setConfigValues(new HashMap<>());
                    }
                    
                    if (strategyData.containsKey("preconditionGroups")) {
                        List<Map<String, Object>> preconditionGroupsData = (List<Map<String, Object>>) strategyData.get("preconditionGroups");
                        List<com.filemanager.domain.dto.PreconditionGroupDTO> preconditionGroups = new ArrayList<>();
                        if (preconditionGroupsData != null) {
                            for (Map<String, Object> groupData : preconditionGroupsData) {
                                com.filemanager.domain.dto.PreconditionGroupDTO group = new com.filemanager.domain.dto.PreconditionGroupDTO();
                                group.setId((String) groupData.get("id"));
                                group.setName((String) groupData.get("name"));
                                group.setDescription((String) groupData.get("description"));
                                group.setLogicType((String) groupData.get("logicType"));
                                
                                if (groupData.containsKey("preconditions")) {
                                    List<Map<String, Object>> preconditionsData = (List<Map<String, Object>>) groupData.get("preconditions");
                                    List<com.filemanager.domain.dto.PreconditionDTO> preconditions = new ArrayList<>();
                                    if (preconditionsData != null) {
                                        for (Map<String, Object> conditionData : preconditionsData) {
                                            com.filemanager.domain.dto.PreconditionDTO condition = new com.filemanager.domain.dto.PreconditionDTO();
                                            condition.setId((String) conditionData.get("id"));
                                            condition.setField((String) conditionData.get("field"));
                                            condition.setSubField((String) conditionData.get("subField"));
                                            
                                            String operatorStr = (String) conditionData.get("operator");
                                            condition.setOperator(com.filemanager.domain.dto.PreconditionDTO.OperatorType.fromValue(operatorStr));
                                            
                                            condition.setValue(conditionData.get("value"));
                                            condition.setDescription((String) conditionData.get("description"));
                                            preconditions.add(condition);
                                        }
                                    }
                                    group.setPreconditions(preconditions);
                                } else {
                                    group.setPreconditions(new ArrayList<>());
                                }
                                preconditionGroups.add(group);
                            }
                        }
                        config.setPreconditionGroups(preconditionGroups);
                    } else {
                        config.setPreconditionGroups(new ArrayList<>());
                    }
                    
                    strategyConfigs.put(strategyId, config);
                    logger.info("[Strategy] 加载策略配置: {}，配置项数量: {}，前置条件组数量: {}", 
                        strategyId, 
                        config.getConfigValues() != null ? config.getConfigValues().size() : 0,
                        config.getPreconditionGroups() != null ? config.getPreconditionGroups().size() : 0);
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
            Map<String, Object> configMap = new HashMap<>();
            for (Map.Entry<String, StrategyConfigDTO> entry : strategyConfigs.entrySet()) {
                String strategyId = entry.getKey();
                StrategyConfigDTO config = entry.getValue();
                Map<String, Object> strategyConfig = new HashMap<>();
                
                if (config.getConfigValues() != null && !config.getConfigValues().isEmpty()) {
                    strategyConfig.put("configValues", config.getConfigValues());
                }
                
                if (config.getPreconditionGroups() != null && !config.getPreconditionGroups().isEmpty()) {
                    strategyConfig.put("preconditionGroups", config.getPreconditionGroups());
                }
                
                if (!strategyConfig.isEmpty()) {
                    configMap.put(strategyId, strategyConfig);
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
        com.filemanager.plugin.impl.advancedrename.AdvancedRenameStrategy advancedRenameStrategy = new com.filemanager.plugin.impl.advancedrename.AdvancedRenameStrategy();
        strategyRegistry.registerStrategy(advancedRenameStrategy);

        // 2. AudioConverterStrategy - 音频格式转换策略
        com.filemanager.plugin.impl.audioconverter.AudioConverterStrategy audioConverterStrategy = new com.filemanager.plugin.impl.audioconverter.AudioConverterStrategy();
        strategyRegistry.registerStrategy(audioConverterStrategy);

        // 3. FileCleanupStrategy - 文件清理与去重策略
        com.filemanager.plugin.impl.filecleanup.FileCleanupStrategy cleanupStrategy = new com.filemanager.plugin.impl.filecleanup.FileCleanupStrategy();
        strategyRegistry.registerStrategy(cleanupStrategy);

        // 4. MetadataScraperStrategy - 元数据抓取策略
        com.filemanager.plugin.impl.metadatascraper.MetadataScraperStrategy metadataScraperStrategy = new com.filemanager.plugin.impl.metadatascraper.MetadataScraperStrategy();
        strategyRegistry.registerStrategy(metadataScraperStrategy);

        // 5. CueSplitterStrategy - CUE分轨策略
        com.filemanager.plugin.impl.cuesplitter.CueSplitterStrategy cueSplitterStrategy = new com.filemanager.plugin.impl.cuesplitter.CueSplitterStrategy();
        strategyRegistry.registerStrategy(cueSplitterStrategy);

        // 6. FileMigrateStrategy - 文件批量归档和移动策略
        com.filemanager.plugin.impl.filemigrate.FileMigrateStrategy fileMigrateStrategy = new com.filemanager.plugin.impl.filemigrate.FileMigrateStrategy();
        strategyRegistry.registerStrategy(fileMigrateStrategy);

        // 7. AlbumDirNormalizeStrategy - 专辑目录标准化策略
        com.filemanager.plugin.impl.albumdirnormalize.AlbumDirNormalizeStrategy albumDirNormalizeStrategy = new com.filemanager.plugin.impl.albumdirnormalize.AlbumDirNormalizeStrategy();
        strategyRegistry.registerStrategy(albumDirNormalizeStrategy);

        // 8. FileUnzipStrategy - 批量智能解压策略
        com.filemanager.plugin.impl.fileunzip.FileUnzipStrategy fileUnzipStrategy = new com.filemanager.plugin.impl.fileunzip.FileUnzipStrategy();
        strategyRegistry.registerStrategy(fileUnzipStrategy);

        // 9. FileCollectionStrategy - 文件收集策略
        com.filemanager.plugin.impl.filecollection.FileCollectionStrategy fileCollectionStrategy = new com.filemanager.plugin.impl.filecollection.FileCollectionStrategy();
        strategyRegistry.registerStrategy(fileCollectionStrategy);

        // 10. FileTypeFixStrategy - 文件类型修复策略
        com.filemanager.plugin.impl.filetypefix.FileTypeFixStrategy fileTypeFixStrategy = new com.filemanager.plugin.impl.filetypefix.FileTypeFixStrategy();
        strategyRegistry.registerStrategy(fileTypeFixStrategy);

        // 11. CueFileRenameStrategy - CUE文件重命名策略
        com.filemanager.plugin.impl.cuefilerename.CueFileRenameStrategy cueFileRenameStrategy = new com.filemanager.plugin.impl.cuefilerename.CueFileRenameStrategy();
        strategyRegistry.registerStrategy(cueFileRenameStrategy);

        // 12. NcmIntegratedStrategy - 网易云音乐集成策略
        com.filemanager.plugin.impl.ncmintegrated.NcmIntegratedStrategy ncmIntegratedStrategy = new com.filemanager.plugin.impl.ncmintegrated.NcmIntegratedStrategy();
        strategyRegistry.registerStrategy(ncmIntegratedStrategy);

        // 13. TrackNumberStrategy - 音轨编号策略
        com.filemanager.plugin.impl.tracknumber.TrackNumberStrategy trackNumberStrategy = new com.filemanager.plugin.impl.tracknumber.TrackNumberStrategy();
        strategyRegistry.registerStrategy(trackNumberStrategy);

        // 14. FileRenameStrategy - 文件重命名策略
        com.filemanager.plugin.impl.filerename.FileRenameStrategy fileRenameStrategy = new com.filemanager.plugin.impl.filerename.FileRenameStrategy();
        strategyRegistry.registerStrategy(fileRenameStrategy);
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
                config.setConfigValues(new HashMap<>());
                config.setPreconditionGroups(new ArrayList<>());
            }
            strategyConfigs.put(strategyId, config);
        }
        logger.info("[Service] 返回策略配置 - strategyId: {}, 配置项数量: {}", strategyId, config.getConfigValues() != null ? config.getConfigValues().size() : 0);
        return config;
    }

    @Override
    public boolean updateStrategyConfig(String strategyId, StrategyConfigDTO config) {
        // 处理参数关系
        Map<String, Object> processedValues = processParameterRelations(strategyId, config.getConfigValues());
        config.setConfigValues(processedValues);
        
        strategyConfigs.put(strategyId, config);
        saveStrategyConfigs();
        return true;
    }
    
    /**
     * 处理参数关系
     * @param strategyId 策略ID
     * @param configValues 配置值
     * @return 处理后的配置值
     */
    private Map<String, Object> processParameterRelations(String strategyId, Map<String, Object> configValues) {
        Map<String, Object> processedValues = new HashMap<>(configValues);
        
        // 获取策略的所有参数
        List<ConfigFieldDTO> allFields = getConfigFieldsByStrategyId(strategyId);
        if (allFields == null || allFields.isEmpty()) {
            return processedValues;
        }
        
        // 设置缓存
        parameterRelationService.setAllFieldsCache(allFields);
        
        // 处理互斥关系
        for (Map.Entry<String, Object> entry : configValues.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            
            // 处理互斥关系
            processedValues = parameterRelationService.handleExclusiveRelation(
                    paramName, paramValue, allFields, processedValues);
            
            // 处理自动填充关系
            processedValues = parameterRelationService.handleAutoFill(
                    paramName, paramValue, allFields, processedValues);
        }
        
        return processedValues;
    }
    
    /**
     * 根据策略ID获取配置字段
     * @param strategyId 策略ID
     * @return 配置字段列表
     */
    private List<ConfigFieldDTO> getConfigFieldsByStrategyId(String strategyId) {
        StrategyInfoDTO strategyInfo = getStrategyInfo(strategyId);
        if (strategyInfo != null) {
            return strategyInfo.getConfigFields();
        }
        return null;
    }

    @Override
    public List<ChangeRecord> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfigDTO config) {
        System.out.println("[Strategy] 开始分析文件: " + strategyId);
        System.out.println("[Strategy] 文件数量: " + (filePaths != null ? filePaths.size() : 0));
        
        // 使用策略实现
        StrategyConfigurable strategy = strategyRegistry.getStrategy(strategyId);
        if (strategy == null) {
            System.out.println("[Strategy] 未找到策略: " + strategyId);
            return new ArrayList<>();
        }
        
        System.out.println("[Strategy] 找到策略: " + strategyId);
        
        // 创建执行上下文
        com.filemanager.plugin.ExecutionContext context = new com.filemanager.plugin.ExecutionContext() {
            private final List<String> logs = new ArrayList<>();
            
            @Override
            public void logInfo(String message) {
                logs.add("[INFO] " + message);
                System.out.println("[INFO] " + message);
            }
            
            @Override
            public void logWarn(String message) {
                logs.add("[WARN] " + message);
                System.out.println("[WARN] " + message);
            }
            
            @Override
            public void logError(String message) {
                logs.add("[ERROR] " + message);
                System.err.println("[ERROR] " + message);
            }
            
            @Override
            public void logDebug(String message) {
                logs.add("[DEBUG] " + message);
                System.out.println("[DEBUG] " + message);
            }
        };
        
        List<ChangeRecord> changes = new ArrayList<>();
        
        try {
            // 将文件路径转换为 File 对象
            List<File> files = new ArrayList<>();
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (file.exists()) {
                    files.add(file);
                } else {
                    context.logWarn("文件不存在: " + filePath);
                }
            }
            
            // 获取根目录列表（用于分析）
            List<File> rootDirs = new ArrayList<>();
            if (!files.isEmpty()) {
                rootDirs.add(files.get(0).getParentFile());
            }
            
            // 调用策略的 analyze 方法
            for (File file : files) {
                ChangeRecord record = new ChangeRecord();
                record.setId("change-" + System.currentTimeMillis() + "-" + file.hashCode());
                record.setOriginalName(file.getName());
                record.setNewName(file.getName());
                record.setFilePath(file.getAbsolutePath());
                record.setFileHandle(file);
                record.setChanged(false);
                record.setStatus("PENDING");
                
                // 调用策略的 analyze 方法
                List<ChangeRecord> analysisResults = strategy.analyze(
                    record,
                    new ArrayList<>(),
                    rootDirs,
                    convertToPluginConfig(config),
                    context
                );
                
                if (analysisResults != null && !analysisResults.isEmpty()) {
                    changes.addAll(analysisResults);
                }
            }
            
            System.out.println("[Strategy] 分析完成，结果数量: " + changes.size());
        } catch (Exception e) {
            System.err.println("[Strategy] 分析异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config) {
        System.out.println("[Strategy] 开始执行策略: " + strategyId);
        System.out.println("[Strategy] 文件数量: " + (filePaths != null ? filePaths.size() : 0));
        System.out.println("[Strategy] 配置项数量: " + (config != null && config.getConfigValues() != null ? config.getConfigValues().size() : 0));
        
        long startTime = System.currentTimeMillis();
        
        StrategyConfigurable strategy = strategyRegistry.getStrategy(strategyId);
        if (strategy == null) {
            System.out.println("[Strategy] 未找到策略: " + strategyId);
            return new ArrayList<>();
        }
        
        System.out.println("[Strategy] 找到策略: " + strategyId);
        
        List<ChangeRecord> changes = new ArrayList<>();
        
        try {
            PluginConfigDTO pluginConfig = convertToPluginConfig(config);
            
            com.filemanager.plugin.ExecutionContext context = new com.filemanager.plugin.ExecutionContext() {
                private final List<String> logs = new ArrayList<>();
                
                @Override
                public void logInfo(String message) {
                    logs.add("[INFO] " + message);
                    System.out.println("[INFO] " + message);
                }
                
                @Override
                public void logWarn(String message) {
                    logs.add("[WARN] " + message);
                    System.out.println("[WARN] " + message);
                }
                
                @Override
                public void logError(String message) {
                    logs.add("[ERROR] " + message);
                    System.err.println("[ERROR] " + message);
                }
                
                @Override
                public void logDebug(String message) {
                    logs.add("[DEBUG] " + message);
                    System.out.println("[DEBUG] " + message);
                }
            };
            
            List<File> files = new ArrayList<>();
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (file.exists()) {
                    files.add(file);
                } else {
                    context.logWarn("文件不存在: " + filePath);
                }
            }
            
            List<File> rootDirs = new ArrayList<>();
            if (!files.isEmpty()) {
                rootDirs.add(files.get(0).getParentFile());
            }
            
            System.out.println("[Strategy] 开始分析阶段");
            for (File file : files) {
                ChangeRecord record = new ChangeRecord();
                record.setId("change-" + System.currentTimeMillis() + "-" + file.hashCode());
                record.setOriginalName(file.getName());
                record.setNewName(file.getName());
                record.setFilePath(file.getAbsolutePath());
                record.setFileHandle(file);
                record.setChanged(false);
                record.setStatus("PENDING");
                
                List<ChangeRecord> analysisResults = strategy.analyze(
                    record,
                    new ArrayList<>(),
                    rootDirs,
                    pluginConfig,
                    context
                );
                
                if (analysisResults != null && !analysisResults.isEmpty()) {
                    changes.addAll(analysisResults);
                }
            }
            System.out.println("[Strategy] 分析完成，结果数量: " + changes.size());
            
            System.out.println("[Strategy] 开始执行阶段");
            for (ChangeRecord record : changes) {
                if (record.isChanged()) {
                    try {
                        strategy.execute(record, pluginConfig, context);
                        record.setStatus("SUCCESS");
                        context.logInfo("执行成功: " + record.getOriginalName() + " -> " + record.getNewName());
                    } catch (Exception e) {
                        record.setStatus("FAILED");
                        context.logError("执行失败: " + record.getOriginalName() + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    record.setStatus("SKIPPED");
                    context.logInfo("跳过: " + record.getOriginalName());
                }
            }
            System.out.println("[Strategy] 执行完成");
            
        } catch (Exception e) {
            System.err.println("[Strategy] 策略执行异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("[Strategy] 策略执行完成: " + strategyId);
        System.out.println("[Strategy] 执行时间: " + (endTime - startTime) + "ms");
        System.out.println("[Strategy] 最终结果数量: " + (changes != null ? changes.size() : 0));
        
        return changes;
    }

    private List<String> filterFilesByPreconditions(List<String> filePaths, StrategyConfigDTO config) {
        if (config == null || config.getPreconditionGroups() == null || config.getPreconditionGroups().isEmpty()) {
            return filePaths;
        }

        return filePaths.stream()
            .filter(filePath -> {
                File file = new File(filePath);
                return PreconditionEvaluator.evaluate(file, config.getPreconditionGroups());
            })
            .collect(Collectors.toList());
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
        if (config != null && config.getConfigValues() != null) {
            for (Map.Entry<String, Object> entry : config.getConfigValues().entrySet()) {
                pluginConfig.setValue(entry.getKey(), entry.getValue());
            }
        }
        return pluginConfig;
    }
}
