package com.filemanager.front;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.RuleCondition;
import com.filemanager.strategy.*;
import com.filemanager.type.ConditionType;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.CheckComboBox;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MusicFileManagerAppV14_Stable extends Application implements FileManagerAppInterface {

    private Stage primaryStage;
    private final Properties appProps = new Properties();
    private final File lastConfigFile = new File(System.getProperty("user.home"), ".echo_music_manager_v14.config");

    // --- 核心数据模型 ---
    private final ObservableList<File> sourceRoots = FXCollections.observableArrayList();
    private final ObservableList<AppStrategy> pipelineStrategies = FXCollections.observableArrayList();
    // 全量变更记录
    private List<ChangeRecord> fullChangeList = new ArrayList<>();

    // --- UI 组件 ---
    private TabPane mainTabPane;
    private Tab tabCompose, tabPreview, tabLog;

    // Tab 1: 编排
    private ListView<File> sourceListView;
    private ListView<AppStrategy> pipelineListView;
    private VBox configContainer;
    private JFXComboBox<AppStrategy> cbStrategyTemplates;
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;

    // Tab 2: 预览与筛选
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label progressLabel;
    private Label etaLabel;
    private Label statsLabel;
    private TextField txtSearchFilter;
    private ComboBox<String> cbStatusFilter;
    private JFXButton btnExecute, btnStop, btnGoPreview;
    private CheckBox chkHideUnchanged;
    private VBox progressBox; // 确保定义

    // Tab 3: 日志
    private TextArea logArea;
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private CheckBox chkSaveLog;
    private PrintWriter fileLogger;
    private AnimationTimer uiUpdater;

    // 任务管理
    private ExecutorService executorService;
    private Task<?> currentTask;
    private volatile boolean isTaskRunning = false;
    private final int executionThreadCount = 1;

    // 策略原型
    private final List<AppStrategy> strategyPrototypes = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 v14.2");

        initStrategyPrototypes();

        Scene scene = new Scene(createMainLayout(), 1400, 950);
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
        // 延迟加载配置以确保UI组件已就绪
        Platform.runLater(() -> loadGlobalConfig(lastConfigFile));

        primaryStage.show();
    }

    // ==================== 1. UI 构建 ====================

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        // 菜单
        MenuBar menuBar = new MenuBar();
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
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        // 标签页
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        createComposeTab();
        createPreviewTab();
        createLogTab();
        mainTabPane.getTabs().addAll(tabCompose, tabPreview, tabLog);
        root.setCenter(mainTabPane);

        // 状态栏
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
        statusBar.getChildren().add(new Label("Ready."));
        root.setBottom(statusBar);
        return root;
    }

    private void createComposeTab() {
        tabCompose = new Tab("1. 任务编排");
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(10));

        // Left: 源文件
        VBox leftBox = new VBox(10);
        leftBox.setPadding(new Insets(0, 10, 0, 0));
        leftBox.setPrefWidth(320);
        Label lblSource = new Label("源目录");
        lblSource.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        sourceListView = new ListView<>(sourceRoots);
        sourceListView.setPlaceholder(new Label("拖拽文件夹到此处"));
        sourceListView.setCellFactory(p -> new ListCell<File>() {
            @Override
            protected void updateItem(File i, boolean e) {
                super.updateItem(i, e);
                if (e || i == null) setText(null);
                else {
                    setText(i.getAbsolutePath());
                    setTooltip(new Tooltip(i.getAbsolutePath()));
                }
            }
        });
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

        HBox srcBtns = new HBox(5);
        JFXButton btnAddSrc = new JFXButton("添加");
        btnAddSrc.setOnAction(e -> addDirectoryAction());
        JFXButton btnRemSrc = new JFXButton("移除");
        btnRemSrc.setOnAction(e -> {
            if (sourceListView.getSelectionModel().getSelectedItem() != null) {
                sourceRoots.remove(sourceListView.getSelectionModel().getSelectedItem());
                invalidatePreview("源变更");
            }
        });
        styleBtn(btnAddSrc, "#3498db");
        styleBtn(btnRemSrc, "#e74c3c");
        srcBtns.getChildren().addAll(btnAddSrc, btnRemSrc);

        VBox filtersBox = new VBox(8);
        filtersBox.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 4;");
        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("仅当前目录", "递归所有子目录", "指定目录深度"));
        cbRecursionMode.getSelectionModel().select(1);
        cbRecursionMode.getSelectionModel().selectedItemProperty().addListener((o, old, v) -> invalidatePreview("递归变更"));
        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.setPrefWidth(80);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录深度"));
        spRecursionDepth.valueProperty().addListener((o, old, v) -> invalidatePreview("递归变更"));
        ccbFileTypes = new CheckComboBox<>(FXCollections.observableArrayList("mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "iso", "jpg", "png", "nfo", "cue", "tak", "tta", "wv", "wma", "aac", "ogg", "opus"));
        ccbFileTypes.getCheckModel().checkAll();
        ccbFileTypes.setMaxWidth(Double.MAX_VALUE);
        ccbFileTypes.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> invalidatePreview("类型变更"));
        filtersBox.getChildren().addAll(new Label("递归:"), new HBox(5, cbRecursionMode, spRecursionDepth), new Label("类型:"), ccbFileTypes);
        leftBox.getChildren().addAll(lblSource, sourceListView, srcBtns, new Separator(), new Label("全局筛选:"), filtersBox);

        // Center: 策略流水线
        SplitPane centerSplit = new SplitPane();
        VBox pipeBox = new VBox(10);
        pipeBox.setPadding(new Insets(0, 10, 0, 10));
        Label lblPipe = new Label("任务流水线");
        lblPipe.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        pipelineListView = new ListView<>(pipelineStrategies);
        pipelineListView.setCellFactory(p -> new ListCell<AppStrategy>() {
            @Override
            protected void updateItem(AppStrategy i, boolean e) {
                super.updateItem(i, e);
                if (e || i == null) setText(null);
                else setText((getIndex() + 1) + ". " + i.getName());
            }
        });
        pipelineListView.getSelectionModel().selectedItemProperty().addListener((o, old, newVal) -> refreshConfigPanel(newVal));

        HBox pipeTools = new HBox(5);
        cbStrategyTemplates = new JFXComboBox<>(FXCollections.observableArrayList(strategyPrototypes));
        cbStrategyTemplates.setPromptText("选择功能...");
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
        JFXButton btnAddStep = new JFXButton("添加");
        styleBtn(btnAddStep, "#2ecc71");
        btnAddStep.setOnAction(e -> addStrategyStep(null));
        JFXButton btnRemStep = new JFXButton("删除");
        styleBtn(btnRemStep, "#e74c3c");
        btnRemStep.setOnAction(e -> {
            if (pipelineListView.getSelectionModel().getSelectedItem() != null) {
                pipelineStrategies.remove(pipelineListView.getSelectionModel().getSelectedItem());
                configContainer.getChildren().clear();
                invalidatePreview("步骤移除");
            }
        });
        pipeTools.getChildren().addAll(cbStrategyTemplates, btnAddStep, btnRemStep);
        pipeBox.getChildren().addAll(lblPipe, pipelineListView, pipeTools);

        // Right: 参数配置
        VBox configBox = new VBox(10);
        configBox.setPadding(new Insets(0, 0, 0, 10));
        Label lblConfig = new Label("参数配置");
        lblConfig.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        configContainer = new VBox(10);
        configContainer.setStyle("-fx-background-color: white; -fx-padding: 15;");
        ScrollPane scrollConfig = new ScrollPane(configContainer);
        scrollConfig.setFitToWidth(true);
        scrollConfig.setStyle("-fx-background-color: transparent; -fx-border-color: #ddd;");
        configBox.getChildren().addAll(lblConfig, scrollConfig);
        VBox.setVgrow(scrollConfig, Priority.ALWAYS);

        centerSplit.getItems().addAll(pipeBox, configBox);
        centerSplit.setDividerPositions(0.4);

        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));
        btnGoPreview = new JFXButton("下一步：生成预览 >");
        styleBtn(btnGoPreview, "#f39c12");
        btnGoPreview.setOnAction(e -> runPipelineAnalysis());
        bottomBox.getChildren().add(btnGoPreview);

        content.setLeft(leftBox);
        content.setCenter(centerSplit);
        content.setBottom(bottomBox);
        tabCompose.setContent(content);
    }

    private void createPreviewTab() {
        tabPreview = new Tab("2. 预览与执行");
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        HBox actionBar = new HBox(15);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        btnExecute = new JFXButton("执行变更");
        styleBtn(btnExecute, "#27ae60");
        btnExecute.setDisable(true);
        btnExecute.setOnAction(e -> runPipelineExecution());
        btnStop = new JFXButton("停止");
        styleBtn(btnStop, "#e74c3c");
        btnStop.setDisable(true);
        btnStop.setOnAction(e -> forceStop());

        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        txtSearchFilter = new TextField();
        txtSearchFilter.setPromptText("搜索名称...");
        txtSearchFilter.textProperty().addListener((o, old, v) -> refreshPreviewTableFilter());
        cbStatusFilter = new ComboBox<>(FXCollections.observableArrayList("全部", "已变更", "未变更", "失败", "成功"));
        cbStatusFilter.getSelectionModel().select(0);
        cbStatusFilter.valueProperty().addListener((o, old, v) -> refreshPreviewTableFilter());
        chkHideUnchanged = new CheckBox("仅显示变更");
        chkHideUnchanged.setSelected(true);
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> refreshPreviewTableFilter());
        filterBox.getChildren().addAll(new Label("筛选:"), txtSearchFilter, cbStatusFilter, chkHideUnchanged);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statsBar = new HBox(15);
        statsBar.setAlignment(Pos.CENTER_LEFT);
        progressLabel = new Label("等待");
        etaLabel = new Label("");
        statsLabel = new Label("0/0");
        mainProgressBar = new ProgressBar(0);
        mainProgressBar.setPrefWidth(150);

        // 确保 progressBox 被初始化
        progressBox = new VBox();
        // 这里我们将进度条组件放入 statsBar 布局
        statsBar.getChildren().addAll(new Label("进度:"), mainProgressBar, progressLabel, etaLabel, new Separator(javafx.geometry.Orientation.VERTICAL), statsLabel);

        actionBar.getChildren().addAll(btnExecute, btnStop, spacer, filterBox);

        previewTable = new TreeTableView<>();
        previewTable.setShowRoot(false);
        previewTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        setupPreviewTable();
        root.getChildren().addAll(actionBar, statsBar, previewTable);
        tabPreview.setContent(root);
    }

    private void createLogTab() {
        tabLog = new Tab("3. 运行日志");
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        chkSaveLog = new CheckBox("输出日志到文件");
        JFXButton btnClear = new JFXButton("清空");
        btnClear.setOnAction(e -> logArea.clear());
        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);
        controls.getChildren().addAll(chkSaveLog, s, btnClear);
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFont(Font.font("Consolas", 12));
        VBox.setVgrow(logArea, Priority.ALWAYS);
        root.getChildren().addAll(controls, logArea);
        tabLog.setContent(root);
    }

    // ==================== 2. 策略逻辑 ====================

    private void initStrategyPrototypes() {
        strategyPrototypes.add(new AdvancedRenameStrategy());
        strategyPrototypes.add(new AudioConverterStrategy());
        strategyPrototypes.add(new FileMigrateStrategy());
        strategyPrototypes.add(new AlbumDirNormalizeStrategy());
        strategyPrototypes.add(new TrackNumberStrategy());
        strategyPrototypes.add(new CueSplitterStrategy());
        strategyPrototypes.add(new MetadataScraperStrategy());
        strategyPrototypes.add(new FileCleanupStrategy());
    }

    private void addStrategyStep(Properties config) {
        AppStrategy template = cbStrategyTemplates.getValue();
        if (template != null) {
            try {
                AppStrategy newStep = template.getClass().getDeclaredConstructor().newInstance();
                newStep.setContext(this);
                if (config != null) {
                    // 未来扩展：支持传入具体配置
                }
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

        Node cfgNode = strategy.getConfigNode();
        if (cfgNode == null) cfgNode = new Label("此功能无需配置");

        TitledPane tpCond = new TitledPane("前置过滤条件", createConditionsUI(strategy));
        tpCond.setExpanded(false);

        configContainer.getChildren().addAll(title, new Separator(), tpCond, new Label("参数配置:"), cfgNode);
    }

    private Node createConditionsUI(AppStrategy strategy) {
        VBox box = new VBox(5);
        ListView<RuleCondition> lv = new ListView<>(FXCollections.observableArrayList(strategy.getGlobalConditions()));
        lv.setPrefHeight(100);
        HBox input = new HBox(5);
        ComboBox<ConditionType> cbType = new ComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
        cbType.getSelectionModel().select(0);
        TextField txtVal = new TextField();
        txtVal.setPromptText("值");
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

    // ==================== 3. 配置持久化 ====================

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
            Properties props = new Properties();
            props.setProperty("g_recMode", String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
            props.setProperty("g_recDepth", String.valueOf(spRecursionDepth.getValue()));
            if (!sourceRoots.isEmpty()) {
                String paths = sourceRoots.stream().map(File::getAbsolutePath).collect(Collectors.joining("||"));
                props.setProperty("g_sources", paths);
            }
            props.setProperty("pipeline.size", String.valueOf(pipelineStrategies.size()));
            for (int i = 0; i < pipelineStrategies.size(); i++) {
                AppStrategy s = pipelineStrategies.get(i);
                Properties strategyProps = new Properties();
                s.saveConfig(strategyProps);
                String prefix = "pipeline." + i + ".";
                props.setProperty(prefix + "class", s.getClass().getName());
                for (String key : strategyProps.stringPropertyNames()) {
                    props.setProperty(prefix + "param." + key, strategyProps.getProperty(key));
                }
            }
            props.store(fos, "Config");
            log("配置已保存至: " + file.getName());
        } catch (Exception e) {
            log("保存失败: " + e.getMessage());
        }
    }

    private void loadGlobalConfig(File file) {
        if (!file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            if (props.containsKey("g_recMode"))
                cbRecursionMode.getSelectionModel().select(Integer.parseInt(props.getProperty("g_recMode")));
            if (props.containsKey("g_recDepth"))
                spRecursionDepth.getValueFactory().setValue(Integer.parseInt(props.getProperty("g_recDepth")));
            String paths = props.getProperty("g_sources");
            if (paths != null && !paths.isEmpty()) {
                sourceRoots.clear();
                for (String p : paths.split("\\|\\|")) {
                    File f = new File(p);
                    if (f.exists()) sourceRoots.add(f);
                }
            }
            pipelineStrategies.clear();
            configContainer.getChildren().clear();
            int size = Integer.parseInt(props.getProperty("pipeline.size", "0"));
            for (int i = 0; i < size; i++) {
                String prefix = "pipeline." + i + ".";
                String className = props.getProperty(prefix + "class");
                if (className == null) continue;
                try {
                    Class<?> clazz = Class.forName(className);
                    AppStrategy strategy = (AppStrategy) clazz.getDeclaredConstructor().newInstance();
                    strategy.setContext(this);
                    Properties strategyProps = new Properties();
                    String paramPrefix = prefix + "param.";
                    for (String key : props.stringPropertyNames()) {
                        if (key.startsWith(paramPrefix)) {
                            strategyProps.setProperty(key.substring(paramPrefix.length()), props.getProperty(key));
                        }
                    }
                    strategy.loadConfig(strategyProps);
                    pipelineStrategies.add(strategy);
                } catch (Exception ex) {
                    log("加载策略失败: " + ex.getMessage());
                }
            }
            if (!pipelineStrategies.isEmpty()) pipelineListView.getSelectionModel().select(0);
            log("配置已加载: " + file.getName());
        } catch (Exception e) {
            log("加载失败: " + e.getMessage());
        }
    }

    // ==================== 4. 核心流水线执行 ====================

    private void runPipelineAnalysis() {
        if (sourceRoots.isEmpty()) {
            log("请先添加源目录！");
            return;
        }
        if (pipelineStrategies.isEmpty()) {
            log("请添加操作步骤！");
            return;
        }
        if (isTaskRunning) return;

        mainTabPane.getSelectionModel().select(tabPreview);
        resetProgressUI("正在扫描...", false);

        for (AppStrategy s : pipelineStrategies) s.captureParams();
        List<String> exts = new ArrayList<>(ccbFileTypes.getCheckModel().getCheckedItems());
        int depth = "仅当前目录".equals(cbRecursionMode.getValue()) ? 1 : ("递归所有子目录".equals(cbRecursionMode.getValue()) ? Integer.MAX_VALUE : spRecursionDepth.getValue());

        Task<List<ChangeRecord>> task = new Task<List<ChangeRecord>>() {
            @Override
            protected List<ChangeRecord> call() throws Exception {
                List<File> files = new ArrayList<>();
                for (File r : sourceRoots) {
                    if (isCancelled()) break;
                    files.addAll(scanFilesRobust(r, depth, exts, this::updateMessage));
                }
                if (isCancelled()) return null;

                List<ChangeRecord> records = files.stream().map(f -> new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE)).collect(Collectors.toList());

                for (int i = 0; i < pipelineStrategies.size(); i++) {
                    if (isCancelled()) break;
                    AppStrategy s = pipelineStrategies.get(i);
                    updateMessage("步骤 " + (i + 1) + ": " + s.getName());
                    records = s.analyze(records, sourceRoots, (p, m) -> updateProgress(p, 1.0));
                }
                return records;
            }
        };

        setupTaskHandlers(task, "预览完成");
        new Thread(task).start();
    }

    private void runPipelineExecution() {
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

                for (ChangeRecord r : todos) {
                    if (isCancelled()) break;
                    executorService.submit(() -> {
                        if (isCancelled()) return;
                        try {
                            Platform.runLater(() -> r.setStatus(ExecStatus.RUNNING));
                            AppStrategy s = findStrategyForOp(r.getOpType());
                            if (s != null) {
                                s.execute(r);
                                Platform.runLater(() -> r.setStatus(ExecStatus.SUCCESS));
                                succ.incrementAndGet();
                                logAndFile("成功: " + r.getNewName());
                            } else {
                                Platform.runLater(() -> r.setStatus(ExecStatus.SKIPPED));
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> r.setStatus(ExecStatus.FAILED));
                            logAndFile("失败: " + e.getMessage());
                        } finally {
                            int c = curr.incrementAndGet();
                            updateProgress(c, total);
                            if (c % 10 == 0) Platform.runLater(() -> updateStats(System.currentTimeMillis() - startT));
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

                Platform.runLater(() -> {
                    logImmediate(String.format("执行结束. 成功: %d", succ.get()));
                });
                return null;
            }
        };
        setupTaskHandlers(task, "执行完成");
        new Thread(task).start();
    }

    // --- 辅助方法与任务控制 ---

    private AppStrategy findStrategyForOp(OperationType op) {
        for (int i = pipelineStrategies.size() - 1; i >= 0; i--) {
            AppStrategy s = pipelineStrategies.get(i);
            if (op == OperationType.RENAME && (s instanceof AdvancedRenameStrategy || s instanceof TrackNumberStrategy || s instanceof AlbumDirNormalizeStrategy))
                return s;
            if (op == OperationType.CONVERT && s instanceof AudioConverterStrategy) return s;
            if (op == OperationType.MOVE && s instanceof FileMigrateStrategy) return s;
            if (op == OperationType.SPLIT && s instanceof CueSplitterStrategy) return s;
        }
        return null;
    }

    private void refreshPreviewTableFilter() {
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
        long fail = fullChangeList.stream().filter(r -> r.getStatus() == ExecStatus.FAILED).count();
        statsLabel.setText(String.format("总计:%d | 变更:%d | 成功:%d | 失败:%d", tot, chg, suc, fail));
    }

    private void setupPreviewTable() {
        TreeTableColumn<ChangeRecord, String> c1 = new TreeTableColumn<>("源文件");
        c1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getOriginalName()));
        c1.setPrefWidth(250);
        TreeTableColumn<ChangeRecord, String> c2 = new TreeTableColumn<>("目标");
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
        c3.setCellFactory(c -> new TreeTableCell<ChangeRecord, String>() {
            @Override
            protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                setText(i);
                if ("SUCCESS".equals(i)) setTextFill(Color.GREEN);
                else if ("FAILED".equals(i)) setTextFill(Color.RED);
                else setTextFill(Color.BLACK);
            }
        });
        TreeTableColumn<ChangeRecord, String> c4 = new TreeTableColumn<>("路径");
        c4.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewPath()));
        c4.setPrefWidth(350);
        previewTable.getColumns().setAll(c1, c2, c3, c4);
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

    private List<File> scanFilesRobust(File root, int depth, List<String> exts, Consumer<String> msg) {
        List<File> list = new ArrayList<>();
        if (!root.exists()) return list;
        try (Stream<Path> s = Files.walk(root.toPath(), depth)) {
            list = s.filter(p -> {
                File f = p.toFile();
                if (f.equals(root) || (f.isDirectory() && depth > 1)) return false;
                if (f.isDirectory()) return true;
                String n = f.getName().toLowerCase();
                for (String e : exts) if (n.endsWith("." + e)) return true;
                return false;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            log("扫描异常: " + e.getMessage());
        }
        return list;
    }

    private void resetProgressUI(String msg, boolean isExec) {
        isTaskRunning = true;
        currentTask = null;
        btnExecute.setDisable(true);
        btnStop.setDisable(false);
        if (!isExec) btnGoPreview.setDisable(true);

        // 关键修复：先解绑，再赋值，防止 Exception
        progressLabel.textProperty().unbind();
        mainProgressBar.progressProperty().unbind();

        progressLabel.setText(msg);
        mainProgressBar.setProgress(-1);
        etaLabel.setText("");
    }

    private void finishTaskUI(String msg) {
        isTaskRunning = false;
        progressLabel.textProperty().unbind();
        mainProgressBar.progressProperty().unbind();

        progressLabel.setText(msg);
        mainProgressBar.setProgress(1.0);
        btnStop.setDisable(true);
        btnGoPreview.setDisable(false);
    }

    private void setupTaskHandlers(Task<?> task, String successMsg) {
        currentTask = task;
        task.setOnSucceeded(e -> {
            // 成功回调中会进行具体的逻辑判断（如是预览还是执行），再调用 finishTaskUI
            if (task.getValue() instanceof List) { // 预览任务
                fullChangeList = (List<ChangeRecord>) task.getValue();
                refreshPreviewTableFilter();
                long count = fullChangeList.stream().filter(ChangeRecord::isChanged).count();
                log("预览完成。变更数: " + count);
                finishTaskUI(successMsg);
                btnExecute.setDisable(count == 0);
            } else { // 执行任务
                finishTaskUI(successMsg);
                closeFileLogger();
                btnExecute.setDisable(false);
            }
        });
        task.setOnFailed(e -> {
            finishTaskUI("出错");
            log("❌ 失败: " + e.getSource().getException().getMessage());
            closeFileLogger();
        });
        task.setOnCancelled(e -> {
            finishTaskUI("已取消");
            closeFileLogger();
        });

        progressLabel.textProperty().bind(task.messageProperty());
        mainProgressBar.progressProperty().bind(task.progressProperty());
    }

    private void forceStop() {
        if (isTaskRunning) {
            isTaskRunning = false;
            if (currentTask != null) currentTask.cancel();
            if (executorService != null) executorService.shutdownNow();
            log("已强制停止");
            finishTaskUI("已停止");
        }
    }

    // --- Utils ---
    private void startLogUpdater() {
        uiUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                StringBuilder sb = new StringBuilder();
                String s;
                while ((s = logQueue.poll()) != null) sb.append(s).append("\n");
                if (sb.length() > 0) logArea.appendText(sb.toString());
            }
        };
        uiUpdater.start();
    }

    public void log(String s) {
        logQueue.offer(s);
    }

    private void logImmediate(String s) {
        Platform.runLater(() -> log(s));
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

    private void styleBtn(Button b, String c) {
        b.setStyle("-fx-background-color: " + c + "; -fx-text-fill: white; -fx-font-weight: bold;");
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

    public void invalidatePreview(String r) {
        if (!fullChangeList.isEmpty()) {
            fullChangeList.clear();
            previewTable.setRoot(null);
            log(r + ", 需重新预览");
            updateStats(0);
        }
        btnExecute.setDisable(true);
    }
}