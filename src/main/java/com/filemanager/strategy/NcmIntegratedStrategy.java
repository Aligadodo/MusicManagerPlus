/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-24 
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.ncm.NcmCacheScanStrategy;
import com.filemanager.strategy.ncm.NcmConvertStrategy;
import com.filemanager.strategy.ncm.NcmLyricDownloadStrategy;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NcmIntegratedStrategy extends IAppStrategy {
    // --- UI 组件 --- 
    private final JFXComboBox<String> cbFunction;
    
    // 策略实例
    private final NcmConvertStrategy convertStrategy;
    private final NcmCacheScanStrategy cacheScanStrategy;
    private final NcmLyricDownloadStrategy lyricDownloadStrategy;

    // 运行时参数
    private String pFunction;

    public NcmIntegratedStrategy() {
        super();
        
        // 创建功能选择下拉框
        cbFunction = new JFXComboBox<>();
        cbFunction.getItems().addAll("NCM转换", "缓存扫描", "歌词下载");
        cbFunction.getSelectionModel().select(0);

        // 初始化策略实例
        convertStrategy = new NcmConvertStrategy();
        cacheScanStrategy = new NcmCacheScanStrategy();
        lyricDownloadStrategy = new NcmLyricDownloadStrategy();
    }

    @Override
    public String getName() {
        return "网易云音乐工具集";
    }

    @Override
    public Node getConfigNode() {
        // 创建主面板容器
        VBox mainPanel = new VBox();
        mainPanel.setSpacing(10);

        // 功能选择部分
        VBox functionSelection = new VBox(
                StyleFactory.createChapter("功能选择"),
                StyleFactory.createParamPairLine("选择功能:", cbFunction));

        // 添加功能选择部分到主面板
        mainPanel.getChildren().add(functionSelection);

        // 根据当前选择的功能构建面板内容
        rebuildPanelContent(mainPanel, cbFunction.getValue());

        // 添加功能选择变化监听器
        cbFunction.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            rebuildPanelContent(mainPanel, newValue);
        });

        return mainPanel;
    }

    /**
     * 根据选择的功能重新构建面板内容
     */
    private void rebuildPanelContent(VBox mainPanel, String function) {
        // 清除除了功能选择部分之外的所有内容
        if (mainPanel.getChildren().size() > 1) {
            mainPanel.getChildren().subList(1, mainPanel.getChildren().size()).clear();
        }

        // 根据选择的功能添加相应的内容
        mainPanel.getChildren().addAll(
                StyleFactory.createSeparator(),
                getCurrentStrategy().getConfigNode()
        );
    }

    /**
     * 获取当前选择的策略
     */
    private IAppStrategy getCurrentStrategy() {
        String function = cbFunction.getValue();
        switch (function) {
            case "NCM转换":
                return convertStrategy;
            case "缓存扫描":
                return cacheScanStrategy;
            case "歌词下载":
                return lyricDownloadStrategy;
            default:
                return convertStrategy;
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords,
            List<File> rootDirs) {
        return getCurrentStrategy().analyze(currentRecord, inputRecords, rootDirs);
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        getCurrentStrategy().execute(rec);
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL; // 支持文件和目录
    }

    @Override
    public String getDescription() {
        return "网易云音乐工具集：支持NCM格式转换、缓存文件扫描和歌词下载";
    }

    @Override
    public void captureParams() {
        pFunction = cbFunction.getValue();
        getCurrentStrategy().captureParams();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("ncm_function", cbFunction.getValue());
        convertStrategy.saveConfig(props);
        cacheScanStrategy.saveConfig(props);
        lyricDownloadStrategy.saveConfig(props);
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("ncm_function")) {
            cbFunction.getSelectionModel().select(props.getProperty("ncm_function"));
        }
        convertStrategy.loadConfig(props);
        cacheScanStrategy.loadConfig(props);
        lyricDownloadStrategy.loadConfig(props);
    }
}
