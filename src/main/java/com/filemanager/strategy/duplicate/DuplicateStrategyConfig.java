/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy.duplicate;

import com.filemanager.app.tools.display.FloatingTooltip;
import com.filemanager.app.tools.display.StyleFactory;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Properties;

/**
 * 去重策略配置组件
 * 用于在UI中配置去重策略
 */
public class DuplicateStrategyConfig {
    private final JFXComboBox<String> cbStrategy;
    private final CheckBox chkKeepLargest;
    private final CheckBox chkKeepNewest;
    private final CheckBox chkAudioSpecial;
    private final JFXTextField txtKeepExt;
    
    private final DuplicateStrategyManager strategyManager;
    
    private boolean pKeepLargest;
    private boolean pKeepNewest;
    private boolean pAudioSpecial;
    private String pKeepExt;
    private String pSelectedStrategy;
    
    /**
     * 构造函数
     */
    public DuplicateStrategyConfig() {
        // 策略选择
        cbStrategy = new JFXComboBox<>();
        cbStrategy.getItems().addAll("保留最佳版本", "添加序号");
        cbStrategy.getSelectionModel().select(0);
        
        ArrayList<String> strategyTooltipLines = new ArrayList<>();
        strategyTooltipLines.add("参数名称：去重策略");
        strategyTooltipLines.add("参数用途：选择处理重复文件的策略");
        strategyTooltipLines.add("选项：");
        strategyTooltipLines.add("- 保留最佳版本：根据文件质量、大小等因素选择保留最佳版本");
        strategyTooltipLines.add("- 添加序号：对同名文件添加序号以避免冲突");
        FloatingTooltip.bindToNode(cbStrategy, "去重策略设置", strategyTooltipLines);
        
        // 保留最大文件
        chkKeepLargest = new CheckBox("保留最大文件");
        chkKeepLargest.setSelected(true);
        
        ArrayList<String> keepLargestTooltipLines = new ArrayList<>();
        keepLargestTooltipLines.add("参数名称：保留最大文件");
        keepLargestTooltipLines.add("参数用途：在选择最佳版本时，优先保留文件大小较大的文件");
        FloatingTooltip.bindToNode(chkKeepLargest, "去重策略设置", keepLargestTooltipLines);
        
        // 保留最新文件
        chkKeepNewest = new CheckBox("保留最新文件");
        chkKeepNewest.setSelected(true);
        
        ArrayList<String> keepNewestTooltipLines = new ArrayList<>();
        keepNewestTooltipLines.add("参数名称：保留最新文件");
        keepNewestTooltipLines.add("参数用途：在选择最佳版本时，优先保留修改时间较新的文件");
        FloatingTooltip.bindToNode(chkKeepNewest, "去重策略设置", keepNewestTooltipLines);
        
        // 音频文件特殊处理
        chkAudioSpecial = new CheckBox("音频文件自动选择最优音质");
        chkAudioSpecial.setSelected(true);
        
        ArrayList<String> audioSpecialTooltipLines = new ArrayList<>();
        audioSpecialTooltipLines.add("参数名称：音频文件特殊处理");
        audioSpecialTooltipLines.add("参数用途：对音频文件进行特殊处理，根据码率等因素选择最佳版本");
        FloatingTooltip.bindToNode(chkAudioSpecial, "去重策略设置", audioSpecialTooltipLines);
        
        // 文件类型优先级顺序
        txtKeepExt = new JFXTextField();
        txtKeepExt.setPromptText("文件类型优先级顺序 (如: flac,mp3,wav)");
        
        ArrayList<String> keepExtTooltipLines = new ArrayList<>();
        keepExtTooltipLines.add("参数名称：文件类型优先级顺序");
        keepExtTooltipLines.add("参数用途：在选择最佳版本时，按照优先级顺序保留文件类型");
        keepExtTooltipLines.add("示例：");
        keepExtTooltipLines.add("- flac,mp3,wav：优先保留FLAC，其次MP3，最后WAV");
        keepExtTooltipLines.add("- jpg,png,gif：优先保留JPG，其次PNG，最后GIF");
        FloatingTooltip.bindToNode(txtKeepExt, "去重策略设置", keepExtTooltipLines);
        
        // 添加策略选择监听器
        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateUIForStrategy(newVal);
        });
        
        // 初始化策略管理器
        this.strategyManager = DuplicateStrategyManager.createDefaultManager(
                chkKeepLargest.isSelected(),
                chkKeepNewest.isSelected(),
                chkAudioSpecial.isSelected(),
                txtKeepExt.getText()
        );
    }
    
    /**
     * 获取配置节点
     * @return 配置节点
     */
    public Node getConfigNode() {
        VBox box = new VBox(10);
        
        // 去重策略设置
        VBox strategyBox = new VBox(10);
        strategyBox.getChildren().addAll(
                StyleFactory.createParamPairLine("重复文件模式:", cbStrategy)
        );
        
        // 子参数设置框
        VBox subParamsBox = new VBox(10);
        subParamsBox.getChildren().addAll(
                StyleFactory.createHBox(chkKeepLargest, chkKeepNewest),
                StyleFactory.createHBox(chkAudioSpecial),
                StyleFactory.createParamPairLine("文件类型优先级顺序:", txtKeepExt)
        );
        
        box.getChildren().addAll(
                strategyBox,
                subParamsBox
        );
        
        // 初始化UI状态
        updateUIForStrategy(cbStrategy.getValue());
        
        return box;
    }
    
    /**
     * 根据选择的策略更新UI
     * @param strategy 策略名称
     */
    private void updateUIForStrategy(String strategy) {
        if ("添加序号".equals(strategy)) {
            // 添加序号策略时隐藏这些选项
            chkKeepLargest.setVisible(false);
            chkKeepLargest.setManaged(false);
            chkKeepNewest.setVisible(false);
            chkKeepNewest.setManaged(false);
            chkAudioSpecial.setVisible(false);
            chkAudioSpecial.setManaged(false);
            txtKeepExt.setVisible(false);
            txtKeepExt.setManaged(false);
        } else {
            // 保留最佳版本策略时显示这些选项
            chkKeepLargest.setVisible(true);
            chkKeepLargest.setManaged(true);
            chkKeepNewest.setVisible(true);
            chkKeepNewest.setManaged(true);
            chkAudioSpecial.setVisible(true);
            chkAudioSpecial.setManaged(true);
            txtKeepExt.setVisible(true);
            txtKeepExt.setManaged(true);
        }
    }
    
    /**
     * 捕获参数
     */
    public void captureParams() {
        pSelectedStrategy = cbStrategy.getValue();
        pKeepLargest = chkKeepLargest.isSelected();
        pKeepNewest = chkKeepNewest.isSelected();
        pAudioSpecial = chkAudioSpecial.isSelected();
        pKeepExt = txtKeepExt.getText();
        
        // 更新策略管理器
        updateStrategyManager();
    }
    
    /**
     * 保存配置
     * @param props 属性对象
     */
    public void saveConfig(Properties props) {
        props.setProperty("dup_strategy", cbStrategy.getValue());
        props.setProperty("dup_keep_largest", String.valueOf(chkKeepLargest.isSelected()));
        props.setProperty("dup_keep_newest", String.valueOf(chkKeepNewest.isSelected()));
        props.setProperty("dup_audio_special", String.valueOf(chkAudioSpecial.isSelected()));
        props.setProperty("dup_keep_ext", txtKeepExt.getText());
    }
    
    /**
     * 加载配置
     * @param props 属性对象
     */
    public void loadConfig(Properties props) {
        if (props.containsKey("dup_strategy")) {
            cbStrategy.getSelectionModel().select(props.getProperty("dup_strategy"));
        }
        if (props.containsKey("dup_keep_largest")) {
            chkKeepLargest.setSelected(Boolean.parseBoolean(props.getProperty("dup_keep_largest")));
        }
        if (props.containsKey("dup_keep_newest")) {
            chkKeepNewest.setSelected(Boolean.parseBoolean(props.getProperty("dup_keep_newest")));
        }
        if (props.containsKey("dup_audio_special")) {
            chkAudioSpecial.setSelected(Boolean.parseBoolean(props.getProperty("dup_audio_special")));
        }
        if (props.containsKey("dup_keep_ext")) {
            txtKeepExt.setText(props.getProperty("dup_keep_ext"));
        }
        
        // 更新策略管理器
        updateStrategyManager();
    }
    
    /**
     * 获取策略管理器
     * @return 策略管理器
     */
    public DuplicateStrategyManager getStrategyManager() {
        return strategyManager;
    }
    
    /**
     * 更新策略管理器
     */
    private void updateStrategyManager() {
        // 移除旧策略
        strategyManager.addStrategy("保留最佳版本", new KeepBestVersionStrategy(
                pKeepLargest,
                pKeepNewest,
                pAudioSpecial,
                pKeepExt
        ));
        
        strategyManager.addStrategy("添加序号", new AddSequenceStrategy(
                true, // 保留原始文件
                " (%d)" // 序号格式
        ));
        
        // 设置当前策略
        strategyManager.setCurrentStrategy(pSelectedStrategy);
    }
}
