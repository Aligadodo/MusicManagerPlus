package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.util.FileClusteringAlgorithm;
import com.filemanager.plugin.util.FilenameNormalizer;
import com.filemanager.plugin.util.TextSimilarityCalculator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件收集策略插件
 * 基于文件名相似度将文件归类到合集文件夹中
 */
public class FileCollectionPlugin extends AbstractPlugin {

    private Map<String, List<File>> parentDirClusters;
    private Set<String> processedParentDirs;

    public FileCollectionPlugin() {
        super("file-collection", "文件收集策略", "基于文件名相似度将文件归类到合集文件夹中", "1.0.0");
        this.parentDirClusters = new HashMap<>();
        this.processedParentDirs = new HashSet<>();
    }

    @Override
    protected void initParameters() {
        addParameter("similarityThreshold", "相似度阈值", "slider", "0.8", "文件名相似度阈值（0.0-1.0）", true,
            Arrays.asList("0.5", "0.6", "0.7", "0.8", "0.9", "1.0"));
        addParameter("collectionSuffix", "合集文件夹格式", "text", "【合集】", "合集文件夹后缀格式", false);
        addParameter("targetType", "目标类型", "select", "全部", "处理的目标类型", true,
            Arrays.asList("仅文件", "仅文件夹", "全部"));
        addParameter("namingStrategy", "命名策略", "select", "简洁风格", "合集名称生成策略", true,
            Arrays.asList("简洁风格", "精确风格", "选取模板"));
        addParameter("mustContainKeywords", "必须包含关键词", "text", "", "文件名必须包含的关键词（逗号分隔）", false);
        addParameter("mustNotContainKeywords", "不能包含关键词", "text", "", "文件名不能包含的关键词（逗号分隔）", false);
        addParameter("overwrite", "覆盖已存在文件", "boolean", false, "是否覆盖已存在的文件", false);
        addParameter("addToExisting", "添加到现有合集", "boolean", true, "是否将文件添加到现有合集", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("similarityThreshold", 0.8);
        setDefaultConfigValue("collectionSuffix", "【合集】");
        setDefaultConfigValue("targetType", "全部");
        setDefaultConfigValue("namingStrategy", "简洁风格");
        setDefaultConfigValue("mustContainKeywords", "");
        setDefaultConfigValue("mustNotContainKeywords", "");
        setDefaultConfigValue("overwrite", false);
        setDefaultConfigValue("addToExisting", true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        double similarityThreshold = getConfigValue(config, "similarityThreshold", 0.8);
        String collectionSuffix = getConfigValue(config, "collectionSuffix", "【合集】");
        String namingStrategy = getConfigValue(config, "namingStrategy", "简洁风格");

        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir == null) {
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        String parentDirPath = parentDir.getAbsolutePath();

        if (processedParentDirs.contains(parentDirPath)) {
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        List<File> filesInDir = getFilesInDirectory(parentDir, config);
        if (filesInDir.size() < 2) {
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        FileClusteringAlgorithm clusteringAlgorithm = new FileClusteringAlgorithm(similarityThreshold, namingStrategy);
        List<FileClusteringAlgorithm.FileCluster> clusters = clusteringAlgorithm.clusterFiles(filesInDir);

        if (clusters.isEmpty()) {
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        parentDirClusters.put(parentDirPath, filesInDir);
        processedParentDirs.add(parentDirPath);

        String targetPath = getTargetPath(file, clusters, collectionSuffix, namingStrategy, config, context);
        return createChangeRecord(filePath, targetPath, "PENDING");
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        double similarityThreshold = getConfigValue(config, "similarityThreshold", 0.8);
        String collectionSuffix = getConfigValue(config, "collectionSuffix", "【合集】");
        String namingStrategy = getConfigValue(config, "namingStrategy", "简洁风格");
        boolean overwrite = getConfigValue(config, "overwrite", false);
        boolean addToExisting = getConfigValue(config, "addToExisting", true);

        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir == null) {
            context.logDebug("File has no parent directory: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        String parentDirPath = parentDir.getAbsolutePath();

        if (processedParentDirs.contains(parentDirPath)) {
            context.logDebug("Parent directory already processed: " + parentDirPath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        if (!file.isFile()) {
            context.logDebug("Not a file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        if (isInCollectionFolder(file, collectionSuffix)) {
            context.logDebug("File already in collection folder: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        if (!passesKeywordFilter(file, config)) {
            context.logDebug("File does not pass keyword filter: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        List<File> filesInDir = getFilesInDirectory(parentDir, config);
        if (filesInDir.size() < 2) {
            context.logDebug("Not enough files in directory: " + parentDirPath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        if (isMostlyInSameCollection(filesInDir, collectionSuffix)) {
            context.logDebug("Most files already in same collection: " + parentDirPath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        FileClusteringAlgorithm clusteringAlgorithm = new FileClusteringAlgorithm(similarityThreshold, namingStrategy);
        List<FileClusteringAlgorithm.FileCluster> clusters = clusteringAlgorithm.clusterFiles(filesInDir);

        if (clusters.isEmpty()) {
            context.logDebug("No clusters found: " + parentDirPath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        parentDirClusters.put(parentDirPath, filesInDir);
        processedParentDirs.add(parentDirPath);

        if (addToExisting) {
            ChangeRecord existingRecord = addToExistingCollection(file, clusters, collectionSuffix, similarityThreshold, namingStrategy, overwrite, config, context);
            if (existingRecord != null) {
                return existingRecord;
            }
        }

        String targetPath = getTargetPath(file, clusters, collectionSuffix, namingStrategy, config, context);
        
        try {
            File targetFile = new File(targetPath);
            
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
                context.logDebug("Created directory: " + targetFile.getParentFile().getPath());
            }
            
            if (overwrite || !targetFile.exists()) {
                Files.move(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                context.logInfo("Moved file: " + filePath + " -> " + targetPath);
                return createChangeRecord(filePath, targetPath, "SUCCESS");
            } else {
                context.logWarn("Target file already exists: " + targetPath);
                return createChangeRecord(filePath, targetPath, "SKIPPED");
            }
        } catch (IOException e) {
            context.logError("Error moving file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, targetPath, "ERROR");
        }
    }

    private List<File> getFilesInDirectory(File dir, PluginConfigDTO config) {
        List<File> files = new ArrayList<>();
        String targetType = getConfigValue(config, "targetType", "全部");
        String collectionSuffix = getConfigValue(config, "collectionSuffix", "【合集】");

        File[] dirFiles = dir.listFiles();
        if (dirFiles == null) {
            return files;
        }

        for (File file : dirFiles) {
            if (shouldProcessFile(file, targetType, collectionSuffix, config)) {
                files.add(file);
            }
        }

        return files;
    }

    private boolean shouldProcessFile(File file, String targetType, String collectionSuffix, PluginConfigDTO config) {
        if (!file.exists()) {
            return false;
        }

        if (!passesKeywordFilter(file, config)) {
            return false;
        }

        switch (targetType) {
            case "仅文件":
                return file.isFile();
            case "仅文件夹":
                return file.isDirectory();
            case "全部":
            default:
                return true;
        }
    }

    private boolean passesKeywordFilter(File file, PluginConfigDTO config) {
        String mustContainKeywords = getConfigValue(config, "mustContainKeywords", "");
        String mustNotContainKeywords = getConfigValue(config, "mustNotContainKeywords", "");
        String fileName = file.getName();

        if (!mustContainKeywords.isEmpty()) {
            String[] keywords = mustContainKeywords.split("[,;，；]");
            boolean containsAny = false;
            for (String keyword : keywords) {
                if (!keyword.trim().isEmpty() && fileName.toLowerCase().contains(keyword.trim().toLowerCase())) {
                    containsAny = true;
                    break;
                }
            }
            if (!containsAny) {
                return false;
            }
        }

        if (!mustNotContainKeywords.isEmpty()) {
            String[] keywords = mustNotContainKeywords.split("[,;，；]");
            for (String keyword : keywords) {
                if (!keyword.trim().isEmpty() && fileName.toLowerCase().contains(keyword.trim().toLowerCase())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isInCollectionFolder(File file, String collectionSuffix) {
        File parentDir = file.getParentFile();
        if (parentDir == null) {
            return false;
        }
        return parentDir.getName().contains(collectionSuffix);
    }

    private boolean isMostlyInSameCollection(List<File> files, String collectionSuffix) {
        if (files.isEmpty()) {
            return false;
        }

        int inCollectionCount = 0;
        for (File file : files) {
            if (isInCollectionFolder(file, collectionSuffix)) {
                inCollectionCount++;
            }
        }

        double ratio = (double) inCollectionCount / files.size();
        return ratio > 0.8;
    }

    private ChangeRecord addToExistingCollection(File file, List<FileClusteringAlgorithm.FileCluster> clusters, 
            String collectionSuffix, double similarityThreshold, String namingStrategy, 
            boolean overwrite, PluginConfigDTO config, ExecutionContext context) {
        File parentDir = file.getParentFile();
        if (parentDir == null) {
            return null;
        }

        File[] existingCollections = parentDir.listFiles((dir, name) -> name.contains(collectionSuffix) && new File(dir, name).isDirectory());
        if (existingCollections == null || existingCollections.length == 0) {
            return null;
        }

        String fileName = file.getName();

        for (File collectionDir : existingCollections) {
            String collectionName = collectionDir.getName().replace(collectionSuffix, "").trim();
            
            double similarityToCollection = TextSimilarityCalculator.calculateSimilarity(fileName, collectionName);
            if (similarityToCollection >= similarityThreshold * 0.9) {
                File[] collectionFiles = collectionDir.listFiles();
                if (collectionFiles != null && collectionFiles.length > 0) {
                    double avgSimilarity = 0;
                    for (File collectionFile : collectionFiles) {
                        avgSimilarity += TextSimilarityCalculator.calculateSimilarity(fileName, collectionFile.getName());
                    }
                    avgSimilarity /= collectionFiles.length;
                    
                    if (avgSimilarity >= similarityThreshold * 0.8) {
                        String targetPath = collectionDir.getAbsolutePath() + File.separator + fileName;
                        
                        try {
                            File targetFile = new File(targetPath);
                            if (overwrite || !targetFile.exists()) {
                                Files.move(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                context.logInfo("Added to existing collection: " + file.getAbsolutePath() + " -> " + targetPath);
                                return createChangeRecord(file.getAbsolutePath(), targetPath, "SUCCESS");
                            }
                        } catch (IOException e) {
                            context.logError("Error adding to collection: " + e.getMessage());
                        }
                    }
                }
            }
        }

        return null;
    }

    private String getTargetPath(File file, List<FileClusteringAlgorithm.FileCluster> clusters, 
            String collectionSuffix, String namingStrategy, PluginConfigDTO config, ExecutionContext context) {
        for (FileClusteringAlgorithm.FileCluster cluster : clusters) {
            if (cluster.getFiles().contains(file)) {
                String clusterName = cluster.getClusterName();
                String collectionName = clusterName + collectionSuffix;
                
                File parentDir = file.getParentFile();
                if (parentDir != null) {
                    return parentDir.getAbsolutePath() + File.separator + collectionName + File.separator + file.getName();
                }
            }
        }

        return file.getAbsolutePath();
    }

    public void reset() {
        parentDirClusters.clear();
        processedParentDirs.clear();
    }
}
