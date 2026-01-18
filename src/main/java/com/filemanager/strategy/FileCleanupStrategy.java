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
import com.filemanager.tool.file.DeleteExecutor;
import com.filemanager.tool.file.DuplicateAnalyzer;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import javafx.scene.Node;

import java.io.File;
import java.util.*;

/**
 * 文件清理策略
 * 支持：文件去重、文件夹去重、空目录清理
 * 删除方式：直接删除、伪删除（归档到垃圾箱）
 */
public class FileCleanupStrategy extends IAppStrategy {
    // 清理模式枚举
    public enum CleanupMode {
        DEDUP_FILES("同目录下的文件去重"),     // 文件去重
        DEDUP_FOLDERS("文件夹去重"),   // 文件夹去重
        REMOVE_EMPTY_DIRS("空目录清理"), // 空目录清理
        DIRECT_CLEANUP("直接清理");   // 直接清理模式

        private final String desc;

        CleanupMode(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    // 删除方式枚举
    public enum DeleteMethod {
        DIRECT_DELETE("直接删除"),       // 直接删除
        PSEUDO_DELETE("伪删除（归档到垃圾箱）"),       // 伪删除（归档到垃圾箱）
        ROLLBACKABLE_DELETE("可回滚删除");  // 可回滚删除

        private final String desc;

        DeleteMethod(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    // 文件大小范围枚举
    public enum FileSizeRange {
        ALL(0, Long.MAX_VALUE, "所有文件"),
        SMALL(0, 1024 * 1024, "小于1MB"),
        MEDIUM(1024 * 1024, 10 * 1024 * 1024, "1MB-10MB"),
        LARGE(10 * 1024 * 1024, 100 * 1024 * 1024, "10MB-100MB"),
        XLARGE(100 * 1024 * 1024, Long.MAX_VALUE, "大于100MB");

        private final long minSize;
        private final long maxSize;
        private final String desc;

        FileSizeRange(long minSize, long maxSize, String desc) {
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.desc = desc;
        }

        public long getMinSize() {
            return minSize;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public String getDesc() {
            return desc;
        }

        public boolean isInRange(long size) {
            return size >= minSize && size < maxSize;
        }

        @Override
        public String toString() {
            return desc;
        }
    }
    // --- 组件引用 ---
    private final CleanupUIConfig uiConfig;
    private final CleanupParams params;
    private DuplicateAnalyzer analyzer;
    private DeleteExecutor executor;

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
        return "智能识别重复文件/文件夹、清理空目录。支持按盘符结构伪删除。";
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
}
