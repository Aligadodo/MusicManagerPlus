/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.strategy.rename.config;

import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.strategy.rename.RenameRule;
import com.filemanager.strategy.rename.RenameRuleListCell;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 高级重命名策略配置类
 * 负责管理高级重命名策略的UI组件和配置参数
 */
public class AdvancedRenameConfig {
    private final ListView<RenameRule> lvRules;
    private final JFXButton btnAddRule, btnRemoveRule, btnMoveUp, btnMoveDown;
    private final JFXComboBox<String> cbCrossDriveMode;
    private final JFXComboBox<String> cbProcessScope;
    
    private List<RenameRule> rules;
    private String crossDriveMode;
    private int processScopeIndex;
    
    private Runnable onAddRule;
    private Runnable onRemoveRule;
    private Runnable onMoveUp;
    private Runnable onMoveDown;

    public AdvancedRenameConfig() {
        lvRules = StyleFactory.createListView();
        lvRules.setCellFactory(p -> new RenameRuleListCell(null));
        lvRules.setPlaceholder(StyleFactory.createParamLabel("暂无规则，请点击下方添加..."));
        lvRules.setPrefHeight(150);
        
        btnAddRule = StyleFactory.createActionButton("添加规则", "#3498db", () -> {
            if (onAddRule != null) {
                onAddRule.run();
            }
        });
        btnRemoveRule = StyleFactory.createActionButton("删除规则", "#e74c3c", () -> {
            RenameRule s = lvRules.getSelectionModel().getSelectedItem();
            if (s != null) {
                lvRules.getItems().remove(s);
            }
        });
        btnMoveUp = StyleFactory.createActionButton("↑(上移)", "", () -> moveRule(-1));
        btnMoveDown = StyleFactory.createActionButton("↓(下移)", "", () -> moveRule(1));

        cbCrossDriveMode = new JFXComboBox<>(FXCollections.observableArrayList("移动 (Move)", "复制 (Copy)"));
        cbCrossDriveMode.getSelectionModel().select(0);
        
        ArrayList<String> crossDriveTooltipLines = new ArrayList<>();
        crossDriveTooltipLines.add("参数名称：跨盘动作");
        crossDriveTooltipLines.add("参数用途：用于设置跨盘操作时的动作");
        crossDriveTooltipLines.add("示例：");
        crossDriveTooltipLines.add("- 移动：将文件从一个盘移动到另一个盘");
        crossDriveTooltipLines.add("- 复制：将文件从一个盘复制到另一个盘");
        com.filemanager.app.tools.display.FloatingTooltip.bindToNode(cbCrossDriveMode, "高级重命名设置", crossDriveTooltipLines);
        
        cbProcessScope = new JFXComboBox<>(FXCollections.observableArrayList("仅处理文件", "仅处理文件夹", "全部处理"));
        cbProcessScope.getSelectionModel().select(2);
        
        ArrayList<String> processScopeTooltipLines = new ArrayList<>();
        processScopeTooltipLines.add("参数名称：处理范围");
        processScopeTooltipLines.add("参数用途：用于设置处理的文件类型范围");
        processScopeTooltipLines.add("示例：");
        processScopeTooltipLines.add("- 仅处理文件：只处理文件，不处理文件夹");
        processScopeTooltipLines.add("- 仅处理文件夹：只处理文件夹，不处理文件");
        processScopeTooltipLines.add("- 全部处理：同时处理文件和文件夹");
        com.filemanager.app.tools.display.FloatingTooltip.bindToNode(cbProcessScope, "高级重命名设置", processScopeTooltipLines);
    }

    private void moveRule(int direction) {
        int index = lvRules.getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        
        ObservableList<RenameRule> items = lvRules.getItems();
        int newIndex = index + direction;
        
        if (newIndex >= 0 && newIndex < items.size()) {
            RenameRule item = items.remove(index);
            items.add(newIndex, item);
            lvRules.getSelectionModel().select(newIndex);
        }
    }

    public Node getConfigNode() {
        javafx.scene.layout.VBox mainBox = new javafx.scene.layout.VBox(15);
        mainBox.setPadding(new javafx.geometry.Insets(10));

        javafx.scene.layout.VBox buttonBox = new javafx.scene.layout.VBox(10);
        buttonBox.getChildren().addAll(btnAddRule, btnRemoveRule, btnMoveUp, btnMoveDown);

        javafx.scene.layout.HBox controlBox = new javafx.scene.layout.HBox(15);
        controlBox.getChildren().addAll(cbCrossDriveMode, cbProcessScope);

        mainBox.getChildren().addAll(lvRules, buttonBox, controlBox);
        return mainBox;
    }

    public void captureParams() {
        rules = new ArrayList<>(lvRules.getItems());
        crossDriveMode = cbCrossDriveMode.getValue();
        processScopeIndex = cbProcessScope.getSelectionModel().getSelectedIndex();
    }

    public void saveConfig(Properties props) {
        props.setProperty("ars_cross_drive_mode", cbCrossDriveMode.getValue());
        props.setProperty("ars_process_scope", String.valueOf(cbProcessScope.getSelectionModel().getSelectedIndex()));
        
        for (int i = 0; i < lvRules.getItems().size(); i++) {
            RenameRule rule = lvRules.getItems().get(i);
            String prefix = "ars_rule_" + i + "_";
            
            if (rule.conditions != null && !rule.conditions.isEmpty()) {
                props.setProperty(prefix + "condition_count", String.valueOf(rule.conditions.size()));
                for (int j = 0; j < rule.conditions.size(); j++) {
                    String condPrefix = prefix + "condition_" + j + "_";
                    props.setProperty(condPrefix + "type", rule.conditions.get(j).getType().name());
                    props.setProperty(condPrefix + "value", rule.conditions.get(j).getValue());
                }
            } else {
                props.setProperty(prefix + "condition_count", "0");
            }
            
            props.setProperty(prefix + "action_type", rule.actionType.name());
            props.setProperty(prefix + "find_str", rule.findStr != null ? rule.findStr : "");
            props.setProperty(prefix + "replace_str", rule.replaceStr != null ? rule.replaceStr : "");
            props.setProperty(prefix + "rename_mode", rule.extensionProcessMode.name());
        }
        props.setProperty("ars_rule_count", String.valueOf(lvRules.getItems().size()));
    }

    public void loadConfig(Properties props) {
        if (props.containsKey("ars_cross_drive_mode")) {
            cbCrossDriveMode.setValue(props.getProperty("ars_cross_drive_mode"));
        }
        if (props.containsKey("ars_process_scope")) {
            int scope = Integer.parseInt(props.getProperty("ars_process_scope"));
            cbProcessScope.getSelectionModel().select(scope);
        }
        
        lvRules.getItems().clear();
        int ruleCount = 0;
        if (props.containsKey("ars_rule_count")) {
            ruleCount = Integer.parseInt(props.getProperty("ars_rule_count"));
        }
        
        for (int i = 0; i < ruleCount; i++) {
            String prefix = "ars_rule_" + i + "_";
            
            if (props.containsKey(prefix + "action_type") && 
                props.containsKey(prefix + "find_str") &&
                props.containsKey(prefix + "replace_str") &&
                props.containsKey(prefix + "rename_mode")) {
                
                String actionType = props.getProperty(prefix + "action_type");
                String findStr = props.getProperty(prefix + "find_str");
                String replaceStr = props.getProperty(prefix + "replace_str");
                String renameMode = props.getProperty(prefix + "rename_mode");
                
                int conditionCount = 0;
                if (props.containsKey(prefix + "condition_count")) {
                    conditionCount = Integer.parseInt(props.getProperty(prefix + "condition_count"));
                }
                
                List<com.filemanager.model.RuleCondition> conditions = new java.util.ArrayList<>();
                for (int j = 0; j < conditionCount; j++) {
                    String condPrefix = prefix + "condition_" + j + "_";
                    if (props.containsKey(condPrefix + "type") && props.containsKey(condPrefix + "value")) {
                        String condType = props.getProperty(condPrefix + "type");
                        String condValue = props.getProperty(condPrefix + "value");
                        
                        com.filemanager.model.RuleCondition condition = new com.filemanager.model.RuleCondition();
                        condition.setType(com.filemanager.type.ConditionType.valueOf(condType));
                        condition.setValue(condValue);
                        conditions.add(condition);
                    }
                }
                
                RenameRule rule = new RenameRule(conditions, 
                    com.filemanager.strategy.rename.RenameActionType.valueOf(actionType),
                    findStr, replaceStr,
                    com.filemanager.strategy.rename.RenameMode.valueOf(renameMode));
                
                lvRules.getItems().add(rule);
            }
        }
    }

    public List<RenameRule> getRules() {
        return rules;
    }

    public String getCrossDriveMode() {
        return crossDriveMode;
    }

    public int getProcessScopeIndex() {
        return processScopeIndex;
    }

    public ListView<RenameRule> getLvRules() {
        return lvRules;
    }

    public JFXButton getBtnAddRule() {
        return btnAddRule;
    }

    public JFXButton getBtnRemoveRule() {
        return btnRemoveRule;
    }

    public JFXButton getBtnMoveUp() {
        return btnMoveUp;
    }

    public JFXButton getBtnMoveDown() {
        return btnMoveDown;
    }

    public void setOnAddRule(Runnable onAddRule) {
        this.onAddRule = onAddRule;
    }

    public void setOnRemoveRule(Runnable onRemoveRule) {
        this.onRemoveRule = onRemoveRule;
    }

    public void setOnMoveUp(Runnable onMoveUp) {
        this.onMoveUp = onMoveUp;
    }

    public void setOnMoveDown(Runnable onMoveDown) {
        this.onMoveDown = onMoveDown;
    }
}
