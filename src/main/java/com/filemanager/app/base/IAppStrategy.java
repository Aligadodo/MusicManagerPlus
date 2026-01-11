/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.base;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.RuleCondition;
import com.filemanager.model.RuleConditionGroup;
import com.filemanager.type.ScanTarget;
import javafx.scene.Node;
import lombok.Getter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 28667
 */
@Getter
public abstract class IAppStrategy implements IAutoReloadAble{
    protected IAppController app;
    // 通用条件配置接口 (UI调用)
    // 通用前置条件 (所有策略都支持)
    @Getter
    protected List<RuleCondition> globalConditions = new ArrayList<>();
    @Getter
    // [修改] 升级为条件组列表 (OR关系)
    protected List<RuleConditionGroup> conditionGroups = new ArrayList<>();

    public void setContext(IAppController app) {
        this.app = app;
    }

    protected void log(String msg) {
        if (app != null) app.log(msg);
    }

    protected void logError(String msg) {
        if (app != null) app.logError(msg);
    }

    // [新增] 便捷日志方法 (子类可直接调用 log("xxx"))
    protected void invalidatePreview() {
        if (app != null) app.invalidatePreview("组件触发");
    }

    public abstract String getName();

    public abstract Node getConfigNode(); // 策略特有的配置UI

    // 核心分析逻辑
    public List<ChangeRecord> analyzeWithPreCheck(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        // 已经变更的文件不支持二次变更
        if (currentRecord.isChanged()) {
            return Collections.emptyList();
        }
        // 前置条件检查
        if (!checkConditions(currentRecord)) {
            return Collections.emptyList();
        }
        // 类型检查
        if (ScanTarget.FILES_ONLY == getTargetType() && currentRecord.getFileHandle().isDirectory()) {
            return Collections.emptyList();
        }
        if (ScanTarget.FOLDERS_ONLY == getTargetType() && currentRecord.getFileHandle().isFile()) {
            return Collections.emptyList();
        }
        return analyze(currentRecord, inputRecords, rootDirs);
    }

    // 核心分析逻辑
    public abstract List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs);

    // 核心执行逻辑
    public abstract void execute(ChangeRecord rec) throws Exception;

    // [修改] 校验逻辑：组间为 OR，只要有一个组满足即可
    protected boolean checkConditions(ChangeRecord rec) {
        File f = rec.getFileHandle();
        // 无条件则通过
        if (conditionGroups.isEmpty() && globalConditions.isEmpty()) {
            return true;
        }
        // 只要有一组满足 (组内是AND)，则通过
        for (RuleConditionGroup group : conditionGroups) {
            if (group.test(f)) {
                return true;
            }
        }
        for (RuleCondition c : globalConditions) {
            if (!c.test(f)) {
                return false;
            }
        }
        // 所有组都不满足
        return false;
    }

    public abstract ScanTarget getTargetType();

    public void captureParams() {
    }

    public abstract String getDescription();

    protected ChangeRecord getTargetFile(File file, Collection<ChangeRecord> changeRecords) {
        return changeRecords.stream().filter(changeRecord -> changeRecord.getFileHandle().equals(file) &&
                file.getName().equals(changeRecord.getFileHandle().getName())).findFirst().orElse(null);
    }

    protected List<ChangeRecord> getFilesUnderDir(File file, Collection<ChangeRecord> changeRecords) {
        return changeRecords.stream().filter(changeRecord -> changeRecord.getFileHandle().getParentFile().equals(file) &&
                file.getName().equals(changeRecord.getFileHandle().getParentFile().getName())).collect(Collectors.toList());
    }

}