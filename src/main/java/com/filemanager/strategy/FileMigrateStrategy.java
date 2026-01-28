/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.base.PathSelectionComponent;
import com.filemanager.strategy.base.ScopeSelectionComponent;
import com.filemanager.strategy.duplicate.DuplicateStrategyConfig;
import com.filemanager.strategy.duplicate.DuplicateStrategyManager;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.google.common.collect.Lists;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.filemanager.app.tools.display.FloatingTooltip;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileMigrateStrategy extends IAppStrategy {
    private final PathSelectionComponent pathSelectionComponent;
    private final ScopeSelectionComponent scopeSelectionComponent;
    private final ComboBox<String> cbOperationMode;
    private final CheckBox chkOverwriteExisting;
    private final CheckBox chkCleanEmpty;
    private final TextField txtFilePattern;
    private final CheckBox chkRequireFilePattern;
    private final CheckBox chkExcludeFilePattern;
    private final DuplicateStrategyConfig duplicateStrategyConfig;
    
    protected String pOperationMode; // COPY or MOVE
    protected boolean pOverwriteExisting;
    protected boolean pCleanEmpty;
    protected String pFilePattern;
    protected boolean pRequireFilePattern;
    protected boolean pExcludeFilePattern;
    protected DuplicateStrategyManager strategyManager;

    private static final String[] OPERATION_MODES = {
        "移动 (MOVE)",
        "复制 (COPY)"
    };

    public FileMigrateStrategy() {
        // 路径选择组件
        pathSelectionComponent = new PathSelectionComponent("fms");
        
        // 生效范围选择组件
        scopeSelectionComponent = new ScopeSelectionComponent("fms");
        
        // 操作模式选择
        cbOperationMode = new ComboBox<>();
        cbOperationMode.getItems().addAll(OPERATION_MODES);
        cbOperationMode.getSelectionModel().select(0);
        
        ArrayList<String> operationModeTooltipLines = new ArrayList<>();
        operationModeTooltipLines.add("参数名称：操作模式");
        operationModeTooltipLines.add("参数用途：选择文件的操作方式");
        operationModeTooltipLines.add("选项：");
        operationModeTooltipLines.add("- 移动：将文件从源位置移动到目标位置");
        operationModeTooltipLines.add("- 复制：保留源文件，在目标位置创建副本");
        FloatingTooltip.bindToNode(cbOperationMode, "文件批量归档设置", operationModeTooltipLines);
        
        // 文件模式输入
        txtFilePattern = new TextField();
        txtFilePattern.setPromptText("输入文件模式 (如: *.mp3,*.flac)");
        
        ArrayList<String> filePatternTooltipLines = new ArrayList<>();
        filePatternTooltipLines.add("参数名称：文件模式");
        filePatternTooltipLines.add("参数用途：设置前置条件检查的文件模式");
        filePatternTooltipLines.add("示例：");
        filePatternTooltipLines.add("- *.mp3,*.flac：匹配MP3和FLAC文件");
        filePatternTooltipLines.add("- *.jpg：匹配JPG图片文件");
        FloatingTooltip.bindToNode(txtFilePattern, "前置条件设置", filePatternTooltipLines);
        
        // 文件模式检查选项
        chkRequireFilePattern = new CheckBox("要求存在匹配文件");
        chkRequireFilePattern.setSelected(false);
        
        ArrayList<String> requireTooltipLines = new ArrayList<>();
        requireTooltipLines.add("参数名称：要求存在匹配文件");
        requireTooltipLines.add("参数用途：当勾选时，只有当目录中存在匹配文件模式的文件时才执行操作");
        FloatingTooltip.bindToNode(chkRequireFilePattern, "前置条件设置", requireTooltipLines);

        chkExcludeFilePattern = new CheckBox("排除存在匹配文件");
        chkExcludeFilePattern.setSelected(false);
        
        ArrayList<String> excludeTooltipLines = new ArrayList<>();
        excludeTooltipLines.add("参数名称：排除存在匹配文件");
        excludeTooltipLines.add("参数用途：当勾选时，当目录中存在匹配文件模式的文件时不执行操作");
        FloatingTooltip.bindToNode(chkExcludeFilePattern, "前置条件设置", excludeTooltipLines);

        // 覆盖选项
        chkOverwriteExisting = new CheckBox("覆盖已存在的文件");
        chkOverwriteExisting.setSelected(false);
        
        ArrayList<String> overwriteTooltipLines = new ArrayList<>();
        overwriteTooltipLines.add("参数名称：覆盖已存在文件");
        overwriteTooltipLines.add("参数用途：当目标位置已存在同名文件时的处理方式");
        overwriteTooltipLines.add("选项：");
        overwriteTooltipLines.add("- 启用：覆盖已存在的文件");
        overwriteTooltipLines.add("- 禁用：跳过已存在的文件");
        FloatingTooltip.bindToNode(chkOverwriteExisting, "文件批量归档设置", overwriteTooltipLines);

        // 清理空文件夹选项
        chkCleanEmpty = new CheckBox("清理源空文件夹");
        chkCleanEmpty.setSelected(false);
        
        ArrayList<String> cleanEmptyTooltipLines = new ArrayList<>();
        cleanEmptyTooltipLines.add("参数名称：清理空文件夹");
        cleanEmptyTooltipLines.add("参数用途：在移动文件后清理源位置的空文件夹");
        cleanEmptyTooltipLines.add("选项：");
        cleanEmptyTooltipLines.add("- 启用：移动后清理源空文件夹");
        cleanEmptyTooltipLines.add("- 禁用：移动后不清理源空文件夹");
        FloatingTooltip.bindToNode(chkCleanEmpty, "文件批量归档设置", cleanEmptyTooltipLines);
        
        // 去重策略配置
        duplicateStrategyConfig = new DuplicateStrategyConfig();
    }

    @Override
    public String getName() {
        return "文件批量归档和移动";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public void captureParams() {
        pathSelectionComponent.captureParams();
        scopeSelectionComponent.captureParams();
        duplicateStrategyConfig.captureParams();
        pOperationMode = cbOperationMode.getValue().contains("移动") ? "MOVE" : "COPY";
        pOverwriteExisting = chkOverwriteExisting.isSelected();
        pCleanEmpty = chkCleanEmpty.isSelected();
        pFilePattern = txtFilePattern.getText();
        pRequireFilePattern = chkRequireFilePattern.isSelected();
        pExcludeFilePattern = chkExcludeFilePattern.isSelected();
        
        // 获取去重策略管理器
        strategyManager = duplicateStrategyConfig.getStrategyManager();
    }

    @Override
    public String getDescription() {
        return "文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择。";
    }

    @Override
    public void saveConfig(Properties props) {
        pathSelectionComponent.saveConfig(props);
        scopeSelectionComponent.saveConfig(props);
        duplicateStrategyConfig.saveConfig(props);
        props.setProperty("fms_operation_mode", cbOperationMode.getValue());
        props.setProperty("fms_overwrite", String.valueOf(chkOverwriteExisting.isSelected()));
        props.setProperty("fms_clean_empty", String.valueOf(chkCleanEmpty.isSelected()));
        props.setProperty("fms_file_pattern", txtFilePattern.getText());
        props.setProperty("fms_require_file_pattern", String.valueOf(chkRequireFilePattern.isSelected()));
        props.setProperty("fms_exclude_file_pattern", String.valueOf(chkExcludeFilePattern.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        pathSelectionComponent.loadConfig(props);
        scopeSelectionComponent.loadConfig(props);
        duplicateStrategyConfig.loadConfig(props);
        if (props.containsKey("fms_operation_mode")) {
            cbOperationMode.getSelectionModel().select(props.getProperty("fms_operation_mode"));
        }
        if (props.containsKey("fms_overwrite")) {
            chkOverwriteExisting.setSelected(Boolean.parseBoolean(props.getProperty("fms_overwrite")));
        }
        if (props.containsKey("fms_clean_empty")) {
            chkCleanEmpty.setSelected(Boolean.parseBoolean(props.getProperty("fms_clean_empty")));
        }
        if (props.containsKey("fms_file_pattern")) {
            txtFilePattern.setText(props.getProperty("fms_file_pattern"));
        }
        if (props.containsKey("fms_require_file_pattern")) {
            chkRequireFilePattern.setSelected(Boolean.parseBoolean(props.getProperty("fms_require_file_pattern")));
        }
        if (props.containsKey("fms_exclude_file_pattern")) {
            chkExcludeFilePattern.setSelected(Boolean.parseBoolean(props.getProperty("fms_exclude_file_pattern")));
        }
        
        // 获取去重策略管理器
        strategyManager = duplicateStrategyConfig.getStrategyManager();
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        box.getChildren().addAll(
                pathSelectionComponent.getConfigNode(),
                scopeSelectionComponent.getConfigNode(),
                StyleFactory.createChapter("操作设置"),
                StyleFactory.createParamPairLine("操作模式:", cbOperationMode),
                StyleFactory.createHBox(chkOverwriteExisting, chkCleanEmpty),
                StyleFactory.createChapter("前置条件设置"),
                StyleFactory.createParamPairLine("文件模式:", txtFilePattern),
                StyleFactory.createHBox(chkRequireFilePattern, chkExcludeFilePattern),
                duplicateStrategyConfig.getConfigNode()
        );
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.MOVE) {
            return;
        }
        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        
        if (!target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
        }
        
        // 检查目标文件是否存在
        if (target.exists() && !pOverwriteExisting) {
            // 使用去重策略处理
            List<File> duplicates = Arrays.asList(source, target);
            List<File> processedFiles = strategyManager.processDuplicates(duplicates);
            
            // 如果处理后第一个文件不是源文件，则跳过
            if (!processedFiles.get(0).equals(source)) {
                log("跳过文件（根据去重策略）: " + source.getName());
                return;
            }
        }
        
        if ("MOVE".equals(pOperationMode)) {
            Files.move(source.toPath(), target.toPath(), 
                pOverwriteExisting ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.ATOMIC_MOVE);
        } else { // COPY
            Files.copy(source.toPath(), target.toPath(), 
                pOverwriteExisting ? StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.COPY_ATTRIBUTES);
        }

        if ("MOVE".equals(pOperationMode) && pCleanEmpty && "true".equals(rec.getExtraParams().get("cleanSource"))) {
            File parent = source.getParentFile();
            if (parent != null && parent.isDirectory() && Objects.requireNonNull(parent.list()).length == 0) {
                parent.delete();
            }
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        // 检查生效范围
        if (!isInScope(rec.getFileHandle())) {
            return Collections.emptyList();
        }
        
        // 检查前置条件
        if (!checkPreconditions(rec.getFileHandle())) {
            return Collections.emptyList();
        }
        
        // 构建目标路径
        String targetPath = buildTargetPath(rec.getFileHandle());
        if (targetPath == null) {
            return Collections.emptyList();
        }
        
        File targetFile = new File(targetPath);

        if (!pOverwriteExisting && targetFile.exists()) {
            log("跳过已存在的文件: " + targetFile.getName());
            return Collections.emptyList();
        }

        Map<String, String> extraParams = new HashMap<>();
        if ("MOVE".equals(pOperationMode) && pCleanEmpty) {
            extraParams.put("cleanSource", "true");
        }

        OperationType opType = OperationType.MOVE;
        return Lists.newArrayList(new ChangeRecord(rec.getOriginalName(), targetFile.getName(), rec.getFileHandle(), true,
                targetFile.getAbsolutePath(), opType, extraParams, ExecStatus.PENDING));
    }

    private boolean isInScope(File file) {
        String scope = scopeSelectionComponent.getScope();
        if ("全部".equals(scope)) {
            return true;
        } else if ("文件".equals(scope)) {
            return file.isFile();
        } else if ("文件夹".equals(scope)) {
            return file.isDirectory();
        }
        return true;
    }

    private boolean checkPreconditions(File file) {
        File parentDir = file.getParentFile();
        if (parentDir == null) {
            parentDir = file.isDirectory() ? file : new File(".");
        }
        
        // 检查文件模式前置条件
        if (!pFilePattern.isEmpty()) {
            boolean hasMatchingFile = hasMatchingFiles(parentDir, pFilePattern);
            
            if (pRequireFilePattern && !hasMatchingFile) {
                log("跳过文件（缺少要求的文件模式）: " + file.getName());
                return false;
            }
            
            if (pExcludeFilePattern && hasMatchingFile) {
                log("跳过文件（存在排除的文件模式）: " + file.getName());
                return false;
            }
        }
        
        return true;
    }

    private boolean hasMatchingFile(File directory, String pattern) {
        String[] patterns = pattern.split(",");
        for (String p : patterns) {
            String trimmedPattern = p.trim();
            File[] files = directory.listFiles(f -> matchesPattern(f.getName(), trimmedPattern));
            if (files != null && files.length > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMatchingFiles(File directory, String pattern) {
        if (!directory.isDirectory()) {
            return false;
        }
        
        String[] patterns = pattern.split(",");
        for (String p : patterns) {
            String trimmedPattern = p.trim();
            File[] files = directory.listFiles(f -> matchesPattern(f.getName(), trimmedPattern));
            if (files != null && files.length > 0) {
                return true;
            }
        }
        
        return false;
    }

    private boolean matchesPattern(String fileName, String pattern) {
        // 简单的通配符匹配
        String regex = pattern.replace(".", "\\.")
                             .replace("*", ".*")
                             .replace("?", ".");
        return fileName.matches(regex);
    }

    private String buildTargetPath(File sourceFile) {
        String basePath = pathSelectionComponent.getOutputPath(sourceFile);
        if (basePath == null || basePath.isEmpty()) {
            return null;
        }
        
        return new File(basePath, sourceFile.getName()).getAbsolutePath();
    }
}
