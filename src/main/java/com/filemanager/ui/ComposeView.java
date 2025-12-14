package com.filemanager.ui;

import com.filemanager.app.IAppController;
import com.filemanager.model.RuleCondition;
import com.filemanager.model.RuleConditionGroup;
import com.filemanager.strategy.AppStrategy;
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
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ComposeView {
    private final IAppController app;
    private final StyleFactory styles;
    private VBox viewNode;
    private VBox configContainer;
    private ListView<AppStrategy> pipelineListView;
    private ListView<File> sourceListView;

    public ComposeView(IAppController app) {
        this.app = app;
        this.styles = app.getStyleFactory();
        buildUI();
    }

    public Node getViewNode() { return viewNode; }

    private void buildUI() {
        viewNode = new VBox(20);

        HBox headers = new HBox(20);
        headers.getChildren().addAll(
            styles.createSectionHeader("1. 源目录", "拖拽添加 / 排序"),
            styles.createSectionHeader("2. 流水线", "按序执行 / 调整"),
            styles.createSectionHeader("3. 参数配置", "选中步骤编辑")
        );
        HBox.setHgrow(headers.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(headers.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(headers.getChildren().get(2), Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(30);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(40);
        grid.getColumnConstraints().addAll(c1, c2, c3);

        // --- Left Panel: Source ---
        VBox leftPanel = styles.createGlassPane();
        leftPanel.setPadding(new Insets(15)); leftPanel.setSpacing(10);

        // 初始化主类成员 sourceListView
        sourceListView = new ListView<>(app.getSourceRoots());
        sourceListView.setPlaceholder(styles.createNormalLabel("拖拽文件夹到此"));
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
                    Label name = styles.createLabel(item.getName(), 13, true);
                    Label path = styles.createInfoLabel(item.getAbsolutePath());
                    path.setTooltip(new Tooltip(item.getAbsolutePath()));
                    content.getChildren().addAll(name, path);

                    HBox actions = new HBox(4);
                    actions.setAlignment(Pos.CENTER_RIGHT);
                    // 文件夹操作：上移、下移、打开、删除
                    JFXButton btnUp = styles.createSmallIconButton("▲", e -> moveListItem(app.getSourceRoots(), getIndex(), -1));
                    JFXButton btnDown = styles.createSmallIconButton("▼", e -> moveListItem(app.getSourceRoots(), getIndex(), 1));
                    JFXButton btnOpen = styles.createSmallIconButton("📂", e -> app.openFileInSystem(item));
                    JFXButton btnDel = styles.createSmallIconButton("✕", e -> {
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
            styles.createActionButton("添加目录", null, app::addDirectoryAction),
            styles.createActionButton("清空", "#e74c3c", app::clearSourceDirs)
        );

        TitledPane tpFilters = new TitledPane("全局筛选", createGlobalFiltersUI());
        tpFilters.setCollapsible(true); tpFilters.setExpanded(true);
        tpFilters.setStyle("-fx-text-fill: " + app.getCurrentTheme().getTextColor() + ";");

        leftPanel.getChildren().addAll(sourceListView, srcTools, tpFilters);
        grid.add(leftPanel, 0, 0);

        // --- Center Panel: Pipeline ---
        VBox centerPanel = styles.createGlassPane();
        centerPanel.setPadding(new Insets(15)); centerPanel.setSpacing(10);

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
                    Label n = styles.createLabel((getIndex() + 1) + ". " + item.getName(), 14, true);
                    Label d = styles.createInfoLabel(item.getDescription());
                    d.setMaxWidth(180);
                    v.getChildren().addAll(n, d);

                    HBox actions = new HBox(4);
                    actions.setAlignment(Pos.CENTER_RIGHT);

                    // 策略操作：上移、下移、删除
                    // (注：配置详情通过列表选中触发，这里不需要额外按钮，或者可以加一个 '⚙' 指示)
                    JFXButton btnUp = styles.createSmallIconButton("▲", e -> {
                        moveListItem(app.getPipelineStrategies(), getIndex(), -1);
                        pipelineListView.getSelectionModel().select(getIndex()); // 保持选中
                    });
                    JFXButton btnDown = styles.createSmallIconButton("▼", e -> {
                        moveListItem(app.getPipelineStrategies(), getIndex(), 1);
                        pipelineListView.getSelectionModel().select(getIndex());
                    });
                    JFXButton btnDel = styles.createSmallIconButton("✕", e -> {
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
        pipelineListView.getSelectionModel().selectedItemProperty().addListener((o,old,val) -> refreshConfig(val));


        HBox pipeActions = new HBox(5);
        JFXComboBox<AppStrategy> cbAdd = new JFXComboBox<>(FXCollections.observableArrayList(app.getStrategyPrototypes()));
        cbAdd.setPromptText("选择功能...");
        cbAdd.setPrefWidth(150);
        cbAdd.setConverter(new StringConverter<AppStrategy>() {
            @Override public String toString(AppStrategy o) { return o == null ? "" : o.getName(); }
            @Override public AppStrategy fromString(String s) { return null; }
        });

        JFXButton btnAddStep = styles.createActionButton("添加步骤", "#2ecc71",
                () -> {
                    try {
                        AppStrategy strategy = cbAdd.getValue().getClass().getDeclaredConstructor().newInstance();
                        strategy.loadConfig(new Properties());
                        app.addStrategyStep(strategy);
                    } catch (Exception e) {
                        app.log("组件添加失败:"+ ExceptionUtils.getStackTrace(e));
                    }
                });
        pipeActions.getChildren().addAll(cbAdd, btnAddStep);

        centerPanel.getChildren().addAll(pipelineListView, pipeActions);
        grid.add(centerPanel, 1, 0);

        // --- Right Panel: Config ---
        VBox rightPanel = styles.createGlassPane();
        rightPanel.setPadding(new Insets(15));
        configContainer = new VBox(10);
        configContainer.setStyle("-fx-background-color: transparent;");
        ScrollPane sc = new ScrollPane(configContainer);
        sc.setFitToWidth(true); sc.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(sc, Priority.ALWAYS);
        rightPanel.getChildren().add(sc);
        grid.add(rightPanel, 2, 0);

        VBox.setVgrow(grid, Priority.ALWAYS);

        // --- Bottom ---
        HBox bottom = new HBox(); bottom.setAlignment(Pos.CENTER_RIGHT); bottom.setPadding(new Insets(10));
        JFXButton btnGo = styles.createActionButton("生成预览  ▶", null, app::runPipelineAnalysis);
        btnGo.setPadding(new Insets(10, 30, 10, 30));
        bottom.getChildren().add(btnGo);

        viewNode.getChildren().addAll(headers, grid, bottom);

        // Auto select first
        refreshList();
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

    private Node createGlobalFiltersUI() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f9f9f9; -fx-text-fill: #333333;");
        box.getChildren().addAll(
            styles.createNormalLabel("递归模式:"), app.getCbRecursionMode(), app.getSpRecursionDepth(),
            styles.createNormalLabel("文件扩展名:"), app.getCcbFileTypes(),
            new Separator(), styles.createNormalLabel("全局线程数:"), app.getSpGlobalThreads()
        );
        return box;
    }
    
    public void refreshConfig(AppStrategy s) {
        configContainer.getChildren().clear();
        if(s==null) return;
        
        configContainer.getChildren().addAll(
            styles.createHeader(s.getName()),
            styles.createInfoLabel(s.getDescription()),
            new Separator(),
            styles.createNormalLabel("前置条件 (可选):"),
            createConditionsUI(s),
            new Separator(),
            styles.createNormalLabel("参数配置:"),
            s.getConfigNode() != null ? s.getConfigNode() : new Label("无")
        );
        styles.forceDarkText(configContainer);
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
        JFXButton btnAddGroup = new JFXButton("添加条件组 (OR)");
        btnAddGroup.setStyle("-fx-background-color: #e0f7fa; -fx-text-fill: #006064; -fx-border-color: #b2ebf2; -fx-border-radius: 4; -fx-cursor: hand;");
        btnAddGroup.setMaxWidth(Double.MAX_VALUE);
        btnAddGroup.setOnAction(e -> {
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
        Label lblTitle = new Label("条件组 " + index + " (且 AND)");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        JFXButton btnDelGroup = styles.createButton("✕", e -> onDeleteGroup.run());
        btnDelGroup.setStyle("-fx-text-fill: red; -fx-background-color: transparent;");
        header.getChildren().addAll(lblTitle, spacer, btnDelGroup);

        // Conditions List
        VBox condList = new VBox(2);
        for (RuleCondition cond : group.getConditions()) {
            HBox row = new HBox(5);
            row.setAlignment(Pos.CENTER_LEFT);
            Label lblC = new Label("• " + cond.toString());
            lblC.setTextFill(Color.web("#333"));
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            JFXButton btnDelC = styles.createButton("−", e -> {
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
            if(!needsVal) txtVal.clear();
        });

        JFXButton btnAdd = styles.createButton("+", e -> {
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