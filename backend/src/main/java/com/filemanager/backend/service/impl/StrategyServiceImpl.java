package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.plugin.PluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private void initBuiltInStrategies() {
        // 文件收集策略
        StrategyInfoDTO collectionStrategy = new StrategyInfoDTO();
        collectionStrategy.setId("file-collection");
        collectionStrategy.setName("文件收集策略");
        collectionStrategy.setDescription("根据配置规则收集和整理文件");
        collectionStrategy.setEnabled(true);
        strategies.put(collectionStrategy.getId(), collectionStrategy);

        // 元数据抓取策略
        StrategyInfoDTO metadataStrategy = new StrategyInfoDTO();
        metadataStrategy.setId("metadata-scraper");
        metadataStrategy.setName("元数据抓取策略");
        metadataStrategy.setDescription("从网络或本地抓取并更新文件的元数据信息");
        metadataStrategy.setEnabled(true);
        strategies.put(metadataStrategy.getId(), metadataStrategy);

        // 文件清理策略
        StrategyInfoDTO cleanupStrategy = new StrategyInfoDTO();
        cleanupStrategy.setId("file-cleanup");
        cleanupStrategy.setName("文件清理策略");
        cleanupStrategy.setDescription("根据配置规则清理不需要的文件");
        cleanupStrategy.setEnabled(true);
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
                    config.setValue("sources", List.of("discogs", "musicbrainz"));
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
        // 模拟分析结果
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
        // 模拟执行结果
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
}
