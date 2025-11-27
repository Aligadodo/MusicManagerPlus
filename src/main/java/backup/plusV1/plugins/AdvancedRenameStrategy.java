package backup.plusV1.plugins;


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
import backup.plusV1.OldAppStrategy;
import backup.plusV1.model.ChangeRecord;
import backup.plusV1.model.RenameRule;
import backup.plusV1.model.RuleCondition;
import backup.plusV1.type.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AdvancedRenameStrategy extends OldAppStrategy {
    private final ListView<RenameRule> lvRules;
    private final JFXButton btnAddRule, btnRemoveRule, btnMoveUp, btnMoveDown;
    private final JFXComboBox<String> cbCrossDriveMode;
    private final Spinner<Integer> spThreads;
    private final JFXComboBox<String> cbProcessScope;

    private List<RenameRule> pRules;
    private String pCrossDriveMode;
    private int pThreads;
    private String pProcessScope;

    public AdvancedRenameStrategy() {
        lvRules = new ListView<>();
        lvRules.setCellFactory(param -> new RuleListCell());
        lvRules.setPlaceholder(new Label("暂无规则，请点击下方添加..."));
        lvRules.setPrefHeight(200);
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
        cbCrossDriveMode = new JFXComboBox<>(FXCollections.observableArrayList("移动 (Move)", "复制 (Copy)"));
        cbCrossDriveMode.getSelectionModel().select(0);
        spThreads = new Spinner<>(1, 64, Runtime.getRuntime().availableProcessors());
        cbProcessScope = new JFXComboBox<>(FXCollections.observableArrayList("仅处理文件 (Files Only)", "仅处理文件夹 (Folders Only)", "全部处理 (Files & Folders)"));
        cbProcessScope.getSelectionModel().select(0);
        cbProcessScope.setTooltip(new Tooltip("选择重命名规则生效的对象类型"));
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
        return "高级重命名 (正则/多规则/递归支持)";
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
        HBox globalSettings = new HBox(15, new Label("处理对象:"), cbProcessScope, new Label("跨盘符:"), cbCrossDriveMode, new Label("并发:"), spThreads);
        globalSettings.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(new Label("规则链 (从上至下依次执行):"), lvRules, toolbar, new Separator(), globalSettings);
        return root;
    }

    @Override
    public void captureParams() {
        pRules = new ArrayList<>(lvRules.getItems());
        pCrossDriveMode = cbCrossDriveMode.getValue();
        pThreads = spThreads.getValue();
        pProcessScope = cbProcessScope.getValue();
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<RenameRule> rules = pRules;
        boolean isCopy = "复制 (Copy)".equals(pCrossDriveMode);
        boolean processFiles = pProcessScope.contains("File") || pProcessScope.contains("全部");
        boolean processFolders = pProcessScope.contains("Folder") || pProcessScope.contains("全部");
        int total = files.size();
        AtomicInteger processed = new AtomicInteger(0);

        return files.parallelStream().map(f -> {
            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 100 == 0) {
                double p = (double) curr / total;
                Platform.runLater(() -> progressReporter.accept(p, "规则计算: " + curr + "/" + total));
            }

            boolean isDir = f.isDirectory();
            if (isDir && !processFolders) return null;
            if (!isDir && !processFiles) return null;
            if (rootDirs.contains(f)) return null;

            String originalPath = f.getAbsolutePath();
            String currentName = f.getName();
            String parentPath = f.getParent();
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
            if (!appliedAny) return null;

            File targetFile;
            OperationType opType;
            if (currentName.contains(File.separator) || (System.getProperty("os.name").toLowerCase().contains("win") && currentName.contains(":"))) {
                File potentialPath = new File(currentName);
                targetFile = potentialPath.isAbsolute() ? potentialPath : new File(parentPath, currentName);
                opType = isCopy ? OperationType.CONVERT : OperationType.MOVE;
            } else {
                targetFile = new File(parentPath, currentName);
                opType = OperationType.RENAME;
            }

            boolean changed = !targetFile.getAbsolutePath().equals(originalPath);
            Map<String, String> params = new HashMap<>();
            if (opType == OperationType.CONVERT) params.put("action", "copy");
            return new ChangeRecord(f.getName(), targetFile.getName(), f, changed, targetFile.getAbsolutePath(), opType, params, ExecStatus.PENDING);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        return Collections.emptyList();
    }

    private void showRuleEditDialog(RenameRule existingRule) {
        Dialog<RenameRule> dialog = new Dialog<>();
        dialog.setTitle(existingRule == null ? "添加重命名规则" : "编辑重命名规则");
        dialog.setHeaderText("配置规则生效条件与替换逻辑");
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
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
        JFXComboBox<ActionType> cbActionType = new JFXComboBox<>(FXCollections.observableArrayList(ActionType.values()));
        cbActionType.getSelectionModel().select(ActionType.REPLACE_TEXT);
        TextField txtFind = new TextField();
        txtFind.setPromptText("查找/正则");
        TextField txtReplace = new TextField();
        txtReplace.setPromptText("替换/追加");
        if (existingRule != null) {
            cbActionType.getSelectionModel().select(existingRule.actionType);
            txtFind.setText(existingRule.findStr);
            txtReplace.setText(existingRule.replaceStr);
        }
        cbActionType.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            boolean needFind = (val == ActionType.REPLACE_TEXT || val == ActionType.REPLACE_REGEX);
            txtFind.setDisable(!needFind);
            if (!needFind) txtFind.clear();
        });
        grid.add(new Label("前置条件:"), 0, 0);
        grid.add(new HBox(5, cbCondType, txtCondValue, btnAddCond, btnDelCond), 0, 1);
        grid.add(lvConditions, 0, 2);
        grid.add(new Separator(), 0, 3);
        grid.add(new Label("执行动作:"), 0, 4);
        grid.add(cbActionType, 0, 5);
        grid.add(new Label("查找:"), 0, 6);
        grid.add(txtFind, 0, 7);
        grid.add(new Label("替换:"), 0, 8);
        grid.add(txtReplace, 0, 9);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> btn == saveButtonType ? new RenameRule(new ArrayList<>(conditions), cbActionType.getValue(), txtFind.getText(), txtReplace.getText()) : null);
        dialog.showAndWait().ifPresent(r -> {
            if (existingRule != null) lvRules.getItems().set(lvRules.getItems().indexOf(existingRule), r);
            else lvRules.getItems().add(r);
        });
    }

    class RuleListCell extends ListCell<RenameRule> {
        @Override
        protected void updateItem(RenameRule item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox v = new VBox(3);
                String cond = item.conditions.isEmpty() ? "无条件" : item.conditions.stream().map(RuleCondition::toString).collect(Collectors.joining(" & "));
                Label l1 = new Label("条件: " + cond);
                l1.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                Label l2 = new Label(item.getActionDesc());
                l2.setStyle("-fx-font-weight: bold;");
                v.getChildren().addAll(l1, l2);
                setGraphic(v);
                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) showRuleEditDialog(item);
                });
            }
        }
    }
}



