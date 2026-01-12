/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.ui;

import com.filemanager.app.base.IAppController;
import com.filemanager.app.base.IAutoReloadAble;
import com.filemanager.app.tools.AdvancedFileTypeManager;
import com.filemanager.app.tools.display.StyleFactory;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.Getter;

import java.io.*;
import java.util.Properties;
import java.util.stream.Collectors;

@Getter
public class GlobalSettingsView implements IAutoReloadAble {
    private final IAppController app;
    private final AdvancedFileTypeManager fileTypeManager = new AdvancedFileTypeManager();
    private VBox viewNode;
    // UI Controls
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;

    public GlobalSettingsView(IAppController app) {
        this.app = app;
        this.initControls();
        this.buildUI();
        StyleFactory.setBasicStyle(viewNode);
    }

    private void initControls() {
        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("当前目录", "全部文件", "指定目录层级"));
        cbRecursionMode.getSelectionModel().select(1);

        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录层级"));
    }

    private void buildUI() {
        viewNode = StyleFactory.createVBoxPanel();
        
        viewNode.getChildren().addAll(
                StyleFactory.createParamPairLine("文件扫描模式:", cbRecursionMode),
                StyleFactory.createParamPairLine("文件扫描层级:", spRecursionDepth),
                fileTypeManager.getView()
        );
    }
    
    /**
     * 如果setting目录不存在则创建
     */
    private void createSettingDirIfNotExists() {
        File settingDir = new File("setting");
        if (!settingDir.exists()) {
            settingDir.mkdir();
        }
    }

    /**
     * 判断是否需要的文件类型
     *
     * @param file
     * @return
     */
    public boolean isFileIncluded(File file) {
        return fileTypeManager.accept(file);
    }

    public Node getViewNode() {
        return viewNode;
    }

    @Override
    public void saveConfig(Properties props) {
        if (props == null) return;
        this.fileTypeManager.saveConfig(props);
        props.setProperty("filter.recursion.mode",
                String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
        props.setProperty("filter.recursion.depth",
                spRecursionDepth.getValue() != null ? String.valueOf(spRecursionDepth.getValue()) : "1");
        ObservableList<File> roots = app.getSourceRoots();
        if (!roots.isEmpty()) {
            String paths = roots.stream().map(File::getAbsolutePath).collect(Collectors.joining("||"));
            props.setProperty("filter.global.sources", paths);
        } else {
            props.remove("filter.global.sources");
        }
    }

    @Override
    public void loadConfig(Properties props) {
        this.fileTypeManager.loadConfig(props);
        int recursionMode = Integer.parseInt(props.getProperty("filter.recursion.mode", "1"));
        cbRecursionMode.getSelectionModel().select(recursionMode);
        int recursionDepth = Integer.parseInt(props.getProperty("filter.recursion.depth", "1"));
        spRecursionDepth.getValueFactory().setValue(recursionDepth);
        String paths = props.getProperty("filter.global.sources");
        if (paths != null && !paths.isEmpty()) {
            app.getSourceRoots().clear();
            for (String p : paths.split("\\|\\|")) {
                File f = new File(p);
                if (f.exists()) app.getSourceRoots().add(f);
            }
        }
    }
}