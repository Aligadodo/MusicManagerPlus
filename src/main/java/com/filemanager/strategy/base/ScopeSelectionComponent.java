/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy.base;

import com.filemanager.app.tools.display.StyleFactory;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.Map;
import java.util.Properties;

/**
 * 通用生效范围选择组件
 * 支持选择生效范围为：文件、文件夹、全部
 *
 * @author hrcao
 */
public class ScopeSelectionComponent implements IConfigComponent {
    // UI组件
    @Getter
    private final JFXComboBox<String> cbScope;

    // 运行时参数
    private String pScope;

    // 属性键前缀
    private final String propPrefix;

    /**
     * 构造函数
     *
     * @param propPrefix 属性键前缀，用于区分不同组件的配置
     */
    public ScopeSelectionComponent(String propPrefix) {
        this.propPrefix = propPrefix;

        // 生效范围选择
        cbScope = new JFXComboBox<>();
        cbScope.getItems().addAll("文件", "文件夹", "全部");
        cbScope.getSelectionModel().select(0);
        
        // 添加悬浮提示信息
        java.util.List<String> scopeTooltipLines = new java.util.ArrayList<>();
        scopeTooltipLines.add("参数名称：生效范围");
        scopeTooltipLines.add("参数用途：用于选择操作的生效范围");
        scopeTooltipLines.add("选项：");
        scopeTooltipLines.add("- 文件：仅对文件生效");
        scopeTooltipLines.add("- 文件夹：仅对文件夹生效");
        scopeTooltipLines.add("- 全部：对文件和文件夹都生效");
        com.filemanager.app.tools.display.FloatingTooltip.bindToNode(cbScope, "生效范围设置", scopeTooltipLines);
    }

    /**
     * 构造函数
     *
     * @param propPrefix    属性键前缀，用于区分不同组件的配置
     * @param defaultValues 默认值映射，键为参数名（不含前缀），值为默认值
     */
    public ScopeSelectionComponent(String propPrefix, Map<String, Object> defaultValues) {
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
     * 获取配置节点
     *
     * @return 配置节点
     */
    public Node getConfigNode() {
        VBox configBox = new VBox();
        configBox.setSpacing(10);

        configBox.getChildren().addAll(
                StyleFactory.createChapter("生效范围设置"),
                StyleFactory.createParamPairLine("生效范围:", cbScope)
        );

        return configBox;
    }

    /**
     * 捕获参数
     */
    public void captureParams() {
        pScope = cbScope.getValue();
    }

    /**
     * 保存配置
     *
     * @param props 属性对象
     */
    public void saveConfig(Properties props) {
        if (cbScope.getValue() != null) {
            props.setProperty(propPrefix + "_scope", cbScope.getValue());
        }
    }

    /**
     * 加载配置
     *
     * @param props 属性对象
     */
    public void loadConfig(Properties props) {
        if (props.containsKey(propPrefix + "_scope")) {
            cbScope.getSelectionModel().select(props.getProperty(propPrefix + "_scope"));
        }
    }

    /**
     * 获取生效范围
     *
     * @return 生效范围
     */
    public String getScope() {
        return pScope != null ? pScope : cbScope.getValue();
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
