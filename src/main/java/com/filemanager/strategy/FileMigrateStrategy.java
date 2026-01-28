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
import com.filemanager.model.RuleCondition;
import com.filemanager.strategy.base.PathSelectionComponent;
import com.filemanager.strategy.base.ScopeSelectionComponent;
import com.filemanager.strategy.duplicate.DuplicateStrategyConfig;
import com.filemanager.strategy.duplicate.DuplicateStrategyManager;
import com.filemanager.type.ConditionType;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.google.common.collect.Lists;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
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
    private final DuplicateStrategyConfig duplicateStrategyConfig;
    
    protected String pOperationMode; // COPY or MOVE
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
    }

    @Override
    public void loadConfig(Properties props) {
        pathSelectionComponent.loadConfig(props);
        scopeSelectionComponent.loadConfig(props);
        duplicateStrategyConfig.loadConfig(props);
        if (props.containsKey("fms_operation_mode")) {
            cbOperationMode.getSelectionModel().select(props.getProperty("fms_operation_mode"));
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
                StyleFactory.createParamPairLine("操作模式:", cbOperationMode),
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
        if (target.exists()) {
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
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } else { // COPY
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        // 检查生效范围
        if (!isInScope(rec.getFileHandle())) {
            return Collections.emptyList();
        }
        
        // 构建目标路径
        String targetPath = buildTargetPath(rec.getFileHandle());
        if (targetPath == null) {
            return Collections.emptyList();
        }
        
        File targetFile = new File(targetPath);

        if (targetFile.exists()) {
            log("跳过已存在的文件: " + targetFile.getName());
            return Collections.emptyList();
        }

        OperationType opType = OperationType.MOVE;
        return Lists.newArrayList(new ChangeRecord(rec.getOriginalName(), targetFile.getName(), rec.getFileHandle(), true,
                targetFile.getAbsolutePath(), opType, new HashMap<>(), ExecStatus.PENDING));
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

    private String buildTargetPath(File sourceFile) {
        String basePath = pathSelectionComponent.getOutputPath(sourceFile);
        if (basePath == null || basePath.isEmpty()) {
            return null;
        }
        
        return new File(basePath, sourceFile.getName()).getAbsolutePath();
    }
}
