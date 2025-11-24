package plus.plugins;


import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import plus.AppStrategy;
import plus.model.ChangeRecord;
import plus.model.RenameRule;
import plus.model.RuleCondition;
import plus.type.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AdvancedRenameStrategy extends AppStrategy {
    // --- UI Controls ---
    private final ListView<RenameRule> lvRules;
    private final JFXButton btnAddRule;
    private final JFXButton btnRemoveRule;
    private final JFXButton btnMoveUp;
    private final JFXButton btnMoveDown;

    private final JFXComboBox<String> cbCrossDriveMode; // 跨盘符操作模式
    private final Spinner<Integer> spThreads;

    // --- Runtime Parameters (Captured) ---
    private List<RenameRule> pRules;
    private String pCrossDriveMode;
    private int pThreads;

    public AdvancedRenameStrategy() {
        // 规则列表视图
        lvRules = new ListView<>();
        lvRules.setCellFactory(param -> new RuleListCell());
        lvRules.setPlaceholder(new Label("暂无规则，请点击下方添加..."));
        lvRules.setPrefHeight(200);

        // 按钮初始化
        btnAddRule = new JFXButton("添加规则");
        btnAddRule.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnAddRule.setOnAction(e -> showRuleEditDialog(null));

        btnRemoveRule = new JFXButton("删除");
        btnRemoveRule.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnRemoveRule.setOnAction(e -> {
            RenameRule selected = lvRules.getSelectionModel().getSelectedItem();
            if (selected != null) lvRules.getItems().remove(selected);
        });

        btnMoveUp = new JFXButton("↑");
        btnMoveUp.setOnAction(e -> moveRule(-1));

        btnMoveDown = new JFXButton("↓");
        btnMoveDown.setOnAction(e -> moveRule(1));

        // 全局配置
        cbCrossDriveMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "移动 (Move)", "复制 (Copy)"
        ));
        cbCrossDriveMode.getSelectionModel().select(0);
        cbCrossDriveMode.setTooltip(new Tooltip("如果新文件名包含路径且跨越了盘符，采取的操作"));

        spThreads = new Spinner<>(1, 64, Runtime.getRuntime().availableProcessors());
        spThreads.setTooltip(new Tooltip("并发处理的线程数"));
    }

    private void moveRule(int offset) {
        int index = lvRules.getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        int newIndex = index + offset;
        if (newIndex >= 0 && newIndex < lvRules.getItems().size()) {
            RenameRule item = lvRules.getItems().remove(index);
            lvRules.getItems().add(newIndex, item);
            lvRules.getSelectionModel().select(newIndex);
        }
    }

    @Override
    public String getName() {
        return "高级重命名 (正则/多规则/条件)";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL;
    }

    @Override
    public int getPreferredThreadCount() {
        return spThreads.getValue();
    }

    @Override
    public Node getConfigNode() {
        VBox root = new VBox(10);

        HBox toolbar = new HBox(10, btnAddRule, btnRemoveRule, new Separator(javafx.geometry.Orientation.VERTICAL), btnMoveUp, btnMoveDown);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        HBox globalSettings = new HBox(15,
                new Label("跨盘符行为:"), cbCrossDriveMode,
                new Label("并发线程:"), spThreads
        );
        globalSettings.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(
                new Label("规则链 (从上至下依次执行):"),
                lvRules,
                toolbar,
                new Separator(),
                globalSettings
        );
        return root;
    }

    @Override
    public void captureParams() {
        // 在 UI 线程捕获所有参数到普通集合中，供后台线程使用
        pRules = new ArrayList<>(lvRules.getItems()); // 浅拷贝列表
        pCrossDriveMode = cbCrossDriveMode.getValue();
        pThreads = spThreads.getValue();
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<RenameRule> rules = pRules;
        boolean isCopy = "复制 (Copy)".equals(pCrossDriveMode);
        int total = files.size();
        AtomicInteger processed = new AtomicInteger(0);

        // 使用并行流处理
        return files.parallelStream().map(f -> {
            // 进度反馈
            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 100 == 0) {
                double p = (double) curr / total;
                Platform.runLater(() -> progressReporter.accept(p, "正在计算: " + curr + "/" + total));
            }

            String originalPath = f.getAbsolutePath();
            String currentName = f.getName(); // 当前处理的文件名
            String parentPath = f.getParent(); // 父目录路径

            // 依次应用所有规则
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

            // 如果没有规则生效，或者是根目录保护
            if (!appliedAny || rootDirs.contains(f)) {
                return new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE);
            }

            // 构建最终路径
            // 检查结果是否包含路径分隔符 (implies moving/absolute path)
            File targetFile;
            OperationType opType;

            if (currentName.contains(File.separator) || (System.getProperty("os.name").toLowerCase().contains("win") && currentName.contains(":"))) {
                // 结果是一个路径
                File potentialPath = new File(currentName);
                if (potentialPath.isAbsolute()) {
                    targetFile = potentialPath;
                } else {
                    targetFile = new File(parentPath, currentName);
                }

                // 判断是 Move 还是 Copy (跨盘符或者是用户配置)
                // 这里简单逻辑：如果用户选了 Copy 且路径变了，就是 Copy。否则 Move。
                // 实际上 Rename 和 Move 在 Java NIO 很多时候是同一回事，除非跨盘。
                // 为了简化，如果路径变了，根据配置决定 Copy/Move
                if (isCopy) {
                    opType = OperationType.CONVERT; // 借用 CONVERT 类型表示“生成新文件”/复制
                } else {
                    opType = OperationType.MOVE;
                }
            } else {
                // 纯重命名
                targetFile = new File(parentPath, currentName);
                opType = OperationType.RENAME;
            }

            boolean changed = !targetFile.getAbsolutePath().equals(originalPath);

            // 如果是 Copy 模式，即使 changed=false 也可能需要操作吗？通常不需要。

            // 构建参数
            Map<String, String> params = new HashMap<>();
            if (opType == OperationType.CONVERT) {
                params.put("action", "copy"); // 标记为复制操作
            }

            return new ChangeRecord(f.getName(), targetFile.getName(), f, changed, targetFile.getAbsolutePath(), opType, params, ExecStatus.PENDING);

        }).collect(Collectors.toList());
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        return Collections.emptyList();
    }

    // --- Helper: Dialog for Editing Rules ---
    private void showRuleEditDialog(RenameRule existingRule) {
        Dialog<RenameRule> dialog = new Dialog<>();
        dialog.setTitle(existingRule == null ? "添加重命名规则" : "编辑重命名规则");
        dialog.setHeaderText("配置规则生效条件与替换逻辑");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // UI Components for Rule
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 1. Conditions
        ListView<RuleCondition> lvConditions = new ListView<>();
        lvConditions.setPrefHeight(100);
        ObservableList<RuleCondition> conditions = FXCollections.observableArrayList();
        if (existingRule != null) conditions.addAll(existingRule.conditions);
        lvConditions.setItems(conditions);

        JFXComboBox<ConditionType> cbCondType = new JFXComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
        cbCondType.getSelectionModel().select(0);
        TextField txtCondValue = new TextField();
        txtCondValue.setPromptText("条件值");
        JFXButton btnAddCond = new JFXButton("+");
        btnAddCond.setOnAction(e -> {
            if (!txtCondValue.getText().isEmpty()) {
                conditions.add(new RuleCondition(cbCondType.getValue(), txtCondValue.getText()));
                txtCondValue.clear();
            }
        });
        JFXButton btnDelCond = new JFXButton("-");
        btnDelCond.setOnAction(e -> {
            RuleCondition sel = lvConditions.getSelectionModel().getSelectedItem();
            if (sel != null) conditions.remove(sel);
        });

        // 2. Action
        JFXComboBox<RenameActionType> cbActionType = new JFXComboBox<>(FXCollections.observableArrayList(RenameActionType.values()));
        cbActionType.getSelectionModel().select(RenameActionType.REPLACE_TEXT);
        TextField txtFind = new TextField();
        txtFind.setPromptText("查找内容 / 正则表达式");
        TextField txtReplace = new TextField();
        txtReplace.setPromptText("替换为");

        if (existingRule != null) {
            cbActionType.getSelectionModel().select(existingRule.renameActionType);
            txtFind.setText(existingRule.findStr);
            txtReplace.setText(existingRule.replaceStr);
        }

        // 动态显隐
        cbActionType.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            boolean needFind = (val == RenameActionType.REPLACE_TEXT || val == RenameActionType.REPLACE_REGEX);
            txtFind.setDisable(!needFind);
            if (!needFind) txtFind.clear();
        });

        grid.add(new Label("前置条件 (满足所有才执行):"), 0, 0);
        HBox condBox = new HBox(5, cbCondType, txtCondValue, btnAddCond, btnDelCond);
        grid.add(condBox, 0, 1);
        grid.add(lvConditions, 0, 2);

        grid.add(new Separator(), 0, 3);

        grid.add(new Label("执行动作:"), 0, 4);
        grid.add(cbActionType, 0, 5);
        grid.add(new Label("查找/参数:"), 0, 6);
        grid.add(txtFind, 0, 7);
        grid.add(new Label("替换/追加:"), 0, 8);
        grid.add(txtReplace, 0, 9);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new RenameRule(new ArrayList<>(conditions), cbActionType.getValue(), txtFind.getText(), txtReplace.getText());
            }
            return null;
        });

        Optional<RenameRule> result = dialog.showAndWait();
        result.ifPresent(rule -> {
            if (existingRule != null) {
                int idx = lvRules.getItems().indexOf(existingRule);
                lvRules.getItems().set(idx, rule);
            } else {
                lvRules.getItems().add(rule);
            }
        });
    }

    // --- Inner Classes for Logic ---

    // 规则列表单元格渲染
    class RuleListCell extends ListCell<RenameRule> {
        @Override
        protected void updateItem(RenameRule item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox v = new VBox(3);
                String condStr = item.conditions.isEmpty() ? "无条件 (总是执行)" :
                        item.conditions.stream().map(RuleCondition::toString).collect(Collectors.joining(" && "));
                Label lCond = new Label("如果: " + condStr);
                lCond.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                String actStr = "";
                switch (item.renameActionType) {
                    case REPLACE_TEXT:
                        actStr = "替换 \"" + item.findStr + "\" 为 \"" + item.replaceStr + "\"";
                        break;
                    case REPLACE_REGEX:
                        actStr = "正则替换 /" + item.findStr + "/ -> \"" + item.replaceStr + "\"";
                        break;
                    case PREPEND:
                        actStr = "前缀添加 \"" + item.replaceStr + "\"";
                        break;
                    case APPEND:
                        actStr = "后缀添加 \"" + item.replaceStr + "\"";
                        break;
                    case TO_LOWER:
                        actStr = "转为小写";
                        break;
                    case TO_UPPER:
                        actStr = "转为大写";
                        break;
                    case TRIM:
                        actStr = "去除首尾空格";
                        break;
                }
                Label lAct = new Label("执行: " + actStr);
                lAct.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                v.getChildren().addAll(lCond, lAct);
                setGraphic(v);
                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) showRuleEditDialog(item);
                });
            }
        }
    }
}



