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
 * 文件归类策略：基于文件名相似度将文件/文件夹归类到合集文件夹中
 */
public class FileCollectionStrategy extends IAppStrategy {
    private final Slider slSimilarityThreshold;
    private final TextField txtCollectionSuffix;
    private final JFXComboBox<ScanTarget> cbTargetType;
    private final Spinner<Integer> spMinFiles;
    private final Spinner<Integer> spMinFileNameLength;
    private final TextField txtMustContainKeywords;
    private final TextField txtMustNotContainKeywords;
    private final CheckBox chkSkipCollections;
    private final Spinner<Integer> spMaxCollectionRatio;
    private final Slider slRecognitionStrictness;
    
    // 配置参数
    private double pThreshold;
    private String pCollectionSuffix;
    private ScanTarget pTargetType;
    private int pMinFiles;
    private int pMinFileNameLength;
    private List<String> pMustContainKeywords;
    private List<String> pMustNotContainKeywords;
    private boolean pSkipCollections;
    private double pMaxCollectionRatio;
    private double pRecognitionStrictness;
    
    // 内部使用：记录已处理的父目录和对应的文件集群
    private final Map<File, Map<String, List<ChangeRecord>>> parentDirClusters = Collections.synchronizedMap(new HashMap<>());
    
    public FileCollectionStrategy() {
        // 相似度阈值滑块 (0.0 - 1.0)
        slSimilarityThreshold = new Slider(0.0, 1.0, 0.5);
        slSimilarityThreshold.setShowTickMarks(true);
        slSimilarityThreshold.setShowTickLabels(true);
        slSimilarityThreshold.setMajorTickUnit(0.1);
        slSimilarityThreshold.setMinorTickCount(9);
        
        // 合集文件夹格式
        txtCollectionSuffix = new TextField("【合集】");
        txtCollectionSuffix.setPromptText("输入合集文件夹格式 (如：【合集】)...");
        
        // 目标类型选择
        cbTargetType = new JFXComboBox<>(FXCollections.observableArrayList(ScanTarget.values()));
        cbTargetType.setValue(ScanTarget.FOLDERS_ONLY); // 默认只对文件夹生效
        
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
        slRecognitionStrictness = new Slider(0.0, 1.0, 0.5);
        slRecognitionStrictness.setShowTickMarks(true);
        slRecognitionStrictness.setShowTickLabels(true);
        slRecognitionStrictness.setMajorTickUnit(0.1);
        slRecognitionStrictness.setMinorTickCount(9);
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
        pMinFiles = spMinFiles.getValue();
        pMinFileNameLength = spMinFileNameLength.getValue();
        
        // 处理关键词
        pMustContainKeywords = parseKeywords(txtMustContainKeywords.getText());
        pMustNotContainKeywords = parseKeywords(txtMustNotContainKeywords.getText());
        
        // 处理新的配置参数
        pSkipCollections = chkSkipCollections.isSelected();
        pMaxCollectionRatio = spMaxCollectionRatio.getValue() / 100.0;
        pRecognitionStrictness = slRecognitionStrictness.getValue();
        
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
        props.setProperty("fcs_min_files", String.valueOf(spMinFiles.getValue()));
        props.setProperty("fcs_min_filename_length", String.valueOf(spMinFileNameLength.getValue()));
        props.setProperty("fcs_must_contain", txtMustContainKeywords.getText());
        props.setProperty("fcs_must_not_contain", txtMustNotContainKeywords.getText());
        props.setProperty("fcs_skip_collections", String.valueOf(chkSkipCollections.isSelected()));
        props.setProperty("fcs_max_collection_ratio", String.valueOf(spMaxCollectionRatio.getValue()));
        props.setProperty("fcs_recognition_strictness", String.valueOf(slRecognitionStrictness.getValue()));
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
            return Collections.emptyList();
        }
        
        // 检查当前文件是否已经在合集文件夹中
        if (pSkipCollections && isInCollectionFolder(currentFile)) {
            return Collections.emptyList();
        }
        
        // 检查当前文件是否本身就是合集文件夹
        if (isCollectionFolder(currentFile)) {
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
        
        // 如果目录下的文件数量不足2个，跳过
        if (dirRecords.size() < 2) {
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }
        
        // 检查目录中是否大部分文件已经属于同一合集，如果是则不再执行合并
        if (isMostlySingleCollection(dirRecords)) {
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }
        
        // 检查父目录是否已经是合集文件夹
        if (isCollectionFolder(parentDir)) {
            // 标记此目录已处理
            parentDirClusters.put(parentDir, Collections.emptyMap());
            return Collections.emptyList();
        }
        
        // 对目录下的文件进行聚类
        Map<String, List<ChangeRecord>> clusters = clusterSimilarRecords(dirRecords);
        
        // 标记此目录已处理
        parentDirClusters.put(parentDir, clusters);
        
        // 生成变更记录
        List<ChangeRecord> changeRecords = new ArrayList<>();
        for (Map.Entry<String, List<ChangeRecord>> entry : clusters.entrySet()) {
            List<ChangeRecord> clusterRecords = entry.getValue();
            
            // 应用约束条件检查
            if (!isClusterValid(clusterRecords)) {
                continue;
            }
            
            // 提取合集名称
            List<File> clusterFiles = clusterRecords.stream()
                    .map(ChangeRecord::getFileHandle)
                    .collect(Collectors.toList());
            String collectionName = extractCollectionName(clusterFiles);
            
            // 创建目标合集文件夹路径
            Path targetDirPath = parentDir.toPath().resolve(collectionName + pCollectionSuffix);
            
            // 为集群中的每个文件生成变更记录
            for (ChangeRecord record : clusterRecords) {
                // 创建变更记录
                ChangeRecord changeRecord = new ChangeRecord(
                        record.getOriginalName(),
                        record.getNewName(),
                        record.getFileHandle(),
                        true,
                        targetDirPath.resolve(record.getFileHandle().getName()).toString(),
                        OperationType.COLLECT,
                        new HashMap<>(),
                        ExecStatus.PENDING
                );
                
                changeRecords.add(changeRecord);
            }
        }
        
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
        // 检查目录名称是否包含合集关键词或用户配置的合集文件夹格式
        return name.contains(pCollectionSuffix) || 
               name.contains("合集") || 
               name.contains("系列") || 
               name.contains("Collection") || 
               name.contains("Series") ||
               name.endsWith("合集") ||
               name.endsWith("系列") ||
               name.endsWith("Collection") ||
               name.endsWith("Series");
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
                
                if (similarity >= pThreshold) {
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
     * 计算两个字符串的相似度 (基于 Levenshtein 距离)
     */
    private double calculateSimilarity(String s1, String s2) {
        // 优化：改进噪音字符过滤，保留更多有意义的信息
        // 保留中文序号和括号内容，只去除真正的噪音字符
        String regex = "[\\s\\[\\]\\.\\,\\!\\?\\;\\:\\'\\\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]";
        
        // 特别处理中文序号（①, ②, ③等），将它们替换为统一标记以便比较
        String processed1 = s1.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "__NUMBER__");
        String processed2 = s2.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "__NUMBER__");
        
        // 去除噪音字符
        String str1 = processed1.replaceAll(regex, "");
        String str2 = processed2.replaceAll(regex, "");
        
        // 如果处理后的字符串为空，使用原始字符串
        if (str1.isEmpty() || str2.isEmpty()) {
            str1 = s1.replaceAll("\\s+", "");
            str2 = s2.replaceAll("\\s+", "");
        }
        
        // 计算基本相似度
        int distance = getLevenshteinDistance(str1, str2);
        int maxLen = Math.max(str1.length(), str2.length());
        double baseSimilarity = (maxLen == 0) ? 1.0 : 1.0 - ((double) distance / maxLen);
        
        // 额外检查：如果文件名包含相同的标题和不同的数字序号，提高相似度
        if (hasSameTitleDifferentNumber(s1, s2)) {
            baseSimilarity = Math.max(baseSimilarity, 0.9);
        }
        
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
        // 去除结尾的序号和格式信息
        String title = fileName.replaceAll("[\\\\d]+[\\\\s\\\\-]*$|[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳][\\\\s\\\\-]*$|[\\\\[\\\\(].*[\\\\]\\\\)][\\\\s\\\\-]*$", "");
        // 去除噪音字符
        title = title.replaceAll("[\\\\s\\\\[\\\\]\\\\.\\\\,\\\\!\\\\?\\\\;\\\\:\\\\'\\\\\"\\\\`\\\\~\\\\|\\\\=\\\\+\\\\\\\\\\\\\\/\\\\#\\\\$\\\\%\\\\^\\\\&\\\\*\\\\_]", "");
        return title;
    }
    
    /**
     * 提取文件名中的数字部分
     */
    private String extractNumber(String fileName) {
        // 提取数字序号
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\\\d]+|[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳])");
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
                String regex = "[\\\\s\\\\[\\\\]《》\\\\-\\\\(\\\\)\\\\{\\\\}\\-.\\,\\!\\?\\;\\:\\'\\\"\\`\\~\\|\\=\\+\\\\\\\\\\/\\#\\$\\%\\^\\&\\*\\_]";
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
            String processed = name.replaceAll("[\\\\d]+[\\\\s\\\\-]*$|[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳][\\\\s\\\\-]*$|[\\\\[\\\\(].*[\\\\]\\\\)][\\\\s\\\\-]*$", "");
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
            cleaned = cleaned.replaceAll("[\\s\\-\\_\\.\\,\\!\\?\\;\\:\\'\\\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\(\\)\\{\\}\\>\\<\\《\\》]+$", "");
            
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