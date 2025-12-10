package com.filemanager.ui;

import com.filemanager.app.IAppController;
import com.filemanager.model.RuleCondition;
import com.filemanager.strategy.AppStrategy;
import com.filemanager.type.ConditionType;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.io.File;

public class ComposeView {
    private final IAppController controller;
    private final StyleFactory styles;
    private VBox viewNode;
    private VBox configContainer;
    private ListView<AppStrategy> pipelineListView;
    private ListView<File> sourceListView;

    public ComposeView(IAppController controller) {
        this.controller = controller;
        this.styles = controller.getStyleFactory();
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
        
        sourceListView = new ListView<>(controller.getSourceRoots());
        sourceListView.setPlaceholder(styles.createNormalLabel("拖拽文件夹到此"));
        VBox.setVgrow(sourceListView, Priority.ALWAYS);
        
        sourceListView.setCellFactory(p -> new ListCell<File>() {
            @Override protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) { 
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;"); 
                } else {
                    setText(null);
                    BorderPane pane = new BorderPane();
                    VBox content = new VBox(2);
                    content.getChildren().addAll(styles.createLabel(item.getName(), 13, true), styles.createInfoLabel(item.getAbsolutePath()));
                    JFXButton btnDel = styles.createIconButton("✕", "#e74c3c", () -> controller.removeSourceDir(item));
                    pane.setCenter(content); pane.setRight(btnDel);
                    setGraphic(pane);
                    setTooltip(new Tooltip(item.getAbsolutePath()));
                    setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                }
            }
        });
        
        sourceListView.setOnDragOver(e -> { if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE); e.consume(); });
        sourceListView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                boolean changed = false;
                for(File f : db.getFiles()) {
                    if(f.isDirectory() && !controller.getSourceRoots().contains(f)) {
                        controller.getSourceRoots().add(f); changed = true;
                    }
                }
                if(changed) controller.invalidatePreview("源变更");
            }
            e.setDropCompleted(true); e.consume();
        });

        HBox srcTools = new HBox(10);
        srcTools.getChildren().addAll(
            styles.createActionButton("添加目录", null, controller::addDirectoryAction),
            styles.createActionButton("清空", "#e74c3c", controller::clearSourceDirs)
        );
        
        TitledPane tpFilters = new TitledPane("全局筛选", createGlobalFiltersUI());
        tpFilters.setCollapsible(true); tpFilters.setExpanded(true);
        tpFilters.setStyle("-fx-text-fill: " + controller.getCurrentTheme().getTextColor() + ";");
        
        leftPanel.getChildren().addAll(sourceListView, srcTools, tpFilters);
        grid.add(leftPanel, 0, 0);

        // --- Center Panel: Pipeline ---
        VBox centerPanel = styles.createGlassPane();
        centerPanel.setPadding(new Insets(15)); centerPanel.setSpacing(10);
        
        pipelineListView = new ListView<>(controller.getPipelineStrategies());
        pipelineListView.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 5;");
        VBox.setVgrow(pipelineListView, Priority.ALWAYS);

        pipelineListView.setCellFactory(param -> new ListCell<AppStrategy>() {
            @Override protected void updateItem(AppStrategy item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                } else {
                    BorderPane pane = new BorderPane();
                    VBox v = new VBox(2);
                    Label n = styles.createLabel((getIndex()+1) + ". " + item.getName(), 13, true);
                    Label d = styles.createInfoLabel(item.getDescription());
                    d.setMaxWidth(180);
                    v.getChildren().addAll(n, d);

                    JFXButton btnDel = new JFXButton("✕");
                    btnDel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-background-color: transparent; -fx-cursor: hand;");
                    btnDel.setOnAction(e -> {
                        controller.getPipelineStrategies().remove(item);
                        configContainer.getChildren().clear();
                        controller.invalidatePreview("步骤移除");
                    });

                    pane.setCenter(v);
                    pane.setRight(btnDel);
                    setGraphic(pane);

                    if (isSelected()) setStyle("-fx-background-color: rgba(52, 152, 219, 0.15); -fx-border-color: #3498db; -fx-border-width: 0 0 1 0;");
                    else setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
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
        pipelineListView.getSelectionModel().selectedItemProperty().addListener((o,old,val) -> refreshConfig(val));
        // Force refresh on click
        pipelineListView.setOnMouseClicked(e -> refreshConfig(pipelineListView.getSelectionModel().getSelectedItem()));

        HBox pipeActions = new HBox(5);
        JFXComboBox<AppStrategy> cbAdd = new JFXComboBox<>(FXCollections.observableArrayList(controller.getStrategyPrototypes()));
        cbAdd.setPromptText("选择功能...");
        cbAdd.setPrefWidth(150);
        cbAdd.setConverter(new StringConverter<AppStrategy>() {
            @Override public String toString(AppStrategy o) { return o == null ? "" : o.getName(); }
            @Override public AppStrategy fromString(String s) { return null; }
        });
        
        JFXButton btnAddStep = styles.createActionButton("添加步骤", "#2ecc71", () -> controller.addStrategyStep(cbAdd.getValue()));
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
        JFXButton btnGo = styles.createActionButton("生成预览  ▶", null, controller::runPipelineAnalysis);
        btnGo.setPadding(new Insets(10, 30, 10, 30));
        bottom.getChildren().add(btnGo);

        viewNode.getChildren().addAll(headers, grid, bottom);
        
        // Auto select first
        refreshList();
    }
    
    private Node createGlobalFiltersUI() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f9f9f9; -fx-text-fill: #333333;");
        box.getChildren().addAll(
            styles.createNormalLabel("递归模式:"), controller.getCbRecursionMode(), controller.getSpRecursionDepth(), 
            styles.createNormalLabel("文件扩展名:"), controller.getCcbFileTypes(),
            new Separator(), styles.createNormalLabel("全局线程数:"), controller.getSpGlobalThreads()
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
        VBox box = new VBox(5);
        
        ObservableList<RuleCondition> obsConditions = FXCollections.observableArrayList(strategy.getGlobalConditions());
        ListView<RuleCondition> lv = new ListView<>(obsConditions);
        lv.setPrefHeight(100);
        lv.setPlaceholder(new Label("无前置条件 (点击 + 添加)"));
        lv.setCellFactory(p -> new ListCell<RuleCondition>() {
            @Override protected void updateItem(RuleCondition item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) { setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;"); } 
                else {
                    setText(null);
                    HBox r = new HBox(10); r.setAlignment(Pos.CENTER_LEFT);
                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                    JFXButton del = styles.createIconButton("✕", "#e74c3c", () -> {
                        strategy.getGlobalConditions().remove(item);
                        obsConditions.remove(item);
                        controller.invalidatePreview("移除条件");
                    });
                    r.getChildren().addAll(styles.createNormalLabel("• " + item.toString()), sp, del);
                    setGraphic(r);
                    setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        HBox input = new HBox(5);
        ComboBox<ConditionType> cbType = new ComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
        cbType.getSelectionModel().select(0);
        TextField txtVal = new TextField(); txtVal.setPromptText("条件值"); HBox.setHgrow(txtVal, Priority.ALWAYS);
        
        JFXButton btnAdd = styles.createActionButton("+", "#3498db", () -> {
            if((!cbType.getValue().needsValue()) || !txtVal.getText().isEmpty()){
                RuleCondition c = new RuleCondition(cbType.getValue(), txtVal.getText());
                strategy.getGlobalConditions().add(c);
                obsConditions.add(c);
                txtVal.clear();
                controller.invalidatePreview("添加条件");
            }
        });
        
        input.getChildren().addAll(cbType, txtVal, btnAdd);
        box.getChildren().addAll(lv, input);
        return box;
    }

    public Tab getTab() {
        return new Tab("流水线管理", getViewNode());
    }

    public void selectFirstStrategy() {
        refreshList();
    }

    public void refreshList() {
        if (!controller.getPipelineStrategies().isEmpty()) {
            pipelineListView.getSelectionModel().selectFirst();
            refreshConfig(controller.getPipelineStrategies().get(0));
        }
    }
}