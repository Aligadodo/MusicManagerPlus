/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-25 
 */
package com.filemanager.strategy.ncm;

import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.ncm.model.NcmDump;
import com.filemanager.type.OperationType;
import com.jfoenix.controls.JFXCheckBox;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * NCM转换策略
 * 负责NCM文件的转换功能
 */
public class NcmConvertStrategy extends NcmBaseStrategy {
    // UI组件
    private final CheckBox chkDeleteSource;
    
    // 运行时参数
    private boolean pDeleteSource;
    
    public NcmConvertStrategy() {
        super("ncm_convert");
        
        // NCM转换选项
        chkDeleteSource = new JFXCheckBox("转换后删除源.ncm文件");
        chkDeleteSource.setSelected(false);
    }
    
    @Override
    public String getName() {
        return "NCM转换";
    }
    
    @Override
    public Node getConfigNode() {
        VBox configBox = new VBox();
        configBox.setSpacing(10);
        
        configBox.getChildren().addAll(
            StyleFactory.createChapter("NCM转换选项"),
            chkDeleteSource,
            StyleFactory.createSeparator(),
            pathSelection.getConfigNode()
        );
        
        return configBox;
    }
    
    @Override
    public void captureParams() {
        pDeleteSource = chkDeleteSource.isSelected();
        pathSelection.captureParams();
    }
    
    @Override
    public void saveConfig(Properties props) {
        pathSelection.saveConfig(props);
        props.setProperty("ncm_convert_delete_source", String.valueOf(chkDeleteSource.isSelected()));
    }
    
    @Override
    public void loadConfig(Properties props) {
        pathSelection.loadConfig(props);
        if (props.containsKey("ncm_convert_delete_source")) {
            chkDeleteSource.setSelected(Boolean.parseBoolean(props.getProperty("ncm_convert_delete_source")));
        }
    }
    
    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        List<ChangeRecord> result = new ArrayList<>();
        
        File file = currentRecord.getFileHandle();
        
        if (file.isFile() && file.getName().toLowerCase().endsWith(".ncm")) {
            ChangeRecord record = new ChangeRecord(file.getName(), file.getName(), file, true,
                    getOutputPath(file), OperationType.NCM_CONVERT);
            result.add(record);
        }
        
        return result;
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File ncmFile = rec.getFileHandle();
        String targetDirPath = rec.getNewPath();
        
        log("开始转换NCM文件: " + ncmFile.getName());
        
        // 确定输出目录
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        // 使用NcmDump执行转换
        NcmDump ncmDump = new NcmDump(ncmFile, targetDir);
        ncmDump.execute();
        
        if (pDeleteSource) {
            if (ncmFile.delete()) {
                log("已删除源NCM文件: " + ncmFile.getName());
            } else {
                logError("无法删除源NCM文件: " + ncmFile.getName());
            }
        }
        
        log("NCM文件转换完成: " + ncmFile.getName());
    }
    
    @Override
    public String getDescription() {
        return "NCM文件转换功能";
    }
    
    @Override
    public com.filemanager.type.ScanTarget getTargetType() {
        return com.filemanager.type.ScanTarget.FILES_ONLY; // 只支持文件
    }
}
