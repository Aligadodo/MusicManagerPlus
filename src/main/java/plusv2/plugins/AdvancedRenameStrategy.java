package plusv2.plugins;


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
import plusv2.AppStrategyV2;
import plusv2.model.ChangeRecord;
import plusv2.model.RenameRule;
import plusv2.model.RuleCondition;
import plusv2.type.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AdvancedRenameStrategy extends AppStrategyV2 {
    private final ListView<RenameRule> lvRules;
    private final JFXButton btnAddRule, btnRemoveRule, btnMoveUp, btnMoveDown;
    private final JFXComboBox<String> cbCrossDriveMode;
    private final Spinner<Integer> spThreads;
    private final JFXComboBox<String> cbProcessScope;

    private List<RenameRule> pRules;
    private String pCrossDriveMode;
    private int pThreads;
    private int pProcessScopeIndex; // 改用索引判断，避免字符串匹配错误

    public AdvancedRenameStrategy() {
        lvRules = new ListView<>();
        lvRules.setCellFactory(p -> new RuleListCell());
        lvRules.setPlaceholder(new Label("暂无规则，请点击下方添加..."));
        lvRules.setPrefHeight(200);

        btnAddRule = new JFXButton("添加规则");
        btnAddRule.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnAddRule.setOnAction(e -> showRuleEditDialog(null));

        btnRemoveRule = new JFXButton("删除");
        btnRemoveRule.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnRemoveRule.setOnAction(e -> {
            RenameRule s = lvRules.getSelectionModel().getSelectedItem();
            if (s != null) lvRules.getItems().remove(s);
        });

        btnMoveUp = new JFXButton("↑");
        btnMoveUp.setOnAction(e -> moveRule(-1));

        btnMoveDown = new JFXButton("↓");
        btnMoveDown.setOnAction(e -> moveRule(1));

        cbCrossDriveMode = new JFXComboBox<>(FXCollections.observableArrayList("移动 (Move)", "复制 (Copy)"));
        cbCrossDriveMode.getSelectionModel().select(0);

        spThreads = new Spinner<>(1, 64, Runtime.getRuntime().availableProcessors());

        cbProcessScope = new JFXComboBox<>(FXCollections.observableArrayList("仅处理文件", "仅处理文件夹", "全部处理"));
        cbProcessScope.getSelectionModel().select(2); // 默认全部处理
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

    @Override public String getName() { return "高级重命名 (正则/多规则/首字母/清理)"; }
    @Override public ScanTarget getTargetType() { return ScanTarget.ALL; }
    @Override public int getPreferredThreadCount() { return spThreads.getValue(); }

    @Override
    public Node getConfigNode() {
        VBox r = new VBox(10);
        HBox t = new HBox(10, btnAddRule, btnRemoveRule, new Separator(javafx.geometry.Orientation.VERTICAL), btnMoveUp, btnMoveDown);
        t.setAlignment(Pos.CENTER_LEFT);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(5);
        g.add(new Label("对象:"), 0, 0); g.add(cbProcessScope, 1, 0);
        g.add(new Label("跨盘:"), 2, 0); g.add(cbCrossDriveMode, 3, 0);
        g.add(new Label("并发:"), 4, 0); g.add(spThreads, 5, 0);

        r.getChildren().addAll(new Label("规则链 (从上至下依次执行):"), lvRules, t, new Separator(), g);
        return r;
    }

    @Override
    public void captureParams() {
        pRules = new ArrayList<>(lvRules.getItems());
        pCrossDriveMode = cbCrossDriveMode.getValue();
        pThreads = spThreads.getValue();
        pProcessScopeIndex = cbProcessScope.getSelectionModel().getSelectedIndex();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("arn_crossDrive", cbCrossDriveMode.getValue());
        props.setProperty("arn_scope_idx", String.valueOf(cbProcessScope.getSelectionModel().getSelectedIndex()));
        props.setProperty("arn_threads", String.valueOf(spThreads.getValue()));
    }
    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("arn_crossDrive")) cbCrossDriveMode.getSelectionModel().select(props.getProperty("arn_crossDrive"));
        if (props.containsKey("arn_scope_idx")) {
            try {
                cbProcessScope.getSelectionModel().select(Integer.parseInt(props.getProperty("arn_scope_idx")));
            } catch (NumberFormatException e) {
                cbProcessScope.getSelectionModel().select(2);
            }
        }
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> roots, BiConsumer<Double, String> rep) {
        List<RenameRule> rs = pRules;
        boolean cpy = "复制 (Copy)".equals(pCrossDriveMode);
        // 修复：使用索引判断，解决 "仅处理文件夹" 包含 "文件" 字眼导致的逻辑错误
        // 0: 仅文件, 1: 仅文件夹, 2: 全部
        boolean pFile = (pProcessScopeIndex == 0 || pProcessScopeIndex == 2);
        boolean pFolder = (pProcessScopeIndex == 1 || pProcessScopeIndex == 2);

        int tot = files.size();
        AtomicInteger proc = new AtomicInteger(0);

        return files.parallelStream().map(f -> {
            int cur = proc.incrementAndGet();
            if (rep != null && cur % 100 == 0) Platform.runLater(() -> rep.accept((double) cur / tot, "计算: " + cur + "/" + tot));

            boolean d = f.isDirectory();
            if (d && !pFolder) return null;
            if (!d && !pFile) return null;
            if (roots.contains(f)) return null;

            String on = f.getName();
            String cn = on;
            boolean app = false;

            for (RenameRule r : rs) {
                if (r.matches(cn)) {
                    String t = r.apply(cn);
                    if (!t.equals(cn)) {
                        cn = t;
                        app = true;
                    }
                }
            }
            if (!app) return null;

            File tf;
            OperationType op;
            if (cn.contains(File.separator) || (System.getProperty("os.name").toLowerCase().contains("win") && cn.contains(":"))) {
                File pp = new File(cn);
                tf = pp.isAbsolute() ? pp : new File(f.getParent(), cn);
                op = cpy ? OperationType.CONVERT : OperationType.MOVE;
            } else {
                tf = new File(f.getParent(), cn);
                op = OperationType.RENAME;
            }

            boolean ch = !tf.getAbsolutePath().equals(f.getAbsolutePath());
            Map<String, String> pm = new HashMap<>();
            if (op == OperationType.CONVERT) pm.put("action", "copy");
            return new ChangeRecord(f.getName(), tf.getName(), f, ch, tf.getAbsolutePath(), op, pm, ExecStatus.PENDING);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (s.equals(t)) return;
        if (rec.getOpType() == OperationType.CONVERT) {
            if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
            if (s.isDirectory()) copyDirectory(s, t); else Files.copy(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
            Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        Files.walk(source.toPath()).forEach(sourcePath -> {
            Path targetPath = target.toPath().resolve(source.toPath().relativize(sourcePath));
            try { Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { throw new UncheckedIOException(e); }
        });
    }

    private void showRuleEditDialog(RenameRule existingRule) {
        Dialog<RenameRule> dialog = new Dialog<>();
        dialog.setTitle(existingRule == null ? "添加重命名规则" : "编辑重命名规则");
        dialog.setHeaderText("配置规则生效条件与替换逻辑");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 10, 10, 10));

        // Conditions
        ListView<RuleCondition> lvConds = new ListView<>();
        lvConds.setPrefHeight(80);
        ObservableList<RuleCondition> condList = FXCollections.observableArrayList();
        if (existingRule != null) condList.addAll(existingRule.conditions);
        lvConds.setItems(condList);

        JFXComboBox<ConditionType> cbCondType = new JFXComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
        cbCondType.getSelectionModel().select(0);
        TextField txtCondVal = new TextField(); txtCondVal.setPromptText("条件值");
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

        // Dynamic Fields
        TextField txtFind = new TextField();
        TextField txtReplace = new TextField();
        Label lblFind = new Label("参数 A:");
        Label lblReplace = new Label("参数 B:");

        cbAction.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            txtFind.setDisable(false); txtReplace.setDisable(false);
            switch (val) {
                case ADD_LETTER_PREFIX:
                    lblFind.setText("忽略字符 (可选):"); txtFind.setPromptText("如: 'The '");
                    lblReplace.setText("分隔符:"); txtReplace.setPromptText("默认: ' - '");
                    if (txtReplace.getText().isEmpty()) txtReplace.setText(" - ");
                    break;
                case CLEAN_NOISE:
                    lblFind.setText("额外干扰词 (逗号分隔):");
                    txtFind.setPromptText("CD,Live (已内置常用符号)");
                    lblReplace.setText("提示:"); txtReplace.setDisable(true);
                    txtReplace.setPromptText("无需填写");
                    break;
                case BATCH_REMOVE:
                    lblFind.setText("要移除的字符/词 (空格分隔):");
                    txtFind.setPromptText("CD MP3 320k [ ]");
                    lblReplace.setText("提示:"); txtReplace.setDisable(true);
                    txtReplace.setPromptText("无需填写");
                    break;
                case REPLACE_TEXT: case REPLACE_REGEX:
                    lblFind.setText("查找内容:"); txtFind.setPromptText("查找...");
                    lblReplace.setText("替换为:"); txtReplace.setPromptText("替换...");
                    break;
                case PREPEND:
                    lblFind.setText("无效:"); txtFind.setDisable(true);
                    lblReplace.setText("前缀:"); txtReplace.setPromptText("要添加的前缀");
                    break;
                case APPEND:
                    lblFind.setText("无效:"); txtFind.setDisable(true);
                    lblReplace.setText("后缀:"); txtReplace.setPromptText("要添加的后缀");
                    break;
                default:
                    lblFind.setText("无效:"); txtFind.setDisable(true);
                    lblReplace.setText("无效:"); txtReplace.setDisable(true);
                    break;
            }
        });

        if (existingRule != null) {
            cbAction.getSelectionModel().select(existingRule.actionType);
            txtFind.setText(existingRule.findStr);
            txtReplace.setText(existingRule.replaceStr);
        } else {
            cbAction.getSelectionModel().select(ActionType.REPLACE_TEXT);
        }

        grid.add(new Label("1. 前置条件:"), 0, 0);
        grid.add(new HBox(5, cbCondType, txtCondVal, btnAddCond, btnDelCond), 1, 0);
        grid.add(lvConds, 1, 1);
        grid.add(new Separator(), 0, 2, 2, 1);
        grid.add(new Label("2. 执行动作:"), 0, 3); grid.add(cbAction, 1, 3);
        grid.add(lblFind, 0, 4); grid.add(txtFind, 1, 4);
        grid.add(lblReplace, 0, 5); grid.add(txtReplace, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b == saveButtonType ? new RenameRule(new ArrayList<>(condList), cbAction.getValue(), txtFind.getText(), txtReplace.getText()) : null);

        dialog.showAndWait().ifPresent(r -> {
            if (existingRule != null) lvRules.getItems().set(lvRules.getItems().indexOf(existingRule), r);
            else lvRules.getItems().add(r);
        });
    }

    class RuleListCell extends ListCell<RenameRule> {
        @Override protected void updateItem(RenameRule item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); } else {
                VBox v = new VBox(3);
                String cond = item.conditions.isEmpty() ? "无条件" : item.conditions.stream().map(RuleCondition::toString).collect(Collectors.joining(" & "));
                Label l1 = new Label("若: " + cond); l1.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                Label l2 = new Label(item.getActionDesc()); l2.setStyle("-fx-font-weight: bold;");
                v.getChildren().addAll(l1, l2); setGraphic(v);
                setOnMouseClicked(e -> { if (e.getClickCount() == 2) showRuleEditDialog(item); });
            }
        }
    }
}