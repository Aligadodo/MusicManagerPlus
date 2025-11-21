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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MusicFileManagerApp extends Application {

    // Data Models
    private final ObservableList<ChangeRecord> changePreviewList = FXCollections.observableArrayList();
    private final ObservableList<String> sourcePathStrings = FXCollections.observableArrayList();
    private final List<File> sourceRootDirs = new ArrayList<>();
    private final List<AppStrategy> strategies = new ArrayList<>();
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
        primaryStage.setTitle("Echo - 音乐文件管理专家 v5.2 (Fixes)");

        initStrategies();
        Scene scene = new Scene(createMainLayout(), 1400, 950);
        primaryStage.setScene(scene);
        primaryStage.show();
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

        progressBox = new VBox(5);
        progressLabel = new Label("准备就绪");
        mainProgressBar = new ProgressBar(0);
        mainProgressBar.setPrefWidth(Double.MAX_VALUE);
        mainProgressBar.setPrefHeight(20);
        progressBox.getChildren().addAll(new HBox(10, new Label("总进度:"), progressLabel), mainProgressBar);
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
                "mp3", "flac", "wav", "ape", "dsf", "dff", "dts", "dfd"
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
        strategyConfigContainer.setStyle("-fx-border-color: #bdc3c7; -fx-padding: 10; -fx-background-color: #fafafa;");
        strategyConfigContainer.setMinHeight(100);

        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            strategyConfigContainer.getChildren().clear();
            if (newVal != null && newVal.getConfigNode() != null) {
                strategyConfigContainer.getChildren().add(newVal.getConfigNode());
            } else {
                strategyConfigContainer.getChildren().add(new Label("无需配置"));
            }
        });

        JFXButton btnPreview = new JFXButton("1. 生成预览");
        btnPreview.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        btnPreview.setPrefWidth(Double.MAX_VALUE);
        btnPreview.setOnAction(e -> runPreview());

        JFXButton btnExecute = new JFXButton("2. 执行变更");
        btnExecute.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnExecute.setPrefWidth(Double.MAX_VALUE);
        btnExecute.setOnAction(e -> runExecute());

        box.getChildren().addAll(lblHeader, new Separator(), new Label("功能选择:"), cbStrategy, new Label("参数设置:"), strategyConfigContainer, new Region(), btnPreview, btnExecute);
        VBox.setVgrow(box.getChildren().get(6), Priority.ALWAYS);
        return box;
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
                        if (item.getOpType() == OperationType.MOVE || item.getOpType() == OperationType.CONVERT) {
                            Tooltip.install(node, new Tooltip("目标路径: " + item.getNewPath()));
                        }
                    } else {
                        node.getChildren().add(oldName);
                    }

                    if (item.getStatus() == ExecStatus.RUNNING) {
                        setStyle("-fx-background-color: #e3f2fd;");
                    } else if (item.getStatus() == ExecStatus.SUCCESS) {
                        setStyle("-fx-background-color: #e8f5e9;");
                    } else if (item.getStatus() == ExecStatus.FAILED) {
                        setStyle("-fx-background-color: #ffebee;");
                    } else {
                        setStyle("");
                    }
                    setGraphic(node);
                }
            }
        });

        box.getChildren().addAll(new Label("变更预览 (支持目录进度显示)"), previewTree);
        return box;
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
        if (count == 0) { log("没有待执行的有效变更。"); return; }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("确定执行 %d 个变更吗？\n当前并发线程数: %d\n操作可能耗时较长。", count, executionThreadCount),
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                progressBox.setVisible(true);

                // 修复：每次开始新任务前先解绑，避免 "A bound value cannot be set" 异常
                mainProgressBar.progressProperty().unbind();

                mainProgressBar.setProgress(0);
                progressLabel.setText("初始化线程池...");

                Task<Void> executeTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        List<ChangeRecord> todos = changePreviewList.stream()
                                .filter(c -> c.isChanged() && c.getStatus() != ExecStatus.SKIPPED)
                                .collect(Collectors.toList());

                        int total = todos.size();
                        AtomicInteger current = new AtomicInteger(0);
                        AtomicInteger successCount = new AtomicInteger(0);

                        ExecutorService executor = Executors.newFixedThreadPool(executionThreadCount);

                        for (ChangeRecord rec : todos) {
                            executor.submit(() -> {
                                try {
                                    updateRecordStatus(rec, ExecStatus.RUNNING);
                                    performFileOperation(rec);
                                    updateRecordStatus(rec, ExecStatus.SUCCESS);
                                    successCount.incrementAndGet();
                                } catch (Exception e) {
                                    updateRecordStatus(rec, ExecStatus.FAILED);
                                    String msg = "失败 [" + rec.getOriginalName() + "]: " + e.getMessage();
                                    Platform.runLater(() -> logArea.appendText(msg + "\n"));
                                    e.printStackTrace();
                                } finally {
                                    int done = current.incrementAndGet();
                                    updateProgress(done, total);
                                    Platform.runLater(() -> progressLabel.setText(String.format("进度: %d / %d", done, total)));
                                }
                            });
                        }

                        executor.shutdown();
                        while (!executor.isTerminated()) {
                            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                        }

                        Platform.runLater(() -> {
                            log("执行完成。成功: " + successCount.get() + " / " + total);
                            progressLabel.setText("完成");
                        });
                        return null;
                    }
                };

                mainProgressBar.progressProperty().bind(executeTask.progressProperty());
                new Thread(executeTask).start();
            }
        });
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
            default: break;
        }
    }

    private void convertAudioFile(File source, File target, Map<String, String> params) throws IOException {
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .overrideOutputFiles(true);

        FFmpegOutputBuilder outputBuilder = builder.addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"))
                .addExtraArgs("-map_metadata", "0");

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
        } catch (IOException e) { return new ArrayList<>(); }
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }


}
