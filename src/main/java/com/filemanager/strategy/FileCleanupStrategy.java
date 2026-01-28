/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.components.CleanupUIConfig;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.CleanupParams;
import com.filemanager.strategy.cleanup.CleanupMode;
import com.filemanager.strategy.duplicate.DuplicateStrategyManager;
import com.filemanager.tool.file.DeleteExecutor;
import com.filemanager.tool.file.DuplicateAnalyzer;
import com.filemanager.type.ScanTarget;
import javafx.scene.Node;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * 文件清理策略
 * 支持：文件去重、文件夹去重、空目录清理
 * 删除方式：直接删除、伪删除（归档到垃圾箱）
 */
public class FileCleanupStrategy extends IAppStrategy {

    // --- 组件引用 --- 
    private final CleanupUIConfig uiConfig;
    private final CleanupParams params;
    private DuplicateAnalyzer analyzer;
    private DeleteExecutor executor;
    private DuplicateStrategyManager strategyManager;

    public FileCleanupStrategy() {
        uiConfig = new CleanupUIConfig();
        params = new CleanupParams();
    }

    @Override
    public String getName() {
        return "文件清理与去重";
    }

    @Override
    public String getDescription() {
        return "智能识别重复文件/文件夹、清理空目录、合并同名父子文件夹。支持按盘符结构伪删除。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL;
    }

    @Override
    public Node getConfigNode() {
        return uiConfig.getConfigNode();
    }

    @Override
    public void captureParams() {
        params.captureParams(uiConfig);
        // 初始化分析器和执行器
        analyzer = new DuplicateAnalyzer(params);
        long taskStartTimestamp = app != null ? app.getTaskStartTimStamp() : System.currentTimeMillis();
        executor = new DeleteExecutor(params, taskStartTimestamp);
        
        // 初始化去重策略管理器
        strategyManager = DuplicateStrategyManager.createDefaultManager(
                params.isKeepLargest(),
                params.isKeepEarliest(), // 注意：这里使用keepEarliest，与keepNewest相反
                params.isAudioSpecial(),
                params.getKeepExt()
        );
    }

    @Override
    public void saveConfig(Properties props) {
        params.saveConfig(props);
    }

    @Override
    public void loadConfig(Properties props) {
        params.loadConfig(props, uiConfig);
        // 初始化分析器和执行器
        analyzer = new DuplicateAnalyzer(params);
        long taskStartTimestamp = app != null ? app.getTaskStartTimStamp() : System.currentTimeMillis();
        executor = new DeleteExecutor(params, taskStartTimestamp);
        
        // 初始化去重策略管理器
        strategyManager = DuplicateStrategyManager.createDefaultManager(
                params.isKeepLargest(),
                params.isKeepEarliest(), // 注意：这里使用keepEarliest，与keepNewest相反
                params.isAudioSpecial(),
                params.getKeepExt()
        );
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        if (analyzer == null) {
            // 如果analyzer还没初始化，先初始化
            analyzer = new DuplicateAnalyzer(params);
        }
        // 调用分析器进行分析
        return analyzer.analyze(rec.getFileHandle());
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (executor == null) {
            // 如果executor还没初始化，先初始化
            long taskStartTimestamp = app != null ? app.getTaskStartTimStamp() : System.currentTimeMillis();
            executor = new DeleteExecutor(params, taskStartTimestamp);
        }
        // 调用执行器执行删除操作
        executor.execute(rec);
    }

    /**
     * 获取去重策略管理器
     * @return 去重策略管理器
     */
    public DuplicateStrategyManager getStrategyManager() {
        return strategyManager;
    }
}
