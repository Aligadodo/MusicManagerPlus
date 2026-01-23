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

import com.filemanager.strategy.AbstractFfmpegStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.file.FileTypeUtil;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.LanguageUtil;
import com.filemanager.util.file.FileExistsChecker;
import com.google.common.collect.Lists;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import com.filemanager.app.tools.display.StyleFactory;

import java.io.File;
import java.util.*;

/**
 * 音频转换策略 (v19.6 CD Mode Fix)
 * 优化点：
 * 1. 修复了 Lambda 表达式中变量非 effectively final 的编译错误。
 * 2. 完善了 CD 模式的参数锁定逻辑，防止被通用参数覆盖。
 */
public class AudioConverterStrategy extends AbstractFfmpegStrategy {
    // --- 新增配置项 ---    
    protected final CheckBox chkSkipCueTracks;
    
    // --- 运行时参数 ---    
    protected boolean pSkipCueTracks;

    public AudioConverterStrategy() {
        super();
        
        // 初始化"不处理音轨转换"复选框
        chkSkipCueTracks = new CheckBox("当音频文件大于100MB且同目录下有.cue文件时，跳过处理");
        chkSkipCueTracks.setTooltip(new javafx.scene.control.Tooltip("当启用此选项时，对于大于100MB的音频文件，如果同目录下存在.cue文件，则会跳过转换处理"));
        chkSkipCueTracks.setSelected(true); // 默认开启
    }

    @Override
    public String getDefaultDirPrefix() {
        return "Convert";
    }

    @Override
    public String getName() {
        return "音频格式转换";
    }

    @Override
    public String getDescription() {
        return "高品质音频转换。支持参数微调、乱码修复及智能覆盖检测等。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        Node parentConfig = super.getConfigNode();
        
        // 创建新的配置面板，包含"不处理音轨转换"选项
        Node skipCueTracksOption = StyleFactory.createVBoxPanel(
                StyleFactory.createChapter("智能跳过选项"),
                chkSkipCueTracks
        );
        
        // 将新的配置面板添加到父配置面板中
        return StyleFactory.createVBoxPanel(
                parentConfig,
                StyleFactory.createSeparator(),
                skipCueTracksOption
        );
    }

    @Override
    public void captureParams() {
        super.captureParams();
        
        // 捕获"不处理音轨转换"选项的参数
        pSkipCueTracks = chkSkipCueTracks.isSelected();
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        
        // 保存"不处理音轨转换"选项的配置
        props.setProperty("ac_skip_cue_tracks", String.valueOf(pSkipCueTracks));
    }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        
        // 加载"不处理音轨转换"选项的配置
        if (props.containsKey("ac_skip_cue_tracks")) {
            chkSkipCueTracks.setSelected(Boolean.parseBoolean(props.getProperty("ac_skip_cue_tracks")));
        }
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.CONVERT) return;
        super.execute(rec);
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        File virtualInput = new File(rec.getNewPath());
        String name = virtualInput.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1) {
            return Collections.emptyList();
        }
        if (!FileTypeUtil.isMusicFile(rec.getFileHandle())) {
            return Collections.emptyList();
        }
        
        // 新增逻辑：当启用了"不处理音轨转换"选项时，检查音频文件大小是否大于100MB，并且同目录下是否存在.cue文件
        if (pSkipCueTracks) {
            File actualFile = rec.getFileHandle();
            // 检查文件大小是否大于100MB
            if (actualFile.length() > 100 * 1024 * 1024) { // 100MB
                // 检查同目录下是否存在.cue文件
                File parentDir = actualFile.getParentFile();
                if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                    File[] files = parentDir.listFiles((dir, filename) -> filename.toLowerCase().endsWith(".cue"));
                    if (files != null && files.length > 0) {
                        // 同目录下存在.cue文件，跳过处理
                        log("跳过处理：" + actualFile.getName() + " (大于100MB且同目录下存在.cue文件)");
                        return Collections.emptyList();
                    }
                }
            }
        }
        
        Map<String, String> param = getParams(virtualInput.getParentFile(), name);
        String newName = name.substring(0, dotIndex) + "." + param.get("format");
        
        // 自动格式化目标文件名
        if (Boolean.parseBoolean(param.getOrDefault("autoFormatFilename", "true"))) {
            newName = LanguageUtil.toSimpleChinese(newName).trim();
        }
        
        File targetFile = new File(param.get("parentPath"), newName);
        ExecStatus status = ExecStatus.PENDING;
        
        // 创建文件存在检查参数
        FileExistsChecker.FileExistsParams checkParams = new FileExistsChecker.FileExistsParams()
                .enableCaseInsensitive()
                .enableSimplifiedChinese()
                .enableTrim();
        
        boolean targetExists = FileExistsChecker.checkFileExists(targetFile.getParentFile(), newName, checkParams);
        if (targetExists && !pOverwrite) {
            return Collections.emptyList();
        }
        if (param.containsKey("doubleCheckParentPath")) {
            File doubleCheckParentDir = new File(param.get("doubleCheckParentPath"));
            if (FileExistsChecker.checkFileExists(doubleCheckParentDir, newName, checkParams) && !pOverwrite) {
                return Collections.emptyList();
            }
        }
        return Lists.newArrayList(new ChangeRecord(rec.getOriginalName(), targetFile.getName(),
                rec.getFileHandle(), true, targetFile.getAbsolutePath(), OperationType.CONVERT, param, status));
    }
}