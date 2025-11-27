package backup;

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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.controlsfx.control.CheckComboBox;

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

public class MusicFileManagerAppV6 extends Application {

    private Stage primaryStage;
    
    // Data Models
    private ObservableList<ChangeRecord> changePreviewList = FXCollections.observableArrayList();
    private ObservableList<String> sourcePathStrings = FXCollections.observableArrayList();
    private List<File> sourceRootDirs = new ArrayList<>();

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

    private List<AppStrategy> strategies = new ArrayList<>();
    
    // 线程池配置 (由策略动态提供，默认单线程)
    private int executionThreadCount = 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 .0 (并发转换加强版)");

        initStrategies();
        Scene scene = new Scene(createMainLayout(), 1400, 950);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initStrategies() {
        strategies.add(new AdvancedRenameStrategy());
        strategies.add(new AudioConverterStrategy()); // 重点优化的策略
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
        
        // 进度区域
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
                "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "dfd", "iso", "jpg", "png", "nfo"
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
            @Override public String toString(AppStrategy object) { return object.getName(); }
            @Override public AppStrategy fromString(String string) { return null; }
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
        
        // 增强的 Cell Factory：支持状态颜色变化
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
                    if ("VIRTUAL_ROOT".equals(item.getNewPath())) { setText("预览根节点"); return; }

                    boolean isDir = item.getFileHandle() != null && item.getFileHandle().isDirectory();
                    Label oldName = new Label(item.getOriginalName());
                    if (isDir) oldName.setStyle("-fx-font-weight: bold;");
                    
                    // 状态指示器
                    Label statusIcon = new Label();
                    switch (item.getStatus()) {
                        case PENDING: statusIcon.setText("⏳"); break;
                        case RUNNING: statusIcon.setText("🔄"); break;
                        case SUCCESS: statusIcon.setText("✅"); statusIcon.setTextFill(Color.GREEN); break;
                        case FAILED:  statusIcon.setText("❌"); statusIcon.setTextFill(Color.RED); break;
                        case SKIPPED: statusIcon.setText("⏭"); statusIcon.setTextFill(Color.GRAY); break;
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
                    
                    // 根据状态设置背景色，实现"实时进度"的视觉效果
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
        if (idx >= 0) { sourceRootDirs.remove(idx); sourcePathStrings.remove(idx); refreshLeftTree(); }
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
        if (sourceRootDirs.isEmpty()) { log("请添加工作目录。"); return; }
        AppStrategy strategy = cbStrategy.getValue();
        if (strategy == null) return;

        // 获取策略建议的线程数
        executionThreadCount = strategy.getPreferredThreadCount();

        int maxDepth = "仅当前目录".equals(cbRecursionMode.getValue()) ? 1 : 
                       "指定目录深度".equals(cbRecursionMode.getValue()) ? spRecursionDepth.getValue() : Integer.MAX_VALUE;

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
        task.setOnSucceeded(e -> { previewTree.setRoot(task.getValue()); log("预览完成。"); });
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
            // 如果已经被标记为 SKIPPED (因为文件已存在)，我们还是添加到树里显示，但状态为SKIPPED
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
                mainProgressBar.setProgress(0);
                progressLabel.setText("初始化线程池...");
                
                // 启动后台任务
                Task<Void> executeTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        List<ChangeRecord> todos = changePreviewList.stream()
                                .filter(c -> c.isChanged() && c.getStatus() != ExecStatus.SKIPPED)
                                .collect(Collectors.toList());

                        int total = todos.size();
                        AtomicInteger current = new AtomicInteger(0);
                        AtomicInteger successCount = new AtomicInteger(0);
                        
                        // 创建线程池
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
                        // 等待所有任务完成，或者每隔一段时间检查一次
                        while (!executor.isTerminated()) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }

                        Platform.runLater(() -> {
                            log("执行完成。成功: " + successCount.get() + " / " + total);
                            progressLabel.setText("完成");
                            // 这里不立刻清除 Tree，以便用户查看成功/失败状态
                            // previewTree.setRoot(null);
                        });
                        return null;
                    }
                };
                
                mainProgressBar.progressProperty().bind(executeTask.progressProperty());
                new Thread(executeTask).start();
            }
        });
    }
    
    // 触发 TreeView 刷新
    private void updateRecordStatus(ChangeRecord rec, ExecStatus status) {
        rec.setStatus(status);
        // 通过 Platform.runLater 强制刷新 UI，这里使用了简单的 trick：触发生性
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
                if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
                convertAudioFile(source, target, rec.getExtraParams());
                break;
            default: break;
        }
    }

    private void convertAudioFile(File source, File target, Map<String, String> params) throws IOException {
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"))
                .addExtraArgs("-map_metadata", "0")
                .done();
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

    private void log(String msg) { Platform.runLater(() -> logArea.appendText(msg + "\n")); }

    // --- Models ---
    
    enum OperationType { NONE, RENAME, MOVE, CONVERT }
    enum ScanTarget { FILES_ONLY, FOLDERS_ONLY, ALL }
    enum ExecStatus { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED } // 新增状态枚举

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ChangeRecord {
        private String originalName;
        private String newName;
        private File fileHandle;
        private boolean changed;
        private String newPath; 
        private OperationType opType;
        private Map<String, String> extraParams;
        private ExecStatus status = ExecStatus.PENDING; // 默认状态

        public ChangeRecord(String oName, String nName, File f, boolean c, String nPath, OperationType type) {
            this(oName, nName, f, c, nPath, type, new HashMap<>(), ExecStatus.PENDING);
        }
        public ChangeRecord(String originalName, String newName, File fileHandle, boolean changed, String newPath, boolean isMove) {
            this.originalName = originalName;
            this.newName = newName;
            this.fileHandle = fileHandle;
            this.changed = changed;
            this.newPath = newPath;
            if (isMove) {
                this.opType = OperationType.MOVE;
            }else{
                this.opType = OperationType.RENAME;
            }
        }

        public String getOriginalPath() { return fileHandle != null ? fileHandle.getAbsolutePath() : ""; }
        @Override public String toString() { return originalName; }
    }

    abstract static class AppStrategy {
        public abstract String getName();
        public abstract Node getConfigNode();
        public abstract ScanTarget getTargetType();
        // 策略可以建议并发线程数
        public int getPreferredThreadCount() { return 1; }
        public abstract List<ChangeRecord> analyze(List<File> files, List<File> rootDirs);
    }

    // --- Strategies ---

    // 1. 音频格式转换策略 (优化后：并发+路径+跳过)
    class AudioConverterStrategy extends AppStrategy {
        private final JFXComboBox<String> cbTargetFormat;
        private final JFXComboBox<String> cbOutputDirMode; // 路径模式
        private final TextField txtRelativePath; // 相对路径参数
        private final CheckBox chkSkipExisting; // 跳过已存在
        private final Spinner<Integer> spThreads; // 线程数
        private final TextField txtFFmpegPath;
        
        public AudioConverterStrategy() {
            cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "MP3"));
            cbTargetFormat.getSelectionModel().select("FLAC");
            
            cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "原目录 (Source)", 
                "子目录 (Sub-folder)", 
                "同级目录 (Sibling folder)", 
                "自定义相对路径"
            ));
            cbOutputDirMode.getSelectionModel().select(0);
            
            txtRelativePath = new TextField("converted");
            txtRelativePath.setPromptText("例如: converted 或 ../wav");
            // 只有非"原目录"时才显示输入框
            txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));
            
            chkSkipExisting = new CheckBox("如果目标文件存在则跳过 (Skip Existing)");
            chkSkipExisting.setSelected(true);
            
            int cores = Runtime.getRuntime().availableProcessors();
            spThreads = new Spinner<>(1, 32, Math.max(1, cores / 2)); // 默认使用一半核心
            spThreads.setTooltip(new Tooltip("并行转换的线程数，建议不超过CPU核心数"));

            txtFFmpegPath = new TextField("ffmpeg");
        }

        @Override public String getName() { return "音频格式转换 (高并发版)"; }
        @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }
        @Override public int getPreferredThreadCount() { return spThreads.getValue(); }

        @Override
        public Node getConfigNode() {
            VBox box = new VBox(10);
            HBox ffmpegBox = new HBox(10, new Label("FFmpeg:"), txtFFmpegPath);
            ffmpegBox.setAlignment(Pos.CENTER_LEFT);
            JFXButton btnPick = new JFXButton("浏览");
            btnPick.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                File f = fc.showOpenDialog(null);
                if(f!=null) txtFFmpegPath.setText(f.getAbsolutePath());
            });
            ffmpegBox.getChildren().add(btnPick);

            box.getChildren().addAll(
                new Label("目标格式:"), cbTargetFormat,
                new Label("输出位置:"), new HBox(10, cbOutputDirMode, txtRelativePath),
                new Separator(),
                new Label("性能与安全:"), 
                new HBox(15, new Label("并发线程数:"), spThreads),
                chkSkipExisting,
                new Separator(),
                ffmpegBox
            );
            return box;
        }

        @Override
        public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<ChangeRecord> records = new ArrayList<>();
            String format = cbTargetFormat.getValue().toLowerCase();
            String mode = cbOutputDirMode.getValue();
            String relPath = txtRelativePath.getText();
            boolean skipExisting = chkSkipExisting.isSelected();
            String ffmpeg = txtFFmpegPath.getText();

            Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dfd", "dts", "ape", "wav", "flac", "m4a"));

            for (File f : files) {
                String name = f.getName().toLowerCase();
                String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";
                
                if (!sourceExts.contains(ext)) continue; 
                // 如果转成同格式且目录相同，跳过
                if (ext.equals(format) && mode.startsWith("原目录")) continue;

                String newName = f.getName().substring(0, f.getName().lastIndexOf(".")) + "." + format;
                
                // 计算目标路径
                File parent = f.getParentFile();
                File targetFile = null;
                
                if (mode.startsWith("原目录")) {
                    targetFile = new File(parent, newName);
                } else if (mode.startsWith("子目录")) {
                    targetFile = new File(new File(parent, relPath.isEmpty() ? "converted" : relPath), newName);
                } else if (mode.startsWith("同级目录")) {
                    // 同级目录逻辑: ../folder_name
                    targetFile = new File(new File(parent.getParentFile(), relPath.isEmpty() ? parent.getName() + "_" + format : relPath), newName);
                } else { // 自定义相对路径
                    targetFile = new File(new File(parent, relPath), newName);
                }
                
                // 检查是否跳过
                ExecStatus status = ExecStatus.PENDING;
                if (skipExisting && targetFile.exists()) {
                    status = ExecStatus.SKIPPED;
                    // 如果你不想在列表中看到它们，可以 continue。
                    // 但通常显示"已跳过"比较好，让用户知道扫描到了。
                }

                Map<String, String> params = new HashMap<>();
                params.put("format", format);
                params.put("ffmpegPath", ffmpeg);
                if ("mp3".equals(format)) params.put("codec", "libmp3lame");
                else if ("flac".equals(format)) params.put("codec", "flac");
                else if ("wav".equals(format)) params.put("codec", "pcm_s24le");

                ChangeRecord rec = new ChangeRecord(f.getName(), newName, f, true, targetFile.getAbsolutePath(), OperationType.CONVERT, params, ExecStatus.PENDING);
                rec.setStatus(status);
                records.add(rec);
            }
            return records;
        }
    }

    // --- Strategy Implementations ---

    class AdvancedRenameStrategy extends MusicFileManagerAppV6.AppStrategy {
        private final JFXComboBox<String> cbMode;
        private final JFXComboBox<String> cbTarget;
        private final TextField txtParam1;
        private final TextField txtParam2;

        public AdvancedRenameStrategy() {
            cbMode = new JFXComboBox<>(FXCollections.observableArrayList(
                    "添加前缀", "添加后缀", "字符替换", "扩展名转小写", "去除空格"
            ));
            cbMode.getSelectionModel().select(0);

            cbTarget = new JFXComboBox<>(FXCollections.observableArrayList(
                    "仅处理文件", "仅处理文件夹", "全部处理"
            ));
            cbTarget.getSelectionModel().select(0);

            txtParam1 = new TextField();
            txtParam1.setPromptText("输入内容...");
            txtParam2 = new TextField();
            txtParam2.setPromptText("替换为...");

            cbMode.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                txtParam1.setDisable(val.contains("小写") || val.contains("去除空格"));
                txtParam2.setVisible(val.contains("替换"));
            });
            txtParam2.setVisible(false);
        }

        @Override public String getName() { return "高级批量重命名"; }

        @Override public MusicFileManagerAppV6.ScanTarget getTargetType() {
            String t = cbTarget.getValue();
            if ("仅处理文件".equals(t)) return MusicFileManagerAppV6.ScanTarget.FILES_ONLY;
            if ("仅处理文件夹".equals(t)) return MusicFileManagerAppV6.ScanTarget.FOLDERS_ONLY;
            return MusicFileManagerAppV6.ScanTarget.ALL;
        }

        @Override
        public Node getConfigNode() {
            VBox box = new VBox(10);
            box.getChildren().addAll(
                    new Label("操作对象:"), cbTarget,
                    new Label("方式:"), cbMode,
                    new Label("参数:"), txtParam1, txtParam2
            );
            return box;
        }

        @Override
        public List<MusicFileManagerAppV6.ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<MusicFileManagerAppV6.ChangeRecord> records = new ArrayList<>();
            String mode = cbMode.getValue();
            String p1 = txtParam1.getText();
            String p2 = txtParam2.getText();
            MusicFileManagerAppV6.ScanTarget target = getTargetType();

            for (File f : files) {
                if (rootDirs.contains(f)) continue;

                boolean isDir = f.isDirectory();
                if (target == MusicFileManagerAppV6.ScanTarget.FILES_ONLY && isDir) continue;
                if (target == MusicFileManagerAppV6.ScanTarget.FOLDERS_ONLY && !isDir) continue;

                String oldName = f.getName();
                String newName = oldName;

                if ("添加前缀".equals(mode) && !p1.isEmpty()) newName = p1 + oldName;
                else if ("添加后缀".equals(mode) && !p1.isEmpty()) {
                    if (isDir) {
                        newName = oldName + p1;
                    } else {
                        int dot = oldName.lastIndexOf(".");
                        if (dot > 0) newName = oldName.substring(0, dot) + p1 + oldName.substring(dot);
                        else newName = oldName + p1;
                    }
                }
                else if ("字符替换".equals(mode) && !p1.isEmpty()) newName = oldName.replace(p1, p2 == null ? "" : p2);
                else if ("扩展名转小写".equals(mode) && !isDir) {
                    int dot = oldName.lastIndexOf(".");
                    if (dot > 0) newName = oldName.substring(0, dot) + oldName.substring(dot).toLowerCase();
                }
                else if ("去除空格".equals(mode)) newName = oldName.replace(" ", "");

                String newPath = f.getParent() + File.separator + newName;
                records.add(new MusicFileManagerAppV6.ChangeRecord(oldName, newName, f, !oldName.equals(newName), newPath, false));
            }
            return records;
        }
    }

    class FileMigrateStrategy extends MusicFileManagerAppV6.AppStrategy {
        private final TextField txtTargetDir;
        private final JFXButton btnPickTarget;

        public FileMigrateStrategy() {
            txtTargetDir = new TextField();
            txtTargetDir.setPromptText("默认：原处创建子文件夹");
            txtTargetDir.setEditable(false);
            btnPickTarget = new JFXButton("...");
            btnPickTarget.setOnAction(e -> {
                DirectoryChooser dc = new DirectoryChooser();
                File f = dc.showDialog(null);
                if (f != null) txtTargetDir.setText(f.getAbsolutePath());
            });
        }

        @Override public String getName() { return "按歌手归档 (文件迁移)"; }
        @Override public MusicFileManagerAppV6.ScanTarget getTargetType() { return MusicFileManagerAppV6.ScanTarget.FILES_ONLY; }

        @Override
        public Node getConfigNode() {
            return new VBox(10, new Label("归档根目录:"), new HBox(5, txtTargetDir, btnPickTarget));
        }

        @Override
        public List<MusicFileManagerAppV6.ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<MusicFileManagerAppV6.ChangeRecord> records = new ArrayList<>();
            String targetBase = txtTargetDir.getText();
            if (targetBase == null || targetBase.trim().isEmpty()) {
                targetBase = rootDirs.isEmpty() ? "" : rootDirs.get(0).getAbsolutePath();
            }

            for (File f : files) {
                String artist = "其他";
                if (f.getName().contains("陈粒")) artist = "陈粒";
                if (f.getName().contains("周杰伦")) artist = "周杰伦";

                String newDirStr = targetBase + File.separator + artist;
                String newPath = newDirStr + File.separator + f.getName();
                boolean changed = !f.getParentFile().getAbsolutePath().equals(newDirStr);
                records.add(new MusicFileManagerAppV6.ChangeRecord(f.getName(), f.getName(), f, changed, newPath, true));
            }
            return records;
        }
    }

    class AlbumDirNormalizeStrategy extends MusicFileManagerAppV6.AppStrategy {
        @Override public String getName() { return "专辑目录标准化"; }
        @Override public Node getConfigNode() { return new Label("自动识别底层文件夹内的歌曲信息并重命名文件夹。"); }
        @Override public MusicFileManagerAppV6.ScanTarget getTargetType() { return MusicFileManagerAppV6.ScanTarget.FILES_ONLY; }

        @Override
        public List<MusicFileManagerAppV6.ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<MusicFileManagerAppV6.ChangeRecord> records = new ArrayList<>();
            Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

            for (Map.Entry<File, List<File>> entry : folderGroups.entrySet()) {
                File folder = entry.getKey();
                if (rootDirs.contains(folder)) continue;

                String artist = "Unknown";
                String album = folder.getName();
                String year = "2000";
                String type = "MP3";

                if (entry.getValue().stream().anyMatch(f -> f.getName().endsWith(".flac"))) type = "FLAC";
                if (folder.getName().contains("U87")) { artist = "陈奕迅"; year="2005"; album="U87"; }

                String newFolderName = String.format("%s - %s - %s - %s", artist, year, album, type);

                if (!folder.getName().equals(newFolderName)) {
                    String newPath = folder.getParent() + File.separator + newFolderName;
                    records.add(new MusicFileManagerAppV6.ChangeRecord(folder.getName(), newFolderName, folder, true, newPath, false));
                }
            }
            return records;
        }
    }

    class TrackNumberStrategy extends MusicFileManagerAppV6.AppStrategy {
        @Override public String getName() { return "歌曲序号补全"; }
        @Override public Node getConfigNode() { return null; }
        @Override public MusicFileManagerAppV6.ScanTarget getTargetType() { return MusicFileManagerAppV6.ScanTarget.FILES_ONLY; }

        @Override
        public List<MusicFileManagerAppV6.ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<MusicFileManagerAppV6.ChangeRecord> records = new ArrayList<>();
            Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

            for (List<File> folderFiles : folderGroups.values()) {
                folderFiles.sort(Comparator.comparing(File::getName));
                for (int i = 0; i < folderFiles.size(); i++) {
                    File f = folderFiles.get(i);
                    String oldName = f.getName();
                    String ext = oldName.contains(".") ? oldName.substring(oldName.lastIndexOf(".")) : "";

                    String title = oldName.replace(ext, "").replaceAll("^\\d+[.\\s-]*", "").trim();
                    String newName = String.format("%02d. %s%s", (i + 1), title, ext);

                    String newPath = f.getParent() + File.separator + newName;
                    records.add(new MusicFileManagerAppV6.ChangeRecord(oldName, newName, f, !oldName.equals(newName), newPath, false));
                }
            }
            return records;
        }
    }
}