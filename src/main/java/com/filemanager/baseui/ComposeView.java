package com.filemanager.baseui;

import com.filemanager.app.IAppController;
import com.filemanager.model.RuleCondition;
import com.filemanager.model.RuleConditionGroup;
import com.filemanager.strategy.AppStrategy;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.type.ConditionType;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ComposeView {
    private final IAppController app;
    private VBox viewNode;
    private VBox configContainer;
    private ListView<AppStrategy> pipelineListView;
    private ListView<File> sourceListView;

    public ComposeView(IAppController app) {
        this.app = app;
        this.buildUI();
        StyleFactory.setBasicStyle(viewNode);
    }

    public Node getViewNode() {
        return viewNode;
    }

    private void buildUI() {
        viewNode = new VBox(20);
        GridPane grid = new GridPane();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(25);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(45);
        grid.getColumnConstraints().addAll(c1, c2, c3);
        grid.add(StyleFactory.createHBoxPanel(StyleFactory.createSectionHeader("step1-选择目录",
                "通过弹窗或者拖拽至空白处来添加需要处理的文件或文件夹。")), 0, 0);
        grid.add(StyleFactory.createHBoxPanel(StyleFactory.createSectionHeader("step2-流水线配置",
                "添加必要的处理流程，可同时应用不同的操作。（同一文件只会被成功修改一次）。")), 1, 0);
        grid.add(StyleFactory.createHBoxPanel(StyleFactory.createSectionHeader("step3-参数配置",
                "选中步骤并编辑步骤下的参数，支持配置步骤的前置条件，以在特定条件下执行。")), 2, 0);
        grid.add(createLeftPanel(), 0, 1);
        grid.add(createMidPanel(), 1, 1);
        grid.add(createRightPanel(), 2, 1);
        VBox.setVgrow(grid, Priority.ALWAYS);
        viewNode.getChildren().addAll(StyleFactory.createSeparator(), grid);
        // Auto select first
        refreshList();
    }

    private Node createLeftPanel() {
        // --- Left Panel: Source ---
        VBox leftPanel = StyleFactory.createVBoxPanel();
        leftPanel.setPadding(new Insets(15));
        leftPanel.setSpacing(10);

        // 初始化主类成员 sourceListView
        sourceListView = new ListView<>(app.getSourceRoots());
        sourceListView.setPlaceholder(StyleFactory.createChapter("拖拽文件夹到此"));
        VBox.setVgrow(sourceListView, Priority.ALWAYS);

        // [增强] 源目录列表单元格：支持完整路径显示 + 行内操作
        sourceListView.setCellFactory(p -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null); // 使用 Graphic 布局
                    BorderPane pane = new BorderPane();

                    VBox content = new VBox(2);
                    Label name = StyleFactory.createLabel(item.getName(), 13, true);
                    Label path = StyleFactory.createInfoLabel(item.getAbsolutePath());
                    path.setTooltip(new Tooltip(item.getAbsolutePath()));
                    content.getChildren().addAll(name, path);

                    HBox actions = new HBox(4);
                    actions.setAlignment(Pos.CENTER_RIGHT);
                    // 文件夹操作：上移、下移、打开、删除
                    JFXButton btnUp = StyleFactory.createSmallIconButton("▲", e -> moveListItem(app.getSourceRoots(), getIndex(), -1));
                    JFXButton btnDown = StyleFactory.createSmallIconButton("▼", e -> moveListItem(app.getSourceRoots(), getIndex(), 1));
                    JFXButton btnOpen = StyleFactory.createSmallIconButton("📂", e -> app.openFileInSystem(item));
                    JFXButton btnDel = StyleFactory.createSmallIconButton("✕", e -> {
                        app.getSourceRoots().remove(item);
                        app.invalidatePreview("移除源目录");
                    });
                    btnDel.setTextFill(Color.web("#e74c3c")); // 红色删除键

                    actions.getChildren().addAll(btnUp, btnDown, btnOpen, btnDel);

                    pane.setCenter(content);
                    pane.setRight(actions);
                    setGraphic(pane);
                    setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

                    // 拖拽支持
                    setOnDragOver(e -> {
                        if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                        e.consume();
                    });
                    setOnDragDropped(e -> handleDragDrop(e));
                }
            }
        });
        // 列表本身的拖拽支持
        sourceListView.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            e.consume();
        });
        sourceListView.setOnDragDropped(this::handleDragDrop);

        HBox srcTools = new HBox(10);
        srcTools.getChildren().addAll(
                StyleFactory.createActionButton("添加目录", null, app::addDirectoryAction),
                StyleFactory.createActionButton("清空", "#e74c3c", app::clearSourceDirs)
        );

        TitledPane tpFilters = new TitledPane("全局筛选", app.getGlobalSettingsView());
        tpFilters.setCollapsible(true);
        tpFilters.setExpanded(true);
        tpFilters.setStyle("-fx-text-fill: " + app.getCurrentTheme().getTextColor() + ";");

        leftPanel.getChildren().addAll(srcTools, sourceListView, tpFilters);
        return leftPanel;
    }

    private Node createMidPanel() {
        // --- Center Panel: Pipeline ---
        VBox centerPanel = StyleFactory.createVBoxPanel();
        centerPanel.setPadding(new Insets(15));
        centerPanel.setSpacing(10);

        pipelineListView = new ListView<>(app.getPipelineStrategies());
        pipelineListView.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 5;");
        VBox.setVgrow(pipelineListView, Priority.ALWAYS);

        pipelineListView.setCellFactory(param -> new ListCell<AppStrategy>() {
            @Override
            protected void updateItem(AppStrategy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null);
                    BorderPane pane = new BorderPane();

                    VBox v = new VBox(2);
                    Label n = StyleFactory.createLabel((getIndex() + 1) + ". " + item.getName(), 14, true);
                    Label d = StyleFactory.createInfoLabel(item.getDescription());
                    d.setMaxWidth(180);
                    v.getChildren().addAll(n, d);

                    HBox actions = new HBox(4);
                    actions.setAlignment(Pos.CENTER_RIGHT);

                    // 策略操作：上移、下移、删除
                    // (注：配置详情通过列表选中触发，这里不需要额外按钮，或者可以加一个 '⚙' 指示)
                    JFXButton btnUp = StyleFactory.createSmallIconButton("▲", e -> {
                        moveListItem(app.getPipelineStrategies(), getIndex(), -1);
                        pipelineListView.getSelectionModel().select(getIndex()); // 保持选中
                    });
                    JFXButton btnDown = StyleFactory.createSmallIconButton("▼", e -> {
                        moveListItem(app.getPipelineStrategies(), getIndex(), 1);
                        pipelineListView.getSelectionModel().select(getIndex());
                    });
                    JFXButton btnDel = StyleFactory.createSmallIconButton("✕", e -> {
                        app.getPipelineStrategies().remove(item);
                        configContainer.getChildren().clear(); // 清空配置面板
                        app.invalidatePreview("步骤移除");
                    });
                    btnDel.setTextFill(Color.web("#e74c3c"));

                    actions.getChildren().addAll(btnUp, btnDown, btnDel);

                    pane.setCenter(v);
                    pane.setRight(actions);
                    setGraphic(pane);

                    // 选中态样式处理
                    if (isSelected()) {
                        setStyle("-fx-background-color: rgba(52, 152, 219, 0.15); -fx-border-color: #3498db; -fx-border-width: 0 0 1 0;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                    }
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (!isEmpty() && getItem() != null) {
                    updateStyle(selected);
                }
            }

            private void updateStyle(boolean selected) {
                if (selected) {
                    // 选中样式：淡蓝色背景 + 左侧/底部蓝色边框
                    setStyle("-fx-background-color: rgba(52, 152, 219, 0.15); -fx-border-color: #3498db; -fx-border-width: 0 0 1 0;");
                } else {
                    // 默认样式
                    setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                }
            }

        });

        // 关键：加载完成后自动选中第一项，触发 UI 刷新，解决“不回显”问题
        if (!app.getPipelineStrategies().isEmpty()) {
            Platform.runLater(() -> {
                pipelineListView.getSelectionModel().select(0);
                refreshConfig(pipelineListView.getSelectionModel().getSelectedItem());
            });
        }

        // [新增] 鼠标点击强制刷新，解决只有一项时点击不显示的问题
        pipelineListView.setOnMouseClicked(e -> {
            AppStrategy s = pipelineListView.getSelectionModel().getSelectedItem();
            if (s != null) refreshConfig(s);
        });
        pipelineListView.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> refreshConfig(val));


        HBox pipeActions = new HBox(5);
        JFXComboBox<AppStrategy> cbAdd = new JFXComboBox<>(FXCollections.observableArrayList(app.getStrategyPrototypes()));
        cbAdd.setPromptText("选择功能...");
        cbAdd.setPrefWidth(150);
        cbAdd.setConverter(new StringConverter<AppStrategy>() {
            @Override
            public String toString(AppStrategy o) {
                return o == null ? "" : o.getName();
            }

            @Override
            public AppStrategy fromString(String s) {
                return null;
            }
        });

        JFXButton btnAddStep = StyleFactory.createActionButton("添加步骤", "#2ecc71",
                () -> {
                    try {
                        AppStrategy strategy = cbAdd.getValue().getClass().getDeclaredConstructor().newInstance();
                        strategy.loadConfig(new Properties());
                        app.addStrategyStep(strategy);
                    } catch (Exception e) {
                        app.logError("组件添加失败:" + ExceptionUtils.getStackTrace(e));
                    }
                });
        pipeActions.getChildren().addAll(cbAdd, btnAddStep);
        centerPanel.getChildren().addAll(pipeActions, pipelineListView);
        return centerPanel;
    }

    private Node createRightPanel() {
        // --- Right Panel: Config ---
        VBox rightPanel = StyleFactory.createVBoxPanel();
        rightPanel.setPadding(new Insets(15));
        configContainer = new VBox(10);
        configContainer.setStyle("-fx-background-color: transparent;");
        ScrollPane sc = new ScrollPane(configContainer);
        sc.setFitToWidth(true);
        sc.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sc, Priority.ALWAYS);
        rightPanel.getChildren().add(sc);
        return rightPanel;
    }

    private void handleDragDrop(javafx.scene.input.DragEvent e) {
        if (e.getDragboard().hasFiles()) {
            boolean changed = false;
            for (File f : e.getDragboard().getFiles()) {
                if (f.isDirectory() && !app.getSourceRoots().contains(f)) {
                    app.getSourceRoots().add(f);
                    changed = true;
                }
            }
            if (changed) app.invalidatePreview("源变更");
        }
        e.setDropCompleted(true);
        e.consume();
    }

    // [新增] 通用：列表项移动辅助方法
    private <T> void moveListItem(ObservableList<T> list, int index, int direction) {
        int newIndex = index + direction;
        if (newIndex >= 0 && newIndex < list.size()) {
            Collections.swap(list, index, newIndex);
            app.invalidatePreview("列表顺序变更");
        }
    }

    public void refreshConfig(AppStrategy s) {
        configContainer.getChildren().clear();
        if (s == null) return;

        configContainer.getChildren().addAll(
                StyleFactory.createHeader(s.getName()),
                StyleFactory.createInfoLabel(s.getDescription()),
                StyleFactory.createSeparator(),
                StyleFactory.createChapter("前置条件:"),
                createConditionsUI(s),
                StyleFactory.createSeparator(),
                StyleFactory.createChapter("参数配置:"),
                s.getConfigNode() != null ? s.getConfigNode() : new Label("无")
        );
        StyleFactory.forceDarkText(configContainer);
    }

    private Node createConditionsUI(AppStrategy strategy) {
        VBox rootBox = new VBox(10);

        // 容器：存放所有条件组
        VBox groupsContainer = new VBox(8);

        // 渲染现有组
        Runnable refreshGroups = () -> {
            groupsContainer.getChildren().clear();
            List<RuleConditionGroup> groups = strategy.getConditionGroups();
            if (groups.isEmpty()) {
                Label placeholder = new Label("无限制 (点击下方添加条件组)");
                placeholder.setTextFill(Color.GRAY);
                groupsContainer.getChildren().add(placeholder);
            } else {
                for (int i = 0; i < groups.size(); i++) {
                    RuleConditionGroup group = groups.get(i);
                    groupsContainer.getChildren().add(createSingleGroupUI(group, i + 1, strategy, () -> {
                        strategy.getConditionGroups().remove(group);
                        // refresh
                        app.invalidatePreview("移除条件组");
                        // Recursively re-render to update
                        // 简单起见，这里需要重新触发 refreshConfigPanel，或者局部刷新
                        refreshConfig(pipelineListView.getSelectionModel().getSelectedItem());
                    }));

                    // 组之间的 "OR" 连接符
                    if (i < groups.size() - 1) {
                        Label lblOr = new Label("- 或 (OR) -");
                        lblOr.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-padding: 0 0 0 20;");
                        groupsContainer.getChildren().add(lblOr);
                    }
                }
            }
        };
        refreshGroups.run();

        // 底部：添加新组按钮
        JFXButton btnAddGroup = StyleFactory.createActionButton("添加条件组", "#3498db", () -> {
            strategy.getConditionGroups().add(new RuleConditionGroup());
            refreshConfig(strategy); // 刷新整个面板
            app.invalidatePreview("添加条件组");
        });
        rootBox.getChildren().addAll(groupsContainer, btnAddGroup);
        return rootBox;
    }

    // [新增] 单个条件组的 UI
    private Node createSingleGroupUI(RuleConditionGroup group, int index, AppStrategy strategy, Runnable onDeleteGroup) {
        VBox groupBox = new VBox(5);
        groupBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 4; -fx-background-color: rgba(255,255,255,0.4); -fx-padding: 8;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = StyleFactory.createDescLabel("条件组 " + index + " (一组条件内为且)");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        JFXButton btnDelGroup = StyleFactory.createActionButton("删除条件组", "#e74c3c", () -> onDeleteGroup.run());
        btnDelGroup.setStyle("-fx-text-fill: red; -fx-background-color: transparent;");
        header.getChildren().addAll(lblTitle, spacer, btnDelGroup);

        // Conditions List
        VBox condList = new VBox(2);
        for (RuleCondition cond : group.getConditions()) {
            HBox row = new HBox(5);
            row.setAlignment(Pos.CENTER_LEFT);
            Label lblC = new Label("• " + cond.toString());
            lblC.setTextFill(Color.web("#333"));
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            JFXButton btnDelC = StyleFactory.createActionButton("移除条件", "#e74c3c", () -> {
                group.remove(cond);
                if (group.getConditions().isEmpty()) {
                    // 如果组空了，保留组还是删除组？这里保留空组
                }
                refreshConfig(strategy);
                app.invalidatePreview("移除条件");
            });
            btnDelC.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px; -fx-padding: 0 4;");
            row.getChildren().addAll(lblC, sp, btnDelC);
            condList.getChildren().add(row);
        }

        // Add Condition Form
        HBox addForm = new HBox(5);
        ComboBox<ConditionType> cbType = new ComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
        cbType.getSelectionModel().select(0);
        cbType.setPrefWidth(120);
        TextField txtVal = new TextField();
        txtVal.setPromptText("值");
        HBox.setHgrow(txtVal, Priority.ALWAYS);

        // 动态禁用值输入框
        cbType.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> {
            boolean needsVal = val.needsValue();
            txtVal.setDisable(!needsVal);
            if (!needsVal) txtVal.clear();
        });

        JFXButton btnAdd = StyleFactory.createActionButton("添加条件", "#3498db", () -> {
            if (cbType.getValue().needsValue() && txtVal.getText().isEmpty()) return;
            group.add(new RuleCondition(cbType.getValue(), txtVal.getText()));
            refreshConfig(strategy);
            app.invalidatePreview("添加条件");
        });
        btnAdd.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-padding: 2 8;");

        addForm.getChildren().addAll(cbType, txtVal, btnAdd);

        groupBox.getChildren().addAll(header, condList, new Separator(), addForm);
        return groupBox;
    }

    public Tab getTab() {
        return new Tab("流水线管理", getViewNode());
    }

    public void selectFirstStrategy() {
        refreshList();
    }

    public void refreshList() {
        if (!app.getPipelineStrategies().isEmpty()) {
            pipelineListView.getSelectionModel().selectFirst();
            refreshConfig(app.getPipelineStrategies().get(0));
        }
    }
}