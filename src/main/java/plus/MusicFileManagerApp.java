package plus;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextArea;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
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
import plus.model.ChangeRecord;
import plus.plugins.*;
import plus.type.ExecStatus;
import plus.type.OperationType;
import plus.type.ScanTarget;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MusicFileManagerApp extends Application {
    private static final SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
    private final List<AppStrategy> strategies = new ArrayList<>();
    // Local Conf
    // 使用 Properties 和本地文件
    private Properties appProps = new Properties();
    private File configFile = new File(System.getProperty("user.home"), ".echo_music_manager.config");

    // Data Models
    private final ObservableList<ChangeRecord> changePreviewList = FXCollections.observableArrayList();
    private final ObservableList<String> sourcePathStrings = FXCollections.observableArrayList();
    private final List<File> sourceRootDirs = new ArrayList<>();
    private Stage primaryStage;

    // UI Controls
    private ListView<String> lvSourcePaths;
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;
    private JFXTextArea logArea;

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

    // 内存信息
    private TreeView<File> dirTree;
    private TreeView<ChangeRecord> previewTree;
    private JFXComboBox<AppStrategy> cbStrategy;
    private VBox strategyConfigContainer;
    // 线程池配置
    private int executionThreadCount = 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 v6.0 (Pro)");

        initStrategies();
        Scene scene = new Scene(createMainLayout(), 1400, 950);
        // 简单的美化：全局字体优化
        scene.getRoot().setStyle("-fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; -fx-font-size: 14px;");

        primaryStage.setScene(scene);

        // 加载保存的配置
        loadPreferences();

        // 退出时保存配置
        primaryStage.setOnCloseRequest(e -> savePreferences());

        primaryStage.show();
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
            for (AppStrategy s : strategies) {
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
            for (AppStrategy s : strategies) {
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
        bottomBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;"); // 底部美化

        progressBox = new VBox(5);
        progressLabel = new Label("准备就绪");
        etaLabel = new Label(""); // ETA 标签
        etaLabel.setTextFill(Color.GRAY);
        etaLabel.setFont(Font.font(12));

        HBox progressInfo = new HBox(20, new Label("总进度:"), progressLabel, new Region(), etaLabel);
        HBox.setHgrow(progressInfo.getChildren().get(2), Priority.ALWAYS); // 让ETA靠右

        mainProgressBar = new ProgressBar(0);
        mainProgressBar.setPrefWidth(Double.MAX_VALUE);
        mainProgressBar.setPrefHeight(15); // 稍微变细一点，更精致
        // 给进度条加点样式
        mainProgressBar.setStyle("-fx-accent: #2ecc71;");

        progressBox.getChildren().addAll(progressInfo, mainProgressBar);
        progressBox.setVisible(false);

        logArea = new JFXTextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-text-fill: #333;");

        bottomBox.getChildren().addAll(progressBox, logArea);
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
        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录深度"));

        ObservableList<String> extensions = FXCollections.observableArrayList(
                "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts"
        );
        ccbFileTypes = new CheckComboBox<>(extensions);
        ccbFileTypes.getCheckModel().checkAll();
        ccbFileTypes.setPrefWidth(150);

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
        cbStrategy.setConverter(new javafx.util.StringConverter<AppStrategy>() {
            @Override public String toString(AppStrategy object) { return object.getName(); }
            @Override public AppStrategy fromString(String string) { return null; }
        });

        // --- 变更开始：UI 优化 ---
        strategyConfigContainer = new VBox();
        strategyConfigContainer.setStyle("-fx-padding: 10; -fx-background-color: #fafafa;");
        // 不再设置固定最小高度，让 ScrollPane 管理

        // 使用 ScrollPane 包裹配置容器
        ScrollPane scrollPane = new ScrollPane(strategyConfigContainer);
        scrollPane.setFitToWidth(true); // 宽度自适应
        scrollPane.setPrefHeight(250);  // 设置首选高度，超过则滚动
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-border-radius: 4;");
        // --- 变更结束 ---

        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            strategyConfigContainer.getChildren().clear();
            if (newVal != null && newVal.getConfigNode() != null) {
                strategyConfigContainer.getChildren().add(newVal.getConfigNode());
            } else {
                strategyConfigContainer.getChildren().add(new Label("无需配置"));
            }
        });

        // 初始化按钮
        btnPreview = new JFXButton("1. 生成预览");
        btnPreview.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        btnPreview.setPrefWidth(Double.MAX_VALUE);
        btnPreview.setOnAction(e -> runPreview());

        btnExecute = new JFXButton("2. 执行变更");
        btnExecute.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnExecute.setPrefWidth(Double.MAX_VALUE);
        btnExecute.setOnAction(e -> runExecute());

        btnStop = new JFXButton("3. 终止任务");
        btnStop.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnStop.setPrefWidth(Double.MAX_VALUE);
        btnStop.setDisable(true);
        btnStop.setOnAction(e -> stopExecution());

        box.getChildren().addAll(
                lblHeader, new Separator(),
                new Label("功能选择:"), cbStrategy,
                new Label("参数设置:"), scrollPane, // 这里放入 scrollPane 而不是 strategyConfigContainer
                new Region(),
                btnPreview, btnExecute, btnStop
        );
        VBox.setVgrow(box.getChildren().get(6), Priority.ALWAYS);
        return box;
    }

    // 新增：停止执行逻辑
    private void stopExecution() {
        if (currentExecutor != null && !currentExecutor.isShutdown()) {
            currentExecutor.shutdownNow(); // 尝试中断所有运行中的线程
            log("正在终止任务，请稍候...");
            btnStop.setDisable(true); // 防止重复点击
        }
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
                    setContextMenu(null); // 清理菜单
                } else {
                    // ... existing rendering code ...
                    HBox node = new HBox(8);
                    // ... (保持原有的渲染逻辑) ...
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

                    // ... existing style logic ...
                    if (item.getStatus() == ExecStatus.RUNNING) setStyle("-fx-background-color: #e3f2fd;");
                    else if (item.getStatus() == ExecStatus.SUCCESS) setStyle("-fx-background-color: #e8f5e9;");
                    else if (item.getStatus() == ExecStatus.FAILED) setStyle("-fx-background-color: #ffebee;");
                    else setStyle("");

                    // 新增：双击播放 / 打开
                    this.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2 && item.getFileHandle() != null && item.getFileHandle().exists()) {
                            openFileInSystem(item.getFileHandle());
                        }
                    });

                    // 新增：右键菜单打开所在目录
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

        box.getChildren().addAll(new Label("变更预览 (双击播放，右键打开目录)"), previewTree);
        return box;
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

    private void runPreview() {
        if (sourceRootDirs.isEmpty()) {
            log("请添加工作目录。");
            return;
        }
        AppStrategy strategy = cbStrategy.getValue();
        if (strategy == null) return;

        executionThreadCount = strategy.getPreferredThreadCount();

        int maxDepth = "仅当前目录".equals(cbRecursionMode.getValue()) ? 1 :
                "指定目录深度".equals(cbRecursionMode.getValue()) ? spRecursionDepth.getValue() : Integer.MAX_VALUE;
        log("预览开始。");
        Task<TreeItem<ChangeRecord>> task = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() throws Exception {
                updateMessage("正在扫描...");
                List<File> allFiles = new ArrayList<>();
                for (File root : sourceRootDirs) {
                    allFiles.addAll(scanFiles(root, strategy.getTargetType(), maxDepth));
                }
                updateMessage("分析变更...");
                List<ChangeRecord> changes = strategy.analyze(allFiles, sourceRootDirs);
                changePreviewList.setAll(changes);
                updateMessage("构建视图...");
                return buildPreviewTree(changes, sourceRootDirs);
            }
        };
        task.setOnSucceeded(e -> {
            previewTree.setRoot(task.getValue());
            log("预览完成。");
        });
        task.setOnFailed(e -> log("预览失败: " + e.getSource().getException().getMessage()));
        new Thread(task).start();
    }

    private TreeItem<ChangeRecord> buildPreviewTree(List<ChangeRecord> records, List<File> rootDirs) {
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
            item.setExpanded(true);
            parent.getChildren().add(item);
            map.put(path, item);
            return item;
        }
        return vRoot;
    }

    // --- Execution Logic (Thread Pool + Progress) ---

    private void runExecute() {
        long count = changePreviewList.stream().filter(c -> c.isChanged() && c.getStatus() != ExecStatus.SKIPPED).count();
        if (count == 0) {
            log("没有待执行的有效变更。");
            return;
        }

        // 检查是否已经在运行
        if (isTaskRunning) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("确定执行 %d 个变更吗？\n当前并发线程数: %d", count, executionThreadCount),
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                // 1. 锁定 UI
                isTaskRunning = true;
                btnPreview.setDisable(true);
                btnExecute.setDisable(true);
                btnStop.setDisable(false);
                cbStrategy.setDisable(true);

                progressBox.setVisible(true);
                mainProgressBar.progressProperty().unbind();
                mainProgressBar.setProgress(0);
                progressLabel.setText("初始化线程池...");
                etaLabel.setText("");

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

                        // 初始化类成员 executor 以便可以被 stopExecution 访问
                        currentExecutor = Executors.newFixedThreadPool(executionThreadCount);

                        for (ChangeRecord rec : todos) {
                            // 如果线程池已关闭（用户点击了停止），跳出循环
                            if (currentExecutor.isShutdown()) break;

                            currentExecutor.submit(() -> {
                                // 双重检查中断状态
                                if (Thread.currentThread().isInterrupted()) return;

                                try {
                                    long begin = System.currentTimeMillis();
                                    updateRecordStatus(rec, ExecStatus.RUNNING);
                                    performFileOperation(rec);
                                    updateRecordStatus(rec, ExecStatus.SUCCESS);
                                    successCount.incrementAndGet();
                                    // 简化的实时反馈
                                    Platform.runLater(() -> logArea.appendText("成功: " + rec.getOriginalPath()+"\\" + rec.getOriginalName() + "，耗时：" +((System.currentTimeMillis()-begin)/1000.0)+  "秒。 \n"));
                                } catch (Exception e) {
                                    updateRecordStatus(rec, ExecStatus.FAILED);
                                    failCount.incrementAndGet();
                                    String msg = String.format("失败 [%s]: %s", rec.getOriginalName(), e.getMessage());
                                    Platform.runLater(() -> logArea.appendText(msg + "\n"));
                                    // 仅在控制台打印堆栈，避免日志区刷屏
                                    System.err.println(msg);
                                } finally {
                                    int done = current.incrementAndGet();
                                    updateProgress(done, total);

                                    // 计算 ETA
                                    long elapsedMillis = System.currentTimeMillis() - startTime;
                                    double speed = (double) done / elapsedMillis;
                                    long remainingItems = total - done;
                                    long remainingMillis = speed > 0 ? (long) (remainingItems / speed) : 0;

                                    String etaStr = formatDuration(remainingMillis);

                                    Platform.runLater(() -> {
                                        progressLabel.setText(String.format("进度: %d / %d (成功:%d 失败:%d)", done, total, successCount.get(), failCount.get()));
                                        etaLabel.setText("预计剩余: " + etaStr);
                                    });
                                }
                            });
                        }

                        currentExecutor.shutdown();
                        try {
                            // 等待任务结束，每秒检查一次
                            while (!currentExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                                if (isCancelled()) {
                                    currentExecutor.shutdownNow();
                                    break;
                                }
                            }
                        } catch (InterruptedException e) {
                            currentExecutor.shutdownNow();
                        }

                        Platform.runLater(() -> {
                            String totalTime = formatDuration(System.currentTimeMillis() - startTime);
                            boolean isStopped = current.get() < total;
                            String statusText = isStopped ? "任务已终止" : "执行完成";

                            log(String.format("=== %s ===", statusText));
                            log(String.format("总耗时: %s", totalTime));
                            log(String.format("总计: %d, 成功: %d, 失败: %d", total, successCount.get(), failCount.get()));

                            progressLabel.setText(statusText);
                            etaLabel.setText("");

                            // 2. 解锁 UI
                            isTaskRunning = false;
                            btnPreview.setDisable(false);
                            btnExecute.setDisable(false);
                            btnStop.setDisable(true);
                            cbStrategy.setDisable(false);
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
        Platform.runLater(() -> previewTree.refresh());
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

    private void convertAudioFile(File source, File target, Map<String, String> params) throws IOException {
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);

        // 第一次尝试：带元数据映射
        try {
            runFFmpegJob(ffmpeg, source, target, params, true);
        } catch (IOException e) {
            // 错误分析：如果是 APE 等格式报 non-zero exit，通常是 Metadata 或者是 Cover Art 导致的问题
            // 尝试降级策略：不带元数据映射重新转换
            System.err.println("转换失败，尝试移除元数据参数重试: " + source.getName());
            try {
                // 删除可能生成的半成品
                if (target.exists()) target.delete();

                // 重试，传入 false 禁用 metadata 映射
                runFFmpegJob(ffmpeg, source, target, params, false);

                // 如果重试成功，手动记录一条日志
                Platform.runLater(() -> logArea.appendText("提示: 文件 [" + source.getName() + "] 通过忽略元数据修复并转换成功。\n"));
            } catch (IOException retryException) {
                // 如果还失败，抛出原始异常或重试异常
                throw new IOException("重试依然失败: " + retryException.getMessage(), retryException);
            }
        }
    }

    // 抽取的底层执行方法
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

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
        executor.createJob(builder).run();
    }


    private List<File> scanFiles(File root, ScanTarget type, int depth) {
        try (Stream<Path> stream = Files.walk(root.toPath(), depth)) {
            ObservableList<String> types = ccbFileTypes.getCheckModel().getCheckedItems();
            return stream.map(Path::toFile).filter(f -> {
                if (f.isDirectory()) return type != ScanTarget.FILES_ONLY;
                if (type == ScanTarget.FOLDERS_ONLY) return false;
                return types.stream().anyMatch(ext -> f.getName().toLowerCase().endsWith("." + ext));
            }).collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void log(String msg) {
        System.out.println(msg);
        Platform.runLater(() -> logArea.appendText(sdf.format(new Date())+ " --- " +msg + "\n"));
    }


}
