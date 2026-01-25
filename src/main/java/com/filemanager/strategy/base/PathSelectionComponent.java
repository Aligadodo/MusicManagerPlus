/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-24
 */
package com.filemanager.strategy.base;

import com.filemanager.app.tools.display.StyleFactory;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import lombok.Getter;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * 通用路径选择组件
 * 支持原目录、子目录、相对路径、指定目录四种模式
 *
 * @author 28667
 */
public class PathSelectionComponent implements IConfigComponent {
    // UI组件
    @Getter
    private final JFXComboBox<String> cbOutputDirMode;
    @Getter
    private final JFXTextField txtPath;
    private final JFXButton btnPickDir;

    // 运行时参数
    private String pOutputDirMode;
    private String pPath;

    // 属性键前缀
    private final String propPrefix;

    /**
     * 构造函数
     *
     * @param propPrefix 属性键前缀，用于区分不同组件的配置
     */
    public PathSelectionComponent(String propPrefix) {
        this.propPrefix = propPrefix;

        // 输出目录模式
        cbOutputDirMode = new JFXComboBox<>();
        cbOutputDirMode.getItems().addAll("原目录", "子目录", "相对路径", "指定目录");
        cbOutputDirMode.getSelectionModel().select(0);

        // 路径输入框
        txtPath = new JFXTextField();
        txtPath.setPromptText("子目录名称或指定目录路径");

        // 选择目录按钮
        btnPickDir = StyleFactory.createActionButton("选择路径", "", this::pickDirectory);

        // 绑定可见性
        txtPath.visibleProperty().bind(
                Bindings.not(cbOutputDirMode.getSelectionModel().selectedItemProperty().isEqualTo("原目录"))
        );
        txtPath.managedProperty().bind(txtPath.visibleProperty());

        btnPickDir.visibleProperty().bind(
                cbOutputDirMode.getSelectionModel().selectedItemProperty().isEqualTo("指定目录")
        );
        btnPickDir.managedProperty().bind(btnPickDir.visibleProperty());
    }

    /**
     * 构造函数
     *
     * @param propPrefix    属性键前缀，用于区分不同组件的配置
     * @param defaultValues 默认值映射，键为参数名（不含前缀），值为默认值
     */
    public PathSelectionComponent(String propPrefix, Map<String, Object> defaultValues) {
        this(propPrefix);

        // 将defaultValues转换为Properties并加载
        if (defaultValues != null) {
            Properties props = new Properties();
            for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                props.setProperty(propPrefix + "_" + entry.getKey(), String.valueOf(entry.getValue()));
            }
            loadConfig(props);
        }
    }

    /**
     * 选择目录
     */
    private void pickDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(null);
        if (f != null) {
            txtPath.setText(f.getAbsolutePath());
            cbOutputDirMode.getSelectionModel().select("指定目录");
        }
    }

    /**
     * 获取配置节点
     *
     * @return 配置节点
     */
    public Node getConfigNode() {
        VBox configBox = new VBox();
        configBox.setSpacing(10);

        // 创建路径行
        HBox pathLine = StyleFactory.createParamPairLine("路径:", new HBox(txtPath, btnPickDir));
        // 绑定路径行的可见性：当输出目录模式不是"原目录"时显示
        pathLine.visibleProperty().bind(
                cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录")
        );
        pathLine.managedProperty().bind(pathLine.visibleProperty());

        configBox.getChildren().addAll(
                StyleFactory.createChapter("输出设置"),
                StyleFactory.createParamPairLine("输出目录模式:", cbOutputDirMode),
                pathLine
        );

        return configBox;
    }

    /**
     * 捕获参数
     */
    public void captureParams() {
        pOutputDirMode = cbOutputDirMode.getValue();
        pPath = txtPath.getText();
    }

    /**
     * 保存配置
     *
     * @param props 属性对象
     */
    public void saveConfig(Properties props) {
        if (cbOutputDirMode.getValue() != null) {
            props.setProperty(propPrefix + "_outputDirMode", cbOutputDirMode.getValue());
        }
        if (txtPath.getText() != null) {
            props.setProperty(propPrefix + "_path", txtPath.getText());
        }
    }

    /**
     * 加载配置
     *
     * @param props 属性对象
     */
    public void loadConfig(Properties props) {
        if (props.containsKey(propPrefix + "_outputDirMode")) {
            cbOutputDirMode.getSelectionModel().select(props.getProperty(propPrefix + "_outputDirMode"));
        }
        if (props.containsKey(propPrefix + "_path")) {
            txtPath.setText(props.getProperty(propPrefix + "_path"));
        }
    }

    /**
     * 获取输出路径
     *
     * @param sourceFile 源文件
     * @return 输出路径
     */
    public String getOutputPath(File sourceFile) {
        String outputDirMode = pOutputDirMode != null ? pOutputDirMode : cbOutputDirMode.getValue();
        String path = pPath != null ? pPath : txtPath.getText();

        switch (outputDirMode) {
            case "原目录":
                return sourceFile.getParent();
            case "子目录":
                return sourceFile.getParent() + File.separator + (path.isEmpty() ? "converted" : path);
            case "相对路径":
                return path.isEmpty() ? sourceFile.getParent() : path;
            case "指定目录":
                return path.isEmpty() ? sourceFile.getParent() : path;
            default:
                return sourceFile.getParent();
        }
    }

    /**
     * 获取输出目录模式
     *
     * @return 输出目录模式
     */
    public String getOutputDirMode() {
        return pOutputDirMode != null ? pOutputDirMode : cbOutputDirMode.getValue();
    }

    /**
     * 获取路径
     *
     * @return 路径
     */
    public String getPath() {
        return pPath != null ? pPath : txtPath.getText();
    }

    /**
     * 重新加载配置节点的样式
     */
    @Override
    public void reload() {
        Node configNode = getConfigNode();
        if (configNode != null) {
            StyleFactory.refreshAllComponents(configNode);
        }
    }
}
