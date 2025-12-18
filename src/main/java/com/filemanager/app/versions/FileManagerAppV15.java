package com.filemanager.app.versions;

import com.filemanager.app.IManagerAppInterface;
import com.filemanager.model.*;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.*;
import com.filemanager.type.ConditionType;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.jfoenix.controls.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.controlsfx.control.CheckComboBox;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Echo Music File Manager v15.1 (Complete & Robust)
 * 修复了配置加载、文件扫描、列表过滤等核心功能的完整实现，无桩代码。
 */
public class FileManagerAppV15 extends Application implements IManagerAppInterface {

    private Stage primaryStage;
    private final Properties appProps = new Properties();
    private final File lastConfigFile = new File(System.getProperty("user.home"), ".echo_music_manager_v15.config");

    // --- 核心数据 ---
    final ObservableList<File> sourceRoots = FXCollections.observableArrayList();
    final ObservableList<AppStrategy> pipelineStrategies = FXCollections.observableArrayList();
    private List<ChangeRecord> fullChangeList = new ArrayList<>();

    // --- UI 容器 (层叠布局实现背景) ---
    private StackPane rootContainer;
    private ImageView backgroundImageView;
    private Region backgroundOverlay;
    private BorderPane mainContent;
    private TabPane mainTabPane;
    private Tab tabCompose;
    private Tab tabPreview;
    private Tab tabLog;

    // --- Tab 1: 编排 (Composer) ---
    private ListView<File> sourceListView;
    private ListView<AppStrategy> pipelineListView;
    private VBox configContainer;
    private JFXComboBox<AppStrategy> cbStrategyTemplates;

    // 全局筛选组件
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;

    // --- Tab 2: 预览 (Preview) ---
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label progressLabel, etaLabel, statsLabel;
    private JFXTextField txtSearchFilter;
    private JFXComboBox<String> cbStatusFilter;
    private JFXButton btnExecute, btnStop, btnGoPreview;
    private JFXCheckBox chkHideUnchanged;
    private VBox progressBox; // 进度信息容器

    // --- Tab 3: 日志 (Log) ---
    private TextArea logArea;
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private JFXCheckBox chkSaveLog;

    // --- 系统与任务 ---
    private PrintWriter fileLogger;
    private ExecutorService executorService;
    private Task<?> currentTask;
    private volatile boolean isTaskRunning = false;
    private AnimationTimer uiUpdater;

    // 外观配置
    private String bgImagePath = "";
    private double bgOpacity = 0.9;
    private String themeColor = "#3498db";

    private List<AppStrategy> strategyPrototypes = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 v15.1");

        this.strategyPrototypes = AppStrategyFactory.getAppStrategies();
        loadGlobalConfig(lastConfigFile);

        // 构建层叠根布局
        rootContainer = new StackPane();

        // 1. 背景层
        backgroundImageView = new ImageView();
        backgroundImageView.setPreserveRatio(true);
        backgroundImageView.fitWidthProperty().bind(rootContainer.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootContainer.heightProperty());

        // 2. 遮罩层
        backgroundOverlay = new Region();
        backgroundOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, " + bgOpacity + ");");

        // 3. 内容层
        mainContent = createMainLayout();

        rootContainer.getChildren().addAll(backgroundImageView, backgroundOverlay, mainContent);
        applyAppearance();

        Scene scene = new Scene(rootContainer, 1400, 950);
        if (getClass().getResource("/css/jfoenix-components.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/jfoenix-components.css").toExternalForm());
        }

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            saveGlobalConfig(lastConfigFile);
            forceStop();
            closeFileLogger();
            Platform.exit();
            System.exit(0);
        });

        startLogUpdater();
        primaryStage.show();
    }

    // ==================== 1. 现代感 UI 构建 ====================

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // 顶部：Logo 与 菜单栏
        VBox topBox = new VBox();
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: transparent;");

        Menu fileMenu = new Menu("文件");
        MenuItem loadItem = new MenuItem("加载配置...");
        loadItem.setOnAction(e -> loadConfigAction());
        MenuItem saveItem = new MenuItem("保存配置...");
        saveItem.setOnAction(e -> saveConfigAction());
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(e -> {
            forceStop();
            primaryStage.close();
        });
        fileMenu.getItems().addAll(loadItem, saveItem, new SeparatorMenuItem(), exitItem);

        Menu viewMenu = new Menu("外观");
        MenuItem themeItem = new MenuItem("界面设置...");
        themeItem.setOnAction(e -> showAppearanceDialog());
        viewMenu.getItems().add(themeItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu);

        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        Label logo = new Label("ECHO MUSIC MANAGER");
        logo.setFont(Font.font("Segoe UI", FontWeight.BLACK, 20));
        logo.setTextFill(Color.web(themeColor));
        header.getChildren().addAll(logo, new Spacer(), menuBar);

        topBox.getChildren().addAll(header, new Separator());
        root.setTop(topBox);

        // 中间：Tab 容器
        mainTabPane = new JFXTabPane();
        mainTabPane.setStyle("-fx-background-color: transparent;");

        createComposeTab();
        createPreviewTab();
        createLogTab();

        mainTabPane.getTabs().addAll(tabCompose, tabPreview, tabLog);
        root.setCenter(mainTabPane);

        // 底部：状态栏
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setStyle("-fx-background-color: rgba(240, 240, 240, 0.8); -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        Label lblStatusIcon = new Label("●");
        lblStatusIcon.setTextFill(Color.GREEN);
        Label lblReady = new Label("就绪");
        statsLabel = new Label("");
        statsLabel.setFont(Font.font("Consolas", 12));

        statusBar.getChildren().addAll(lblStatusIcon, lblReady, new Spacer(), statsLabel);
        root.setBottom(statusBar);

        return root;
    }

    // --- Tab 1: 任务编排 ---
    private void createComposeTab() {
        tabCompose = new Tab("任务编排");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(20);
        grid.setVgap(15);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(30);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(30);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(40);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        grid.add(styledHeader("1. 选择源目录", "拖拽文件夹到下方列表"), 0, 0);
        grid.add(styledHeader("2. 编排操作流程", "自上而下顺序执行"), 1, 0);
        grid.add(styledHeader("3. 参数详细配置", "选中左侧步骤进行调整"), 2, 0);

        // --- Left: Source ---
        VBox leftBox = new VBox(10);
        sourceListView = new ListView<>(sourceRoots);
        sourceListView.setPlaceholder(new Label("拖拽文件夹到此处"));
        VBox.setVgrow(sourceListView, Priority.ALWAYS);

        sourceListView.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            e.consume();
        });
        sourceListView.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                for (File f : e.getDragboard().getFiles())
                    if (f.isDirectory() && !sourceRoots.contains(f)) sourceRoots.add(f);
                invalidatePreview("源变更");
            }
            e.setDropCompleted(true);
            e.consume();
        });

        HBox srcTools = new HBox(10);
        JFXButton btnAddSrc = createButton("添加目录", "plus", e -> addDirectoryAction());
        JFXButton btnClrSrc = createButton("清空", "trash", e -> {
            sourceRoots.clear();
            invalidatePreview("清空源");
        });
        srcTools.getChildren().addAll(btnAddSrc, btnClrSrc);

        TitledPane tpFilters = new TitledPane("全局筛选设置", createGlobalFiltersUI());
        tpFilters.setCollapsible(true);
        tpFilters.setExpanded(true);

        leftBox.getChildren().addAll(sourceListView, srcTools, tpFilters);
        grid.add(leftBox, 0, 1);

        // --- Center: Pipeline ---
        VBox centerBox = new VBox(10);
        pipelineListView = new ListView<>(pipelineStrategies);
        pipelineListView.setCellFactory(param -> new ListCell<AppStrategy>() {
            @Override
            protected void updateItem(AppStrategy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox v = new VBox(2);
                    Label name = new Label((getIndex() + 1) + ". " + item.getName());
                    name.setFont(Font.font("System", FontWeight.BOLD, 13));
                    Label desc = new Label(item.getDescription());
                    desc.setFont(Font.font("System", 10));
                    desc.setTextFill(Color.GRAY);
                    v.getChildren().addAll(name, desc);
                    setGraphic(v);
                }
            }
        });
        pipelineListView.getSelectionModel().selectedItemProperty().addListener((o, old, newVal) -> refreshConfigPanel(newVal));
        VBox.setVgrow(pipelineListView, Priority.ALWAYS);

        HBox pipeTools = new HBox(10);
        cbStrategyTemplates = new JFXComboBox<>(FXCollections.observableArrayList(strategyPrototypes));
        cbStrategyTemplates.setPromptText("选择添加的功能...");
        cbStrategyTemplates.setPrefWidth(200);
        cbStrategyTemplates.setConverter(new javafx.util.StringConverter<AppStrategy>() {
            @Override
            public String toString(AppStrategy o) {
                return o.getName();
            }

            @Override
            public AppStrategy fromString(String s) {
                return null;
            }
        });

        JFXButton btnAddStep = createButton("添加步骤", "plus", e -> addStrategyStep());
        JFXButton btnDelStep = createButton("移除", "minus", e -> {
            AppStrategy s = pipelineListView.getSelectionModel().getSelectedItem();
            if (s != null) {
                pipelineStrategies.remove(s);
                configContainer.getChildren().clear();
                invalidatePreview("移除步骤");
            }
        });

        pipeTools.getChildren().addAll(cbStrategyTemplates, btnAddStep, btnDelStep);
        centerBox.getChildren().addAll(pipelineListView, pipeTools);
        grid.add(centerBox, 1, 1);

        // --- Right: Config ---
        configContainer = new VBox(10);
        configContainer.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 5; -fx-padding: 15;");
        ScrollPane configScroll = new ScrollPane(configContainer);
        configScroll.setFitToWidth(true);
        configScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        grid.add(configScroll, 2, 1);

        // --- Bottom: Action ---
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10));
        btnGoPreview = new JFXButton("下一步：生成预览 >");
        btnGoPreview.setStyle("-fx-background-color: " + themeColor + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 30;");
        btnGoPreview.setOnAction(e -> runPipelineAnalysis());
        bottomBox.getChildren().add(btnGoPreview);

        BorderPane tabRoot = new BorderPane();
        tabRoot.setCenter(grid);
        tabRoot.setBottom(bottomBox);
        tabCompose.setContent(tabRoot);
    }

    private Node createGlobalFiltersUI() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("仅当前目录", "递归所有子目录", "指定目录深度"));
        cbRecursionMode.getSelectionModel().select(1);
        cbRecursionMode.setMaxWidth(Double.MAX_VALUE);
        cbRecursionMode.getSelectionModel().selectedItemProperty().addListener((o, old, v) -> invalidatePreview("递归模式变更"));

        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.setMaxWidth(Double.MAX_VALUE);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录深度"));
        spRecursionDepth.valueProperty().addListener((o, old, v) -> invalidatePreview("递归深度变更"));

        ObservableList<String> extensions = FXCollections.observableArrayList("mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "iso", "jpg", "png", "nfo", "cue");
        ccbFileTypes = new CheckComboBox<>(extensions);
        ccbFileTypes.getCheckModel().checkAll();
        ccbFileTypes.setMaxWidth(Double.MAX_VALUE);
        ccbFileTypes.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> invalidatePreview("类型变更"));

        box.getChildren().addAll(new Label("递归模式:"), cbRecursionMode, spRecursionDepth, new Label("文件扩展名:"), ccbFileTypes);
        return box;
    }

    // --- Tab 2: 预览与执行 ---
    private void createPreviewTab() {
        tabPreview = new Tab("预览与执行");
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        // 1. Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        btnExecute = new JFXButton("执行变更");
        btnExecute.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnExecute.setDisable(true);
        btnExecute.setOnAction(e -> runPipelineExecution());

        btnStop = new JFXButton("停止");
        btnStop.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnStop.setDisable(true);
        btnStop.setOnAction(e -> forceStop());

        // Filter Box
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 20; -fx-padding: 5 15;");

        Label lblFilter = new Label("筛选:");
        txtSearchFilter = new JFXTextField();
        txtSearchFilter.setPromptText("搜索文件名...");
        txtSearchFilter.textProperty().addListener((o, old, v) -> refreshPreviewTableFilter());

        cbStatusFilter = new JFXComboBox<>(FXCollections.observableArrayList("全部", "执行中", "成功", "失败", "已跳过", "待处理"));
        cbStatusFilter.getSelectionModel().select(0);
        cbStatusFilter.valueProperty().addListener((o, old, v) -> refreshPreviewTableFilter());

        chkHideUnchanged = new JFXCheckBox("隐藏无变更项");
        chkHideUnchanged.setSelected(true);
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> refreshPreviewTableFilter());

        filterBox.getChildren().addAll(lblFilter, txtSearchFilter, cbStatusFilter, chkHideUnchanged);
        toolbar.getChildren().addAll(btnExecute, btnStop, new Spacer(), filterBox);

        // 2. Dashboard
        HBox dashboard = new HBox(20);
        dashboard.setAlignment(Pos.CENTER_LEFT);
        dashboard.setPadding(new Insets(10));
        dashboard.setStyle("-fx-background-color: rgba(236, 240, 241, 0.8); -fx-background-radius: 5;");

        mainProgressBar = new ProgressBar(0);
        mainProgressBar.setPrefWidth(400);
        mainProgressBar.setPrefHeight(18);

        progressLabel = new Label("等待任务启动...");
        etaLabel = new Label("--:--");
        etaLabel.setFont(Font.font("Consolas", 12));

        progressBox = new VBox(2); // Ensure initialized
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        progressBox.getChildren().addAll(progressLabel, etaLabel);

        dashboard.getChildren().addAll(new Label("总进度:"), mainProgressBar, progressBox);

        // 3. Table
        previewTable = new TreeTableView<>();
        previewTable.setShowRoot(false);
        previewTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        setupPreviewColumns();

        root.getChildren().addAll(toolbar, dashboard, previewTable);
        tabPreview.setContent(root);
    }

    private void createLogTab() {
        tabLog = new Tab("运行日志");
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        chkSaveLog = new JFXCheckBox("同时写入 execution.log");
        JFXButton btnClear = new JFXButton("清空日志");
        btnClear.setOnAction(e -> logArea.clear());
        controls.getChildren().addAll(chkSaveLog, new Spacer(), btnClear);
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFont(Font.font("Consolas", 12));
        VBox.setVgrow(logArea, Priority.ALWAYS);
        root.getChildren().addAll(controls, logArea);
        tabLog.setContent(root);
    }

    // ==================== 2. 核心逻辑实现 ====================

    private void addStrategyStep() {
        AppStrategy template = cbStrategyTemplates.getValue();
        if (template != null) {
            try {
                AppStrategy newStep = template.getClass().getDeclaredConstructor().newInstance();
                newStep.setContext(this);
                // Inherit global config logic if needed here
                pipelineStrategies.add(newStep);
                pipelineListView.getSelectionModel().select(newStep);
                invalidatePreview("添加步骤");
            } catch (Exception e) {
                log("添加失败: " + e.getMessage());
            }
        }
    }

    private void refreshConfigPanel(AppStrategy strategy) {
        configContainer.getChildren().clear();
        if (strategy == null) return;
        Label title = new Label(strategy.getName());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        Label desc = new Label(strategy.getDescription());
        desc.setWrapText(true);
        desc.setTextFill(Color.GRAY);
        Node cfgNode = strategy.getConfigNode();
        if (cfgNode == null) cfgNode = new Label("此功能无需配置");
        TitledPane tpCond = new TitledPane("前置过滤条件", createConditionsUI(strategy));
        tpCond.setExpanded(false);
        configContainer.getChildren().addAll(title, desc, new Separator(), tpCond, new Separator(), new Label("参数配置:"), cfgNode);
        forceDarkText(cfgNode);
    }

    // --- 核心流水线逻辑 ---

    public void invalidatePreview(String reason) {
        if (!fullChangeList.isEmpty()) {
            fullChangeList.clear();
            previewTable.setRoot(null);
            log("配置变更 (" + reason + ") - 请重新生成预览");
            updateStats(0);
        }
        if (btnExecute != null) btnExecute.setDisable(true);
    }

    void runPipelineAnalysis() {
        if (sourceRoots.isEmpty()) {
            showToast("请先添加源目录！");
            return;
        }
        if (pipelineStrategies.isEmpty()) {
            showToast("请添加至少一个操作步骤！");
            return;
        }
        if (isTaskRunning) return;

        mainTabPane.getSelectionModel().select(tabPreview);
        resetProgressUI("初始化扫描...", false);

        for (AppStrategy s : pipelineStrategies) s.captureParams();
        List<String> exts = new ArrayList<>(ccbFileTypes.getCheckModel().getCheckedItems());
        int depth = "仅当前目录".equals(cbRecursionMode.getValue()) ? 1 : ("递归所有子目录".equals(cbRecursionMode.getValue()) ? Integer.MAX_VALUE : spRecursionDepth.getValue());

        Task<List<ChangeRecord>> task = new Task<List<ChangeRecord>>() {
            @Override
            protected List<ChangeRecord> call() throws Exception {
                long t0 = System.currentTimeMillis();
                updateMessage("扫描源文件...");
                List<File> initialFiles = new ArrayList<>();
                for (File r : sourceRoots) {
                    if (isCancelled()) break;
                    initialFiles.addAll(scanFilesRobust(r, depth, exts, this::updateMessage));
                }
                if (isCancelled()) return null;
                log("扫描完成，共 " + initialFiles.size() + " 个文件。");

                List<ChangeRecord> currentRecords = initialFiles.stream()
                        .map(f -> new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE))
                        .collect(Collectors.toList());

                for (int i = 0; i < pipelineStrategies.size(); i++) {
                    if (isCancelled()) break;
                    AppStrategy strategy = pipelineStrategies.get(i);
                    updateMessage("步骤 " + (i + 1) + ": " + strategy.getName());
                    List<ChangeRecord> stepResults = strategy.analyze(currentRecords, sourceRoots, (p, m) -> updateProgress(p, 1.0));
                    // Merge results by path
                    Map<String, ChangeRecord> resultMap = stepResults.stream()
                            .collect(Collectors.toMap(r -> r.getFileHandle().getAbsolutePath(), r -> r, (o, n) -> n));
                    for (ChangeRecord original : currentRecords) {
                        ChangeRecord update = resultMap.get(original.getFileHandle().getAbsolutePath());
                        if (update != null) {
                            original.setNewName(update.getNewName());
                            original.setNewPath(update.getNewPath());
                            if (update.isChanged()) {
                                original.setChanged(true);
                                original.setOpType(update.getOpType());
                                original.setExtraParams(update.getExtraParams());
                            }
                        }
                    }
                }
                updateMessage("构建视图...");
                return currentRecords;
            }
        };

        task.setOnSucceeded(e -> {
            fullChangeList = task.getValue();
            refreshPreviewTableFilter();
            long count = fullChangeList.stream().filter(ChangeRecord::isChanged).count();
            log("预览完成。变更数: " + count);
            finishTaskUI("预览完成");
            btnExecute.setDisable(count == 0);
        });

        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    void runPipelineExecution() {
        if (fullChangeList.isEmpty() || isTaskRunning) return;
        long count = fullChangeList.stream().filter(ChangeRecord::isChanged).count();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定执行 " + count + " 个变更吗？", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        resetProgressUI("正在执行...", true);
        if (chkSaveLog.isSelected()) initFileLogger();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<ChangeRecord> todos = fullChangeList.stream().filter(ChangeRecord::isChanged).collect(Collectors.toList());
                int total = todos.size();
                AtomicInteger curr = new AtomicInteger(0);
                AtomicInteger succ = new AtomicInteger(0);
                long startT = System.currentTimeMillis();

                executorService = Executors.newFixedThreadPool(4);
                for (ChangeRecord rec : todos) {
                    if (isCancelled()) break;
                    executorService.submit(() -> {
                        if (isCancelled()) return;
                        try {
                            Platform.runLater(() -> rec.setStatus(ExecStatus.RUNNING));
                            AppStrategy s = AppStrategyFactory.findStrategyForOp(rec.getOpType(), pipelineStrategies);
                            if (s != null) {
                                s.execute(rec);
                                Platform.runLater(() -> rec.setStatus(ExecStatus.SUCCESS));
                                succ.incrementAndGet();
                                logAndFile("成功: " + rec.getNewName());
                            } else {
                                Platform.runLater(() -> rec.setStatus(ExecStatus.SKIPPED));
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> rec.setStatus(ExecStatus.FAILED));
                            logAndFile("失败: " + e.getMessage());
                        } finally {
                            int c = curr.incrementAndGet();
                            updateProgress(c, total);
                            if (c % 10 == 0) Platform.runLater(() -> {
                                updateStats(System.currentTimeMillis() - startT);
                                previewTable.refresh();
                            });
                        }
                    });
                }
                executorService.shutdown();
                while (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    if (isCancelled()) {
                        executorService.shutdownNow();
                        break;
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            finishTaskUI("执行完成");
            closeFileLogger();
            btnExecute.setDisable(false);
        });
        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    // --- 辅助方法 ---

    void refreshPreviewTableFilter() {
        if (fullChangeList.isEmpty()) return;
        String search = txtSearchFilter.getText().toLowerCase();
        String status = cbStatusFilter.getValue();
        boolean hide = chkHideUnchanged.isSelected();

        Task<TreeItem<ChangeRecord>> t = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() {
                TreeItem<ChangeRecord> root = new TreeItem<>(new ChangeRecord());
                root.setExpanded(true);
                for (ChangeRecord r : fullChangeList) {
                    if (hide && !r.isChanged() && r.getStatus() != ExecStatus.FAILED) continue;
                    if (!search.isEmpty() && !r.getOriginalName().toLowerCase().contains(search)) continue;
                    if ("成功".equals(status) && r.getStatus() != ExecStatus.SUCCESS) continue;
                    if ("失败".equals(status) && r.getStatus() != ExecStatus.FAILED) continue;
                    if ("已跳过".equals(status) && r.getStatus() != ExecStatus.SKIPPED) continue;
                    if ("待处理".equals(status) && r.getStatus() != ExecStatus.PENDING) continue;
                    root.getChildren().add(new TreeItem<>(r));
                }
                return root;
            }
        };
        t.setOnSucceeded(e -> {
            previewTable.setRoot(t.getValue());
            updateStats(0);
        });
        new Thread(t).start();
    }

    private void updateStats(long ms) {
        long tot = fullChangeList.size();
        long chg = fullChangeList.stream().filter(ChangeRecord::isChanged).count();
        long suc = fullChangeList.stream().filter(r -> r.getStatus() == ExecStatus.SUCCESS).count();
        String timeStr = ms > 0 ? String.format("%.1fs", ms / 1000.0) : "-";
        Platform.runLater(() -> statsLabel.setText(String.format("总计: %d | 变更: %d | 成功: %d | 耗时: %s", tot, chg, suc, timeStr)));
    }

    private List<File> scanFilesRobust(File root, int maxDepth, List<String> exts, Consumer<String> msg) {
        AtomicInteger countScan = new AtomicInteger(0);
        AtomicInteger countIgnore = new AtomicInteger(0);
        List<File> list = new ArrayList<>();
        if (!root.exists()) return list;
        try (Stream<Path> s = Files.walk(root.toPath(), maxDepth)) {
            list = s.filter(p -> {
                try {
                    File f = p.toFile();
                    if (f.equals(root)) {
                        countIgnore.incrementAndGet();
                        return false;
                    } // 排除根目录本身

                    // [修复] 始终保留文件夹，无论递归深度如何。
                    // 之前的逻辑错误地排除了递归子目录，导致文件夹重命名/删除策略失效。
                    // 具体的策略（Strategy）会根据自己的 getTargetType() 再次过滤是否处理文件夹。
                    if (f.isDirectory()) return true;

                    // 文件则应用扩展名过滤
                    String n = f.getName().toLowerCase();
                    for (String e : exts) if (n.endsWith("." + e)) return true;
                    countIgnore.incrementAndGet();
                    return false;
                }finally {
                    countScan.incrementAndGet();
                    if (countScan.incrementAndGet() % 1000 == 0) {
                        String msgStr = "目录下：" + root.getAbsolutePath()
                                + "，已扫描" + countScan.get() + "个文件"
                                + "，已忽略" + countIgnore.get() + "个文件"
                                + "，已收纳" + (countScan.get() - countIgnore.get()) + "个文件";
                        msg.accept(msgStr);
                        log(msgStr);
                    }
                }
            }).filter(path -> {
                try {
                    path.toFile();
                } catch (Exception e) {
                    log(path + " 扫描异常: " + e.getMessage());
                    return false;
                }
                return true;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            log("扫描异常: " + e.getMessage());
        }
        String msgStr = "目录下(总共)：" + root.getAbsolutePath()
                + "，已扫描" + countScan.get() + "个文件"
                + "，已忽略" + countIgnore.get() + "个文件"
                + "，已收纳" + (countScan.get() - countIgnore.get()) + "个文件";
        msg.accept(msgStr);
        log(msgStr);
        // 反转列表，便于由下而上处理文件，保证处理成功
        Collections.reverse(list);
        return list;
    }

    void setupPreviewColumns() {
        TreeTableColumn<ChangeRecord, String> c1 = new TreeTableColumn<>("源文件");
        c1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getOriginalName()));
        c1.setPrefWidth(250);

        TreeTableColumn<ChangeRecord, String> cSize = new TreeTableColumn<>("大小");
        cSize.setPrefWidth(80);
        cSize.setCellValueFactory(p -> {
            File f = p.getValue().getValue().getFileHandle();
            return new SimpleStringProperty(f != null ? formatFileSize(f.length()) : "-");
        });

        TreeTableColumn<ChangeRecord, String> c2 = new TreeTableColumn<>("目标名称");
        c2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewName()));
        c2.setPrefWidth(250);
        c2.setCellFactory(c -> new TreeTableCell<ChangeRecord, String>() {
            @Override
            protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                setText(i);
                if (i != null && getTreeTableRow().getItem() != null && !i.equals(getTreeTableRow().getItem().getOriginalName()))
                    setTextFill(Color.web("#27ae60"));
                else setTextFill(Color.BLACK);
            }
        });

        TreeTableColumn<ChangeRecord, String> c3 = new TreeTableColumn<>("状态");
        c3.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getStatus().toString()));
        c3.setPrefWidth(80);

        TreeTableColumn<ChangeRecord, String> c4 = new TreeTableColumn<>("完整路径");
        c4.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewPath()));
        c4.setPrefWidth(350);

        previewTable.getColumns().setAll(c1, cSize, c2, c3, c4);
        previewTable.setRowFactory(tv -> {
            TreeTableRow<ChangeRecord> row = new TreeTableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem i1 = new MenuItem("打开文件");
            i1.setOnAction(e -> openFileInSystem(row.getItem().getFileHandle()));
            MenuItem i2 = new MenuItem("打开目录");
            i2.setOnAction(e -> openParentDirectory(row.getItem().getFileHandle()));
            cm.getItems().addAll(i1, i2);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(cm));
            return row;
        });
    }

    // --- Task & UI State Management ---
    private void resetProgressUI(String msg, boolean isExec) {
        isTaskRunning = true;
        currentTask = null;
        btnExecute.setDisable(true);
        btnStop.setDisable(false);
        if (!isExec) btnGoPreview.setDisable(true);
        progressLabel.textProperty().unbind();
        progressLabel.setText(msg);
        mainProgressBar.progressProperty().unbind();
        mainProgressBar.setProgress(-1);
        etaLabel.setText("");
    }

    private void finishTaskUI(String msg) {
        isTaskRunning = false;
        progressLabel.textProperty().unbind();
        progressLabel.setText(msg);
        mainProgressBar.progressProperty().unbind();
        mainProgressBar.setProgress(1.0);
        btnStop.setDisable(true);
        btnGoPreview.setDisable(false);
    }

    private void handleTaskLifecycle(Task<?> t) {
        currentTask = t;
        progressLabel.textProperty().bind(t.messageProperty());
        mainProgressBar.progressProperty().bind(t.progressProperty());
        t.setOnFailed(e -> {
            finishTaskUI("出错");
            log("❌ 失败: " + ExceptionUtils.getStackTrace(e.getSource().getException()));
            closeFileLogger();
        });
        t.setOnCancelled(e -> {
            finishTaskUI("已取消");
            closeFileLogger();
        });
    }

    void forceStop() {
        if (isTaskRunning) {
            isTaskRunning = false;
            if (currentTask != null) currentTask.cancel();
            if (executorService != null) executorService.shutdownNow();
            log("🛑 强制停止");
            finishTaskUI("已停止");
        }
    }

    // --- Config Persistence ---
    private void saveConfigAction() {
        FileChooser fc = new FileChooser();
        File f = fc.showSaveDialog(primaryStage);
        if (f != null) saveGlobalConfig(f);
    }

    private void loadConfigAction() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(primaryStage);
        if (f != null) loadGlobalConfig(f);
    }

    private void saveGlobalConfig(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            appProps.setProperty("g_recMode", String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
            appProps.setProperty("g_recDepth", String.valueOf(spRecursionDepth.getValue()));
            if (!sourceRoots.isEmpty())
                appProps.setProperty("g_sources", sourceRoots.stream().map(File::getAbsolutePath).collect(Collectors.joining("||")));
            propsSavePipeline(appProps);
            appProps.store(fos, "Config");
            log("配置保存成功");
        } catch (Exception e) {
            log("保存失败: " + e.getMessage());
        }
    }

    private void propsSavePipeline(Properties p) {
        p.setProperty("pipeline.size", String.valueOf(pipelineStrategies.size()));
        for (int i = 0; i < pipelineStrategies.size(); i++) {
            AppStrategy s = pipelineStrategies.get(i);
            Properties sp = new Properties();
            s.saveConfig(sp);
            String pre = "pipeline." + i + ".";
            p.setProperty(pre + "class", s.getClass().getName());
            for (String k : sp.stringPropertyNames()) p.setProperty(pre + "param." + k, sp.getProperty(k));
        }
    }

    private void loadGlobalConfig(File file) {
        if (!file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            appProps.load(fis);
            if (appProps.containsKey("g_recMode"))
                cbRecursionMode.getSelectionModel().select(Integer.parseInt(appProps.getProperty("g_recMode")));
            String paths = appProps.getProperty("g_sources");
            if (paths != null) {
                sourceRoots.clear();
                for (String pt : paths.split("\\|\\|")) {
                    File f = new File(pt);
                    if (f.exists()) sourceRoots.add(f);
                }
            }
            loadPipelineConfig(appProps);
            // Appearance
            if (appProps.containsKey("ui_color")) themeColor = appProps.getProperty("ui_color");
            if (appProps.containsKey("ui_bg")) bgImagePath = appProps.getProperty("ui_bg");
            applyAppearance();
        } catch (Exception e) {
            log("加载失败: " + e.getMessage());
        }
    }

    private void loadPipelineConfig(Properties p) {
        pipelineStrategies.clear();
        configContainer.getChildren().clear();
        int size = Integer.parseInt(p.getProperty("pipeline.size", "0"));
        for (int i = 0; i < size; i++) {
            String pre = "pipeline." + i + ".";
            String cls = p.getProperty(pre + "class");
            if (cls == null) continue;
            try {
                Class<?> clazz = Class.forName(cls);
                AppStrategy s = (AppStrategy) clazz.getDeclaredConstructor().newInstance();
                s.setContext(this);
                Properties sp = new Properties();
                String paramPre = pre + "param.";
                for (String k : p.stringPropertyNames())
                    if (k.startsWith(paramPre)) sp.setProperty(k.substring(paramPre.length()), p.getProperty(k));
                s.loadConfig(sp);
                pipelineStrategies.add(s);
            } catch (Exception e) {
                log("策略加载失败: " + e.getMessage());
            }
        }
        if (!pipelineStrategies.isEmpty()) pipelineListView.getSelectionModel().select(0);
    }

    // --- Utils ---
    private void startLogUpdater() {
        uiUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                String s;
                while ((s = logQueue.poll()) != null) logArea.appendText(s + "\n");
            }
        };
        uiUpdater.start();
    }

    public void log(String s) {
        logQueue.offer(s);
    }

    private void logAndFile(String s) {
        log(s);
        if (fileLogger != null) fileLogger.println(s);
    }

    private void initFileLogger() {
        try {
            fileLogger = new PrintWriter(new FileWriter("exec.log", true), true);
        } catch (Exception e) {
        }
    }

    private void closeFileLogger() {
        if (fileLogger != null) {
            fileLogger.close();
            fileLogger = null;
        }
    }

    private void addDirectoryAction() {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(primaryStage);
        if (f != null && !sourceRoots.contains(f)) {
            sourceRoots.add(f);
            invalidatePreview("源增加");
        }
    }

    private void openFileInSystem(File f) {
        try {
            if (f != null && f.exists()) Desktop.getDesktop().open(f);
        } catch (Exception e) {
        }
    }

    private void openParentDirectory(File f) {
        if (f != null) openFileInSystem(f.isDirectory() ? f : f.getParentFile());
    }

    private void showToast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.show();
    }

    private JFXButton createButton(String text, String icon, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        JFXButton b = new JFXButton(text);
        b.setOnAction(h);
        b.setStyle("-fx-background-color:#ecf0f1;");
        return b;
    }

    private VBox styledHeader(String t, String s) {
        VBox v = new VBox(2);
        Label l1 = new Label(t);
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label l2 = new Label(s);
        l2.setFont(Font.font(10));
        l2.setTextFill(Color.GRAY);
        v.getChildren().addAll(l1, l2);
        return v;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    // --- Conditions UI ---
    private Node createConditionsUI(AppStrategy strategy) {
        VBox box = new VBox(5);
        ListView<RuleCondition> lv = new ListView<>(FXCollections.observableArrayList(strategy.getGlobalConditions()));
        lv.setPrefHeight(100);
        HBox input = new HBox(5);
        ComboBox<ConditionType> cbType = new ComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
        cbType.getSelectionModel().select(0);
        TextField txtVal = new TextField();
        txtVal.setPromptText("Value");
        Button btnAdd = new Button("+");
        btnAdd.setOnAction(e -> {
            if (!txtVal.getText().isEmpty()) {
                strategy.getGlobalConditions().add(new RuleCondition(cbType.getValue(), txtVal.getText()));
                lv.getItems().setAll(strategy.getGlobalConditions());
                invalidatePreview("添加条件");
            }
        });
        Button btnDel = new Button("-");
        btnDel.setOnAction(e -> {
            RuleCondition s = lv.getSelectionModel().getSelectedItem();
            if (s != null) {
                strategy.getGlobalConditions().remove(s);
                lv.getItems().setAll(strategy.getGlobalConditions());
                invalidatePreview("移除条件");
            }
        });
        input.getChildren().addAll(cbType, txtVal, btnAdd, btnDel);
        box.getChildren().addAll(lv, input);
        return box;
    }

    // --- Appearance ---
    private void showAppearanceDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("界面设置");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        ColorPicker cp = new ColorPicker(Color.web(themeColor));
        cp.setOnAction(e -> {
            themeColor = toHexString(cp.getValue());
            applyAppearance();
        });
        Slider opSlider = new Slider(0, 1, bgOpacity);
        opSlider.valueProperty().addListener((o, old, v) -> {
            bgOpacity = v.doubleValue();
            applyAppearance();
        });
        TextField txtBgPath = new TextField(bgImagePath);
        JFXButton btnPickBg = new JFXButton("背景图...");
        btnPickBg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) {
                txtBgPath.setText(f.getAbsolutePath());
                bgImagePath = f.getAbsolutePath();
                applyAppearance();
            }
        });
        grid.add(new Label("主题色:"), 0, 0);
        grid.add(cp, 1, 0);
        grid.add(new Label("透明度:"), 0, 1);
        grid.add(opSlider, 1, 1);
        grid.add(new Label("背景图:"), 0, 2);
        grid.add(new HBox(5, txtBgPath, btnPickBg), 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.show();
    }

    private void applyAppearance() {
        backgroundOverlay.setStyle("-fx-background-color: rgba(255,255,255," + bgOpacity + ");");
        if (!bgImagePath.isEmpty()) {
            try {
                backgroundImageView.setImage(new Image(Files.newInputStream(Paths.get(bgImagePath))));
            } catch (Exception e) {
            }
        }
        appProps.setProperty("ui_color", themeColor);
        appProps.setProperty("ui_bg", bgImagePath);
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private static class Spacer extends Region {
        public Spacer() {
            HBox.setHgrow(this, Priority.ALWAYS);
        }
    }

    // 递归设置深色文本，防止第三方组件(如自定义策略UI)使用默认颜色
    private void forceDarkText(Node node) {
        if (node instanceof Label) ((Label) node).setTextFill(Color.web("#333"));
        if (node instanceof CheckBox) ((CheckBox) node).setTextFill(Color.web("#333"));
        if (node instanceof RadioButton) ((RadioButton) node).setTextFill(Color.web("#333"));
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) forceDarkText(child);
        }
    }
}