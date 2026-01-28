package com.filemanager.strategy.scraper.ui;

import com.filemanager.app.tools.display.FloatingTooltip;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.strategy.scraper.config.AlbumInfoModuleConfig;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/**
 * 专辑信息模块配置UI组件
 * 用于配置专辑信息刮削模块的参数
 */
public class AlbumInfoConfigUI extends VBox {
    private final AlbumInfoModuleConfig config;
    private final CheckBox chkEnabled;
    private final JFXComboBox<String> cbSaveMode;
    private final JFXComboBox<String> cbDuplicateMode;
    private final CheckBox chkUseCache;
    private final CheckBox chkIncludeTrackList;
    private final CheckBox chkIncludeDescription;
    private final CheckBox chkIncludeCopyright;
    
    public AlbumInfoConfigUI(AlbumInfoModuleConfig config) {
        this.config = config;
        
        chkEnabled = new CheckBox("启用" + config.getModuleName());
        chkEnabled.setSelected(config.isEnabled());
        
        ArrayList<String> enabledTooltipLines = new ArrayList<>();
        enabledTooltipLines.add("参数名称：启用" + config.getModuleName());
        enabledTooltipLines.add("参数用途：控制是否启用该刮削模块");
        enabledTooltipLines.add("说明：" + config.getModuleDescription());
        FloatingTooltip.bindToNode(chkEnabled, "模块配置", enabledTooltipLines);
        
        cbSaveMode = new JFXComboBox<>();
        cbSaveMode.getItems().addAll("保存为独立文件");
        cbSaveMode.getSelectionModel().select(0);
        
        ArrayList<String> saveModeTooltipLines = new ArrayList<>();
        saveModeTooltipLines.add("参数名称：保存方式");
        saveModeTooltipLines.add("参数用途：设置刮削结果的保存方式");
        saveModeTooltipLines.add("选项：");
        saveModeTooltipLines.add("- 保存为独立文件：将数据保存为独立文件（AlbumInfo.txt）");
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
        
        chkIncludeTrackList = new CheckBox("包含曲目列表");
        chkIncludeTrackList.setSelected(config.isIncludeTrackList());
        
        ArrayList<String> trackListTooltipLines = new ArrayList<>();
        trackListTooltipLines.add("参数名称：包含曲目列表");
        trackListTooltipLines.add("参数用途：是否在专辑信息中包含曲目列表");
        trackListTooltipLines.add("说明：");
        trackListTooltipLines.add("- 启用：在AlbumInfo.txt中包含完整的曲目列表");
        trackListTooltipLines.add("- 禁用：不包含曲目列表");
        FloatingTooltip.bindToNode(chkIncludeTrackList, "模块配置", trackListTooltipLines);
        
        chkIncludeDescription = new CheckBox("包含专辑简介");
        chkIncludeDescription.setSelected(config.isIncludeDescription());
        
        ArrayList<String> descriptionTooltipLines = new ArrayList<>();
        descriptionTooltipLines.add("参数名称：包含专辑简介");
        descriptionTooltipLines.add("参数用途：是否在专辑信息中包含专辑简介");
        descriptionTooltipLines.add("说明：");
        descriptionTooltipLines.add("- 启用：在AlbumInfo.txt中包含专辑简介");
        descriptionTooltipLines.add("- 禁用：不包含专辑简介");
        FloatingTooltip.bindToNode(chkIncludeDescription, "模块配置", descriptionTooltipLines);
        
        chkIncludeCopyright = new CheckBox("包含版权信息");
        chkIncludeCopyright.setSelected(config.isIncludeCopyright());
        
        ArrayList<String> copyrightTooltipLines = new ArrayList<>();
        copyrightTooltipLines.add("参数名称：包含版权信息");
        copyrightTooltipLines.add("参数用途：是否在专辑信息中包含版权信息");
        copyrightTooltipLines.add("说明：");
        copyrightTooltipLines.add("- 启用：在AlbumInfo.txt中包含版权信息");
        copyrightTooltipLines.add("- 禁用：不包含版权信息");
        FloatingTooltip.bindToNode(chkIncludeCopyright, "模块配置", copyrightTooltipLines);
        
        setupListeners();
        setupUI();
    }
    
    private void setupListeners() {
        chkEnabled.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setEnabled(newVal);
            updateUIState();
        });
        
        cbSaveMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            config.setSaveMode(com.filemanager.strategy.scraper.config.ModuleConfig.SaveMode.SEPARATE_FILE);
        });
        
        cbDuplicateMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("跳过已有文件".equals(newVal)) {
                config.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.SKIP);
            } else if ("覆盖已有文件".equals(newVal)) {
                config.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.OVERWRITE);
            } else {
                config.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.RENAME);
            }
        });
        
        chkUseCache.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setUseCache(newVal);
        });
        
        chkIncludeTrackList.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setIncludeTrackList(newVal);
        });
        
        chkIncludeDescription.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setIncludeDescription(newVal);
        });
        
        chkIncludeCopyright.selectedProperty().addListener((obs, oldVal, newVal) -> {
            config.setIncludeCopyright(newVal);
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
                chkUseCache,
                chkIncludeTrackList,
                chkIncludeDescription,
                chkIncludeCopyright
        );
        
        updateUIState();
    }
    
    private void updateUIState() {
        boolean enabled = config.isEnabled();
        cbSaveMode.setDisable(!enabled);
        cbDuplicateMode.setDisable(!enabled);
        chkUseCache.setDisable(!enabled);
        chkIncludeTrackList.setDisable(!enabled);
        chkIncludeDescription.setDisable(!enabled);
        chkIncludeCopyright.setDisable(!enabled);
    }
    
    public void captureParams() {
        config.setEnabled(chkEnabled.isSelected());
        config.setUseCache(chkUseCache.isSelected());
        config.setIncludeTrackList(chkIncludeTrackList.isSelected());
        config.setIncludeDescription(chkIncludeDescription.isSelected());
        config.setIncludeCopyright(chkIncludeCopyright.isSelected());
        
        String duplicateMode = cbDuplicateMode.getValue();
        if ("跳过已有文件".equals(duplicateMode)) {
            config.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.SKIP);
        } else if ("覆盖已有文件".equals(duplicateMode)) {
            config.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.OVERWRITE);
        } else {
            config.setDuplicateMode(com.filemanager.strategy.scraper.config.ModuleConfig.DuplicateMode.RENAME);
        }
    }
    
    public void loadFromConfig(AlbumInfoModuleConfig config) {
        chkEnabled.setSelected(config.isEnabled());
        chkUseCache.setSelected(config.isUseCache());
        chkIncludeTrackList.setSelected(config.isIncludeTrackList());
        chkIncludeDescription.setSelected(config.isIncludeDescription());
        chkIncludeCopyright.setSelected(config.isIncludeCopyright());
        
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
    
    public AlbumInfoModuleConfig getConfig() {
        return config;
    }
}