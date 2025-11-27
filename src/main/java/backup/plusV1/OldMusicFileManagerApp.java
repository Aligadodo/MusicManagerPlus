package backup.plusV1;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import org.controlsfx.control.CheckComboBox;
import backup.plusV1.model.ChangeRecord;
import backup.plusV1.plugins.*;
import backup.plusV1.type.ExecStatus;
import backup.plusV1.type.OperationType;
import backup.plusV1.type.ScanTarget;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class OldMusicFileManagerApp extends Application {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 格式化时间
    private final List<OldAppStrategy> strategies = new ArrayList<>();
    // Data Models
    private final ObservableList<ChangeRecord> changePreviewList = FXCollections.observableArrayList();
    private final ObservableList<String> sourcePathStrings = FXCollections.observableArrayList();
    private final List<File> sourceRootDirs = new ArrayList<>();
    // 性能优化：日志缓冲区
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    // Local Conf
    // 使用 Properties 和本地文件
    private final Properties appProps = new Properties();
    private final File configFile = new File(System.getProperty("user.home"), ".echo_music_manager.config");
    private Stage primaryStage;
    // UI Controls
    private ListView<String> lvSourcePaths;
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;
    private ListView<String> logView;
    private final ObservableList<String> logItems = FXCollections.observableArrayList();
    private AnimationTimer uiUpdater; // 用于定时刷新UI


    // 进度显示增强
    private VBox progressBox;
    private ProgressBar mainProgressBar;
    private Label progressLabel;
    private Label etaLabel; // 新增：预计剩余时间显示

    // 新增/提升 UI 控件为类成员以便控制状态
    private JFXButton btnPreview;
    private JFXButton btnExecute;
    private JFXButton btnStop; // 新增停止按钮
    private ExecutorService currentExecutor; // 当前运行的线程池，用于终止
    private volatile boolean isTaskRunning = false; // 任务状态标记
    // [变更] 新增复选框控件
    private CheckBox chkHideUnchanged;

    // 内存信息
    private TreeView<File> dirTree;
    private TreeView<ChangeRecord> previewTree;
    private JFXComboBox<OldAppStrategy> cbStrategy;
    private VBox strategyConfigContainer;
    // 线程池配置
    private int executionThreadCount = 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 v7.0 (高性能稳定版)");

        initStrategies();
        Scene scene = new Scene(createMainLayout(), 1400, 950);
        scene.getRoot().setStyle("-fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; -fx-font-size: 14px;");

        primaryStage.setScene(scene);
        loadPreferences();
        primaryStage.setOnCloseRequest(e -> {
            savePreferences();
            stopExecution(); // 退出时确保杀掉进程
            Platform.exit();
            System.exit(0);
        });

        // 启动 UI 定时刷新器 (节流机制)
        startUiUpdater();

        primaryStage.show();
    }

    // --- UI 节流更新机制 ---
    private void startUiUpdater() {
        uiUpdater = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                // 每 100ms 刷新一次日志，避免高频操作卡死 UI
                if (now - lastUpdate >= 100_000_000) {
                    List<String> newLogs = new ArrayList<>();
                    String msg;
                    while ((msg = logQueue.poll()) != null) {
                        newLogs.add(msg);
                    }
                    if (!newLogs.isEmpty()) {
                        logItems.addAll(newLogs);
                        // 限制日志条数，防止内存溢出
                        if (logItems.size() > 1000) {
                            logItems.remove(0, logItems.size() - 1000);
                        }
                        logView.scrollTo(logItems.size() - 1);
                    }
                    lastUpdate = now;
                }
            }
        };
        uiUpdater.start();
    }

    // 将日志放入队列，而非直接操作 UI
    private void log(String msg) {
        System.out.println(sdf.format(new Date()) + " --- " + msg);
        logQueue.offer(sdf.format(new Date()) + " --- " + msg);
    }

    // 立即刷新的 Log (用于状态变化等低频重要信息)
    private void logImmediate(String msg) {
        System.out.println(sdf.format(new Date()) + " --- " + msg);
        Platform.runLater(() -> {
            logItems.add(sdf.format(new Date()) + " --- " + msg);
            logView.scrollTo(logItems.size() - 1);
        });
    }

    // 新增：重置预览状态的方法
    private void invalidatePreview() {
        if (!changePreviewList.isEmpty()) {
            changePreviewList.clear();
            previewTree.setRoot(null);
            log("配置已变更，请重新点击 [生成预览]");
        }
        if (btnExecute != null) btnExecute.setDisable(true);
    }

    // 修改：基于文件的配置保存逻辑
    private void savePreferences() {
        try {
            // 保存当前策略索引
            if (cbStrategy.getSelectionModel().getSelectedIndex() >= 0) {
                appProps.setProperty("lastStrategyIdx", String.valueOf(cbStrategy.getSelectionModel().getSelectedIndex()));
            }
            // 保存递归模式
            appProps.setProperty("recursionModeIdx", String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
            appProps.setProperty("recursionDepth", String.valueOf(spRecursionDepth.getValue()));

            // 保存特定的策略配置
            for (OldAppStrategy s : strategies) {
                if (s instanceof AudioConverterStrategy) {
                    ((AudioConverterStrategy) s).savePrefs(appProps);
                }
            }

            // 保存路径列表
            if (!sourcePathStrings.isEmpty()) {
                appProps.setProperty("lastSourcePath", sourcePathStrings.get(0));
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                appProps.store(fos, "Echo Music File Manager Configuration");
            }
        } catch (Exception e) {
            System.err.println("无法保存配置: " + e.getMessage());
        }
    }

    // 修改：基于文件的配置加载逻辑
    private void loadPreferences() {
        if (!configFile.exists()) return;

        try (FileInputStream fis = new FileInputStream(configFile)) {
            appProps.load(fis);

            int strategyIdx = Integer.parseInt(appProps.getProperty("lastStrategyIdx", "0"));
            if (strategyIdx < strategies.size()) {
                cbStrategy.getSelectionModel().select(strategyIdx);
            }

            int recIdx = Integer.parseInt(appProps.getProperty("recursionModeIdx", "1"));
            cbRecursionMode.getSelectionModel().select(recIdx);

            int depth = Integer.parseInt(appProps.getProperty("recursionDepth", "2"));
            spRecursionDepth.getValueFactory().setValue(depth);

            // 恢复策略配置
            for (OldAppStrategy s : strategies) {
                if (s instanceof AudioConverterStrategy) {
                    ((AudioConverterStrategy) s).loadPrefs(appProps);
                }
            }

            // 恢复上次路径
            String lastPath = appProps.getProperty("lastSourcePath");
            if (lastPath != null) {
                File f = new File(lastPath);
                if (f.exists()) {
                    sourceRootDirs.add(f);
                    sourcePathStrings.add(lastPath);
                    refreshLeftTree();
                }
            }
        } catch (Exception e) {
            System.err.println("无法加载配置: " + e.getMessage());
        }
    }

    private void initStrategies() {
        strategies.add(new AdvancedRenameStrategy());
        strategies.add(new AudioConverterStrategy());
        strategies.add(new CueSplitterStrategy());
        strategies.add(new AlbumDirNormalizeStrategy());
        strategies.add(new TrackNumberStrategy());
        strategies.add(new FileMigrateStrategy());
    }

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // Top
        VBox topContainer = new VBox(10);
        topContainer.setPadding(new Insets(15));
        topContainer.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        Label title = new Label("Echo 文件管理工作台");
        title.setFont(Font.font("Segoe UI", 22));
        title.setTextFill(Color.web("#2c3e50"));
        topContainer.getChildren().addAll(title, createGlobalConfigPanel());
        root.setTop(topContainer);

        // Center
        SplitPane centerSplit = new SplitPane();
        VBox leftPanel = createLeftPanel();
        VBox actionPanel = createActionPanel();
        VBox previewPanel = createPreviewPanel();

        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(leftPanel, actionPanel, previewPanel);
        mainSplit.setDividerPositions(0.2, 0.55);
        root.setCenter(mainSplit);

        // Bottom
        VBox bottomBox = new VBox(5);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        progressBox = new VBox(5);
        progressLabel = new Label("准备就绪");
        etaLabel = new Label("");
        etaLabel.setTextFill(Color.GRAY);
        etaLabel.setFont(Font.font(12));

        HBox progressInfo = new HBox(20, new Label("总进度:"), progressLabel, new Region(), etaLabel);
        HBox.setHgrow(progressInfo.getChildren().get(2), Priority.ALWAYS);

        mainProgressBar = new ProgressBar(0);
        mainProgressBar.setPrefWidth(Double.MAX_VALUE);
        mainProgressBar.setPrefHeight(15);
        mainProgressBar.setStyle("-fx-accent: #2ecc71;");

        progressBox.getChildren().addAll(progressInfo, mainProgressBar);
        progressBox.setVisible(false);

        // 性能优化：使用 ListView 替代 TextArea
        logView = new ListView<>(logItems);
        logView.setPrefHeight(150);
        logView.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");

        bottomBox.getChildren().addAll(progressBox, logView);
        root.setBottom(bottomBox);

        return root;
    }
    // --- UI Helpers ---

    private GridPane createGlobalConfigPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_LEFT);

        Label lblSources = new Label("工作目录:");
        GridPane.setValignment(lblSources, javafx.geometry.VPos.TOP);

        lvSourcePaths = new ListView<>(sourcePathStrings);
        lvSourcePaths.setPrefHeight(80);
        lvSourcePaths.setPrefWidth(500);
        lvSourcePaths.setPlaceholder(new Label("请添加目录..."));

        VBox btnBox = new VBox(5);
        JFXButton btnAdd = new JFXButton("添加目录");
        btnAdd.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnAdd.setOnAction(e -> addDirectory());
        JFXButton btnRemove = new JFXButton("移除选中");
        btnRemove.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnRemove.setOnAction(e -> removeDirectory());
        btnBox.getChildren().addAll(btnAdd, btnRemove);

        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("仅当前目录", "递归所有子目录", "指定目录深度"));
        cbRecursionMode.getSelectionModel().select(1);
        cbRecursionMode.getSelectionModel().selectedItemProperty().addListener((o, old, v) -> invalidatePreview());

        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录深度"));
        spRecursionDepth.valueProperty().addListener((o, old, v) -> invalidatePreview());

        ObservableList<String> extensions = FXCollections.observableArrayList(
                "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "dfd", "cue", "iso"
        );
        ccbFileTypes = new CheckComboBox<>(extensions);
        ccbFileTypes.getCheckModel().checkAll();
        ccbFileTypes.setPrefWidth(150);
        ccbFileTypes.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> invalidatePreview());

        grid.add(lblSources, 0, 0);
        grid.add(lvSourcePaths, 1, 0);
        grid.add(btnBox, 2, 0);

        HBox filters = new HBox(20, new Label("范围:"), cbRecursionMode, spRecursionDepth, new Separator(javafx.geometry.Orientation.VERTICAL), new Label("类型:"), ccbFileTypes);
        filters.setAlignment(Pos.CENTER_LEFT);
        grid.add(filters, 1, 1, 2, 1);
        return grid;
    }

    private VBox createLeftPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.getChildren().addAll(new Label("资源浏览"), dirTree = new TreeView<>());
        dirTree.setShowRoot(true);
        VBox.setVgrow(dirTree, Priority.ALWAYS);

        // 新增：右键菜单在系统中打开
        dirTree.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<File>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (item.getPath().equals("ALL_ROOTS")) {
                            setText("已选目录汇总");
                        } else {
                            setText(item.getName().isEmpty() ? item.getAbsolutePath() : item.getName());
                        }
                    }
                }
            };

            ContextMenu cm = new ContextMenu();
            MenuItem openItem = new MenuItem("在资源管理器中打开");
            openItem.setOnAction(e -> openFileInSystem(cell.getItem()));
            cm.getItems().add(openItem);

            // 只对非空且真实存在的目录显示菜单
            cell.itemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null && !newVal.getPath().equals("ALL_ROOTS")) {
                    cell.setContextMenu(cm);
                } else {
                    cell.setContextMenu(null);
                }
            });

            return cell;
        });

        return box;
    }

    private VBox createActionPanel() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #ffffff;");

        Label lblHeader = new Label("操作配置");
        lblHeader.setFont(Font.font("Segoe UI", 16));
        lblHeader.setStyle("-fx-font-weight: bold;");

        cbStrategy = new JFXComboBox<>();
        cbStrategy.setItems(FXCollections.observableArrayList(strategies));
        cbStrategy.setPrefWidth(Double.MAX_VALUE);
        cbStrategy.setConverter(new javafx.util.StringConverter<OldAppStrategy>() {
            @Override
            public String toString(OldAppStrategy object) {
                return object.getName();
            }

            @Override
            public OldAppStrategy fromString(String string) {
                return null;
            }
        });

        strategyConfigContainer = new VBox();
        strategyConfigContainer.setStyle("-fx-padding: 10; -fx-background-color: #fafafa;");

        ScrollPane scrollPane = new ScrollPane(strategyConfigContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(250);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-border-radius: 4;");

        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            strategyConfigContainer.getChildren().clear();
            if (newVal != null && newVal.getConfigNode() != null) {
                strategyConfigContainer.getChildren().add(newVal.getConfigNode());
            } else {
                strategyConfigContainer.getChildren().add(new Label("无需配置"));
            }
            invalidatePreview();
        });

        btnPreview = new JFXButton("1. 生成预览");
        btnPreview.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        btnPreview.setPrefWidth(Double.MAX_VALUE);
        btnPreview.setOnAction(e -> runPreview());

        btnExecute = new JFXButton("2. 执行变更");
        btnExecute.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnExecute.setPrefWidth(Double.MAX_VALUE);
        btnExecute.setDisable(true);
        btnExecute.setOnAction(e -> runExecute());

        btnStop = new JFXButton("停止任务");
        btnStop.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnStop.setPrefWidth(Double.MAX_VALUE);
        btnStop.setDisable(true);
        btnStop.setOnAction(e -> stopExecution());

        // [变更] 初始化复选框并添加监听
        chkHideUnchanged = new CheckBox("仅显示变更项 (Hide Unchanged)");
        chkHideUnchanged.setSelected(true); // 默认开启以优化性能
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> refreshPreviewTree());

        box.getChildren().addAll(
                lblHeader, new Separator(),
                new Label("功能选择:"), cbStrategy,
                new Label("参数设置:"), scrollPane,
                new Region(),
                // [变更] 将复选框加入布局
                chkHideUnchanged,
                btnPreview, btnExecute, btnStop
        );
        VBox.setVgrow(box.getChildren().get(6), Priority.ALWAYS);
        return box;
    }

    // [变更] 新增方法：无需重新扫描，仅根据缓存数据刷新视图
    private void refreshPreviewTree() {
        if (changePreviewList.isEmpty()) return;

        boolean hide = chkHideUnchanged.isSelected();
        // 在 UI 线程捕获数据快照，避免并发修改异常
        List<ChangeRecord> snapshot = new ArrayList<>(changePreviewList);

        Task<TreeItem<ChangeRecord>> task = new Task<TreeItem<ChangeRecord>>() {
            @Override protected TreeItem<ChangeRecord> call() {
                // 复用构建逻辑
                return buildPreviewTree(snapshot, sourceRootDirs, hide);
            }
        };
        task.setOnSucceeded(e -> previewTree.setRoot(task.getValue()));
        new Thread(task).start();
    }

    private VBox createPreviewPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        previewTree = new TreeView<>();
        VBox.setVgrow(previewTree, Priority.ALWAYS);
        previewTree.setCellFactory(tv -> new TreeCell<ChangeRecord>() {
            @Override
            protected void updateItem(ChangeRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setContextMenu(null);
                } else {
                    HBox node = new HBox(8);
                    node.setAlignment(Pos.CENTER_LEFT);
                    if ("VIRTUAL_ROOT".equals(item.getNewPath())) {
                        setText("预览根节点");
                        return;
                    }
                    boolean isDir = item.getFileHandle() != null && item.getFileHandle().isDirectory();
                    Label oldName = new Label(item.getOriginalName());
                    if (isDir) oldName.setStyle("-fx-font-weight: bold;");
                    Label statusIcon = new Label();
                    switch (item.getStatus()) {
                        case PENDING:
                            statusIcon.setText("⏳");
                            break;
                        case RUNNING:
                            statusIcon.setText("🔄");
                            break;
                        case SUCCESS:
                            statusIcon.setText("✅");
                            statusIcon.setTextFill(Color.GREEN);
                            break;
                        case FAILED:
                            statusIcon.setText("❌");
                            statusIcon.setTextFill(Color.RED);
                            break;
                        case SKIPPED:
                            statusIcon.setText("⏭");
                            statusIcon.setTextFill(Color.GRAY);
                            break;
                    }
                    node.getChildren().add(statusIcon);
                    if (item.isChanged()) {
                        Label arrow = new Label("➜");
                        Label newName = new Label(item.getNewName());
                        newName.setTextFill(Color.web("#27ae60"));
                        oldName.setStyle("-fx-strikethrough: true; -fx-text-fill: #e74c3c;");
                        Label tag = new Label("[" + item.getOpType() + "]");
                        tag.setTextFill(Color.BLUE);
                        tag.setStyle("-fx-font-size: 10px; -fx-border-color: blue; -fx-border-radius: 2;");
                        node.getChildren().addAll(tag, oldName, arrow, newName);
                    } else {
                        node.getChildren().add(oldName);
                    }
                    setGraphic(node);
                    if (item.getStatus() == ExecStatus.RUNNING) setStyle("-fx-background-color: #e3f2fd;");
                    else if (item.getStatus() == ExecStatus.SUCCESS) setStyle("-fx-background-color: #e8f5e9;");
                    else if (item.getStatus() == ExecStatus.FAILED) setStyle("-fx-background-color: #ffebee;");
                    else setStyle("");
                    this.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2 && item.getFileHandle() != null && item.getFileHandle().exists())
                            openFileInSystem(item.getFileHandle());
                    });
                    ContextMenu cm = new ContextMenu();
                    MenuItem openDirItem = new MenuItem("打开所在文件夹");
                    openDirItem.setOnAction(e -> openParentDirectory(item.getFileHandle()));
                    MenuItem playItem = new MenuItem("播放/打开文件");
                    playItem.setOnAction(e -> openFileInSystem(item.getFileHandle()));
                    cm.getItems().addAll(playItem, openDirItem);
                    setContextMenu(cm);
                }
            }
        });
        box.getChildren().addAll(new Label("变更预览 (双击播放)"), previewTree);
        return box;
    }


    // 优化：停止执行逻辑，确保快速响应
    private void stopExecution() {
        isTaskRunning = false; // 1. 立即阻断新任务提交
        if (currentExecutor != null && !currentExecutor.isShutdown()) {
            currentExecutor.shutdownNow(); // 2. 发送中断信号
            logImmediate("正在强制终止任务...");
        }

        // 3. 强制重置 UI 状态，不等待线程完全退出
        Platform.runLater(() -> {
            btnStop.setDisable(true);
            btnPreview.setDisable(false);
            btnExecute.setDisable(true);
            cbStrategy.setDisable(false);

            // 修复：先解绑，再设置文本
            mainProgressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            progressLabel.setText("任务已终止");
            etaLabel.setText("");
            logImmediate("任务已停止。请重新生成预览。");
        });
    }


    // --- 优化：runPreview 支持进度反馈 ---
    private void runPreview() {
        if (sourceRootDirs.isEmpty()) {
            log("请添加工作目录。");
            return;
        }
        OldAppStrategy strategy = cbStrategy.getValue();
        if (strategy == null) return;

        changePreviewList.clear();
        previewTree.setRoot(null);
        btnExecute.setDisable(true);

        strategy.captureParams();
        executionThreadCount = strategy.getPreferredThreadCount();

        // [变更] 捕获过滤选项状态
        boolean hideUnchanged = chkHideUnchanged.isSelected();

        int maxDepth = "仅当前目录".equals(cbRecursionMode.getValue()) ? 1 :
                "指定目录深度".equals(cbRecursionMode.getValue()) ? spRecursionDepth.getValue() : Integer.MAX_VALUE;

        // 绑定进度条，准备开始
        progressBox.setVisible(true);
        mainProgressBar.progressProperty().unbind();
        mainProgressBar.setProgress(0);

        // 修复：必须先解绑，再设置文本
        progressLabel.textProperty().unbind();
        progressLabel.setText("准备扫描...");

        Task<TreeItem<ChangeRecord>> task = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() throws Exception {
                long t0 = System.currentTimeMillis();

                // 1. 扫描阶段：传入 Consumer 更新 UI 消息
                logImmediate("开始扫描文件");
                List<File> allFiles = new ArrayList<>();
                for (File root : sourceRootDirs) {
                    updateMessage("正在扫描目录: " + root.getName());
                    allFiles.addAll(scanFiles(root, strategy.getTargetType(), maxDepth, this::updateMessage));
                }

                long t1 = System.currentTimeMillis();
                logImmediate("扫描完成，耗时: " + (t1 - t0) + "ms");

                logImmediate("正在分析变更 (并行处理)...");

                // 2. 分析阶段：传入 BiConsumer 更新进度和消息
                List<ChangeRecord> changes = strategy.analyze(allFiles, sourceRootDirs, (progress, msg) -> {
                    updateProgress(progress, 1.0);
                    if (msg != null) updateMessage(msg);
                });

                long t2 = System.currentTimeMillis();
                logImmediate("分析完成，耗时: " + (t2 - t1) + "ms");

                Platform.runLater(() -> changePreviewList.setAll(changes));

                updateMessage("正在构建视图...");
                return buildPreviewTree(changes, sourceRootDirs, hideUnchanged);
            }
        };

        task.setOnSucceeded(e -> {
            previewTree.setRoot(task.getValue());
            long count = changePreviewList.stream().filter(ChangeRecord::isChanged).count();
            log("预览完成。有效变更: " + count + " / 总数: " + changePreviewList.size());
            btnExecute.setDisable(count == 0);
            progressLabel.textProperty().unbind();
            progressLabel.setText("就绪");
            mainProgressBar.progressProperty().unbind();
            mainProgressBar.setProgress(1.0);
        });

        task.setOnFailed(e -> {
            log("预览失败: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
        });

        // 绑定任务消息到 UI
        progressLabel.textProperty().bind(task.messageProperty());
        mainProgressBar.progressProperty().bind(task.progressProperty());

        new Thread(task).start();
    }

    // --- 优化：scanFiles 支持进度反馈 ---
    private List<File> scanFiles(File root, ScanTarget type, int depth, Consumer<String> logger) {
        List<File> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root.toPath(), depth)) {
            ObservableList<String> types = ccbFileTypes.getCheckModel().getCheckedItems();
            AtomicInteger count = new AtomicInteger(0);

            result = stream.filter(p -> {
                // 节流日志：每 1000 个文件更新一次
                int c = count.incrementAndGet();
                if (c % 1000 == 0 && logger != null) {
                    Platform.runLater(() -> logger.accept("已扫描 " + c + " 个项目..."));
                }

                // TODO 优化转换出来的文件目录
                if(p.toString().contains("Converted")){
                    return false;
                }

                File f = p.toFile();
                if (f.isDirectory()) return type != ScanTarget.FILES_ONLY;
                if (type == ScanTarget.FOLDERS_ONLY) return false;

                String name = f.getName().toLowerCase();
                for (String ext : types) {
                    if (name.endsWith("." + ext)) return true;
                }
                return false;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            log("扫描错误 [" + root.getName() + "]: " + e.getMessage());
        }
        return result;
    }


    // 新增：系统文件操作辅助方法
    private void openFileInSystem(File file) {
        if (file == null || !file.exists()) return;
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            log("无法打开文件: " + e.getMessage());
        }
    }

    private void openParentDirectory(File file) {
        if (file == null) return;
        File parent = file.isDirectory() ? file : file.getParentFile();
        if (parent != null && parent.exists()) {
            openFileInSystem(parent);
        }
    }


    // --- Actions ---

    private void addDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(primaryStage);
        if (f != null && !sourceRootDirs.contains(f)) {
            sourceRootDirs.add(f);
            sourcePathStrings.add(f.getAbsolutePath());
            refreshLeftTree();
        }
    }

    private void removeDirectory() {
        int idx = lvSourcePaths.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            sourceRootDirs.remove(idx);
            sourcePathStrings.remove(idx);
            refreshLeftTree();
        }
    }

    private void refreshLeftTree() {
        TreeItem<File> root = new TreeItem<>(new File("ALL_ROOTS"));
        root.setExpanded(true);
        for (File dir : sourceRootDirs) {
            TreeItem<File> item = new TreeItem<>(dir);
            item.setExpanded(true);
            root.getChildren().add(item);
        }
        dirTree.setRoot(root);
    }

    private TreeItem<ChangeRecord> buildPreviewTree(List<ChangeRecord> records, List<File> rootDirs, boolean hideUnchanged) {
        ChangeRecord vRoot = new ChangeRecord("ROOT", "", null, false, "VIRTUAL_ROOT", OperationType.NONE);
        TreeItem<ChangeRecord> rootItem = new TreeItem<>(vRoot);
        rootItem.setExpanded(true);

        Map<String, TreeItem<ChangeRecord>> pathMap = new HashMap<>();
        pathMap.put("VIRTUAL_ROOT", rootItem);

        for (File r : rootDirs) {
            ChangeRecord rec = new ChangeRecord(r.getAbsolutePath(), "", r, false, r.getAbsolutePath(), OperationType.NONE);
            TreeItem<ChangeRecord> item = new TreeItem<>(rec);
            item.setExpanded(true);
            rootItem.getChildren().add(item);
            pathMap.put(r.getAbsolutePath(), item);
        }

        records.sort(Comparator.comparing(ChangeRecord::getOriginalPath));
        for (ChangeRecord rec : records) {
            // [变更] 核心过滤逻辑：如果开启隐藏，且文件未变更（且非失败状态），则跳过不显示
            if (hideUnchanged && !rec.isChanged() && rec.getStatus() != ExecStatus.FAILED) continue;

            if (rootDirs.contains(rec.getFileHandle())) continue;

            // ensureParent 会自动只创建必要的父目录路径，这也会大幅减少树节点的数量
            TreeItem<ChangeRecord> parent = ensureParent(rec.getFileHandle().getParentFile(), rootDirs, pathMap, rootItem);
            parent.getChildren().add(new TreeItem<>(rec));
        }
        return rootItem;
    }

    private TreeItem<ChangeRecord> ensureParent(File dir, List<File> roots, Map<String, TreeItem<ChangeRecord>> map, TreeItem<ChangeRecord> vRoot) {
        String path = dir.getAbsolutePath();
        if (map.containsKey(path)) return map.get(path);

        File matchRoot = roots.stream().filter(r -> path.startsWith(r.getAbsolutePath())).findFirst().orElse(null);
        if (matchRoot != null && !dir.equals(matchRoot)) {
            TreeItem<ChangeRecord> parent = ensureParent(dir.getParentFile(), roots, map, vRoot);
            ChangeRecord rec = new ChangeRecord(dir.getName(), "", dir, false, path, OperationType.NONE);
            TreeItem<ChangeRecord> item = new TreeItem<>(rec);
            item.setExpanded(true);
            parent.getChildren().add(item);
            map.put(path, item);
            return item;
        }
        return vRoot;
    }

    // --- Execution Logic (Thread Pool + Progress) ---
    // 优化：runExecute，增加节流反馈
    private void runExecute() {
        long count = changePreviewList.stream().filter(c -> c.isChanged() && c.getStatus() != ExecStatus.SKIPPED).count();
        if (count == 0) {
            logImmediate("没有待执行的有效变更，立即结束。");
            return;
        }
        if (isTaskRunning) {
            logImmediate("已有执行中的变更，不可操作。");
            return;
        }
        OldAppStrategy strategy = cbStrategy.getValue();
        if (strategy == null) return;
        executionThreadCount = strategy.getPreferredThreadCount();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("确定执行 %d 个变更吗？\n当前并发线程数: %d", count, executionThreadCount),
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                isTaskRunning = true;
                btnPreview.setDisable(true);
                btnExecute.setDisable(true);
                btnStop.setDisable(false);
                cbStrategy.setDisable(true);

                progressBox.setVisible(true);
                mainProgressBar.progressProperty().unbind();
                mainProgressBar.setProgress(0);
                progressLabel.textProperty().unbind(); // 解绑之前的 Preview Task
                progressLabel.setText("初始化线程池...");

                // 确保执行时也使用最新的参数（虽然通常预览已经捕获了，但保险起见）
                cbStrategy.getValue().captureParams();

                Task<Void> executeTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        List<ChangeRecord> todos = changePreviewList.stream()
                                .filter(c -> c.isChanged() && c.getStatus() != ExecStatus.SKIPPED)
                                .collect(Collectors.toList());

                        int total = todos.size();
                        AtomicInteger current = new AtomicInteger(0);
                        AtomicInteger successCount = new AtomicInteger(0);
                        AtomicInteger failCount = new AtomicInteger(0);
                        long startTime = System.currentTimeMillis();
                        currentExecutor = Executors.newFixedThreadPool(executionThreadCount);

                        for (ChangeRecord rec : todos) {
                            if (currentExecutor.isShutdown() || !isTaskRunning) break;
                            currentExecutor.submit(() -> {
                                if (Thread.currentThread().isInterrupted() || !isTaskRunning) return;
                                try {
                                    log("开始处理: [" + rec.getOriginalName() + "]: ");
                                    long before = System.currentTimeMillis();
                                    updateRecordStatus(rec, ExecStatus.RUNNING);
                                    performFileOperation(rec);
                                    updateRecordStatus(rec, ExecStatus.SUCCESS);
                                    successCount.incrementAndGet();
                                    log("成功处理: [" + rec.getOriginalName() + "]: " + "，耗时：" + (System.currentTimeMillis() - before)/1000 + "s");
                                } catch (Exception e) {
                                    updateRecordStatus(rec, ExecStatus.FAILED);
                                    failCount.incrementAndGet();
                                    log("失败处理 [" + rec.getOriginalName() + "]: " + e.getMessage());
                                } finally {
                                    int done = current.incrementAndGet();
                                    updateProgress(done, total);
                                    if (done % 5 == 0 || done == total) {
                                        long elapsedMillis = System.currentTimeMillis() - startTime;
                                        double speed = (double) done / elapsedMillis;
                                        long remainingItems = total - done;
                                        long remainingMillis = speed > 0 ? (long) (remainingItems / speed) : 0;
                                        String etaStr = formatDuration(remainingMillis);
                                        Platform.runLater(() -> {
                                            progressLabel.textProperty().unbind();
                                            progressLabel.setText(String.format("进度: %d / %d (成功:%d 失败:%d)", done, total, successCount.get(), failCount.get()));
                                            etaLabel.textProperty().unbind();
                                            etaLabel.setText("预计剩余: " + etaStr);
                                        });
                                    }
                                }
                            });
                        }
                        currentExecutor.shutdown();
                        try {
                            while (!currentExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                                if (!isTaskRunning) {
                                    currentExecutor.shutdownNow();
                                    break;
                                }
                            }
                        } catch (InterruptedException e) {
                            currentExecutor.shutdownNow();
                        }

                        Platform.runLater(() -> {
                            if (isTaskRunning) {
                                String totalTime = formatDuration(System.currentTimeMillis() - startTime);
                                logImmediate("=== 执行完成 === 总耗时: " + totalTime);
                                progressLabel.textProperty().unbind();
                                progressLabel.setText("执行完成");
                                etaLabel.textProperty().unbind();
                                etaLabel.setText("");
                                isTaskRunning = false;
                                btnPreview.setDisable(false);
                                btnExecute.setDisable(false);
                                btnStop.setDisable(true);
                                cbStrategy.setDisable(false);
                            }
                        });
                        return null;
                    }
                };
                mainProgressBar.progressProperty().bind(executeTask.progressProperty());
                new Thread(executeTask).start();
            }
        });
    }


    // 简单的格式化时间工具
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "秒";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "分 " + remainingSeconds + "秒";
    }

    private void updateRecordStatus(ChangeRecord rec, ExecStatus status) {
        rec.setStatus(status);
        // 只有状态变更时才触发 TreeView 刷新，这里不手动触发 refresh，依赖 JavaFX 属性绑定或用户滚动
        // 如果需要实时视觉反馈，且数量巨大，不建议全局 refresh。
        // 可以在 Cell Factory 中绑定 Status Property，这里简化处理，仍然只更新数据模型
//        Platform.runLater(() -> previewTree.refresh());
    }

    private void performFileOperation(ChangeRecord rec) throws Exception {
        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());

        switch (rec.getOpType()) {
            case RENAME:
            case MOVE:
                if (source.equals(target)) return;
                if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                break;
            case SPLIT: // 新增 SPLIT 操作
                if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
                splitAudioTrack(rec);
                break;
            case CONVERT:
                File finalTarget = target;
                File stagingFile = null;

                // 检查是否启用了 SSD 缓存暂存
                if (rec.getExtraParams().containsKey("stagingPath")) {
                    stagingFile = new File(rec.getExtraParams().get("stagingPath"));
                    if (!stagingFile.getParentFile().exists()) stagingFile.getParentFile().mkdirs();
                    // 将转换目标重定向到暂存文件
                    target = stagingFile;
                } else {
                    if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
                }

                // 执行转换 (输出到 target，可能是最终路径，也可能是 staging)
                convertAudioFile(source, target, rec.getExtraParams());

                // 如果使用了暂存，执行移动操作
                if (stagingFile != null && stagingFile.exists()) {
                    if (!finalTarget.getParentFile().exists()) finalTarget.getParentFile().mkdirs();
                    try {
                        Files.move(stagingFile.toPath(), finalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new IOException("缓存移动失败: " + e.getMessage(), e);
                    }
                }
                break;

            default:
                break;
        }
    }

    // --- New: Split Audio Track Implementation ---
    private void splitAudioTrack(ChangeRecord rec) throws IOException {
        Map<String, String> params = rec.getExtraParams();
        String audioSourcePath = params.get("audioSource");
        File audioSource = new File(audioSourcePath);
        if (!audioSource.exists()) throw new FileNotFoundException("源音频文件不存在: " + audioSourcePath);

        String startTime = params.get("startTime"); // seconds
        String duration = params.get("duration");   // seconds (optional)
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");

        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        FFmpegBuilder builder = new FFmpegBuilder();

        // 输入选项：使用 -ss 进行快速 seek (注意：对于某些格式放在 -i 之前更精确/快)
        // builder.addExtraArgs("-ss", startTime);
        builder.setInput(audioSourcePath);

        FFmpegOutputBuilder outputBuilder = builder.addOutput(rec.getNewPath())
                .setStartOffset(Long.parseLong(startTime), TimeUnit.SECONDS) // 精确 seek
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"))
                .addExtraArgs("-map_metadata", "-1"); // 清除原文件元数据，使用自定义的

        // 如果有持续时间（除了最后一轨通常都有）
        if (duration != null) {
            outputBuilder.setDuration(Long.parseLong(duration), TimeUnit.SECONDS);
        }

        // 添加 Metadata
        if (params.containsKey("title")) outputBuilder.addMetaTag("title", params.get("title"));
        if (params.containsKey("artist")) outputBuilder.addMetaTag("artist", params.get("artist"));
        if (params.containsKey("album")) outputBuilder.addMetaTag("album", params.get("album"));
        if (params.containsKey("track")) outputBuilder.addMetaTag("track", params.get("track"));

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    // 核心修复：APE 转换支持
    private void convertAudioFile(File source, File target, Map<String, String> params) throws IOException {
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        // 第一次尝试：带元数据映射
        try {
            runFFmpegJob(ffmpeg, source, target, params, true);
        } catch (IOException e) {
            // 错误分析：如果是 APE 等格式报 non-zero exit，通常是 Metadata 或者是 Cover Art 导致的问题
            // 尝试降级策略：不带元数据映射重新转换
            log("转换失败，尝试移除元数据参数重试: " + source.getName());
            try {
                if (target.exists()) target.delete();
                runFFmpegJob(ffmpeg, source, target, params, false);
            } catch (IOException retryException) {
                throw new IOException("重试依然失败: " + retryException.getMessage());
            }
        }
    }

    // 核心修复：强制 APE 格式映射
    private void runFFmpegJob(FFmpeg ffmpeg, File source, File target, Map<String, String> params, boolean mapMetadata) throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .overrideOutputFiles(true);
        FFmpegOutputBuilder outputBuilder = builder.addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"));

        // 只有在 mapMetadata 为 true 时才添加该参数
        if (mapMetadata) {
            outputBuilder.addExtraArgs("-map_metadata", "0");
        }
        outputBuilder.addExtraArgs("-threads", "4");

        if (params.containsKey("sample_rate")) {
            outputBuilder.setAudioSampleRate(Integer.parseInt(params.get("sample_rate")));
        }

        if (params.containsKey("channels")) {
            outputBuilder.setAudioChannels(Integer.parseInt(params.get("channels")));
        }

        // 使用 run() 同步执行，以便 ExecutorService 管理
        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }


}
