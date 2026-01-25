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

import com.filemanager.app.tools.display.FloatingTooltip;
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
        
        // 添加悬浮提示信息
        java.util.List<String> tooltipLines = new java.util.ArrayList<>();
        tooltipLines.add("参数名称：转换后删除源.ncm文件");
        tooltipLines.add("参数用途：用于控制转换完成后是否删除原始的.ncm文件");
        tooltipLines.add("示例：");
        tooltipLines.add("- 选中：转换完成后会自动删除源文件");
        tooltipLines.add("- 不选中：源文件会被保留");
        FloatingTooltip.bindToNode(chkDeleteSource, "NCM转换选项", tooltipLines);
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
            // 根据文件大小预先估计音频格式
            String estimatedFormat = estimateAudioFormatBySize(file);
            
            // 构建目标文件名
            String fileNameWithoutExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
            String targetFileName = fileNameWithoutExt + "." + estimatedFormat;
            
            // 构建完整的目标文件路径
            String targetDir = getOutputPath(file);
            String targetPath = targetDir + File.separator + targetFileName;
            
            ChangeRecord record = new ChangeRecord(file.getName(), targetFileName, file, true,
                    targetPath, OperationType.NCM_CONVERT);
            
            // 存储估计的音频格式到额外参数
            record.getExtraParams().put("estimatedFormat", estimatedFormat);
            
            result.add(record);
        }
        
        return result;
    }
    
    /**
     * 根据文件大小估计音频格式
     * @param file 源文件
     * @return 估计的音频格式
     */
    private String estimateAudioFormatBySize(File file) {
        long fileSize = file.length();
        
        // 根据文件大小粗略估计音频格式
        // 这里只是一个简单的估计，实际的音频格式需要在转换后通过 AudioTypeInspector 来诊断
        if (fileSize > 50 * 1024 * 1024) { // 大于50MB
            return "wav"; // 无损格式
        } else if (fileSize > 10 * 1024 * 1024) { // 大于10MB
            return "flac"; // 高品质MP3
        } else {
            return "mp3"; // 默认MP3
        }
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File ncmFile = rec.getFileHandle();
        String targetPath = rec.getNewPath();
        
        log("开始转换NCM文件: " + ncmFile.getName());
        
        // 确定输出目录
        File targetFile = new File(targetPath);
        File targetDir = targetFile.getParentFile();
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        // 使用NcmDump执行转换
        NcmDump ncmDump = new NcmDump(ncmFile, targetDir);
        ncmDump.execute();
        
        // 查找转换后的文件
        File convertedFile = findConvertedFile(targetDir, ncmFile);
        if (convertedFile != null) {
            // 使用AudioTypeInspector检测并修复文件类型
            try {
                com.filemanager.tool.file.AudioTypeInspector.FileTypeCheckResult checkResult = com.filemanager.tool.file.AudioTypeInspector.inspectHard(convertedFile);
                if (checkResult.success) {
                    if (checkResult.needsFix) {
                        // 需要修复文件类型
                        String filename = convertedFile.getName();
                        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
                        File newFile = new File(convertedFile.getParent(), nameWithoutExt + checkResult.suggestedExtension);
                        
                        // 重命名文件
                        if (convertedFile.renameTo(newFile)) {
                            log("文件类型修复完成: " + convertedFile.getName() + " -> " + newFile.getName());
                            // 更新targetFile为修复后的文件
                            targetFile = newFile;
                            // 更新ChangeRecord中的新路径
                            rec.setNewPath(newFile.getAbsolutePath());
                        } else {
                            logError("文件类型修复失败: 无法重命名文件");
                        }
                    } else {
                        log("文件类型正确，无需修复: " + convertedFile.getName());
                        // 更新ChangeRecord中的新路径
                        rec.setNewPath(convertedFile.getAbsolutePath());
                    }
                } else {
                    logError("文件类型检测失败: " + checkResult.message);
                }
            } catch (Exception e) {
                logError("文件类型检测和修复失败: " + e.getMessage());
            }
                    if (pDeleteSource) {
            if (ncmFile.delete()) {
                    log("已删除源NCM文件: " + ncmFile.getName());
                } else {
                    logError("无法删除源NCM文件: " + ncmFile.getName());
                }
            }
        } else {
            logError("无法找到转换后的文件");
        }
        

        
        log("NCM文件转换完成: " + ncmFile.getName());
    }
    
    /**
     * 查找转换后的文件
     * @param targetDir 目标目录
     * @param ncmFile 源NCM文件
     * @return 转换后的文件
     */
    private File findConvertedFile(File targetDir, File ncmFile) {
        String fileNameWithoutExt = ncmFile.getName().substring(0, ncmFile.getName().lastIndexOf('.'));
        
        // 查找与源文件名相似的文件（不包括.ncm扩展名）
        File[] files = targetDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().toLowerCase().endsWith(".ncm")) {
                    String fileBaseName = file.getName();
                    if (fileBaseName.startsWith(fileNameWithoutExt)) {
                        return file;
                    }
                }
            }
        }
        
        return null;
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
