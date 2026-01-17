/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.RuleCondition;
import com.filemanager.rule.rename.RenameActionType;
import com.filemanager.rule.rename.RenameMode;
import com.filemanager.rule.rename.RenameRule;
import com.filemanager.rule.rename.RenameRuleListCell;
import com.filemanager.type.ConditionType;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class AdvancedRenameStrategy extends IAppStrategy {

    // UI Controls
    private final ListView<RenameRule> lvRules;
    private final JFXButton btnAddRule, btnRemoveRule, btnMoveUp, btnMoveDown;
    private final JFXComboBox<String> cbCrossDriveMode;
    private final JFXComboBox<String> cbProcessScope;

    // Runtime Params
    private List<RenameRule> pRules;
    private String pCrossDriveMode;
    private int pProcessScopeIndex;

    public AdvancedRenameStrategy() {
        lvRules = StyleFactory.createListView();
        lvRules.setCellFactory(p -> new RenameRuleListCell(this));
        lvRules.setPlaceholder(StyleFactory.createParamLabel("暂无规则，请点击下方添加..."));
        lvRules.setPrefHeight(150);
        // 双击打开配置
        lvRules.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1 && lvRules.getSelectionModel().getSelectedItem() != null) {
                showRuleEditDialog(lvRules.getSelectionModel().getSelectedItem());
            }
        });
        btnAddRule = StyleFactory.createActionButton("添加规则", "#3498db", () -> showRuleEditDialog(null));
        btnRemoveRule = StyleFactory.createActionButton("删除规则", "#e74c3c", () -> {
            RenameRule s = lvRules.getSelectionModel().getSelectedItem();
            if (s != null) {
                lvRules.getItems().remove(s);
            }
        });
        btnMoveUp = StyleFactory.createActionButton("↑(上移)", "", () -> moveRule(-1));
        btnMoveDown = StyleFactory.createActionButton("↓(下移)", "", () -> moveRule(-1));

        cbCrossDriveMode = new JFXComboBox<>(FXCollections.observableArrayList("移动 (Move)", "复制 (Copy)"));
        cbCrossDriveMode.getSelectionModel().select(0);
        cbProcessScope = new JFXComboBox<>(FXCollections.observableArrayList("仅处理文件", "仅处理文件夹", "全部处理"));
        cbProcessScope.getSelectionModel().select(2);
    }

    private void moveRule(int o) {
        int i = lvRules.getSelectionModel().getSelectedIndex();
        if (i < 0)
            return;
        int n = i + o;
        if (n >= 0 && n < lvRules.getItems().size()) {
            RenameRule it = lvRules.getItems().remove(i);
            lvRules.getItems().add(n, it);
            lvRules.getSelectionModel().select(n);
        }
    }

    @Override
    public String getName() {
        return "文件和目录重命名";
    }

    @Override
    public Node getConfigNode() {
        VBox r = StyleFactory.createVBoxPanel();
        r.getChildren().addAll(StyleFactory.createParamLabel("规则链 (从上至下依次执行):"),
                lvRules,
                StyleFactory.createHBox(btnAddRule, btnRemoveRule, btnMoveUp, btnMoveDown),
                StyleFactory.createSeparator(),
                StyleFactory.createParamPairLine("处理范围:", cbProcessScope),
                StyleFactory.createParamPairLine("跨盘动作:", cbCrossDriveMode));
        return r;
    }

    @Override
    public void captureParams() {
        pRules = new ArrayList<>(lvRules.getItems());
        pCrossDriveMode = cbCrossDriveMode.getValue();
        pProcessScopeIndex = cbProcessScope.getSelectionModel().getSelectedIndex();
    }

    @Override
    public String getDescription() {
        return "支持文件和文件夹的重命名，使用多条规则对命名进行多次调整直至符合预期。";
    }

    // --- Config Persistence ---
    @Override
    public void saveConfig(Properties props) {
        props.setProperty("arn_crossDrive", String.valueOf(cbCrossDriveMode.getSelectionModel().getSelectedIndex()));
        props.setProperty("arn_scope", String.valueOf(cbProcessScope.getSelectionModel().getSelectedIndex()));

        // Save Rules
        props.setProperty("arn_rule_count", String.valueOf(lvRules.getItems().size()));
        for (int i = 0; i < lvRules.getItems().size(); i++) {
            RenameRule r = lvRules.getItems().get(i);
            String prefix = "arn_rule_" + i + "_";
            props.setProperty(prefix + "action", r.actionType.name());
            props.setProperty(prefix + "find", r.findStr == null ? "" : r.findStr);
            props.setProperty(prefix + "replace", r.replaceStr == null ? "" : r.replaceStr);
            props.setProperty(prefix + "extensionMode", r.extensionProcessMode.name());

            // Save Conditions
            props.setProperty(prefix + "cond_count", String.valueOf(r.conditions.size()));
            for (int j = 0; j < r.conditions.size(); j++) {
                RuleCondition c = r.conditions.get(j);
                String cPrefix = prefix + "cond_" + j + "_";
                props.setProperty(cPrefix + "type", c.type.name());
                props.setProperty(cPrefix + "val", c.value == null ? "" : c.value);
            }
        }
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("arn_crossDrive"))
            cbCrossDriveMode.getSelectionModel().select(Integer.parseInt(props.getProperty("arn_crossDrive")));
        if (props.containsKey("arn_scope"))
            cbProcessScope.getSelectionModel().select(Integer.parseInt(props.getProperty("arn_scope")));

        // Load Rules
        lvRules.getItems().clear();
        int count = Integer.parseInt(props.getProperty("arn_rule_count", "0"));
        for (int i = 0; i < count; i++) {
            String prefix = "arn_rule_" + i + "_";
            try {
                RenameActionType action = RenameActionType.valueOf(props.getProperty(prefix + "action"));
                String find = props.getProperty(prefix + "find");
                String replace = props.getProperty(prefix + "replace");
                RenameMode extensionMode;
                try {
                    extensionMode = RenameMode.valueOf(
                            props.getProperty(prefix + "extensionMode", RenameMode.ONLY_FILENAME.name()));
                } catch (IllegalArgumentException e) {
                    // 如果加载失败，使用默认值
                    extensionMode = RenameMode.ONLY_FILENAME;
                }

                List<RuleCondition> conds = new ArrayList<>();
                int cCount = Integer.parseInt(props.getProperty(prefix + "cond_count", "0"));
                for (int j = 0; j < cCount; j++) {
                    String cPrefix = prefix + "cond_" + j + "_";
                    ConditionType cType = ConditionType.valueOf(props.getProperty(cPrefix + "type"));
                    String cVal = props.getProperty(cPrefix + "val");
                    conds.add(new RuleCondition(cType, cVal));
                }
                lvRules.getItems().add(new RenameRule(conds, action, find, replace, extensionMode));
            } catch (Exception e) {
                System.err.println("Failed to load rename com.filemanager.rule " + i + ": " + e.getMessage());
            }
        }
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (s.equals(t))
            return;

        if (rec.getOpType() == OperationType.CONVERT && "copy".equals(rec.getExtraParams().get("action"))) {
            if (!t.getParentFile().exists())
                t.getParentFile().mkdirs();
            if (s.isDirectory())
                copyDirectory(s, t);
            else
                Files.copy(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            if (!t.getParentFile().exists())
                t.getParentFile().mkdirs();
            Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        Files.walk(source.toPath()).forEach(sourcePath -> {
            Path targetPath = target.toPath().resolve(source.toPath().relativize(sourcePath));
            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> roots) {
        List<RenameRule> rules = pRules;
        boolean isCopy = "复制 (Copy)".equals(pCrossDriveMode);
        boolean pFile = (pProcessScopeIndex == 0 || pProcessScopeIndex == 2);
        boolean pFolder = (pProcessScopeIndex == 1 || pProcessScopeIndex == 2);

        File currentVirtualFile = new File(rec.getNewPath());

        boolean d = currentVirtualFile.isDirectory();
        // 注意：Pipeline 中间状态的文件可能不存在，用 isDirectory 判断可能不准
        // 如果是中间步骤，我们假设如果原始文件是目录，它也是目录
        if (rec.getFileHandle().isDirectory()) {
            d = true;
        } else if (rec.getFileHandle().isFile()) {
            d = false;
        }

        if (d && !pFolder) {
            return Collections.emptyList();
        }
        if (!d && !pFile) {
            return Collections.emptyList();
        }

        String currentName = currentVirtualFile.getName();
        boolean appliedAny = false;
        for (RenameRule rule : rules) {
            if (rule.matches(currentName)) {
                String temp = rule.apply(currentName, d);
                if (!temp.equals(currentName)) {
                    currentName = temp;
                    appliedAny = true;
                }
            }
        }

        if (!appliedAny) {
            return Collections.emptyList();
        }

        File parentDir = currentVirtualFile.getParentFile();
        File targetFile;
        OperationType newOp;

        if (currentName.contains(File.separator)
                || (System.getProperty("os.name").toLowerCase().contains("win") && currentName.contains(":"))) {
            File potentialPath = new File(currentName);
            targetFile = potentialPath.isAbsolute() ? potentialPath : new File(parentDir, currentName);
            newOp = isCopy ? OperationType.CONVERT : OperationType.MOVE;
        } else {
            targetFile = new File(parentDir, currentName);
            newOp = OperationType.RENAME;
        }

        rec.setNewName(targetFile.getName());
        rec.setNewPath(targetFile.getAbsolutePath());
        rec.setChanged(true);
        rec.setOpType(newOp);

        if (newOp == OperationType.CONVERT) {
            rec.getExtraParams().put("action", "copy");
        }
        return Collections.emptyList();
    }

    public void showRuleEditDialog(RenameRule existingRule) {
        Dialog<RenameRule> dialog = new Dialog<>();
        dialog.setTitle(existingRule == null ? "添加规则" : "编辑规则");
        ButtonType saveBtn = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Conditions
        ListView<RuleCondition> lvConds = StyleFactory.createListView();
        lvConds.setPrefHeight(80);
        ObservableList<RuleCondition> condList = FXCollections.observableArrayList();
        if (existingRule != null)
            condList.addAll(existingRule.conditions);
        lvConds.setItems(condList);

        JFXComboBox<ConditionType> cbCondType = new JFXComboBox<>(
                FXCollections.observableArrayList(ConditionType.values()));
        cbCondType.getSelectionModel().select(0);
        TextField txtCondVal = new TextField();
        txtCondVal.setPromptText("条件值");
        JFXButton btnAddCond = new JFXButton("+");
        btnAddCond.setOnAction(e -> {
            if (!txtCondVal.getText().isEmpty()) {
                condList.add(new RuleCondition(cbCondType.getValue(), txtCondVal.getText()));
                txtCondVal.clear();
            }
        });
        JFXButton btnDelCond = new JFXButton("-");
        btnDelCond.setOnAction(e -> {
            RuleCondition sel = lvConds.getSelectionModel().getSelectedItem();
            if (sel != null)
                condList.remove(sel);
        });

        // Actions
        JFXComboBox<RenameActionType> cbAction = new JFXComboBox<>(FXCollections.observableArrayList(RenameActionType.values()));
        cbAction.getSelectionModel().select(0);
        TextField txtFind = new TextField();
        txtFind.setPromptText("查找内容");
        TextField txtReplace = new TextField();
        txtReplace.setPromptText("替换内容");

        // Extension Process Mode
        JFXComboBox<RenameMode> cbExtensionMode = new JFXComboBox<>(
                FXCollections.observableArrayList(RenameMode.values()));
        cbExtensionMode.getSelectionModel().select(RenameMode.ONLY_FILENAME);

        cbAction.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> {
            txtFind.setDisable(false);
            txtReplace.setDisable(false);
            switch (val) {
                case REPLACE_TEXT:
                    txtFind.setPromptText("查找文本");
                    txtReplace.setPromptText("替换为");
                    break;
                case REPLACE_REGEX:
                    txtFind.setPromptText("正则 (Regex)");
                    txtReplace.setPromptText("替换为");
                    break;
                case PREPEND:
                    txtFind.setDisable(true);
                    txtReplace.setPromptText("前缀");
                    break;
                case APPEND:
                    txtFind.setDisable(true);
                    txtReplace.setPromptText("后缀");
                    break;
                case ADD_LETTER_PREFIX:
                    txtFind.setPromptText("忽略起始词 (可选)");
                    txtReplace.setPromptText("分隔符 (默认 ' - ')");
                    break;
                case CLEAN_NOISE:
                    txtFind.setPromptText("额外干扰词 (逗号分隔)");
                    txtReplace.setDisable(true);
                    break;
                case BATCH_REMOVE:
                    txtFind.setPromptText("要移除的词 (空格分隔)");
                    txtReplace.setDisable(true);
                    break;
                case CUT_PREFIX:
                    txtFind.setPromptText("移除前N个字符(不含文件类型)");
                    txtReplace.setDisable(true);
                    break;
                case CUT_SUFFIX:
                    txtFind.setPromptText("移除后N个字符(不含文件类型)");
                    txtReplace.setDisable(true);
                    break;
                case KEEP_PREFIX:
                    txtFind.setPromptText("仅保留前N个字符(不含文件类型)");
                    txtReplace.setDisable(true);
                    break;
                case KEEP_SUFFIX:
                    txtFind.setPromptText("仅保留后N个字符(不含文件类型)");
                    txtReplace.setDisable(true);
                    break;
                case REMOVE_PREFIX:
                    txtFind.setPromptText("移除前缀");
                    txtReplace.setDisable(true);
                    break;
                case REMOVE_SUFFIX:
                    txtFind.setPromptText("移除后缀");
                    txtReplace.setDisable(true);
                    break;
                case TRADITIONAL_TO_SIMPLIFIED:
                    txtFind.setPromptText("繁体转简体");
                    txtReplace.setDisable(true);
                    break;
                default:
                    txtFind.setDisable(true);
                    txtReplace.setDisable(true);
            }
        });

        if (existingRule != null) {
            cbAction.getSelectionModel().select(existingRule.actionType);
            txtFind.setText(existingRule.findStr);
            txtReplace.setText(existingRule.replaceStr);
            cbExtensionMode.getSelectionModel().select(existingRule.extensionProcessMode);
        }

        grid.add(StyleFactory.createParamLabel("1. 前置条件:"), 0, 0);
        grid.add(new HBox(5, cbCondType, txtCondVal, btnAddCond, btnDelCond), 1, 0);
        grid.add(lvConds, 1, 1);
        grid.add(new Separator(), 0, 2, 2, 1);
        grid.add(StyleFactory.createParamLabel("2. 执行动作:"), 0, 3);
        grid.add(cbAction, 1, 3);
        grid.add(StyleFactory.createParamLabel("参数 A:"), 0, 4);
        grid.add(txtFind, 1, 4);
        grid.add(StyleFactory.createParamLabel("参数 B:"), 0, 5);
        grid.add(txtReplace, 1, 5);
        grid.add(StyleFactory.createParamLabel("扩展名处理:"), 0, 6);
        grid.add(cbExtensionMode, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(
                b -> b == saveBtn
                        ? new RenameRule(new ArrayList<>(condList), cbAction.getValue(), txtFind.getText(),
                                txtReplace.getText(), cbExtensionMode.getValue())
                        : null);

        dialog.showAndWait().ifPresent(r -> {
            if (existingRule != null)
                lvRules.getItems().set(lvRules.getItems().indexOf(existingRule), r);
            else
                lvRules.getItems().add(r);
        });
    }
  
}