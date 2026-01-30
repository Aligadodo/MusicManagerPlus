/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.strategy.collection.config;

import com.filemanager.strategy.collection.CollectionNamingStrategy;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 文件归类策略配置类
 * 负责管理文件归类策略的UI组件和配置参数
 */
public class FileCollectionConfig {
    private final Slider slSimilarityThreshold;
    private final TextField txtCollectionSuffix;
    private final JFXComboBox<ScanTarget> cbTargetType;
    private final JFXComboBox<CollectionNamingStrategy> cbNamingStrategy;
    private final TextField txtMustContainKeywords;
    private final TextField txtMustNotContainKeywords;

    private double threshold;
    private String collectionSuffix;
    private ScanTarget targetType;
    private CollectionNamingStrategy namingStrategy;
    private List<String> mustContainKeywords;
    private List<String> mustNotContainKeywords;

    public FileCollectionConfig() {
        slSimilarityThreshold = new Slider(0.0, 1.0, 0.9);
        slSimilarityThreshold.setShowTickMarks(true);
        slSimilarityThreshold.setShowTickLabels(true);
        slSimilarityThreshold.setMajorTickUnit(0.05);
        slSimilarityThreshold.setMinorTickCount(9);

        txtCollectionSuffix = new TextField("【合集】");
        txtCollectionSuffix.setPromptText("输入合集文件夹格式 (如：【合集】)...");

        cbTargetType = new JFXComboBox<>(FXCollections.observableArrayList(ScanTarget.values()));
        cbTargetType.setValue(ScanTarget.FOLDERS_ONLY);
        cbTargetType.setCellFactory(param -> new javafx.scene.control.ListCell<ScanTarget>() {
            @Override
            protected void updateItem(ScanTarget item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        cbTargetType.setConverter(new javafx.util.StringConverter<ScanTarget>() {
            @Override
            public String toString(ScanTarget target) {
                return target != null ? target.getDisplayName() : "";
            }

            @Override
            public ScanTarget fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                for (ScanTarget target : ScanTarget.values()) {
                    if (target.getDisplayName().equals(string)) {
                        return target;
                    }
                }
                return null;
            }
        });

        cbNamingStrategy = new JFXComboBox<>(FXCollections.observableArrayList(CollectionNamingStrategy.values()));
        cbNamingStrategy.setValue(CollectionNamingStrategy.PRECISE);
        cbNamingStrategy.setCellFactory(param -> new javafx.scene.control.ListCell<CollectionNamingStrategy>() {
            @Override
            protected void updateItem(CollectionNamingStrategy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        cbNamingStrategy.setConverter(new javafx.util.StringConverter<CollectionNamingStrategy>() {
            @Override
            public String toString(CollectionNamingStrategy strategy) {
                return strategy != null ? strategy.getDisplayName() : "";
            }

            @Override
            public CollectionNamingStrategy fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                for (CollectionNamingStrategy strategy : CollectionNamingStrategy.values()) {
                    if (strategy.getDisplayName().equals(string)) {
                        return strategy;
                    }
                }
                return null;
            }
        });

        txtMustContainKeywords = new TextField();
        txtMustContainKeywords.setPromptText("输入必须包含的关键词，多个关键词用逗号分隔...");

        txtMustNotContainKeywords = new TextField();
        txtMustNotContainKeywords.setPromptText("输入必须不包含的关键词，多个关键词用逗号分隔...");
    }

    public Node getConfigNode() {
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));
        vbox.getChildren().addAll(
                new javafx.scene.control.Label("相似度阈值:"),
                slSimilarityThreshold,
                new javafx.scene.control.Label("合集文件夹格式:"),
                txtCollectionSuffix,
                new javafx.scene.control.Label("目标类型:"),
                cbTargetType,
                new javafx.scene.control.Label("命名策略:"),
                cbNamingStrategy,
                new javafx.scene.control.Label("必须包含的关键词:"),
                txtMustContainKeywords,
                new javafx.scene.control.Label("必须不包含的关键词:"),
                txtMustNotContainKeywords
        );
        return vbox;
    }

    public void captureParams() {
        threshold = slSimilarityThreshold.getValue();
        collectionSuffix = txtCollectionSuffix.getText();
        targetType = cbTargetType.getValue();
        namingStrategy = cbNamingStrategy.getValue();

        mustContainKeywords = parseKeywords(txtMustContainKeywords.getText());
        mustNotContainKeywords = parseKeywords(txtMustNotContainKeywords.getText());

        if (collectionSuffix == null || collectionSuffix.trim().isEmpty()) {
            collectionSuffix = "【合集】";
        }
        if (targetType == null) {
            targetType = ScanTarget.FOLDERS_ONLY;
        }
        if (namingStrategy == null) {
            namingStrategy = CollectionNamingStrategy.PRECISE;
        }
    }

    public void saveConfig(Properties props) {
        props.setProperty("fcs_threshold", String.valueOf(slSimilarityThreshold.getValue()));
        props.setProperty("fcs_suffix", txtCollectionSuffix.getText());
        props.setProperty("fcs_target_type", cbTargetType.getValue().name());
        props.setProperty("fcs_naming_strategy", cbNamingStrategy.getValue().name());
        props.setProperty("fcs_must_contain", txtMustContainKeywords.getText());
        props.setProperty("fcs_must_not_contain", txtMustNotContainKeywords.getText());
    }

    public void loadConfig(Properties props) {
        if (props.containsKey("fcs_threshold")) {
            slSimilarityThreshold.setValue(Double.parseDouble(props.getProperty("fcs_threshold")));
        }
        if (props.containsKey("fcs_suffix")) {
            txtCollectionSuffix.setText(props.getProperty("fcs_suffix"));
        }
        if (props.containsKey("fcs_target_type")) {
            cbTargetType.setValue(ScanTarget.valueOf(props.getProperty("fcs_target_type")));
        }
        if (props.containsKey("fcs_naming_strategy")) {
            cbNamingStrategy.setValue(CollectionNamingStrategy.valueOf(props.getProperty("fcs_naming_strategy")));
        }
        if (props.containsKey("fcs_must_contain")) {
            txtMustContainKeywords.setText(props.getProperty("fcs_must_contain"));
        }
        if (props.containsKey("fcs_must_not_contain")) {
            txtMustNotContainKeywords.setText(props.getProperty("fcs_must_not_contain"));
        }
    }

    private List<String> parseKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        if (text != null && !text.trim().isEmpty()) {
            String[] parts = text.split("[,，;；]\\s*");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    keywords.add(part.trim());
                }
            }
        }
        return keywords;
    }

    public double getThreshold() {
        return threshold;
    }

    public String getCollectionSuffix() {
        return collectionSuffix;
    }

    public ScanTarget getTargetType() {
        return targetType;
    }

    public CollectionNamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public List<String> getMustContainKeywords() {
        return mustContainKeywords;
    }

    public List<String> getMustNotContainKeywords() {
        return mustNotContainKeywords;
    }
}
