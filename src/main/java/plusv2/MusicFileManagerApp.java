package plusv2;

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
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.CheckComboBox;
import plusv2.model.ChangeRecord;
import plusv2.plugins.*;
import plusv2.type.ExecStatus;
import plusv2.type.OperationType;
import plusv2.type.ScanTarget;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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


public class MusicFileManagerApp extends Application {
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final Properties appProps = new Properties();
    // Data Models
    private final ObservableList<ChangeRecord> changePreviewList = FXCollections.observableArrayList();
    private final List<File> sourceRootDirs = new ArrayList<>();
    private final ObservableList<String> logItems = FXCollections.observableArrayList();
    private final List<AppStrategy> strategies = new ArrayList<>();
    private File configFile;
    private Stage primaryStage;
    // UI Controls
    private TreeView<File> sourceTree; // 左侧源目录树
    private TreeTableView<ChangeRecord> previewTable; // 中间预览表(升级)
    // Global Settings Controls
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;
    private CheckBox chkSaveLog;
    private CheckBox chkHideUnchanged;
    // Logging
    private ListView<String> logView;
    private AnimationTimer uiUpdater;
    private PrintWriter fileLogger;
    // Progress
    private VBox progressBox;
    private ProgressBar mainProgressBar;
    private Label progressLabel;
    private Label etaLabel;
    // Strategies & Actions
    private JFXComboBox<AppStrategy> cbStrategy;
    private VBox strategyConfigContainer;
    private JFXButton btnPreview, btnExecute, btnCancel;

    // Task Management
    private ExecutorService currentExecutor;
    private Task<?> currentTask; // 当前运行的主任务(扫描/预览/执行)
    private volatile boolean isTaskRunning = false;
    private int executionThreadCount = 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 v10.0 (Ultimate)");

        initStrategies();
        Scene scene = new Scene(createMainLayout(), 1400, 950);
        scene.getRoot().setStyle("-fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; -fx-font-size: 14px;");

        primaryStage.setScene(scene);

        // 默认不自动加载，让用户手动选择或加载最后一次
        // loadConfigFromFile(new File(...));

        primaryStage.setOnCloseRequest(e -> {
            cancelTask();
            closeFileLogger();
            Platform.exit();
            System.exit(0);
        });

        startUiUpdater();
        primaryStage.show();
    }

    // --- UI Construction ---

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // 1. Top Menu & Toolbar
        root.setTop(createTopBar());

        // 2. Main Split: Left (Source) | Center (Action & Preview)
        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(createLeftPanel(), createCenterPanel());
        mainSplit.setDividerPositions(0.25);
        root.setCenter(mainSplit);

        // 3. Bottom: Status & Log
        root.setBottom(createBottomPanel());

        return root;
    }

    private VBox createTopBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("文件 (File)");
        MenuItem loadConfigItem = new MenuItem("加载配置...");
        loadConfigItem.setOnAction(e -> loadConfigAction());
        MenuItem saveConfigItem = new MenuItem("保存当前配置...");
        saveConfigItem.setOnAction(e -> saveConfigAction());
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(e -> primaryStage.close());
        fileMenu.getItems().addAll(loadConfigItem, saveConfigItem, new SeparatorMenuItem(), exitItem);
        menuBar.getMenus().add(fileMenu);

        return new VBox(menuBar);
    }

    private VBox createLeftPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");

        Label title = new Label("源目录管理");
        title.setFont(Font.font("Segoe UI", 16));
        title.setStyle("-fx-font-weight: bold;");

        Label subHint = new Label("支持拖拽文件夹到此处");
        subHint.setTextFill(Color.GRAY);
        subHint.setFont(Font.font(12));

        sourceTree = new TreeView<>();
        sourceTree.setShowRoot(true);
        VBox.setVgrow(sourceTree, Priority.ALWAYS);

        // 初始化根节点
        refreshLeftTree();

        // 拖拽支持
        sourceTree.setOnDragOver(event -> {
            if (event.getGestureSource() != sourceTree && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        sourceTree.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    if (file.isDirectory() && !sourceRootDirs.contains(file)) {
                        sourceRootDirs.add(file);
                    }
                }
                refreshLeftTree();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // 单元格渲染 & 右键菜单
        sourceTree.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<File>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (item.getPath().equals("ALL_ROOTS")) {
                            setText("📚 所有源目录 (" + sourceRootDirs.size() + ")");
                            setStyle("-fx-font-weight: bold;");
                        } else {
                            setText((item.isDirectory() ? "📁 " : "📄 ") + item.getName());
                            setTooltip(new Tooltip(item.getAbsolutePath()));
                            setStyle("");
                        }
                    }
                }
            };

            ContextMenu cm = new ContextMenu();
            MenuItem removeItem = new MenuItem("移除此目录");
            removeItem.setOnAction(e -> {
                if (cell.getItem() != null && !cell.getItem().getPath().equals("ALL_ROOTS")) {
                    sourceRootDirs.remove(cell.getItem());
                    refreshLeftTree();
                }
            });
            MenuItem openItem = new MenuItem("打开文件夹");
            openItem.setOnAction(e -> openFileInSystem(cell.getItem()));

            cm.getItems().addAll(openItem, new SeparatorMenuItem(), removeItem);

            cell.itemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null && !newVal.getPath().equals("ALL_ROOTS")) {
                    cell.setContextMenu(cm);
                } else {
                    cell.setContextMenu(null);
                }
            });
            return cell;
        });

        JFXButton btnAdd = new JFXButton("添加文件夹...");
        btnAdd.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> addDirectory());

        JFXButton btnClear = new JFXButton("清空列表");
        btnClear.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setOnAction(e -> {
            sourceRootDirs.clear();
            refreshLeftTree();
        });

        box.getChildren().addAll(title, subHint, sourceTree, btnAdd, btnClear);
        return box;
    }

    private VBox createCenterPanel() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        // 1. Global Filters Section (FlowPane for responsiveness)
        FlowPane filters = new FlowPane(15, 10);
        filters.setAlignment(Pos.CENTER_LEFT);

        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("仅当前目录", "递归所有子目录", "指定目录深度"));
        cbRecursionMode.getSelectionModel().select(1);
        cbRecursionMode.getSelectionModel().selectedItemProperty().addListener((o, old, v) -> invalidatePreview());

        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.setPrefWidth(70);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录深度"));
        spRecursionDepth.valueProperty().addListener((o, old, v) -> invalidatePreview());

        ObservableList<String> extensions = FXCollections.observableArrayList(
                "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "iso", "jpg", "png", "nfo", "cue", "tak", "tta", "wv", "wma", "aac", "ogg"
        );
        ccbFileTypes = new CheckComboBox<>(extensions);
        ccbFileTypes.getCheckModel().checkAll();
        ccbFileTypes.setPrefWidth(120);
        ccbFileTypes.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> invalidatePreview());

        filters.getChildren().addAll(
                new Label("扫描范围:"), cbRecursionMode, spRecursionDepth,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                new Label("文件类型:"), ccbFileTypes
        );

        // 2. Strategy Config Section
        HBox strategyHeader = new HBox(10, new Label("功能选择:"), cbStrategy = new JFXComboBox<>());
        strategyHeader.setAlignment(Pos.CENTER_LEFT);
        cbStrategy.setItems(FXCollections.observableArrayList(strategies));
        cbStrategy.setPrefWidth(300);
        cbStrategy.setConverter(new javafx.util.StringConverter<AppStrategy>() {
            @Override
            public String toString(AppStrategy object) {
                return object.getName();
            }

            @Override
            public AppStrategy fromString(String string) {
                return null;
            }
        });

        strategyConfigContainer = new VBox();
        strategyConfigContainer.setStyle("-fx-padding: 15; -fx-background-color: #fdfdfd; -fx-border-color: #eee; -fx-border-radius: 4;");
        ScrollPane scrollConfig = new ScrollPane(strategyConfigContainer);
        scrollConfig.setFitToWidth(true);
        scrollConfig.setPrefHeight(200);
        scrollConfig.setStyle("-fx-background-color: transparent;");

        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            strategyConfigContainer.getChildren().clear();
            if (newVal != null && newVal.getConfigNode() != null) {
                strategyConfigContainer.getChildren().add(newVal.getConfigNode());
            } else {
                strategyConfigContainer.getChildren().add(new Label("该功能无需额外配置。"));
            }
            invalidatePreview();
        });

        // 3. Action Bar
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_LEFT);

        btnPreview = new JFXButton("生成预览");
        btnPreview.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        btnPreview.setOnAction(e -> runPreview());

        btnExecute = new JFXButton("执行变更");
        btnExecute.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        btnExecute.setDisable(true);
        btnExecute.setOnAction(e -> runExecute());

        btnCancel = new JFXButton("取消");
        btnCancel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        btnCancel.setDisable(true);
        btnCancel.setOnAction(e -> cancelTask());

        chkHideUnchanged = new CheckBox("仅显示有变更的项目");
        chkHideUnchanged.setSelected(true);
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> refreshPreviewTable());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(btnPreview, btnExecute, btnCancel, spacer, chkHideUnchanged);

        // 4. Preview Table (Replaces TreeView)
        createPreviewTable();

        box.getChildren().addAll(
                new Label("1. 筛选与配置"), filters,
                new Separator(),
                strategyHeader, scrollConfig,
                new Separator(),
                new Label("2. 预览与执行"), actions, previewTable
        );
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        return box;
    }

    @SuppressWarnings("unchecked")
    private void createPreviewTable() {
        previewTable = new TreeTableView<>();
        previewTable.setShowRoot(false); // 隐藏根节点

        TreeTableColumn<ChangeRecord, String> colName = new TreeTableColumn<>("原名称/文件");
        colName.setPrefWidth(250);
        colName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getOriginalName()));

        TreeTableColumn<ChangeRecord, String> colNewName = new TreeTableColumn<>("新名称");
        colNewName.setPrefWidth(250);
        colNewName.setCellValueFactory(param -> {
            ChangeRecord r = param.getValue().getValue();
            return new SimpleStringProperty(r.isChanged() ? r.getNewName() : "-");
        });
        // 样式化：变更的显示绿色
        colNewName.setCellFactory(col -> new TreeTableCell<ChangeRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (item != null && !item.equals("-")) setTextFill(Color.web("#27ae60"));
                else setTextFill(Color.BLACK);
            }
        });

        TreeTableColumn<ChangeRecord, String> colStatus = new TreeTableColumn<>("状态");
        colStatus.setPrefWidth(100);
        colStatus.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getStatus().toString()));

        TreeTableColumn<ChangeRecord, String> colPath = new TreeTableColumn<>("目标路径");
        colPath.setPrefWidth(300);
        colPath.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getNewPath()));

        previewTable.getColumns().addAll(colName, colNewName, colStatus, colPath);

        // 右键菜单
        previewTable.setRowFactory(tv -> {
            TreeTableRow<ChangeRecord> row = new TreeTableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem openFile = new MenuItem("打开文件/播放");
            openFile.setOnAction(e -> openFileInSystem(row.getItem().getFileHandle()));
            MenuItem openDir = new MenuItem("打开所在目录");
            openDir.setOnAction(e -> openParentDirectory(row.getItem().getFileHandle()));
            MenuItem openTargetDir = new MenuItem("打开目标目录");
            openTargetDir.setOnAction(e -> openFileInSystem(new File(row.getItem().getNewPath()).getParentFile()));

            cm.getItems().addAll(openFile, openDir, openTargetDir);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(cm)
            );
            return row;
        });
    }

    private VBox createBottomPanel() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        // Progress Area
        progressBox = new VBox(5);
        progressLabel = new Label("准备就绪");
        etaLabel = new Label("");
        HBox info = new HBox(20, new Label("状态:"), progressLabel, new Region(), etaLabel);
        HBox.setHgrow(info.getChildren().get(2), Priority.ALWAYS);

        mainProgressBar = new ProgressBar(0);
        mainProgressBar.setPrefWidth(Double.MAX_VALUE);
        mainProgressBar.setPrefHeight(12);
        progressBox.getChildren().addAll(info, mainProgressBar);
        progressBox.setVisible(false);

        // Log Area
        HBox logControls = new HBox(10);
        logControls.setAlignment(Pos.CENTER_LEFT);
        Label logTitle = new Label("系统日志:");
        logTitle.setStyle("-fx-font-weight: bold;");
        chkSaveLog = new CheckBox("输出日志到文件 (execution.log)");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        JFXButton btnClearLog = new JFXButton("清空日志");
        btnClearLog.setOnAction(e -> logItems.clear());
        logControls.getChildren().addAll(logTitle, spacer, chkSaveLog, btnClearLog);

        logView = new ListView<>(logItems);
        logView.setPrefHeight(100);
        logView.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        box.getChildren().addAll(progressBox, logControls, logView);
        return box;
    }

    // --- Core Logic & Task Management ---

    private void runPreview() {
        if (isTaskRunning) return;
        if (sourceRootDirs.isEmpty()) {
            logImmediate("❌ 请先添加源目录！");
            return;
        }
        AppStrategy strategy = cbStrategy.getValue();
        if (strategy == null) return;

        resetState(true);
        strategy.captureParams();
        executionThreadCount = strategy.getPreferredThreadCount();
        boolean hideUnchanged = chkHideUnchanged.isSelected();
        int maxDepth = "仅当前目录".equals(cbRecursionMode.getValue()) ? 1 :
                "指定目录深度".equals(cbRecursionMode.getValue()) ? spRecursionDepth.getValue() : Integer.MAX_VALUE;

        // 启动任务
        startTaskUI("正在扫描并分析...", false);

        Task<TreeItem<ChangeRecord>> task = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() throws Exception {
                long t0 = System.currentTimeMillis();

                // 1. 扫描
                List<File> allFiles = new ArrayList<>();
                for (File root : sourceRootDirs) {
                    if (isCancelled()) break;
                    updateMessage("正在扫描: " + root.getName());
                    allFiles.addAll(scanFiles(root, strategy.getTargetType(), maxDepth, this::updateMessage));
                }

                if (isCancelled()) return null;
                log("扫描完成，共 " + allFiles.size() + " 个对象。");

                // 2. 分析
                updateMessage("正在分析变更...");
                List<ChangeRecord> changes = strategy.analyze(allFiles, sourceRootDirs, (p, msg) -> {
                    updateProgress(p, 1.0);
                    if (msg != null) updateMessage(msg);
                });

                if (isCancelled()) return null;
                Platform.runLater(() -> changePreviewList.setAll(changes));

                updateMessage("生成视图...");
                return buildPreviewTree(changes, sourceRootDirs, hideUnchanged);
            }
        };

        task.setOnSucceeded(e -> {
            previewTable.setRoot(task.getValue());
            long changedCount = changePreviewList.stream().filter(ChangeRecord::isChanged).count();
            logImmediate("✅ 预览完成。预计变更: " + changedCount + " / 总数: " + changePreviewList.size());
            btnExecute.setDisable(changedCount == 0);
            stopTaskUI();
        });

        task.setOnFailed(e -> {
            logImmediate("❌ 预览失败: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
            stopTaskUI();
        });

        task.setOnCancelled(e -> {
            logImmediate("⚠️ 预览已取消");
            stopTaskUI();
        });

        currentTask = task;
        new Thread(task).start();
    }

    private void runExecute() {
        if (isTaskRunning) return;
        long count = changePreviewList.stream().filter(c -> c.isChanged() && c.getStatus() != ExecStatus.SKIPPED).count();
        if (count == 0) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("确定执行 %d 个变更吗？\n(并发数: %d)", count, executionThreadCount),
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                // 准备日志文件
                if (chkSaveLog.isSelected()) initFileLogger();

                startTaskUI("正在执行...", true);
                cbStrategy.getValue().captureParams(); // 二次确认参数

                Task<Void> executeTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        List<ChangeRecord> todos = changePreviewList.stream()
                                .filter(c -> c.isChanged() && c.getStatus() != ExecStatus.SKIPPED)
                                .collect(Collectors.toList());
                        int total = todos.size();
                        AtomicInteger current = new AtomicInteger(0);
                        AtomicInteger success = new AtomicInteger(0);
                        AtomicInteger fail = new AtomicInteger(0);
                        long startT = System.currentTimeMillis();

                        currentExecutor = Executors.newFixedThreadPool(executionThreadCount);

                        for (ChangeRecord rec : todos) {
                            if (currentExecutor.isShutdown()) break;
                            currentExecutor.submit(() -> {
                                try {
                                    updateRecordStatus(rec, ExecStatus.RUNNING);
                                    cbStrategy.getValue().execute(rec);
                                    updateRecordStatus(rec, ExecStatus.SUCCESS);
                                    success.incrementAndGet();
                                    logFile("成功: " + rec.getOriginalName() + " -> " + rec.getNewName());
                                } catch (Exception e) {
                                    updateRecordStatus(rec, ExecStatus.FAILED);
                                    fail.incrementAndGet();
                                    String err = "失败 [" + rec.getOriginalName() + "]: " + e.getMessage();
                                    logFile(err);
                                    logImmediate(err);
                                } finally {
                                    int done = current.incrementAndGet();
                                    updateProgress(done, total);
                                    // 更新 ETA (节流)
                                    if (done % 5 == 0 || done == total) {
                                        long elapsed = System.currentTimeMillis() - startT;
                                        double rate = (double) done / elapsed;
                                        long remain = rate > 0 ? (long) ((total - done) / rate) : 0;
                                        Platform.runLater(() -> {
                                            progressLabel.setText(String.format("进度: %d/%d (成:%d 败:%d)", done, total, success.get(), fail.get()));
                                            etaLabel.setText("剩余: " + formatDuration(remain));
                                        });
                                    }
                                }
                            });
                        }

                        currentExecutor.shutdown();
                        try {
                            while (!currentExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                                if (isCancelled()) {
                                    currentExecutor.shutdownNow();
                                    break;
                                }
                            }
                        } catch (InterruptedException e) {
                            currentExecutor.shutdownNow();
                        }

                        Platform.runLater(() -> {
                            String time = formatDuration(System.currentTimeMillis() - startT);
                            logImmediate(String.format("🏁 执行结束. 耗时: %s. 成功: %d, 失败: %d", time, success.get(), fail.get()));
                            closeFileLogger();
                            stopTaskUI();
                            // 完成后允许再次预览或执行失败的
                            btnPreview.setDisable(false);
                        });
                        return null;
                    }
                };

                executeTask.setOnCancelled(e -> {
                    logImmediate("⚠️ 任务已终止");
                    closeFileLogger();
                    stopTaskUI();
                });

                currentTask = executeTask;
                mainProgressBar.progressProperty().bind(executeTask.progressProperty());
                new Thread(executeTask).start();
            }
        });
    }

    private void cancelTask() {
        if (currentTask != null && isTaskRunning) {
            logImmediate("正在取消...");
            currentTask.cancel();
            if (currentExecutor != null) currentExecutor.shutdownNow();
        }
    }

    // --- Helper Logic ---

    private void startTaskUI(String msg, boolean isExec) {
        isTaskRunning = true;
        btnPreview.setDisable(true);
        btnExecute.setDisable(true);
        btnCancel.setDisable(false);
        cbStrategy.setDisable(true);

        progressBox.setVisible(true);
        progressLabel.textProperty().unbind();
        progressLabel.setText(msg);
        mainProgressBar.progressProperty().unbind();
        mainProgressBar.setProgress(0);
        etaLabel.setText("");
    }

    private void stopTaskUI() {
        isTaskRunning = false;
        btnPreview.setDisable(false);
        btnCancel.setDisable(true);
        cbStrategy.setDisable(false);
        // Execute button state depends on preview result, handle by caller or default disabled
    }

    private void refreshPreviewTable() {
        if (changePreviewList.isEmpty()) return;
        boolean hide = chkHideUnchanged.isSelected();
        List<ChangeRecord> snapshot = new ArrayList<>(changePreviewList);
        Task<TreeItem<ChangeRecord>> task = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() {
                return buildPreviewTree(snapshot, sourceRootDirs, hide);
            }
        };
        task.setOnSucceeded(e -> previewTable.setRoot(task.getValue()));
        new Thread(task).start();
    }

    private TreeItem<ChangeRecord> buildPreviewTree(List<ChangeRecord> records, List<File> rootDirs, boolean hideUnchanged) {
        ChangeRecord vRoot = new ChangeRecord("ROOT", "", null, false, "VIRTUAL_ROOT", OperationType.NONE);
        TreeItem<ChangeRecord> rootItem = new TreeItem<>(vRoot);
        rootItem.setExpanded(true);

        Map<String, TreeItem<ChangeRecord>> pathMap = new HashMap<>();
        pathMap.put("VIRTUAL_ROOT", rootItem);

        // 创建根节点
        for (File r : rootDirs) {
            ChangeRecord rec = new ChangeRecord(r.getAbsolutePath(), "", r, false, r.getAbsolutePath(), OperationType.NONE);
            TreeItem<ChangeRecord> item = new TreeItem<>(rec);
            item.setExpanded(true);
            rootItem.getChildren().add(item);
            pathMap.put(r.getAbsolutePath(), item);
        }

        records.sort(Comparator.comparing(ChangeRecord::getOriginalPath));
        for (ChangeRecord rec : records) {
            if (hideUnchanged && !rec.isChanged() && rec.getStatus() != ExecStatus.FAILED) continue;
            if (rootDirs.contains(rec.getFileHandle())) continue;

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
            item.setExpanded(false); // 默认折叠以优化性能
            parent.getChildren().add(item);
            map.put(path, item);
            return item;
        }
        return vRoot;
    }

    private List<File> scanFiles(File root, ScanTarget type, int depth, Consumer<String> logger) {
        List<File> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root.toPath(), depth)) {
            ObservableList<String> types = ccbFileTypes.getCheckModel().getCheckedItems();
            AtomicInteger count = new AtomicInteger(0);
            result = stream.filter(p -> {
                int c = count.incrementAndGet();
                if (c % 2000 == 0 && logger != null) Platform.runLater(() -> logger.accept("扫描中... (" + c + ")"));

                File f = p.toFile();
                if (f.isDirectory()) {
                    // 如果只处理文件，且当前是目录，跳过返回结果，但walk还是会继续遍历子项
                    return type != ScanTarget.FILES_ONLY;
                }
                // 如果是文件
                if (type == ScanTarget.FOLDERS_ONLY) return false;

                // 检查扩展名
                String name = f.getName().toLowerCase();
                for (String ext : types) {
                    if (name.endsWith("." + ext)) return true;
                }
                return false;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            logImmediate("扫描受限: " + e.getMessage());
        }
        return result;
    }

    // --- Config & Logging IO ---

    private void saveConfigAction() {
        FileChooser fc = new FileChooser();
        fc.setTitle("保存配置");
        fc.setInitialFileName("music_manager_profile.properties");
        File f = fc.showSaveDialog(primaryStage);
        if (f != null) {
            configFile = f;
            savePreferences();
            logImmediate("配置已保存至: " + f.getName());
        }
    }

    private void loadConfigAction() {
        FileChooser fc = new FileChooser();
        fc.setTitle("加载配置");
        File f = fc.showOpenDialog(primaryStage);
        if (f != null) {
            configFile = f;
            loadPreferences();
            logImmediate("配置已加载: " + f.getName());
        }
    }

    private void savePreferences() {
        try {
            // Global
            if (cbStrategy.getSelectionModel().getSelectedIndex() >= 0)
                appProps.setProperty("g_strategy", String.valueOf(cbStrategy.getSelectionModel().getSelectedIndex()));
            appProps.setProperty("g_recMode", String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
            appProps.setProperty("g_recDepth", String.valueOf(spRecursionDepth.getValue()));
            // Strategies
            for (AppStrategy s : strategies) s.saveConfig(appProps);

            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                appProps.store(fos, "MusicFileManager Config");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPreferences() {
        if (!configFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            appProps.load(fis);
            // Global
            int sIdx = Integer.parseInt(appProps.getProperty("g_strategy", "0"));
            if (sIdx < strategies.size()) cbStrategy.getSelectionModel().select(sIdx);
            cbRecursionMode.getSelectionModel().select(Integer.parseInt(appProps.getProperty("g_recMode", "1")));
            spRecursionDepth.getValueFactory().setValue(Integer.parseInt(appProps.getProperty("g_recDepth", "2")));
            // Strategies
            for (AppStrategy s : strategies) s.loadConfig(appProps);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFileLogger() {
        try {
            fileLogger = new PrintWriter(new FileWriter("execution.log", true), true);
            fileLogger.println("--- Session Start: " + new Date() + " ---");
        } catch (IOException e) {
            logImmediate("无法创建日志文件: " + e.getMessage());
        }
    }

    private void logFile(String msg) {
        if (fileLogger != null) fileLogger.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " " + msg);
    }

    private void closeFileLogger() {
        if (fileLogger != null) {
            fileLogger.println("--- Session End ---");
            fileLogger.close();
            fileLogger = null;
        }
    }

    // --- Common Utils ---
    private void startUiUpdater() {
        uiUpdater = new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (now - last >= 100_000_000) {
                    List<String> list = new ArrayList<>();
                    String s;
                    while ((s = logQueue.poll()) != null) list.add(s);
                    if (!list.isEmpty()) {
                        logItems.addAll(list);
                        if (logItems.size() > 2000) logItems.remove(0, list.size());
                        logView.scrollTo(logItems.size() - 1);
                    }
                    last = now;
                }
            }
        };
        uiUpdater.start();
    }

    void log(String m) {
        logQueue.offer(m);
    }

    private void logImmediate(String m) {
        Platform.runLater(() -> {
            logItems.add(m);
            logView.scrollTo(logItems.size() - 1);
        });
    }

    private void addDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(primaryStage);
        if (f != null && !sourceRootDirs.contains(f)) {
            sourceRootDirs.add(f);
            refreshLeftTree();
        }
    }

    private void removeDirectory() { /* Used by Context Menu */ }

    private void refreshLeftTree() {
        if (sourceTree.getRoot() == null) sourceTree.setRoot(new TreeItem<>(new File("ALL_ROOTS")));
        sourceTree.getRoot().getChildren().clear();
        for (File f : sourceRootDirs) sourceTree.getRoot().getChildren().add(new TreeItem<>(f));
        sourceTree.getRoot().setExpanded(true);
    }

    private void resetState(boolean clearLog) {
        if (clearLog) logItems.clear();
        changePreviewList.clear();
        previewTable.setRoot(null);
        btnExecute.setDisable(true);
    }

    void invalidatePreview() {
        if (!changePreviewList.isEmpty()) {
            changePreviewList.clear();
            previewTable.setRoot(null);
            log("配置已变，请重新预览");
        }
        btnExecute.setDisable(true);
    }

    private String formatDuration(long ms) {
        long s = ms / 1000;
        long m = s / 60;
        return String.format("%02d:%02d", m, s % 60);
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

    private void updateRecordStatus(ChangeRecord r, ExecStatus s) {
        r.setStatus(s); /* Table refresh handled by prop binding or refresh() */
    }

    // --- Context Injection ---
    private void initStrategies() {
        register(new AdvancedRenameStrategy());
        register(new AudioConverterStrategy());
        register(new AlbumDirNormalizeStrategy());
        register(new TrackNumberStrategy());
        register(new FileMigrateStrategy());
        register(new CueSplitterStrategy());
    }

    private void register(AppStrategy s) {
        s.setContext(this);
        strategies.add(s);
    }
}


