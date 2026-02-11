package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.collection.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class FileCollectionPlugin implements IPlugin {
    private static final String PLUGIN_ID = "file-collection";
    private static final String PLUGIN_NAME = "文件智能归类";
    private static final String PLUGIN_DESCRIPTION = "基于文件名相似度和特征将文件/文件夹归类到系列合集文件夹中";
    private static final String PLUGIN_VERSION = "1.0.0";

    // 核心组件
    private SimilarityCalculator similarityCalculator;
    private FileCluster fileCluster;
    private CollectionNameGenerator nameGenerator;
    private KeywordFilter keywordFilter;

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
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("similarityThreshold", 0.9);
        configValues.put("collectionSuffix", "【合集】");
        configValues.put("targetType", "FOLDERS_ONLY");
        configValues.put("namingStrategy", "PRECISE");
        configValues.put("mustContainKeywords", "CD,系列,合集");
        configValues.put("mustNotContainKeywords", "下载,Album,群星");

        return new PluginConfigDTO(configValues);
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();

        parameters.add(new PluginParameterDTO(
                "similarityThreshold",
                "相似度阈值",
                "文件相似度阈值（0.0-1.0）",
                "number",
                0.9,
                true
        ));

        parameters.add(new PluginParameterDTO(
                "collectionSuffix",
                "合集文件夹格式",
                "合集文件夹的后缀格式",
                "text",
                "【合集】",
                true
        ));

        parameters.add(new PluginParameterDTO(
                "targetType",
                "目标类型",
                "要处理的目标类型",
                "select",
                "FOLDERS_ONLY",
                true
        ));

        parameters.add(new PluginParameterDTO(
                "namingStrategy",
                "命名策略",
                "合集命名策略",
                "select",
                "PRECISE",
                true
        ));

        parameters.add(new PluginParameterDTO(
                "mustContainKeywords",
                "必须包含关键词",
                "必须包含的关键词，用逗号分隔",
                "text",
                "CD,系列,合集",
                false
        ));

        parameters.add(new PluginParameterDTO(
                "mustNotContainKeywords",
                "不能包含关键词",
                "不能包含的关键词，用逗号分隔",
                "text",
                "下载,Album,群星",
                false
        ));

        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return Collections.emptyList();
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return processFiles(filePaths, config, context, true);
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return processFiles(filePaths, config, context, false);
    }

    private List<ChangeRecord> processFiles(List<String> filePaths, PluginConfigDTO config, ExecutionContext context, boolean execute) {
        initializeComponents(config);

        List<File> files = filePaths.stream()
                .map(File::new)
                .filter(File::exists)
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        // 按父目录分组处理
        Map<File, List<File>> filesByParent = files.stream()
                .collect(Collectors.groupingBy(file -> {
                    File parent = file.getParentFile();
                    return parent != null ? parent : file;
                }));

        List<ChangeRecord> changeRecords = new ArrayList<>();

        for (Map.Entry<File, List<File>> entry : filesByParent.entrySet()) {
            File parentDir = entry.getKey();
            List<File> dirFiles = entry.getValue();

            // 过滤符合条件的文件
            List<File> validFiles = filterValidFiles(dirFiles, config);

            if (validFiles.size() < 2) {
                continue;
            }

            // 聚类文件
            List<List<File>> clusters = clusterFiles(validFiles, config);

            // 为每个集群生成变更记录
            for (List<File> cluster : clusters) {
                if (cluster.size() < 2) {
                    continue;
                }

                // 生成合集名称
                String collectionName = generateCollectionName(cluster, config);
                if (collectionName == null || collectionName.isEmpty()) {
                    continue;
                }

                // 创建合集文件夹
                String collectionSuffix = config.getConfigValues().getOrDefault("collectionSuffix", "【合集】").toString();
                File collectionDir = new File(parentDir, collectionName + collectionSuffix);

                // 为集群中的每个文件生成变更记录
                for (File file : cluster) {
                    ChangeRecord record = createChangeRecord(file, collectionDir, config);
                    if (record != null) {
                        changeRecords.add(record);

                        // 如果是执行模式，实际创建文件夹并移动文件
                        if (execute) {
                            executeCollection(file, collectionDir);
                        }
                    }
                }
            }
        }

        return changeRecords;
    }

    private void initializeComponents(PluginConfigDTO config) {
        double similarityThreshold = Double.parseDouble(
                config.getConfigValues().getOrDefault("similarityThreshold", 0.9).toString()
        );

        similarityCalculator = new SimilarityCalculator(similarityThreshold);
        fileCluster = new FileCluster(similarityCalculator);
        nameGenerator = new CollectionNameGenerator();
        keywordFilter = KeywordFilter.builder().build();
    }

    private List<File> filterValidFiles(List<File> files, PluginConfigDTO config) {
        String mustContainKeywords = config.getConfigValues().getOrDefault("mustContainKeywords", "").toString();
        String mustNotContainKeywords = config.getConfigValues().getOrDefault("mustNotContainKeywords", "").toString();
        String targetType = config.getConfigValues().getOrDefault("targetType", "FOLDERS_ONLY").toString();

        return files.stream()
                .filter(file -> {
                    // 检查目标类型
                    if ("FOLDERS_ONLY".equals(targetType) && !file.isDirectory()) {
                        return false;
                    } else if ("FILES_ONLY".equals(targetType) && !file.isFile()) {
                        return false;
                    }

                    // 检查关键词
                    String fileName = file.getName();
                    return checkKeywords(fileName, mustContainKeywords, mustNotContainKeywords);
                })
                .collect(Collectors.toList());
    }

    private boolean checkKeywords(String fileName, String mustContainKeywords, String mustNotContainKeywords) {
        if (mustContainKeywords.isEmpty() && mustNotContainKeywords.isEmpty()) {
            return true;
        }

        KeywordFilter filter = KeywordFilter.builder()
                .addMustIncludeKeywords(Arrays.asList(mustContainKeywords.split("[,;]")).stream()
                        .filter(k -> !k.trim().isEmpty())
                        .collect(Collectors.toList()))
                .addMustNotIncludeKeywords(Arrays.asList(mustNotContainKeywords.split("[,;]")).stream()
                        .filter(k -> !k.trim().isEmpty())
                        .collect(Collectors.toList()))
                .build();

        return filter.matches(fileName);
    }

    private List<List<File>> clusterFiles(List<File> files, PluginConfigDTO config) {
        return fileCluster.cluster(files);
    }

    private String generateCollectionName(List<File> cluster, PluginConfigDTO config) {
        List<String> fileNames = cluster.stream()
                .map(File::getName)
                .collect(Collectors.toList());

        return CollectionNameGenerator.generateCollectionName(fileNames);
    }

    private ChangeRecord createChangeRecord(File sourceFile, File targetDir, PluginConfigDTO config) {
        ChangeRecord record = new ChangeRecord();
        record.setOriginalName(sourceFile.getName());
        record.setNewName(sourceFile.getName());
        record.setFilePath(sourceFile.getPath());
        record.setChanged(true);
        record.setOperationType(ChangeRecord.OperationType.MOVE);
        record.setStatus(ChangeRecord.ExecStatus.PENDING);

        // 添加额外参数
        Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("collection_name", targetDir.getName());
        extraParams.put("cluster_size", config.getConfigValues().getOrDefault("clusterSize", 0));
        extraParams.put("similarity_threshold", config.getConfigValues().getOrDefault("similarityThreshold", 0.9));
        extraParams.put("target_path", new File(targetDir, sourceFile.getName()).getPath());
        record.setExtraParams(extraParams);

        return record;
    }

    private void executeCollection(File sourceFile, File targetDir) {
        try {
            // 创建合集文件夹
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // 这里可以添加实际的文件移动逻辑
            // 由于这只是示例，实际实现需要考虑文件移动的各种情况
        } catch (Exception e) {
            // 记录错误
            System.err.println("Error creating collection: " + e.getMessage());
        }
    }
}