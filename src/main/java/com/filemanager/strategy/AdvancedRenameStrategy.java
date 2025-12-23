package com.filemanager.strategy;


import com.filemanager.model.ChangeRecord;
import com.filemanager.model.RuleCondition;
import com.filemanager.tool.display.NodeUtils;
import com.filemanager.tool.display.StyleFactory;
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
import java.util.stream.Collectors;

public class AdvancedRenameStrategy extends AppStrategy {

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
        lvRules = new ListView<>();
        lvRules.setCellFactory(p -> new RuleListCell());
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
        if (i < 0) return;
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
                ActionType action = ActionType.valueOf(props.getProperty(prefix + "action"));
                String find = props.getProperty(prefix + "find");
                String replace = props.getProperty(prefix + "replace");

                List<RuleCondition> conds = new ArrayList<>();
                int cCount = Integer.parseInt(props.getProperty(prefix + "cond_count", "0"));
                for (int j = 0; j < cCount; j++) {
                    String cPrefix = prefix + "cond_" + j + "_";
                    ConditionType cType = ConditionType.valueOf(props.getProperty(cPrefix + "type"));
                    String cVal = props.getProperty(cPrefix + "val");
                    conds.add(new RuleCondition(cType, cVal));
                }
                lvRules.getItems().add(new RenameRule(conds, action, find, replace));
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
        if (s.equals(t)) return;

        if (rec.getOpType() == OperationType.CONVERT && "copy".equals(rec.getExtraParams().get("action"))) {
            if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
            if (s.isDirectory()) copyDirectory(s, t);
            else Files.copy(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
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
                String temp = rule.apply(currentName);
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

        if (currentName.contains(File.separator) || (System.getProperty("os.name").toLowerCase().contains("win") && currentName.contains(":"))) {
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

    private void showRuleEditDialog(RenameRule existingRule) {
        Dialog<RenameRule> dialog = new Dialog<>();
        dialog.setTitle(existingRule == null ? "添加规则" : "编辑规则");
        ButtonType saveBtn = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Conditions
        ListView<RuleCondition> lvConds = new ListView<>();
        lvConds.setPrefHeight(80);
        ObservableList<RuleCondition> condList = FXCollections.observableArrayList();
        if (existingRule != null) condList.addAll(existingRule.conditions);
        lvConds.setItems(condList);

        JFXComboBox<ConditionType> cbCondType = new JFXComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
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
            if (sel != null) condList.remove(sel);
        });

        // Actions
        JFXComboBox<ActionType> cbAction = new JFXComboBox<>(FXCollections.observableArrayList(ActionType.values()));
        cbAction.getSelectionModel().select(0);
        TextField txtFind = new TextField();
        txtFind.setPromptText("查找内容");
        TextField txtReplace = new TextField();
        txtReplace.setPromptText("替换内容");

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
                default:
                    txtFind.setDisable(true);
                    txtReplace.setDisable(true);
            }
        });

        if (existingRule != null) {
            cbAction.getSelectionModel().select(existingRule.actionType);
            txtFind.setText(existingRule.findStr);
            txtReplace.setText(existingRule.replaceStr);
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

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b == saveBtn ? new RenameRule(new ArrayList<>(condList), cbAction.getValue(), txtFind.getText(), txtReplace.getText()) : null);

        dialog.showAndWait().ifPresent(r -> {
            if (existingRule != null) lvRules.getItems().set(lvRules.getItems().indexOf(existingRule), r);
            else lvRules.getItems().add(r);
        });
    }

    enum ActionType {
        REPLACE_TEXT("文本替换"), REPLACE_REGEX("正则替换"), PREPEND("前缀添加"), APPEND("后缀添加"),
        TO_LOWER("转小写"), TO_UPPER("转大写"), TRIM("去空格"), ADD_LETTER_PREFIX("首字母前缀"),
        CLEAN_NOISE("智能清理"), BATCH_REMOVE("批量移除");
        private final String d;

        ActionType(String d) {
            this.d = d;
        }

        @Override
        public String toString() {
            return d;
        }
    }

    static class RenameRule {
        List<RuleCondition> conditions;
        ActionType actionType;
        String findStr;
        String replaceStr;

        public RenameRule(List<RuleCondition> c, ActionType a, String f, String r) {
            conditions = c;
            actionType = a;
            findStr = f;
            replaceStr = r;
        }

        public boolean matches(String s) {
            if (conditions == null || conditions.isEmpty()) return true;
            for (RuleCondition c : conditions)
                if (!c.test(new File(s)))
                    return false; // Condition test needs File but here we check string logic inside RuleCondition if possible, or adapt.
            // Note: RuleCondition.test takes File. Here we are matching filename string.
            // Simplified for this context: RenameRule conditions usually match filename string.
            // Adapting RuleCondition to check String s:
            for (RuleCondition c : conditions) {
                // Temporary hack: RuleCondition expects File, but we have String name.
                // We should construct a dummy file or overload test.
                // Assuming RuleCondition.test logic mainly checks getName().
                if (!c.test(new File(s))) return false;
            }
            return true;
        }

        public String apply(String s) {
            String r = s;
            String v = replaceStr == null ? "" : replaceStr;
            try {
                switch (actionType) {
                    case REPLACE_TEXT:
                        if (findStr != null && !findStr.isEmpty()) r = s.replace(findStr, v);
                        break;
                    case REPLACE_REGEX:
                        if (findStr != null && !findStr.isEmpty()) r = s.replaceAll(findStr, v);
                        break;
                    case PREPEND:
                        r = v + s;
                        break;
                    case APPEND:
                        int d = s.lastIndexOf('.');
                        if (d > 0) r = s.substring(0, d) + v + s.substring(d);
                        else r = s + v;
                        break;
                    case TO_LOWER:
                        r = s.toLowerCase();
                        break;
                    case TO_UPPER:
                        r = s.toUpperCase();
                        break;
                    case TRIM:
                        r = s.trim();
                        break;
                    case BATCH_REMOVE:
                        if (findStr != null) for (String t : findStr.split(" ")) if (!t.isEmpty()) r = r.replace(t, "");
                        r = r.trim();
                        break;
                    case CLEAN_NOISE:
                        r = r.replaceAll("(?i)\\[(mqms|flac|mp3|wav|cue|log|iso|ape|dsf|dff).*?\\]", "");
                        r = r.replaceAll("[《》]", "");
                        if (findStr != null) for (String w : findStr.split("[,，、]"))
                            if (!w.trim().isEmpty()) r = r.replace(w.trim(), "");
                        r = r.replaceAll("\\s+", " ").trim();
                        break;
                    case ADD_LETTER_PREFIX:
                        String core = s;
                        if (findStr != null && !findStr.isEmpty() && s.toLowerCase().startsWith(findStr.toLowerCase()))
                            core = s.substring(findStr.length()).trim();
                        char first = getFirstValidChar(core);
                        if (first != 0) {
                            char py = getPinyinFirstLetter(first);
                            if (py != 0) {
                                String sep = v.isEmpty() ? " - " : v;
                                String p = Character.toUpperCase(py) + sep;
                                if (!s.startsWith(p)) r = p + s;
                            }
                        }
                        break;
                }
            } catch (Exception e) {

            }
            return r;
        }

        public String getActionDesc() {
            return actionType + " " + (findStr != null ? findStr : "") + (replaceStr != null && !replaceStr.isEmpty() ? " -> " + replaceStr : "");
        }

        private char getFirstValidChar(String s) {
            for (char c : s.toCharArray()) if (Character.isLetterOrDigit(c) || (c >= 0x4e00 && c <= 0x9fa5)) return c;
            return 0;
        }

        private char getPinyinFirstLetter(char c) {
            if (c >= 'a' && c <= 'z') return (char) (c - 32);
            if (c >= 'A' && c <= 'Z') return c;
            try {
                byte[] b = String.valueOf(c).getBytes("GBK");
                if (b.length < 2) return 0;
                int code = (b[0] & 0xFF) * 256 + (b[1] & 0xFF);
                if (code >= 45217 && code <= 45252) return 'A';
                if (code >= 45253 && code <= 45760) return 'B';
                if (code >= 45761 && code <= 46317) return 'C';
                if (code >= 46318 && code <= 46825) return 'D';
                if (code >= 46826 && code <= 47009) return 'E';
                if (code >= 47010 && code <= 47296) return 'F';
                if (code >= 47297 && code <= 47613) return 'G';
                if (code >= 47614 && code <= 48118) return 'H';
                if (code >= 48119 && code <= 49061) return 'J';
                if (code >= 49062 && code <= 49323) return 'K';
                if (code >= 49324 && code <= 49895) return 'L';
                if (code >= 49896 && code <= 50370) return 'M';
                if (code >= 50371 && code <= 50613) return 'N';
                if (code >= 50614 && code <= 50621) return 'O';
                if (code >= 50622 && code <= 50905) return 'P';
                if (code >= 50906 && code <= 51386) return 'Q';
                if (code >= 51387 && code <= 51445) return 'R';
                if (code >= 51446 && code <= 52217) return 'S';
                if (code >= 52218 && code <= 52697) return 'T';
                if (code >= 52698 && code <= 52979) return 'W';
                if (code >= 52980 && code <= 53688) return 'X';
                if (code >= 53689 && code <= 54480) return 'Y';
                if (code >= 54481 && code <= 55289) return 'Z';
            } catch (Exception e) {
            }
            return 0;
        }
    }

    // --- Helper Classes (Need to be static for standalone file) ---
    class RuleListCell extends ListCell<RenameRule> {
        @Override
        protected void updateItem(RenameRule item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String cond = item.conditions.isEmpty() ? "无条件" : item.conditions.stream().map(RuleCondition::toString).collect(Collectors.joining(" & "));
                Label l1 = StyleFactory.createDescLabel("若: " + cond);
                l1.setStyle("-fx-font-size: 11px;");
                Label l2 = StyleFactory.createDescLabel(item.getActionDesc());
                l2.setStyle("-fx-font-weight: bold;");
                HBox actions = StyleFactory.createTreeItemMenu(
                        e -> showRuleEditDialog(item),
                        e -> NodeUtils.moveListItem(getListView().getItems(), getIndex(), -1),
                        e -> NodeUtils.moveListItem(getListView().getItems(), getIndex(), 1),
                        e -> getListView().getItems().remove(item)
                );
                setGraphic(StyleFactory.createHBox(StyleFactory.createVBox(l1, l2), StyleFactory.createSpacer(), actions));
            }
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (!isEmpty() && getItem() != null) {
                StyleFactory.updateTreeItemStyle(this, selected);
            }
        }
    }
}