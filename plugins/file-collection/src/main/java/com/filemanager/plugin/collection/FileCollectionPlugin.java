package com.filemanager.plugin.collection;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class FileCollectionPlugin implements IPlugin {
    
    private static final String DEFAULT_TARGET_DIRECTORY = "/tmp/collected";
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final String DEFAULT_NAMING_STRATEGY = "exact";
    private static final int DEFAULT_MIN_CLUSTER_SIZE = 2;
    
    @Override
    public String getId() {
        return "file-collection";
    }

    @Override
    public String getName() {
        return "文件收集插件";
    }

    @Override
    public String getDescription() {
        return "根据配置规则收集和整理文件，支持相似度聚类、命名策略和关键词过滤";
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("targetDirectory", DEFAULT_TARGET_DIRECTORY);
        config.setValue("recursive", true);
        config.setValue("includePatterns", Arrays.asList("*.mp3", "*.wav", "*.flac"));
        config.setValue("excludePatterns", Arrays.asList("*.tmp", "*.log"));
        config.setValue("similarityThreshold", DEFAULT_SIMILARITY_THRESHOLD);
        config.setValue("namingStrategy", DEFAULT_NAMING_STRATEGY);
        config.setValue("minClusterSize", DEFAULT_MIN_CLUSTER_SIZE);
        config.setValue("mustIncludeKeywords", "");
        config.setValue("mustNotIncludeKeywords", "");
        config.setValue("caseSensitive", false);
        config.setValue("useRegex", false);
        config.setValue("autoDetectKeywords", true);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        parameters.add(new PluginParameterDTO(
            "targetDirectory",
            "目标目录",
            "文件收集的目标目录",
            "directory",
            DEFAULT_TARGET_DIRECTORY,
            true
        ));
        
        parameters.add(new PluginParameterDTO(
            "recursive",
            "递归收集",
            "是否递归收集子目录中的文件",
            "boolean",
            true,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "includePatterns",
            "包含模式",
            "要收集的文件模式列表，多个模式用逗号分隔",
            "text",
            "*.mp3,*.wav,*.flac",
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "excludePatterns",
            "排除模式",
            "要排除的文件模式列表，多个模式用逗号分隔",
            "text",
            "*.tmp,*.log",
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "similarityThreshold",
            "相似度阈值",
            "文件聚类的相似度阈值（0.0-1.0）",
            "number",
            DEFAULT_SIMILARITY_THRESHOLD,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "namingStrategy",
            "命名策略",
            "合集文件夹的命名策略（exact/simple/template/universal）",
            "enum",
            DEFAULT_NAMING_STRATEGY,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "minClusterSize",
            "最小合集大小",
            "合集的最小文件数量",
            "number",
            DEFAULT_MIN_CLUSTER_SIZE,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "mustIncludeKeywords",
            "必须包含关键词",
            "文件名必须包含的关键词，多个关键词用逗号分隔",
            "text",
            "",
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "mustNotIncludeKeywords",
            "不能包含关键词",
            "文件名不能包含的关键词，多个关键词用逗号分隔",
            "text",
            "",
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "caseSensitive",
            "区分大小写",
            "关键词匹配是否区分大小写",
            "boolean",
            false,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "useRegex",
            "使用正则表达式",
            "关键词是否使用正则表达式",
            "boolean",
            false,
            false
        ));
        
        parameters.add(new PluginParameterDTO(
            "autoDetectKeywords",
            "自动检测关键词",
            "是否自动从文件名中检测关键词",
            "boolean",
            true,
            false
        ));
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("文件收集的默认前置条件");
        group.setLogicType(PreconditionGroupDTO.LogicType.AND);
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        PreconditionDTO existCondition = new PreconditionDTO();
        existCondition.setId("exist-condition");
        existCondition.setField("fileExists");
        existCondition.setOperator(PreconditionDTO.OperatorType.EQUALS);
        existCondition.setValue(true);
        existCondition.setDescription("文件存在");
        preconditions.add(existCondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        try {
            // 开始文件收集，共 " + filePaths.size() + " 个文件
            
            String targetDirectory = (String) config.getValue("targetDirectory");
            if (targetDirectory == null) {
                targetDirectory = DEFAULT_TARGET_DIRECTORY;
            }
            
            boolean recursive = (Boolean) config.getValue("recursive", true);
            double similarityThreshold = (Double) config.getValue("similarityThreshold", DEFAULT_SIMILARITY_THRESHOLD);
            String namingStrategyId = (String) config.getValue("namingStrategy", DEFAULT_NAMING_STRATEGY);
            int minClusterSize = (Integer) config.getValue("minClusterSize", DEFAULT_MIN_CLUSTER_SIZE);
            boolean autoDetectKeywords = (Boolean) config.getValue("autoDetectKeywords", true);
            
            KeywordFilter keywordFilter = buildKeywordFilter(config);
            
            List<String> filteredFiles = filterFiles(filePaths, keywordFilter, context);
            
            // 过滤后剩余 " + filteredFiles.size() + " 个文件
            
            if (filteredFiles.isEmpty()) {
            // 没有符合条件的文件
                return changes;
            }
            
            List<FileCluster> clusters = FileClusterer.clusterFiles(filteredFiles, similarityThreshold);
            
            // 聚类后生成 " + clusters.size() + " 个合集
            
            List<FileCluster> optimizedClusters = FileClusterer.optimizeClusters(clusters, minClusterSize);
            
            // 优化后剩余 " + optimizedClusters.size() + " 个合集
            
            NamingStrategy namingStrategy = NamingStrategyFactory.getStrategy(namingStrategyId);
            
            for (FileCluster cluster : optimizedClusters) {
                String clusterName = generateClusterName(cluster, namingStrategy, config, context);
                String clusterPath = targetDirectory + File.separator + clusterName;
                
                createClusterDirectory(clusterPath, context);
                
                for (String filePath : cluster.getFilePaths()) {
                    ChangeRecord record = processFile(filePath, clusterPath, context);
                    changes.add(record);
                }
            }
            
            // 文件收集完成，共生成 " + changes.size() + " 条变更记录
            
        } catch (Exception e) {
            // 文件收集失败: " + e.getMessage()
            e.printStackTrace();
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        try {
            // 开始预览文件收集，共 " + filePaths.size() + " 个文件
            
            String targetDirectory = (String) config.getValue("targetDirectory");
            if (targetDirectory == null) {
                targetDirectory = DEFAULT_TARGET_DIRECTORY;
            }
            
            double similarityThreshold = (Double) config.getValue("similarityThreshold", DEFAULT_SIMILARITY_THRESHOLD);
            String namingStrategyId = (String) config.getValue("namingStrategy", DEFAULT_NAMING_STRATEGY);
            int minClusterSize = (Integer) config.getValue("minClusterSize", DEFAULT_MIN_CLUSTER_SIZE);
            
            KeywordFilter keywordFilter = buildKeywordFilter(config);
            
            List<String> filteredFiles = filterFiles(filePaths, keywordFilter, context);
            
            if (filteredFiles.isEmpty()) {
                // 没有符合条件的文件
                return changes;
            }
            
            List<FileCluster> clusters = FileClusterer.clusterFiles(filteredFiles, similarityThreshold);
            List<FileCluster> optimizedClusters = FileClusterer.optimizeClusters(clusters, minClusterSize);
            
            NamingStrategy namingStrategy = NamingStrategyFactory.getStrategy(namingStrategyId);
            
            for (FileCluster cluster : optimizedClusters) {
                String clusterName = generateClusterName(cluster, namingStrategy, config, context);
                String clusterPath = targetDirectory + File.separator + clusterName;
                
                for (String filePath : cluster.getFilePaths()) {
                    ChangeRecord record = previewFile(filePath, clusterPath, context);
                    changes.add(record);
                }
            }
            
            // 预览完成，共生成 " + changes.size() + " 条变更记录
            
        } catch (Exception e) {
            // 预览失败: " + e.getMessage()
            e.printStackTrace();
        }
        
        return changes;
    }
    
    private KeywordFilter buildKeywordFilter(PluginConfigDTO config) {
        String mustIncludeStr = (String) config.getValue("mustIncludeKeywords", "");
        String mustNotIncludeStr = (String) config.getValue("mustNotIncludeKeywords", "");
        boolean caseSensitive = (Boolean) config.getValue("caseSensitive", false);
        boolean useRegex = (Boolean) config.getValue("useRegex", false);
        
        KeywordFilter.Builder builder = KeywordFilter.builder()
            .setCaseSensitive(caseSensitive)
            .setUseRegex(useRegex);
        
        if (!mustIncludeStr.isEmpty()) {
            List<String> mustIncludeKeywords = KeywordFilterUtils.parseKeywordString(mustIncludeStr);
            builder.addMustIncludeKeywords(mustIncludeKeywords);
        }
        
        if (!mustNotIncludeStr.isEmpty()) {
            List<String> mustNotIncludeKeywords = KeywordFilterUtils.parseKeywordString(mustNotIncludeStr);
            builder.addMustNotIncludeKeywords(mustNotIncludeKeywords);
        }
        
        return builder.build();
    }
    
    private List<String> filterFiles(List<String> filePaths, KeywordFilter keywordFilter, ExecutionContext context) {
        List<String> filtered = keywordFilter.filterFilePaths(filePaths);
        
        // 文件过滤完成
        
        return filtered;
    }
    
    private String generateClusterName(FileCluster cluster, NamingStrategy namingStrategy, 
                                      PluginConfigDTO config, ExecutionContext context) {
        Map<String, Object> namingContext = new HashMap<>();
        
        String template = (String) config.getValue("template");
        if (template != null && !template.isEmpty()) {
            namingContext.put("template", template);
        }
        
        String prefix = (String) config.getValue("prefix");
        if (prefix != null) {
            namingContext.put("prefix", prefix);
        }
        
        String suffix = (String) config.getValue("suffix");
        if (suffix != null) {
            namingContext.put("suffix", suffix);
        }
        
        return namingStrategy.generateName(cluster, namingContext);
    }
    
    private void createClusterDirectory(String clusterPath, ExecutionContext context) {
        try {
            Path path = Paths.get(clusterPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                // 创建合集目录: " + clusterPath
            }
        } catch (Exception e) {
            // 创建合集目录失败: " + clusterPath + ", " + e.getMessage()
        }
    }
    
    private ChangeRecord processFile(String filePath, String clusterPath, ExecutionContext context) {
        ChangeRecord record = new ChangeRecord();
        record.setId("change-" + System.currentTimeMillis() + "-" + filePath.hashCode());
        record.setOriginalName(filePath);
        
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        String targetPath = clusterPath + File.separator + fileName;
        
        try {
            Path source = Paths.get(filePath);
            Path target = Paths.get(targetPath);
            
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            record.setNewName(targetPath);
            record.setFilePath(filePath);
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.MOVE);
            record.setStatus(ChangeRecord.ExecStatus.SUCCESS);
            record.setReason("文件移动成功");
            
            // 移动文件: " + filePath + " -> " + targetPath
            
        } catch (Exception e) {
            record.setNewName(filePath);
            record.setFilePath(filePath);
            record.setChanged(false);
            record.setOperationType(ChangeRecord.OperationType.MOVE);
            record.setStatus(ChangeRecord.ExecStatus.FAILED);
            record.setReason("文件移动失败: " + e.getMessage());
            
            // 移动文件失败: " + filePath + ", " + e.getMessage()
        }
        
        return record;
    }
    
    private ChangeRecord previewFile(String filePath, String clusterPath, ExecutionContext context) {
        ChangeRecord record = new ChangeRecord();
        record.setId("preview-" + System.currentTimeMillis() + "-" + filePath.hashCode());
        record.setOriginalName(filePath);
        
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        String targetPath = clusterPath + File.separator + fileName;
        
        record.setNewName(targetPath);
        record.setFilePath(filePath);
        record.setChanged(true);
        record.setOperationType(ChangeRecord.OperationType.MOVE);
        record.setStatus(ChangeRecord.ExecStatus.PENDING);
        record.setReason("预览模式，文件未被修改");
        
        return record;
    }
    
    private String getTargetPath(String filePath, PluginConfigDTO config) {
        String targetDir = (String) config.getValue("targetDirectory");
        if (targetDir == null) {
            targetDir = DEFAULT_TARGET_DIRECTORY;
        }
        
        String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        return targetDir + File.separator + fileName;
    }
}
