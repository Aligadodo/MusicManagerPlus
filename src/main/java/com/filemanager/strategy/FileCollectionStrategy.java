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
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 合集命名风格枚举
 */
enum CollectionNamingStyle {
    DEFAULT("默认风格", "基于最长公共前缀"),
    REMOVE_DIFFERENCES("去除差异词", "去除文件名中的差异部分"),
    PRESERVE_EXTENSIONS("保留扩展名", "保留文件类型等限定词"),
    SEQUENCE_INFO("序列信息", "包含最大序列数和当前数量");
    
    private final String name;
    private final String description;
    
    CollectionNamingStyle(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

/**
 * 文件归类策略：基于文件名相似度将文件/文件夹归类到合集文件夹中
 */
public class FileCollectionStrategy extends IAppStrategy {
    private final Slider slSimilarityThreshold;
    private final TextField txtCollectionSuffix;
    private final JFXComboBox<ScanTarget> cbTargetType;
    private final JFXComboBox<CollectionNamingStyle> cbNamingStyle;
    private final CheckBox chkPreserveFileTypes;
    private final CheckBox chkAddSequenceInfo;
    private final CheckBox chkSmartMode;
    private final Spinner<Integer> spMinFiles;
    private final Spinner<Integer> spMinFileNameLength;
    private final TextField txtMustContainKeywords;
    private final TextField txtMustNotContainKeywords;
    private final CheckBox chkSkipCollections;
    private final Spinner<Integer> spMaxCollectionRatio;
    private final Slider slRecognitionStrictness;
    private final CheckBox chkAutoAddToExistingCollections;
    
    // 配置参数
    private double pThreshold;
    private String pCollectionSuffix;
    private ScanTarget pTargetType;
    private CollectionNamingStyle pNamingStyle;
    private boolean pPreserveFileTypes;
    private boolean pAddSequenceInfo;
    private boolean pSmartMode;
    private int pMinFiles;
    private int pMinFileNameLength;
    private List<String> pMustContainKeywords;
    private List<String> pMustNotContainKeywords;
    private boolean pSkipCollections;
    private double pMaxCollectionRatio;
    private double pRecognitionStrictness;
    private boolean pAutoAddToExistingCollections;
    
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
        
        // 命名风格选择
        cbNamingStyle = new JFXComboBox<>(FXCollections.observableArrayList(CollectionNamingStyle.values()));
        cbNamingStyle.setValue(CollectionNamingStyle.DEFAULT); // 默认使用默认风格
        
        // 保留文件类型
        chkPreserveFileTypes = new CheckBox("保留文件类型等限定词");
        chkPreserveFileTypes.setSelected(true); // 默认开启
        
        // 添加序列信息
        chkAddSequenceInfo = new CheckBox("添加序列信息【最大数-当前数】");
        chkAddSequenceInfo.setSelected(true);
        
        // 智能模式
        chkSmartMode = new CheckBox("智能模式：自动调整参数以适应不同命名风格");
        chkSmartMode.setSelected(false); // 默认开启
        
        // 系列文件最少数量
        spMinFiles = new Spinner<>(2, 50, 2);
        spMinFiles.setEditable(true);
        spMinFiles.setMaxWidth(80);
        
        // 最短文件名限制
        spMinFileNameLength = new Spinner<>(5, 100, 8);
        spMinFileNameLength.setEditable(true);
        spMinFileNameLength.setMaxWidth(80);
        
        // 必须包含的关键词
        txtMustContainKeywords = new TextField("CD,系列,合集");
        txtMustContainKeywords.setPromptText("输入必须包含的关键词，用逗号分隔...");
        
        // 不能包含的关键词
        txtMustNotContainKeywords = new TextField("下载,Album,群星");
        txtMustNotContainKeywords.setPromptText("输入不能包含的关键词，用逗号分隔...");
        
        // 跳过已在合集文件夹中的文件
        chkSkipCollections = new CheckBox("跳过已在合集文件夹中的文件");
        chkSkipCollections.setSelected(true); // 默认开启
        
        // 最大合集比例 (0-100%，表示如果目录中超过该比例的文件属于同一合集，则不再执行合并)
        spMaxCollectionRatio = new Spinner<>(50, 100, 80);
        spMaxCollectionRatio.setEditable(true);
        spMaxCollectionRatio.setMaxWidth(80);
        
        // 合集识别严格程度 (0.0-1.0，值越高识别越严格)
        slRecognitionStrictness = new Slider(0.0, 1.0, 0.9);
        slRecognitionStrictness.setShowTickMarks(true);
        slRecognitionStrictness.setShowTickLabels(true);
        slRecognitionStrictness.setMajorTickUnit(0.05);
        slRecognitionStrictness.setMinorTickCount(9);
        
        // 自动添加到已有合集
        chkAutoAddToExistingCollections = new CheckBox("自动添加到已有合集");
        chkAutoAddToExistingCollections.setSelected(false); // 默认关闭，避免暗中决策
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
                StyleFactory.createParamPairLine("目标类型:", cbTargetType),
                StyleFactory.createParamPairLine("命名风格:", cbNamingStyle),
                chkPreserveFileTypes,
                chkAddSequenceInfo,
                chkSmartMode
        );
        TitledPane basicPane = new TitledPane("基础设置", basicBox);
        basicPane.setCollapsible(false);
        
        // 合集规则
        VBox ruleBox = new VBox(10);
        ruleBox.getChildren().addAll(
                StyleFactory.createParamPairLine("系列最少文件数:", spMinFiles),
                StyleFactory.createParamPairLine("最短文件名长度:", spMinFileNameLength)
        );
        TitledPane rulePane = new TitledPane("合集规则", ruleBox);
        rulePane.setCollapsible(false);
        
        // 关键词过滤
        VBox keywordBox = new VBox(10);
        keywordBox.getChildren().addAll(
                StyleFactory.createParamPairLine("必须包含关键词 (逗号分隔):", txtMustContainKeywords),
                StyleFactory.createParamPairLine("不能包含关键词 (逗号分隔):", txtMustNotContainKeywords)
        );
        TitledPane keywordPane = new TitledPane("关键词过滤", keywordBox);
        keywordPane.setCollapsible(false);
        
        // 高级选项
        VBox advancedBox = new VBox(10);
        advancedBox.getChildren().addAll(
                chkSkipCollections,
                chkAutoAddToExistingCollections,
                StyleFactory.createParamPairLine("最大合集比例 (%):", spMaxCollectionRatio),
                StyleFactory.createParamPairLine("合集识别严格程度 (0.0-1.0):", slRecognitionStrictness)
        );
        TitledPane advancedPane = new TitledPane("高级选项", advancedBox);
        advancedPane.setCollapsible(false);
        
        // 将所有分类添加到主容器
        mainBox.getChildren().addAll(
                basicPane,
                rulePane,
                keywordPane,
                advancedPane
        );
        
        return mainBox;
    }
    
    @Override
    public void captureParams() {
        pThreshold = slSimilarityThreshold.getValue();
        pCollectionSuffix = txtCollectionSuffix.getText();
        pTargetType = cbTargetType.getValue();
        pNamingStyle = cbNamingStyle.getValue();
        pPreserveFileTypes = chkPreserveFileTypes.isSelected();
        pAddSequenceInfo = chkAddSequenceInfo.isSelected();
        pSmartMode = chkSmartMode.isSelected();
        pMinFiles = spMinFiles.getValue();
        pMinFileNameLength = spMinFileNameLength.getValue();
        
        // 处理关键词
        pMustContainKeywords = parseKeywords(txtMustContainKeywords.getText());
        pMustNotContainKeywords = parseKeywords(txtMustNotContainKeywords.getText());
        
        // 处理新的配置参数
        pSkipCollections = chkSkipCollections.isSelected();
        pMaxCollectionRatio = spMaxCollectionRatio.getValue() / 100.0;
        pRecognitionStrictness = slRecognitionStrictness.getValue();
        pAutoAddToExistingCollections = chkAutoAddToExistingCollections.isSelected();
        
        // 参数验证和默认值设置
        if (pCollectionSuffix == null || pCollectionSuffix.trim().isEmpty()) {
            pCollectionSuffix = "【合集】";
        }
        if (pTargetType == null) {
            pTargetType = ScanTarget.FOLDERS_ONLY;
        }
        if (pMinFiles < 2) {
            pMinFiles = 2;
        }
        if (pMinFileNameLength < 0) {
            pMinFileNameLength = 0;
        }
        if (pMaxCollectionRatio < 0.5) {
            pMaxCollectionRatio = 0.5;
        } else if (pMaxCollectionRatio > 1.0) {
            pMaxCollectionRatio = 1.0;
        }
        
        // 清空处理记录和集群信息
        parentDirClusters.clear();
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
        props.setProperty("fcs_naming_style", cbNamingStyle.getValue().name());
        props.setProperty("fcs_preserve_file_types", String.valueOf(chkPreserveFileTypes.isSelected()));
        props.setProperty("fcs_add_sequence_info", String.valueOf(chkAddSequenceInfo.isSelected()));
        props.setProperty("fcs_smart_mode", String.valueOf(chkSmartMode.isSelected()));
        props.setProperty("fcs_min_files", String.valueOf(spMinFiles.getValue()));
        props.setProperty("fcs_min_filename_length", String.valueOf(spMinFileNameLength.getValue()));
        props.setProperty("fcs_must_contain", txtMustContainKeywords.getText());
        props.setProperty("fcs_must_not_contain", txtMustNotContainKeywords.getText());
        props.setProperty("fcs_skip_collections", String.valueOf(chkSkipCollections.isSelected()));
        props.setProperty("fcs_max_collection_ratio", String.valueOf(spMaxCollectionRatio.getValue()));
        props.setProperty("fcs_recognition_strictness", String.valueOf(slRecognitionStrictness.getValue()));
        props.setProperty("fcs_auto_add_to_existing", String.valueOf(chkAutoAddToExistingCollections.isSelected()));
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
        if (props.containsKey("fcs_naming_style")) {
            cbNamingStyle.setValue(CollectionNamingStyle.valueOf(props.getProperty("fcs_naming_style")));
        }
        if (props.containsKey("fcs_preserve_file_types")) {
            chkPreserveFileTypes.setSelected(Boolean.parseBoolean(props.getProperty("fcs_preserve_file_types")));
        }
        if (props.containsKey("fcs_add_sequence_info")) {
            chkAddSequenceInfo.setSelected(Boolean.parseBoolean(props.getProperty("fcs_add_sequence_info")));
        }
        if (props.containsKey("fcs_smart_mode")) {
            chkSmartMode.setSelected(Boolean.parseBoolean(props.getProperty("fcs_smart_mode")));
        }
        if (props.containsKey("fcs_min_files")) {
            spMinFiles.getValueFactory().setValue(Integer.parseInt(props.getProperty("fcs_min_files")));
        }
        if (props.containsKey("fcs_min_filename_length")) {
            spMinFileNameLength.getValueFactory().setValue(Integer.parseInt(props.getProperty("fcs_min_filename_length")));
        }
        if (props.containsKey("fcs_must_contain")) {
            txtMustContainKeywords.setText(props.getProperty("fcs_must_contain"));
        }
        if (props.containsKey("fcs_must_not_contain")) {
            txtMustNotContainKeywords.setText(props.getProperty("fcs_must_not_contain"));
        }
        if (props.containsKey("fcs_skip_collections")) {
            chkSkipCollections.setSelected(Boolean.parseBoolean(props.getProperty("fcs_skip_collections")));
        }
        if (props.containsKey("fcs_max_collection_ratio")) {
            spMaxCollectionRatio.getValueFactory().setValue(Integer.parseInt(props.getProperty("fcs_max_collection_ratio")));
        }
        if (props.containsKey("fcs_recognition_strictness")) {
            slRecognitionStrictness.setValue(Double.parseDouble(props.getProperty("fcs_recognition_strictness")));
        }
        if (props.containsKey("fcs_auto_add_to_existing")) {
            chkAutoAddToExistingCollections.setSelected(Boolean.parseBoolean(props.getProperty("fcs_auto_add_to_existing")));
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
        
        // 检查当前文件是否符合目标类型
        if (!isFileTypeMatch(currentFile)) {
            return Collections.emptyList();
        }
        
        // 检查是否已经处理过这个父目录
        if (parentDirClusters.containsKey(parentDir)) {
            app.log("ℹ️ 文件归类策略：跳过，父目录已处理 " + parentDir.getAbsolutePath());
            return Collections.emptyList();
        }
        
        // 检查当前文件是否已经在合集文件夹中
        if (pSkipCollections && isInCollectionFolder(currentFile)) {
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
                           (!pSkipCollections || !isInCollectionFolder(recordFile)) &&
                           !isCollectionFolder(recordFile); // 跳过本身就是合集文件夹的文件
                })
                .collect(Collectors.toList());
        
        app.log("📁 文件归类策略：在目录 " + parentDir.getAbsolutePath() + " 中找到 " + dirRecords.size() + " 个符合条件的文件");
        
        // 如果目录下的文件数量不足2个，但有现有集合，则只执行添加到现有集合的逻辑
        if (dirRecords.size() < 2) {
            // 只有当启用了自动添加到已有合集选项时，才尝试添加到现有集合中
            if (pAutoAddToExistingCollections) {
                app.log("📁 文件归类策略：文件数量不足2个，尝试添加到现有集合中");
                List<ChangeRecord> changes = addFilesToExistingCollections(inputRecords, rootDirs, parentDir);
                
                // 标记此目录已处理
                parentDirClusters.put(parentDir, Collections.emptyMap());
                
                app.log("📁 文件归类策略：处理完成，已标记目录为已处理 " + parentDir.getAbsolutePath());
                return changes;
            } else {
                app.log("📁 文件归类策略：文件数量不足2个，且未启用自动添加到已有合集选项，跳过处理");
                // 标记此目录已处理
                parentDirClusters.put(parentDir, Collections.emptyMap());
                return Collections.emptyList();
            }
        }
        
        // 检查目录中是否大部分文件已经属于同一合集，如果是则不再执行合并
        if (isMostlySingleCollection(dirRecords)) {
            app.log("📁 文件归类策略：目录中大部分文件已经属于同一合集，跳过处理 " + parentDir.getAbsolutePath());
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }
        
        // 检查父目录是否已经是合集文件夹
        if (isCollectionFolder(parentDir)) {
            app.log("📁 文件归类策略：父目录已经是合集文件夹，跳过处理 " + parentDir.getAbsolutePath());
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }
        
        // 对目录下的文件进行聚类
        app.log("📁 文件归类策略：开始对目录 " + parentDir.getAbsolutePath() + " 下的文件进行聚类");
        Map<String, List<ChangeRecord>> clusters = clusterSimilarRecords(dirRecords);
        
        app.log("📁 文件归类策略：聚类完成，共生成 " + clusters.size() + " 个集群");
        
        // 标记此目录已处理
        parentDirClusters.put(parentDir, clusters);
        
        // 生成变更记录
        List<ChangeRecord> changeRecords = new ArrayList<>();
        
        // 1. 首先尝试将文件添加到现有集合中
        List<ChangeRecord> existingCollectionChanges = Collections.emptyList();
        if (pAutoAddToExistingCollections) {
            app.log("📁 文件归类策略：尝试将文件添加到现有集合中");
            existingCollectionChanges = addFilesToExistingCollections(inputRecords, rootDirs, parentDir);
            changeRecords.addAll(existingCollectionChanges);
            app.log("📁 文件归类策略：成功将 " + existingCollectionChanges.size() + " 个文件添加到现有集合中");
        } else {
            app.log("📁 文件归类策略：未启用自动添加到已有合集选项，跳过此步骤");
        }
        
        // 2. 然后处理新的集合创建
        app.log("📁 文件归类策略：开始处理新的集合创建");
        int validClusters = 0;
        int processedFiles = 0;
        
        for (Map.Entry<String, List<ChangeRecord>> entry : clusters.entrySet()) {
            List<ChangeRecord> clusterRecords = entry.getValue();
            
            // 应用约束条件检查
            if (!isClusterValid(clusterRecords)) {
                app.log("⚠️ 文件归类策略：集群无效，跳过处理 " + entry.getKey());
                continue;
            }
            
            validClusters++;
            app.log("📁 文件归类策略：处理集群 " + entry.getKey() + "，包含 " + clusterRecords.size() + " 个文件");
            
            // 提取合集名称
            List<File> clusterFiles = clusterRecords.stream()
                    .map(ChangeRecord::getFileHandle)
                    .collect(Collectors.toList());
            String collectionName = extractCollectionName(clusterFiles);
            
            // 创建目标合集文件夹路径
            Path targetDirPath = parentDir.toPath().resolve(collectionName + pCollectionSuffix);
            app.log("📁 文件归类策略：为集群创建合集文件夹 " + targetDirPath.getFileName());
            
            // 为集群中的每个文件生成变更记录
            for (ChangeRecord record : clusterRecords) {
                // 检查该文件是否已经在现有集合中（避免重复处理）
                boolean alreadyAdded = existingCollectionChanges.stream()
                        .anyMatch(change -> change.getOriginalName().equals(record.getOriginalName()));
                
                if (!alreadyAdded) {
                    processedFiles++;
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
                    params.put("naming_style", pNamingStyle.getName());
                    params.put("cluster_size", String.valueOf(clusterRecords.size()));
                    params.put("threshold_used", String.valueOf(pThreshold));
                    
                    // 计算与合集名称的相似度
                    String fileName = record.getFileHandle().getName();
                    double similarity = calculateSimilarity(fileName, collectionName);
                    double difference = calculateDifference(fileName, collectionName);
                    params.put("similarity_to_collection", String.format("%.3f", similarity));
                    params.put("difference_to_collection", String.format("%.3f", difference));
                    
                    changeRecords.add(record);
                }
            }
        }
        
        app.log("📁 文件归类策略：处理完成，共生成 " + validClusters + " 个有效集群，处理了 " + processedFiles + " 个文件");
        
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
        File parentDir = file.getParentFile();
        if (parentDir == null) {
            return false;
        }
        
        String parentName = parentDir.getName();
        // 检查父目录名称是否包含合集关键词或用户配置的合集文件夹格式
        boolean isCollection = parentName.contains(pCollectionSuffix) || 
                              parentName.contains("合集") || 
                              parentName.contains("系列") || 
                              parentName.contains("Collection") || 
                              parentName.contains("Series") ||
                              parentName.endsWith("合集") ||
                              parentName.endsWith("系列") ||
                              parentName.endsWith("Collection") ||
                              parentName.endsWith("Series");
        
        // 检查父目录是否已经是由本策略生成的合集文件夹
        if (parentName.contains(pCollectionSuffix) && parentName.length() > pCollectionSuffix.length()) {
            // 检查父目录名称是否包含明显的截断痕迹
            if (parentName.endsWith("- " + pCollectionSuffix) || parentName.endsWith(". " + pCollectionSuffix)) {
                return true;
            }
        }
        
        return isCollection;
    }
    
    /**
     * 检查文件是否本身就是合集文件夹
     */
    private boolean isCollectionFolder(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        
        String name = file.getName();
        
        // 1. 首先检查目录名称是否包含合集关键词或用户配置的合集文件夹格式
        boolean hasCollectionKeyword = name.contains(pCollectionSuffix) || 
                                       name.contains("合集") || 
                                       name.contains("系列") || 
                                       name.contains("Collection") || 
                                       name.contains("Series") ||
                                       name.endsWith("合集") ||
                                       name.endsWith("系列") ||
                                       name.endsWith("Collection") ||
                                       name.endsWith("Series");
        
        // 2. 如果目录名称包含合集关键词，进一步检查目录内部的文件
        if (hasCollectionKeyword) {
            // 获取目录下的文件
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                // 空目录不认为是合集文件夹
                return false;
            }
            
            // 检查目录下的文件是否符合配置的条件
            int validFileCount = 0;
            for (File f : files) {
                // 只考虑符合目标类型的文件
                if (isFileTypeMatch(f)) {
                    // 检查文件名是否达到最短长度要求
                    if (f.getName().length() >= pMinFileNameLength) {
                        // 检查文件名是否包含必须的关键词
                        boolean hasRequiredKeyword = pMustContainKeywords.isEmpty();
                        if (!hasRequiredKeyword) {
                            for (String keyword : pMustContainKeywords) {
                                if (f.getName().toLowerCase().contains(keyword.toLowerCase())) {
                                    hasRequiredKeyword = true;
                                    break;
                                }
                            }
                        }
                        
                        // 检查文件名是否不包含禁止的关键词
                        boolean hasForbiddenKeyword = false;
                        if (!pMustNotContainKeywords.isEmpty()) {
                            for (String keyword : pMustNotContainKeywords) {
                                if (f.getName().toLowerCase().contains(keyword.toLowerCase())) {
                                    hasForbiddenKeyword = true;
                                    break;
                                }
                            }
                        }
                        
                        if (hasRequiredKeyword && !hasForbiddenKeyword) {
                            validFileCount++;
                        }
                    }
                }
            }
            
            // 只有当目录下有足够数量的有效文件时，才认为是合集文件夹
            return validFileCount >= pMinFiles;
        }
        
        return false;
    }
    
    /**
     * 检查文件是否应该被添加到现有集合中
     */
    private boolean shouldAddToExistingCollection(File file, File collectionDir, ChangeRecord record) {
        // 1. 检查文件是否符合目标类型
        if (!isFileTypeMatch(file)) {
            return false;
        }
        
        // 2. 检查文件名是否达到最短长度要求
        if (file.getName().length() < pMinFileNameLength) {
            return false;
        }
        
        // 3. 检查文件名是否包含必须的关键词
        boolean hasRequiredKeyword = pMustContainKeywords.isEmpty();
        if (!hasRequiredKeyword) {
            for (String keyword : pMustContainKeywords) {
                if (file.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    hasRequiredKeyword = true;
                    break;
                }
            }
        }
        if (!hasRequiredKeyword) {
            return false;
        }
        
        // 4. 检查文件名是否不包含禁止的关键词
        boolean hasForbiddenKeyword = false;
        if (!pMustNotContainKeywords.isEmpty()) {
            for (String keyword : pMustNotContainKeywords) {
                if (file.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    hasForbiddenKeyword = true;
                    break;
                }
            }
        }
        if (hasForbiddenKeyword) {
            return false;
        }
        
        // 5. 检查文件是否与集合名称相似
        String fileName = file.getName();
        String collectionName = collectionDir.getName().replace(pCollectionSuffix, "");
        
        // 使用calculateSimilarity方法检查相似度
        double similarity = calculateSimilarity(fileName, collectionName);
        double difference = calculateDifference(fileName, collectionName);
        
        // 6. 检查集合目录下的文件是否与当前文件相似
        File[] collectionFiles = collectionDir.listFiles();
        double avgSimilarity = 0;
        int count = 0;
        if (collectionFiles != null && collectionFiles.length > 0) {
            // 计算当前文件与集合中文件的平均相似度
            double totalSimilarity = 0;
            for (File collectionFile : collectionFiles) {
                if (isFileTypeMatch(collectionFile)) {
                    totalSimilarity += calculateSimilarity(fileName, collectionFile.getName());
                    count++;
                }
            }
            if (count > 0) {
                avgSimilarity = totalSimilarity / count;
                // 如果平均相似度低于阈值，不应该添加到现有集合
                if (avgSimilarity < pThreshold * 0.8) {
                    return false;
                }
            }
        }
        
        // 7. 如果相似度高于阈值，应该添加到现有集合
        if (similarity >= pThreshold * 0.9) { // 使用略微降低的阈值
            // 添加合并命中的具体策略和相似度信息到extraParams
            Map<String, String> params = record.getExtraParams();
            if (params == null) {
                params = new HashMap<>();
                record.setExtraParams(params);
            }
            params.put("similarity", String.format("%.3f", similarity));
            params.put("difference", String.format("%.3f", difference));
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
        
        // 获取父目录下的所有现有集合文件夹
        List<File> existingCollections = Arrays.stream(parentDir.listFiles(File::isDirectory))
                .filter(this::isCollectionFolder)
                .collect(Collectors.toList());
        
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
        
        // 对文件进行聚类
        Map<String, List<ChangeRecord>> clusters = clusterSimilarRecords(records);
        
        // 找出最大的集群
        int maxClusterSize = 0;
        for (List<ChangeRecord> cluster : clusters.values()) {
            if (cluster.size() > maxClusterSize) {
                maxClusterSize = cluster.size();
            }
        }
        
        // 计算最大集群占比
        double ratio = (double) maxClusterSize / records.size();
        
        // 如果最大集群占比超过配置的阈值，则认为大部分文件已经属于同一合集
        return ratio >= pMaxCollectionRatio;
    }
    
    /**
     * 检查集群是否符合所有约束条件
     */
    private boolean isClusterValid(List<ChangeRecord> cluster) {
        // 1. 检查集群中的文件数量是否达到最小值
        if (cluster.size() < pMinFiles) {
            return false;
        }
        
        // 提取所有文件名
        List<String> fileNames = cluster.stream()
                .map(record -> record.getFileHandle().getName())
                .collect(Collectors.toList());
        
        // 2. 检查集群中的所有文件名是否都达到最短长度要求
        for (String fileName : fileNames) {
            if (fileName.length() < pMinFileNameLength) {
                return false;
            }
        }
        
        // 3. 检查集群中是否包含必须的关键词
        if (!pMustContainKeywords.isEmpty()) {
            boolean hasRequiredKeyword = false;
            for (String keyword : pMustContainKeywords) {
                for (String fileName : fileNames) {
                    if (fileName.toLowerCase().contains(keyword.toLowerCase())) {
                        hasRequiredKeyword = true;
                        break;
                    }
                }
                if (hasRequiredKeyword) {
                    break;
                }
            }
            if (!hasRequiredKeyword) {
                return false;
            }
        }
        
        // 4. 检查集群中是否包含禁止的关键词
        if (!pMustNotContainKeywords.isEmpty()) {
            for (String keyword : pMustNotContainKeywords) {
                for (String fileName : fileNames) {
                    if (fileName.toLowerCase().contains(keyword.toLowerCase())) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * 对相似的记录进行聚类
     */
    private Map<String, List<ChangeRecord>> clusterSimilarRecords(List<ChangeRecord> records) {
        Map<String, List<ChangeRecord>> clusters = new HashMap<>();
        Set<ChangeRecord> processed = new HashSet<>();
        
        for (ChangeRecord record1 : records) {
            if (processed.contains(record1)) {
                continue;
            }
            
            List<ChangeRecord> cluster = new ArrayList<>();
            cluster.add(record1);
            processed.add(record1);
            
            String name1 = record1.getFileHandle().getName();
            
            for (ChangeRecord record2 : records) {
                if (record1.equals(record2) || processed.contains(record2)) {
                    continue;
                }
                
                String name2 = record2.getFileHandle().getName();
                double similarity = calculateSimilarity(name1, name2);
                double difference = calculateDifference(name1, name2);
                
                // 基于相似度和差异度的双重判断
                if (similarity >= pThreshold && difference <= (1.0 - pThreshold)) {
                    // 添加相似度和差异度信息到extraParams
                    Map<String, String> params1 = record1.getExtraParams();
                    if (params1 == null) {
                        params1 = new HashMap<>();
                        record1.setExtraParams(params1);
                    }
                    params1.put("similarity", String.format("%.3f", similarity));
                    params1.put("difference", String.format("%.3f", difference));
                    params1.put("clustering_strategy", "基于相似度阈值" + pThreshold);
                    
                    Map<String, String> params2 = record2.getExtraParams();
                    if (params2 == null) {
                        params2 = new HashMap<>();
                        record2.setExtraParams(params2);
                    }
                    params2.put("similarity", String.format("%.3f", similarity));
                    params2.put("difference", String.format("%.3f", difference));
                    params2.put("clustering_strategy", "基于相似度阈值" + pThreshold);
                    
                    cluster.add(record2);
                    processed.add(record2);
                }
            }
            
            if (cluster.size() > 0) {
                // 使用第一个记录的名称作为集群键
                clusters.put(cluster.get(0).getOriginalName(), cluster);
            }
        }
        
        return clusters;
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
    private double calculateSimilarity(String s1, String s2) {
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
        if (pSmartMode && hasSameArtistAlbumInfo(s1, s2)) {
            baseSimilarity = Math.max(baseSimilarity, 0.88);
        }
        
        // 根据识别严格程度调整相似度阈值
        baseSimilarity *= pRecognitionStrictness;
        
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
    private List<String> extractCoreKeywords(String fileName) {
        List<String> keywords = new ArrayList<>();
        
        // 去除常见前缀/后缀
        String processed = fileName.replaceAll("(?i)^DTS-", "");
        processed = processed.replaceAll("(?i)(CD|VOL|DISC)\\s*\\d+", "");
        processed = processed.replaceAll("(?i)(2CD|3CD|4CD)", "");
        
        // 去除括号和特殊字符
        processed = processed.replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】\\.\\,\\!\\?\\;\\:\'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", " ");
        
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
    private String processSpecialSymbolsAndNumbers(String input) {
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
    private boolean hasSameTitleDifferentNumber(String s1, String s2) {
        // 提取标题部分（去除序号和格式信息）
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
        title = title.replaceAll("(?i)^DTS-|", ""); // 去除DTS前缀
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
        
        // 6. 去除括号和噪音字符
        title = title.replaceAll("[\\s\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】\\.\\,\\!\\?\\;\\:\'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", "");
        
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
    
    /**
     * 提取合集名称：找出多个文件名的最长公共前缀
     */
    private String extractCollectionName(List<File> similarFiles) {
        if (similarFiles.isEmpty()) {
            return "未命名";
        }
        
        // 获取所有文件名
        List<String> fileNames = similarFiles.stream()
                .map(File::getName)
                .collect(Collectors.toList());
        
        // 根据选择的命名风格生成合集名称
        String collectionName;
        switch (pNamingStyle) {
            case REMOVE_DIFFERENCES:
                collectionName = extractNameByRemovingDifferences(fileNames);
                break;
            case PRESERVE_EXTENSIONS:
                collectionName = extractNameWithExtensions(fileNames);
                break;
            case SEQUENCE_INFO:
                collectionName = extractNameWithSequenceInfo(fileNames);
                break;
            default:
                // 默认风格：基于最长公共前缀
                collectionName = extractNameByCommonPrefix(fileNames);
                break;
        }
        
        // 清理合集名称
        collectionName = cleanCollectionName(collectionName);
        
        return collectionName;
    }
    
    /**
     * 基于最长公共前缀提取合集名称（默认风格）
     */
    private String extractNameByCommonPrefix(List<String> fileNames) {
        // 找出最长公共前缀
        String commonPrefix = findLongestCommonPrefix(fileNames);
        
        // 优化：避免生成太短的合集名称，确保包含有意义的信息
        if (commonPrefix.length() < 5) {
            // 使用更智能的方法提取合集名称
            String collectionName = extractSmartCollectionName(fileNames);
            
            // 如果提取失败，使用原始的处理方式
            if (collectionName.length() < 5) {
                // 简单处理：取第一个文件名的前几个字符作为基础
                String firstFileName = fileNames.get(0);
                
                // 去除特殊字符和噪音
                String regex = "[\\s\\[\\]《》\\-\\(\\)\\{\\}\\-.\\,\\!\\?\\;\\:\'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]";
                String cleanName = firstFileName.replaceAll(regex, " ").trim();
                
                // 尝试提取空格前的部分（通常是系列名或作者名）
                if (cleanName.contains(" ")) {
                    // 取更多部分，而不仅仅是第一个空格前的内容
                    String[] parts = cleanName.split(" ");
                    if (parts.length > 1) {
                        return parts[0] + " " + parts[1];
                    }
                    return parts[0];
                }
                
                // 如果没有空格，取更长的部分
                return cleanName.substring(0, Math.min(cleanName.length(), 15));
            }
            
            return collectionName;
        }
        
        return commonPrefix;
    }
    
    /**
     * 通过去除差异词提取合集名称
     */
    private String extractNameByRemovingDifferences(List<String> fileNames) {
        if (fileNames.isEmpty()) {
            return "未命名";
        }
        
        // 1. 提取所有文件名的共同部分，去除差异部分
        // 首先处理每个文件名，去除序号和差异词
        List<String> processedNames = new ArrayList<>();
        for (String fileName : fileNames) {
            // 去除序号（如 ①、②、01、02 等）
            String processed = removeSequences(fileName);
            // 去除括号中的内容（可能是差异词）
            processed = removeBracketedContent(processed);
            processedNames.add(processed);
        }
        
        // 找出最长公共前缀
        String commonPrefix = findLongestCommonPrefix(processedNames);
        
        // 2. 如果公共前缀太短，尝试从第一个文件名中提取基础部分
        if (commonPrefix.length() < 5) {
            // 从第一个文件名中提取基础部分
            String firstFileName = fileNames.get(0);
            commonPrefix = extractBasePart(firstFileName);
        }
        
        // 3. 保留文件类型等限定词
        if (pPreserveFileTypes) {
            String fileType = extractFileType(fileNames.get(0));
            if (!fileType.isEmpty()) {
                commonPrefix += " " + fileType;
            }
        }
        
        return commonPrefix;
    }
    
    /**
     * 提取包含文件类型等限定词的合集名称
     */
    private String extractNameWithExtensions(List<String> fileNames) {
        if (fileNames.isEmpty()) {
            return "未命名";
        }
        
        // 1. 提取基础名称
        String baseName = extractNameByCommonPrefix(fileNames);
        
        // 2. 提取并保留文件类型等限定词
        String fileType = extractFileType(fileNames.get(0));
        if (!fileType.isEmpty()) {
            baseName += " " + fileType;
        }
        
        return baseName;
    }
    
    /**
     * 提取包含序列信息的合集名称
     */
    private String extractNameWithSequenceInfo(List<String> fileNames) {
        if (fileNames.isEmpty()) {
            return "未命名";
        }
        
        // 1. 提取基础名称
        String baseName = extractNameByRemovingDifferences(fileNames);
        
        // 2. 提取序列信息
        if (pAddSequenceInfo) {
            int maxSequence = findMaxSequence(fileNames);
            int currentCount = fileNames.size();
            if (maxSequence > 0) {
                baseName += " 【" + maxSequence + "-" + currentCount + "】";
            }
        }
        
        return baseName;
    }
    
    /**
     * 去除文件名中的序号
     */
    private String removeSequences(String fileName) {
        String result = fileName;
        // 去除阿拉伯数字序号（如 1, 2, 3, 01, 02 等）
        result = result.replaceAll("\\b\\d+\\b", "");
        // 去除中文数字序号（如一, 二, 三 等）
        result = result.replaceAll("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]", "");
        // 去除圆形序号（如①, ②, ③ 等）
        result = result.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "");
        // 去除字母序号（如 A, B, C 等）
        result = result.replaceAll("\\b[A-Za-z]\\b", "");
        return result;
    }
    
    /**
     * 去除括号中的内容
     */
    private String removeBracketedContent(String text) {
        // 去除各种括号中的内容
        String result = text;
        result = result.replaceAll("\\[[^\\]]*\\]", ""); // []
        result = result.replaceAll("\\([^\\)]*\\)", ""); // ()
        result = result.replaceAll("\\{[^\\}]*\\}", ""); // {}
        result = result.replaceAll("《[^》]*》", ""); // 《》
        result = result.replaceAll("<[^>]*>", ""); // <>
        return result;
    }
    
    /**
     * 提取文件名的基础部分
     */
    private String extractBasePart(String fileName) {
        // 去除序号
        String basePart = removeSequences(fileName);
        // 去除括号中的内容
        basePart = removeBracketedContent(basePart);
        // 去除特殊字符
        basePart = basePart.replaceAll("[\\s\\[\\]\\.\\,\\!\\?\\;\\:\'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", " ");
        // 清理空格
        basePart = basePart.trim();
        // 取前几个词作为基础部分
        if (basePart.contains(" ")) {
            String[] parts = basePart.split(" ");
            if (parts.length > 1) {
                return parts[0] + " " + parts[1];
            }
            return parts[0];
        }
        return basePart;
    }
    
    /**
     * 提取文件类型等限定词
     */
    private String extractFileType(String fileName) {
        // 提取括号中的文件类型信息
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\(([^)]+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            String content = matcher.group(1);
            // 检查是否是文件类型
            if (content.matches("[A-Z]+") || content.matches("[a-zA-Z0-9]+")) {
                return "(" + content + ")";
            }
        }
        return "";
    }
    
    /**
     * 查找文件名中的最大序列数
     */
    private int findMaxSequence(List<String> fileNames) {
        int maxSequence = 0;
        
        for (String fileName : fileNames) {
            // 查找阿拉伯数字
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d+)\\b");
            java.util.regex.Matcher matcher = pattern.matcher(fileName);
            while (matcher.find()) {
                int sequence = Integer.parseInt(matcher.group(1));
                if (sequence > maxSequence) {
                    maxSequence = sequence;
                }
            }
            
            // 查找圆形数字
            int circleNum = extractCircleNumber(fileName);
            if (circleNum > maxSequence) {
                maxSequence = circleNum;
            }
        }
        
        return maxSequence;
    }
    
    /**
     * 提取圆形数字
     */
    private int extractCircleNumber(String text) {
        // 圆形数字映射
        Map<Character, Integer> circleNums = new HashMap<>();
        circleNums.put('①', 1);
        circleNums.put('②', 2);
        circleNums.put('③', 3);
        circleNums.put('④', 4);
        circleNums.put('⑤', 5);
        circleNums.put('⑥', 6);
        circleNums.put('⑦', 7);
        circleNums.put('⑧', 8);
        circleNums.put('⑨', 9);
        circleNums.put('⑩', 10);
        
        for (char c : text.toCharArray()) {
            if (circleNums.containsKey(c)) {
                return circleNums.get(c);
            }
        }
        return 0;
    }
    
    /**
     * 智能提取合集名称，避免截断过多内容
     */
    private String extractSmartCollectionName(List<String> fileNames) {
        if (fileNames.isEmpty()) {
            return "";
        }
        
        // 提取所有文件名的公共部分，忽略序号和版本信息
        List<String> processedNames = new ArrayList<>();
        for (String name : fileNames) {
            // 去除序号和版本信息
            String processed = name.replaceAll("\\b\\d+\\b", "");
            processed = processed.replaceAll("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]", "");
            processed = processed.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "");
            processed = processed.replaceAll("\\b[A-Za-z]\\b", "");
            processedNames.add(processed);
        }
        
        // 找出最长公共前缀
        String commonPrefix = findLongestCommonPrefix(processedNames);
        
        // 如果公共前缀仍然太短，尝试提取标题
        if (commonPrefix.length() < 5) {
            // 尝试从第一个文件名中提取标题
            String firstFileName = fileNames.get(0);
            
            // 提取《》之间的内容作为标题
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("《([^》]+)》");
            java.util.regex.Matcher matcher = pattern.matcher(firstFileName);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 提取()之间的内容作为标题
            pattern = java.util.regex.Pattern.compile("\\(([^)]+)\\)");
            matcher = pattern.matcher(firstFileName);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return commonPrefix;
    }
    
    /**
     * 找出多个字符串的最长公共前缀
     */
    private String findLongestCommonPrefix(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        
        // 取第一个字符串作为基准
        String prefix = strings.get(0);
        
        // 遍历所有字符串，逐步缩短前缀直到找到所有字符串都包含的公共前缀
        for (String str : strings) {
            // 如果当前前缀比字符串长，缩短前缀
            while (str.length() < prefix.length() || !str.startsWith(prefix)) {
                prefix = prefix.substring(0, prefix.length() - 1);
                
                // 如果前缀为空，直接返回
                if (prefix.isEmpty()) {
                    return "";
                }
            }
        }
        
        // 清理公共前缀末尾的特殊字符和文件格式信息
        return cleanCollectionName(prefix);
    }
    
    /**
     * 清理合集名称，去除末尾的特殊字符和文件格式信息
     */
    private String cleanCollectionName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        
        try {
            // 去除末尾的文件格式信息（如 [WAV], (FLAC) 等）
            String cleaned = name.replaceAll("\\[\\w+\\]$|", "").trim();
            
            // 清理末尾的特殊字符（使用更安全的正则表达式）
            cleaned = cleaned.replaceAll("[\\s\\-\\_\\.\\,\\!\\?\\;\\:\'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\(\\)\\{\\}\\>\\<\\《\\》]+$", "");
            
            return cleaned;
        } catch (Exception e) {
            // 如果正则表达式处理失败，返回原始名称的简化版本
            logError("清理合集名称失败: " + e.getMessage());
            return name.replaceAll("[^\\w\\u4e00-\\u9fa5]", "").trim();
        }
    }
    
    @Override
    public void reload() {
        super.reload();
        // 重新加载时清空处理记录
        parentDirClusters.clear();
    }
}