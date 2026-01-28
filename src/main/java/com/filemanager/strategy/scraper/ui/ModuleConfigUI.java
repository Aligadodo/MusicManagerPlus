package com.filemanager.strategy.scraper.ui;

import com.filemanager.app.tools.display.FloatingTooltip;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.strategy.scraper.config.ModuleConfig;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/**
 * 模块配置UI组件
 * 用于配置各个刮削模块的参数
 */
public class ModuleConfigUI extends VBox {
    private final ModuleConfig config;
    private final CheckBox chkEnabled;
    private final JFXComboBox<String> cbSaveMode;
    private final JFXComboBox<String> cbDuplicateMode;
    private final CheckBox chkUseCache;
    
    public ModuleConfigUI(ModuleConfig config) {
        this.config = config;
        
        chkEnabled = new CheckBox("启用" + config.getModuleName());
        chkEnabled.setSelected(config.isEnabled());
        
        ArrayList<String> enabledTooltipLines = new ArrayList<>();
        enabledTooltipLines.add("参数名称：启用" + config.getModuleName());
        enabledTooltipLines.add("参数用途：控制是否启用该刮削模块");
        enabledTooltipLines.add("说明：" + config.getModuleDescription());
        FloatingTooltip.bindToNode(chkEnabled, "模块配置", enabledTooltipLines);
        
        cbSaveMode = new JFXComboBox<>();
        cbSaveMode.getItems().addAll("嵌入到文件", "保存为独立文件", "同时嵌入和保存");
        cbSaveMode.getSelectionModel().select(0);
        
        ArrayList<String> saveModeTooltipLines = new ArrayList<>();
        saveModeTooltipLines.add("参数名称：保存方式");
        saveModeTooltipLines.add("参数用途：设置刮削结果的保存方式");
        saveModeTooltipLines.add("选项：");
        saveModeTooltipLines.add("- 嵌入到文件：将数据嵌入到音频文件中（如歌词）");
        saveModeTooltipLines.add("- 保存为独立文件：将数据保存为独立文件（如.lrc、.jpg）");
        saveModeTooltipLines.add("- 同时嵌入和保存：既嵌入文件又保存独立文件");
        FloatingTooltip.bindToNode(cbSaveMode, "模块配置", saveModeTooltipLines);
        
        cbDuplicateMode = new JFXComboBox<>();
        cbDuplicateMode.getItems().addAll("跳过已有文件", "覆盖已有文件", "重命名新文件");
        cbDuplicateMode.getSelectionModel().select(0);
        
        ArrayList<String> duplicateTooltipLines = new ArrayList<>();
        duplicateTooltipLines.add("参数名称：重复处理方式");
        duplicateTooltipLines.add("参数用途：设置遇到已有文件时的处理方式");
        duplicateTooltipLines.add("选项：");
        duplicateTooltipLines.add("- 跳过已有文件：不处理已存在的文件");
        duplicateTooltipLines.add("- 覆盖已有文件：覆盖已存在的文件");
        duplicateTooltipLines.add("- 重命名新文件：重命名新文件以避免冲突");
        FloatingTooltip.bindToNode(cbDuplicateMode, "模块配置", duplicateTooltipLines);
        
        chkUseCache = new CheckBox("使用缓存");
        chkUseCache.setSelected(config.isUseCache());
        
        ArrayList<String> cacheTooltipLines = new ArrayList<>();
        cacheTooltipLines.add("参数名称：使用缓存");
        cacheTooltipLines.add("参数用途：缓存已刮削的数据，避免重复请求");
        cacheTooltipLines.add("优势：");
        cacheTooltipLines.add("- 提高处理速度");
        cacheTooltipLines.add("- 减少网络请求");
        cacheTooltipLines.add("- 降低服务器压力");
        FloatingTooltip.bindToNode(chkUseCache, "模块配置", cacheTooltipLines);
        
        setupListeners();
        setupUI();
    }
    
    private void setupListeners() {
        chkEnabled.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setEnabled(newVal);
            updateUIState();
        });
        
        cbSaveMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("嵌入到文件".equals(newVal)) {
                config.setSaveMode(ModuleConfig.SaveMode.EMBEDDED);
            } else if ("保存为独立文件".equals(newVal)) {
                config.setSaveMode(ModuleConfig.SaveMode.SEPARATE_FILE);
            } else {
                config.setSaveMode(ModuleConfig.SaveMode.BOTH);
            }
        });
        
        cbDuplicateMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("跳过已有文件".equals(newVal)) {
                config.setDuplicateMode(ModuleConfig.DuplicateMode.SKIP);
            } else if ("覆盖已有文件".equals(newVal)) {
                config.setDuplicateMode(ModuleConfig.DuplicateMode.OVERWRITE);
            } else {
                config.setDuplicateMode(ModuleConfig.DuplicateMode.RENAME);
            }
        });
        
        chkUseCache.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setUseCache(newVal);
        });
    }
    
    private void setupUI() {
        setSpacing(10);
        
        HBox saveModeLine = StyleFactory.createParamPairLine("保存方式:", cbSaveMode);
        HBox duplicateModeLine = StyleFactory.createParamPairLine("重复处理:", cbDuplicateMode);
        
        getChildren().addAll(
                chkEnabled,
                saveModeLine,
                duplicateModeLine,
                chkUseCache
        );
        
        updateUIState();
    }
    
    private void updateUIState() {
        boolean enabled = config.isEnabled();
        cbSaveMode.setDisable(!enabled);
        cbDuplicateMode.setDisable(!enabled);
        chkUseCache.setDisable(!enabled);
    }
    
    public void captureParams() {
        config.setEnabled(chkEnabled.isSelected());
        config.setUseCache(chkUseCache.isSelected());
        
        String saveMode = cbSaveMode.getValue();
        if ("嵌入到文件".equals(saveMode)) {
            config.setSaveMode(ModuleConfig.SaveMode.EMBEDDED);
        } else if ("保存为独立文件".equals(saveMode)) {
            config.setSaveMode(ModuleConfig.SaveMode.SEPARATE_FILE);
        } else {
            config.setSaveMode(ModuleConfig.SaveMode.BOTH);
        }
        
        String duplicateMode = cbDuplicateMode.getValue();
        if ("跳过已有文件".equals(duplicateMode)) {
            config.setDuplicateMode(ModuleConfig.DuplicateMode.SKIP);
        } else if ("覆盖已有文件".equals(duplicateMode)) {
            config.setDuplicateMode(ModuleConfig.DuplicateMode.OVERWRITE);
        } else {
            config.setDuplicateMode(ModuleConfig.DuplicateMode.RENAME);
        }
    }
    
    public void loadFromConfig(ModuleConfig config) {
        chkEnabled.setSelected(config.isEnabled());
        chkUseCache.setSelected(config.isUseCache());
        
        switch (config.getSaveMode()) {
            case EMBEDDED:
                cbSaveMode.getSelectionModel().select("嵌入到文件");
                break;
            case SEPARATE_FILE:
                cbSaveMode.getSelectionModel().select("保存为独立文件");
                break;
            case BOTH:
                cbSaveMode.getSelectionModel().select("同时嵌入和保存");
                break;
        }
        
        switch (config.getDuplicateMode()) {
            case SKIP:
                cbDuplicateMode.getSelectionModel().select("跳过已有文件");
                break;
            case OVERWRITE:
                cbDuplicateMode.getSelectionModel().select("覆盖已有文件");
                break;
            case RENAME:
                cbDuplicateMode.getSelectionModel().select("重命名新文件");
                break;
        }
        
        updateUIState();
    }
    
    public ModuleConfig getConfig() {
        return config;
    }
}