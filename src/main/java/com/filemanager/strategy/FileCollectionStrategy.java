/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-20
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.collection.CollectionDeterminationAlgorithm;
import com.filemanager.strategy.collection.FileClusteringAlgorithm;
import com.filemanager.strategy.collection.FilenameNormalizer;
import com.filemanager.strategy.collection.TextSimilarityCalculator;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件归类策略：基于文件名相似度将文件/文件夹归类到合集文件夹中
 */
public class FileCollectionStrategy extends IAppStrategy {
    private final Slider slSimilarityThreshold;
    private final TextField txtCollectionSuffix;
    private final JFXComboBox<ScanTarget> cbTargetType;
    private final TextField txtMustContainKeywords;
    private final TextField txtMustNotContainKeywords;

    // 配置参数
    private double pThreshold;
    private String pCollectionSuffix;
    private ScanTarget pTargetType;
    private List<String> pMustContainKeywords;
    private List<String> pMustNotContainKeywords;

    // 模块化组件
    private FilenameNormalizer filenameNormalizer;
    private TextSimilarityCalculator similarityCalculator;
    private FileClusteringAlgorithm clusteringAlgorithm;
    private CollectionDeterminationAlgorithm determinationAlgorithm;

    // 内部使用：记录已处理的父目录和对应的文件集群
    private final Map<File, Map<String, List<ChangeRecord>>> parentDirClusters = Collections.synchronizedMap(new HashMap<>());

    public FileCollectionStrategy() {
        // 相似度阈值滑块 (0.0 - 1.0)
        slSimilarityThreshold = new Slider(0.0, 1.0, 0.9);
        slSimilarityThreshold.setShowTickMarks(true);
        slSimilarityThreshold.setShowTickLabels(true);
        slSimilarityThreshold.setMajorTickUnit(0.05);
        slSimilarityThreshold.setMinorTickCount(9);

        // 合集文件夹格式
        txtCollectionSuffix = new TextField("【合集】");
        txtCollectionSuffix.setPromptText("输入合集文件夹格式 (如：【合集】)...");

        // 目标类型选择
        cbTargetType = new JFXComboBox<>(FXCollections.observableArrayList(ScanTarget.values()));
        cbTargetType.setValue(ScanTarget.FOLDERS_ONLY); // 默认只对文件夹生效

        // 必须包含的关键词
        txtMustContainKeywords = new TextField("CD,系列,合集");
        txtMustContainKeywords.setPromptText("输入必须包含的关键词，用逗号分隔...");

        // 不能包含的关键词
        txtMustNotContainKeywords = new TextField("下载,Album,群星");
        txtMustNotContainKeywords.setPromptText("输入不能包含的关键词，用逗号分隔...");
    }

    @Override
    public String getName() {
        return "文件智能归类";
    }

    @Override
    public String getDescription() {
        return "基于文件名相似度和特征将文件/文件夹归类到系列合集文件夹中";
    }

    @Override
    public ScanTarget getTargetType() {
        return pTargetType != null ? pTargetType : ScanTarget.FOLDERS_ONLY;
    }

    @Override
    public Node getConfigNode() {
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(10));

        // 基础设置
        VBox basicBox = new VBox(10);
        basicBox.getChildren().addAll(
                StyleFactory.createParamPairLine("相似度阈值 (0.0-1.0):", slSimilarityThreshold),
                StyleFactory.createParamPairLine("合集文件夹格式:", txtCollectionSuffix),
                StyleFactory.createParamPairLine("目标类型:", cbTargetType)
        );
        TitledPane basicPane = new TitledPane("基础设置", basicBox);
        basicPane.setCollapsible(false);

        // 关键词过滤
        VBox keywordBox = new VBox(10);
        keywordBox.getChildren().addAll(
                StyleFactory.createParamPairLine("必须包含关键词 (逗号分隔):", txtMustContainKeywords),
                StyleFactory.createParamPairLine("不能包含关键词 (逗号分隔):", txtMustNotContainKeywords)
        );
        TitledPane keywordPane = new TitledPane("关键词过滤", keywordBox);
        keywordPane.setCollapsible(false);

        // 将所有分类添加到主容器
        mainBox.getChildren().addAll(
                basicPane,
                keywordPane
        );

        return mainBox;
    }

    @Override
    public void captureParams() {
        pThreshold = slSimilarityThreshold.getValue();
        pCollectionSuffix = txtCollectionSuffix.getText();
        pTargetType = cbTargetType.getValue();

        // 处理关键词
        pMustContainKeywords = parseKeywords(txtMustContainKeywords.getText());
        pMustNotContainKeywords = parseKeywords(txtMustNotContainKeywords.getText());

        // 参数验证和默认值设置
        if (pCollectionSuffix == null || pCollectionSuffix.trim().isEmpty()) {
            pCollectionSuffix = "【合集】";
        }
        if (pTargetType == null) {
            pTargetType = ScanTarget.FOLDERS_ONLY;
        }

        // 初始化模块化组件
        initializeComponents();

        // 清空处理记录和集群信息
        parentDirClusters.clear();
    }

    /**
     * 初始化模块化组件
     */
    private void initializeComponents() {
        filenameNormalizer = FilenameNormalizer.builder()
                .build();

        similarityCalculator = TextSimilarityCalculator.builder()
                .similarityThreshold(pThreshold)
                .build();

        clusteringAlgorithm = FileClusteringAlgorithm.builder()
                .normalizer(filenameNormalizer)
                .similarityCalculator(similarityCalculator)
                .similarityThreshold(pThreshold)
                .build();

        determinationAlgorithm = CollectionDeterminationAlgorithm.builder()
                .mustContainKeywords(pMustContainKeywords)
                .mustNotContainKeywords(pMustNotContainKeywords)
                .build();

        determinationAlgorithm.setCollectionSuffix(pCollectionSuffix);
    }

    /**
     * 解析关键词文本框内容为关键词列表
     */
    private List<String> parseKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        if (text != null && !text.trim().isEmpty()) {
            String[] parts = text.split("[,，;；]\\s*");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    keywords.add(part.trim());
                }
            }
        }
        return keywords;
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("fcs_threshold", String.valueOf(slSimilarityThreshold.getValue()));
        props.setProperty("fcs_suffix", txtCollectionSuffix.getText());
        props.setProperty("fcs_target_type", cbTargetType.getValue().name());
        props.setProperty("fcs_must_contain", txtMustContainKeywords.getText());
        props.setProperty("fcs_must_not_contain", txtMustNotContainKeywords.getText());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("fcs_threshold")) {
            slSimilarityThreshold.setValue(Double.parseDouble(props.getProperty("fcs_threshold")));
        }
        if (props.containsKey("fcs_suffix")) {
            txtCollectionSuffix.setText(props.getProperty("fcs_suffix"));
        }
        if (props.containsKey("fcs_target_type")) {
            cbTargetType.setValue(ScanTarget.valueOf(props.getProperty("fcs_target_type")));
        }
        if (props.containsKey("fcs_must_contain")) {
            txtMustContainKeywords.setText(props.getProperty("fcs_must_contain"));
        }
        if (props.containsKey("fcs_must_not_contain")) {
            txtMustNotContainKeywords.setText(props.getProperty("fcs_must_not_contain"));
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        File currentFile = currentRecord.getFileHandle();
        File parentDir = currentFile.getParentFile();

        // 如果父目录为空，跳过
        if (parentDir == null) {
            return Collections.emptyList();
        }

        // 检查组件是否已初始化
        if (determinationAlgorithm == null || clusteringAlgorithm == null) {
            app.log("⚠️ 文件归类策略：组件未初始化，跳过处理");
            return Collections.emptyList();
        }

        // 检查当前文件是否符合目标类型
        if (!isFileTypeMatch(currentFile)) {
            return Collections.emptyList();
        }

        // 检查是否已经处理过这个父目录
        if (parentDirClusters.containsKey(parentDir)) {
            if (parentDir != null) {
                app.log("ℹ️ 文件归类策略：跳过，父目录已处理 " + parentDir.getAbsolutePath());
            }
            return Collections.emptyList();
        }

        // 检查当前文件是否已经在合集文件夹中
        if (isInCollectionFolder(currentFile)) {
            app.log("ℹ️ 文件归类策略：跳过，文件已在合集文件夹中 " + currentFile.getAbsolutePath());
            return Collections.emptyList();
        }

        // 检查当前文件是否本身就是合集文件夹
        if (isCollectionFolder(currentFile)) {
            app.log("ℹ️ 文件归类策略：跳过，文件本身是合集文件夹 " + currentFile.getAbsolutePath());
            return Collections.emptyList();
        }

        // 获取父目录下的所有符合条件的文件记录
        List<ChangeRecord> dirRecords = inputRecords.stream()
                .filter(record -> {
                    File recordFile = record.getFileHandle();
                    File recordParentDir = recordFile.getParentFile();
                    return recordParentDir != null && recordParentDir.equals(parentDir) &&
                            isFileTypeMatch(recordFile) &&
                            !isInCollectionFolder(recordFile) &&
                            !isCollectionFolder(recordFile); // 跳过本身就是合集文件夹的文件
                })
                .collect(Collectors.toList());

        if (parentDir == null) {
            app.log("⚠️ 文件归类策略：父目录为空，跳过处理");
            return Collections.emptyList();
        }
        app.log("📁 文件归类策略：在目录 " + parentDir.getAbsolutePath() + " 中找到 " + dirRecords.size() + " 个符合条件的文件");

        // 如果目录下的文件数量不足2个，跳过处理
        if (dirRecords.size() < 2) {
            app.log("📁 文件归类策略：文件数量不足2个，跳过处理");
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }

        // 检查目录中是否大部分文件已经属于同一合集，如果是则不再执行合并
        if (isMostlySingleCollection(dirRecords)) {
            if (parentDir != null) {
                app.log("📁 文件归类策略：目录中大部分文件已经属于同一合集，跳过处理 " + parentDir.getAbsolutePath());
            }
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }

        // 检查父目录是否已经是合集文件夹
        if (parentDir != null && isCollectionFolder(parentDir)) {
            app.log("📁 文件归类策略：父目录已经是合集文件夹，跳过处理 " + parentDir.getAbsolutePath());
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }

        // 对目录下的文件进行聚类
        if (parentDir != null) {
            app.log("📁 文件归类策略：开始对目录 " + parentDir.getAbsolutePath() + " 下的文件进行聚类");
        }

        if (clusteringAlgorithm == null) {
            app.log("⚠️ 文件归类策略：clusteringAlgorithm 未初始化，跳过处理");
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }

        List<File> files = dirRecords.stream()
                .map(ChangeRecord::getFileHandle)
                .collect(Collectors.toList());

        Map<String, List<File>> fileClusters = clusteringAlgorithm.clusterFiles(files);

        // 转换为ChangeRecord集群
        Map<String, List<ChangeRecord>> clusters = new HashMap<>();
        for (Map.Entry<String, List<File>> entry : fileClusters.entrySet()) {
            List<ChangeRecord> clusterRecords = dirRecords.stream()
                    .filter(record -> entry.getValue().contains(record.getFileHandle()))
                    .collect(Collectors.toList());
            clusters.put(entry.getKey(), clusterRecords);
        }

        // 验证集群
        Map<String, List<ChangeRecord>> validClusters = determinationAlgorithm.filterValidChangeRecordClusters(clusters);

        app.log("📁 文件归类策略：聚类完成，共生成 " + clusters.size() + " 个集群，其中有效集群 " + validClusters.size() + " 个");

        // 标记此目录已处理
        parentDirClusters.put(parentDir, clusters);

        // 生成变更记录
        List<ChangeRecord> changeRecords = new ArrayList<>();

        // 1. 首先尝试将文件添加到现有集合中
        List<ChangeRecord> existingCollectionChanges = Collections.emptyList();
        app.log("📁 文件归类策略：尝试将文件添加到现有集合中");
        existingCollectionChanges = addFilesToExistingCollections(inputRecords, rootDirs, parentDir);
        changeRecords.addAll(existingCollectionChanges);
        app.log("📁 文件归类策略：成功将 " + existingCollectionChanges.size() + " 个文件添加到现有集合中");

        // 2. 然后处理新的集合创建
        app.log("📁 文件归类策略：开始处理新的集合创建");
        int processedFiles = 0;

        for (Map.Entry<String, List<ChangeRecord>> entry : validClusters.entrySet()) {
            List<ChangeRecord> clusterRecords = entry.getValue();

            processedFiles += clusterRecords.size();
            app.log("📁 文件归类策略：处理集群 " + entry.getKey() + "，包含 " + clusterRecords.size() + " 个文件");

            // 提取合集名称
            List<File> clusterFiles = clusterRecords.stream()
                    .map(ChangeRecord::getFileHandle)
                    .collect(Collectors.toList());
            String collectionName = entry.getKey();

            // 创建目标合集文件夹路径
            if (parentDir == null) {
                continue;
            }
            Path targetDirPath = parentDir.toPath().resolve(collectionName + pCollectionSuffix);
            app.log("📁 文件归类策略：为集群创建合集文件夹 " + targetDirPath.getFileName());

            // 为集群中的每个文件生成变更记录
            for (ChangeRecord record : clusterRecords) {
                // 检查该文件是否已经在现有集合中（避免重复处理）
                boolean alreadyAdded = existingCollectionChanges.stream()
                        .anyMatch(change -> change.getOriginalName().equals(record.getOriginalName()));

                if (!alreadyAdded) {
                    // 直接修改原有的记录状态，而不是创建新的记录
                    record.setChanged(true);
                    record.setNewPath(targetDirPath.resolve(record.getFileHandle().getName()).toString());
                    record.setOpType(OperationType.COLLECT);
                    record.setStatus(ExecStatus.PENDING);

                    // 添加合并命中的具体策略和相似度信息到extraParams
                    Map<String, String> params = record.getExtraParams();
                    if (params == null) {
                        params = new HashMap<>();
                        record.setExtraParams(params);
                    }
                    params.put("merge_strategy", "创建新合集");
                    params.put("collection_name", collectionName);
                    params.put("collection_suffix", pCollectionSuffix);
                    params.put("cluster_size", String.valueOf(clusterRecords.size()));
                    params.put("threshold_used", String.valueOf(pThreshold));

                    // 计算与合集名称的相似度
                    String fileName = record.getFileHandle().getName();
                    double similarity = similarityCalculator.calculateSimilarity(fileName, collectionName);
                    double difference = 1.0 - similarity;
                    params.put("similarity_to_collection", String.format("%.3f", similarity));
                    params.put("difference_to_collection", String.format("%.3f", difference));

                    changeRecords.add(record);
                }
            }
        }

        app.log("📁 文件归类策略：处理完成，共生成 " + validClusters.size() + " 个有效集群，处理了 " + processedFiles + " 个文件");

        return changeRecords;
    }

    /**
     * 检查文件是否符合目标类型
     */
    private boolean isFileTypeMatch(File file) {
        if (pTargetType == ScanTarget.FOLDERS_ONLY) {
            return file.isDirectory();
        } else if (pTargetType == ScanTarget.FILES_ONLY) {
            return file.isFile();
        } else {
            return true; // ScanTarget.ALL
        }
    }

    /**
     * 检查文件是否已经在合集文件夹中
     */
    private boolean isInCollectionFolder(File file) {
        if (determinationAlgorithm == null) {
            return false;
        }
        return determinationAlgorithm.isInCollectionFolder(file);
    }

    /**
     * 检查文件是否本身就是合集文件夹
     */
    private boolean isCollectionFolder(File file) {
        if (determinationAlgorithm == null) {
            return false;
        }
        return determinationAlgorithm.isCollectionFolder(file);
    }

    /**
     * 检查文件是否应该被添加到现有集合中
     */
    private boolean shouldAddToExistingCollection(File file, File collectionDir, ChangeRecord record) {
        if (determinationAlgorithm == null || similarityCalculator == null) {
            return false;
        }

        if (!determinationAlgorithm.isValidFile(file)) {
            return false;
        }

        String fileName = file.getName();
        String collectionName = collectionDir.getName().replace(pCollectionSuffix, "");

        if (!determinationAlgorithm.shouldAddToExistingCollection(file, collectionDir, collectionName)) {
            return false;
        }

        double similarity = similarityCalculator.calculateSimilarity(fileName, collectionName);

        File[] collectionFiles = collectionDir.listFiles();
        double avgSimilarity = 0;
        int count = 0;
        if (collectionFiles != null && collectionFiles.length > 0) {
            double totalSimilarity = 0;
            for (File collectionFile : collectionFiles) {
                if (isFileTypeMatch(collectionFile)) {
                    totalSimilarity += similarityCalculator.calculateSimilarity(fileName, collectionFile.getName());
                    count++;
                }
            }
            if (count > 0) {
                avgSimilarity = totalSimilarity / count;
                if (avgSimilarity < pThreshold * 0.8) {
                    return false;
                }
            }
        }

        if (similarity >= pThreshold * 0.9) {
            Map<String, String> params = record.getExtraParams();
            if (params == null) {
                params = new HashMap<>();
                record.setExtraParams(params);
            }
            params.put("similarity", String.format("%.3f", similarity));
            params.put("difference", String.format("%.3f", 1.0 - similarity));
            params.put("avg_similarity_with_collection", String.format("%.3f", avgSimilarity));
            params.put("collection_name", collectionDir.getName());
            params.put("merge_strategy", "添加到现有集合");
            params.put("threshold_used", String.format("%.3f", pThreshold * 0.9));

            return true;
        }

        return false;
    }

    /**
     * 为目录中的现有集合添加新文件
     */
    private List<ChangeRecord> addFilesToExistingCollections(List<ChangeRecord> inputRecords, List<File> rootDirs, File parentDir) {
        List<ChangeRecord> changeRecords = new ArrayList<>();

        // 检查组件是否已初始化
        if (determinationAlgorithm == null) {
            return changeRecords;
        }

        // 获取父目录下的所有现有集合文件夹
        List<File> existingCollections = Collections.emptyList();
        if (parentDir != null) {
            existingCollections = Arrays.stream(parentDir.listFiles(File::isDirectory))
                    .filter(this::isCollectionFolder)
                    .collect(Collectors.toList());
        }

        // 如果没有现有集合，直接返回
        if (existingCollections.isEmpty()) {
            return changeRecords;
        }

        // 获取父目录下的所有非集合文件
        List<ChangeRecord> nonCollectionRecords = inputRecords.stream()
                .filter(record -> {
                    File recordFile = record.getFileHandle();
                    File recordParentDir = recordFile.getParentFile();
                    return recordParentDir != null && recordParentDir.equals(parentDir) &&
                            !isCollectionFolder(recordFile) &&
                            !isInCollectionFolder(recordFile) &&
                            isFileTypeMatch(recordFile);
                })
                .collect(Collectors.toList());

        // 为每个非集合文件检查是否应该添加到现有集合
        for (ChangeRecord record : nonCollectionRecords) {
            for (File collectionDir : existingCollections) {
                if (shouldAddToExistingCollection(record.getFileHandle(), collectionDir, record)) {
                    // 直接修改原有的记录状态，而不是创建新的记录
                    record.setChanged(true);
                    record.setNewPath(collectionDir.toPath().resolve(record.getFileHandle().getName()).toString());
                    record.setOpType(OperationType.COLLECT);
                    record.setStatus(ExecStatus.PENDING);

                    changeRecords.add(record);
                    break; // 一个文件只添加到一个集合
                }
            }
        }

        return changeRecords;
    }

    /**
     * 检查目录中是否大部分文件已经属于同一合集
     */
    private boolean isMostlySingleCollection(List<ChangeRecord> records) {
        if (records.size() < 2) {
            return false;
        }

        if (clusteringAlgorithm == null) {
            return false;
        }

        List<File> files = records.stream()
                .map(ChangeRecord::getFileHandle)
                .collect(Collectors.toList());

        Map<String, List<File>> clusters = clusteringAlgorithm.clusterFiles(files);

        int maxClusterSize = 0;
        for (List<File> cluster : clusters.values()) {
            if (cluster.size() > maxClusterSize) {
                maxClusterSize = cluster.size();
            }
        }

        // 如果大部分文件属于同一合集（超过80%），则返回true
        double ratio = (double) maxClusterSize / records.size();
        return ratio >= 0.8;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.COLLECT) {
            return;
        }

        File sourceFile = rec.getFileHandle();
        File targetFile = new File(rec.getNewPath());

        // 详细日志记录
        log("开始移动: " + sourceFile.getAbsolutePath() + " -> " + targetFile.getAbsolutePath());

        // 确保目标文件夹存在
        File targetDir = targetFile.getParentFile();
        if (targetDir != null && !targetDir.exists()) {
            log("创建合集文件夹: " + targetDir.getAbsolutePath());
            targetDir.mkdirs();
        }

        // 移动文件（直接抛出原始异常，让上层处理）
        Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log("移动成功: " + sourceFile.getName() + " -> " + targetDir.getName());
    }

    /**
     * 计算两个字符串的相似度 (基于 Levenshtein 距离，返回0-1范畴的值)
     */
    protected double calculateSimilarity(String s1, String s2) {
        // 使用更通用的策略处理各种类型的序号和特殊符号
        String processed1 = processSpecialSymbolsAndNumbers(s1);
        String processed2 = processSpecialSymbolsAndNumbers(s2);

        // 计算基本相似度
        double baseSimilarity = calculateBasicSimilarity(processed1, processed2);

        // 额外检查：如果文件名包含相同的标题和不同的数字序号，提高相似度
        if (hasSameTitleDifferentNumber(s1, s2)) {
            // 增加相似度权重，确保序号的文件能被识别为系列
            baseSimilarity = Math.max(baseSimilarity, 0.9);
        }

        // 额外检查：如果两个文件名包含相同的核心关键词，提高相似度
        if (hasCommonCoreKeywords(s1, s2)) {
            baseSimilarity = Math.max(baseSimilarity, 0.85);
        }

        // 智能模式：如果两个文件名包含相同的艺术家和专辑信息，提高相似度
        if (hasSameArtistAlbumInfo(s1, s2)) {
            baseSimilarity = Math.max(baseSimilarity, 0.88);
        }

        return Math.max(0.0, Math.min(1.0, baseSimilarity));
    }

    /**
     * 计算两个字符串的差异度 (基于 Levenshtein 距离，返回0-1范畴的值，值越大差异越大)
     */
    private double calculateDifference(String s1, String s2) {
        double similarity = calculateSimilarity(s1, s2);
        return 1.0 - similarity;
    }

    /**
     * 检查两个文件名是否包含相同的艺术家和专辑信息
     */
    private boolean hasSameArtistAlbumInfo(String s1, String s2) {
        // 提取艺术家和专辑信息
        String artist1 = extractArtist(s1);
        String album1 = extractAlbum(s1);
        String artist2 = extractArtist(s2);
        String album2 = extractAlbum(s2);

        // 如果艺术家相同且专辑名称相似，则认为是同一系列
        if (!artist1.isEmpty() && !artist2.isEmpty() && artist1.equals(artist2)) {
            if (!album1.isEmpty() && !album2.isEmpty()) {
                // 计算专辑名称的相似度
                double albumSimilarity = calculateBasicSimilarity(album1, album2);
                return albumSimilarity >= 0.7;
            }
        }

        return false;
    }

    /**
     * 提取文件名中的艺术家信息
     */
    private String extractArtist(String fileName) {
        // 简单实现：尝试从文件名中提取艺术家信息
        // 假设文件名格式为 "艺术家 - 专辑" 或 "艺术家《专辑》"
        String artist = "";

        // 尝试匹配 "艺术家 - 专辑" 格式
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("^(.*?)\\s*-\\s*");
        java.util.regex.Matcher matcher1 = pattern1.matcher(fileName);
        if (matcher1.find()) {
            artist = matcher1.group(1).trim();
        }

        // 尝试匹配 "艺术家《专辑》" 格式
        if (artist.isEmpty()) {
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("^(.*?)《");
            java.util.regex.Matcher matcher2 = pattern2.matcher(fileName);
            if (matcher2.find()) {
                artist = matcher2.group(1).trim();
            }
        }

        // 去除可能的前缀
        artist = artist.replaceAll("(?i)^DTS-", "").trim();

        return artist;
    }

    /**
     * 提取文件名中的专辑信息
     */
    private String extractAlbum(String fileName) {
        // 简单实现：尝试从文件名中提取专辑信息
        // 假设文件名格式为 "艺术家 - 专辑" 或 "艺术家《专辑》"
        String album = "";

        // 尝试匹配 "艺术家 - 专辑" 格式
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("\\s*-\\s*(.*?)(\\s*\\(|\\s*\\[|\\s*CD|\\s*VOL|$)");
        java.util.regex.Matcher matcher1 = pattern1.matcher(fileName);
        if (matcher1.find()) {
            album = matcher1.group(1).trim();
        }

        // 尝试匹配 "艺术家《专辑》" 格式
        if (album.isEmpty()) {
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("《(.*?)》");
            java.util.regex.Matcher matcher2 = pattern2.matcher(fileName);
            if (matcher2.find()) {
                album = matcher2.group(1).trim();
            }
        }

        return album;
    }

    /**
     * 检查两个文件名是否包含相同的核心关键词
     */
    private boolean hasCommonCoreKeywords(String s1, String s2) {
        // 提取核心关键词
        List<String> keywords1 = extractCoreKeywords(s1);
        List<String> keywords2 = extractCoreKeywords(s2);

        // 计算共同关键词数量
        int commonCount = 0;
        for (String keyword : keywords1) {
            if (keywords2.contains(keyword)) {
                commonCount++;
            }
        }

        // 如果有至少两个共同关键词，或者共同关键词占总关键词的比例较高，则认为是同一系列
        int totalKeywords = Math.max(keywords1.size(), keywords2.size());
        return commonCount >= 2 || (totalKeywords > 0 && (double) commonCount / totalKeywords >= 0.5);
    }

    /**
     * 提取文件名中的核心关键词
     */
    protected List<String> extractCoreKeywords(String fileName) {
        List<String> keywords = new ArrayList<>();

        // 去除常见前缀/后缀
        String processed = fileName.replaceAll("(?i)^DTS-", "");
        processed = processed.replaceAll("(?i)(CD|VOL|DISC)\\s*\\d+", "");
        processed = processed.replaceAll("(?i)(2CD|3CD|4CD)", "");

        // 去除括号和特殊字符
        processed = processed.replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】\\.\\,\\!\\?\\;\\:'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", " ");

        // 去除数字
        processed = processed.replaceAll("\\b\\d+\\b", "");

        // 分割成单词
        String[] parts = processed.split("\\s+");

        // 过滤短词和无意义的词
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() >= 2) { // 至少2个字符
                keywords.add(trimmed);
            }
        }

        return keywords;
    }

    /**
     * 处理特殊符号和序号，使用更通用的策略
     */
    protected String processSpecialSymbolsAndNumbers(String input) {
        String result = input;

        // 1. 处理各种类型的序号
        // 阿拉伯数字（如 1, 2, 3, 01, 02, 03 等）
        result = result.replaceAll("\\b\\d+\\b", "__NUMBER__");

        // 2. 处理中文序号
        // 中文数字（如 一, 二, 三, 十, 百 等）
        result = result.replaceAll("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]", "__CHINESE_NUM__");

        // 3. 处理特殊符号序号
        // 圆形序号（如 ①, ②, ③ 等）
        result = result.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "__CIRCLE_NUM__");

        // 4. 处理其他常见的序号格式
        // 字母序号（如 A, B, C, a, b, c 等）
        result = result.replaceAll("\\b[A-Za-z]\\b", "__LETTER__");

        // 5. 处理括号和特殊符号
        // 去除无意义的括号和特殊符号
        result = result.replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】]", "");

        return result;
    }

    /**
     * 计算基本相似度，不包含特殊情况的处理
     */
    private double calculateBasicSimilarity(String s1, String s2) {
        // 优化：改进噪音字符过滤，保留更多有意义的信息
        // 保留中文序号和括号内容，只去除真正的噪音字符
        String regex = "[\\s\\[\\]\\.\\,\\!\\?\\;\\:\\'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]";

        // 去除噪音字符
        String str1 = s1.replaceAll(regex, "");
        String str2 = s2.replaceAll(regex, "");

        // 如果处理后的字符串为空，使用原始字符串
        if (str1.isEmpty() || str2.isEmpty()) {
            str1 = s1.replaceAll("\\s+", "");
            str2 = s2.replaceAll("\\s+", "");
        }

        // 计算基本相似度
        int distance = getLevenshteinDistance(str1, str2);
        int maxLen = Math.max(str1.length(), str2.length());
        double baseSimilarity = (maxLen == 0) ? 1.0 : 1.0 - ((double) distance / maxLen);

        return baseSimilarity;
    }

    /**
     * 检查两个文件名是否包含相同的标题和不同的数字序号
     */
    protected boolean hasSameTitleDifferentNumber(String s1, String s2) {

        // 通用处理：提取标题部分（去除序号和格式信息）
        String title1 = extractTitle(s1);
        String title2 = extractTitle(s2);

        if (title1.equals(title2)) {
            // 检查是否包含不同的数字序号
            String num1 = extractNumber(s1);
            String num2 = extractNumber(s2);

            // 如果都包含数字且数字不同，则认为是同一系列
            return !num1.isEmpty() && !num2.isEmpty() && !num1.equals(num2);
        }

        return false;
    }

    /**
     * 提取文件名中的标题部分（去除序号和格式信息）
     */
    private String extractTitle(String fileName) {
        // 提取文件名中的标题部分，去除序号和格式信息
        String title = fileName;

        // 1. 去除常见的系列标识前缀/后缀
        title = title.replaceAll("(?i)^DTS-", ""); // 去除DTS前缀
        title = title.replaceAll("(?i)(CD|VOL|DISC)\\s*\\d+", ""); // 去除CD、VOL、DISC等标识
        title = title.replaceAll("(?i)(2CD|3CD|4CD)", ""); // 去除多CD标识

        // 2. 去除阿拉伯数字序号
        title = title.replaceAll("\\b\\d+\\b", "");

        // 3. 去除中文数字序号
        title = title.replaceAll("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]", "");

        // 4. 去除圆形序号
        title = title.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "");

        // 5. 去除字母序号
        title = title.replaceAll("\\b[A-Za-z]\\b", "");

        // 6. 只去除噪音字符，保留括号内容
        title = title.replaceAll("[\\s\\.\\,\\!\\?\\;\\:'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", "");

        // 7. 提取核心标题部分，去除具体的歌曲名称


        return title;
    }

    /**
     * 提取文件名中的序号部分（支持多种类型）
     */
    private String extractNumber(String fileName) {
        // 提取各种类型的序号
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\b\\d+\\b|[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]|[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]|\\b[A-Za-z]\\b)");
        java.util.regex.Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 计算 Levenshtein 距离
     */
    private int getLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    @Override
    public void reload() {
        super.reload();
        // 重新加载时清空处理记录
        parentDirClusters.clear();
    }
}