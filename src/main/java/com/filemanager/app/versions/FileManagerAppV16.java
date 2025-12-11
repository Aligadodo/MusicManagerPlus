package com.filemanager.app.versions;

import com.filemanager.app.IManagerAppInterface;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.RuleCondition;
import com.filemanager.strategy.*;
import com.filemanager.type.ConditionType;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import javafx.util.Duration;
import org.controlsfx.control.CheckComboBox;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
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

/**
 * Echo Music File Manager v16.0 (Aero Glass Edition)
 * * 设计理念：
 * 1. Glassmorphism: 使用半透明层+模糊背景模拟毛玻璃质感。
 * 2. Component-based: 界面构建逻辑拆分为独立的 View 类。
 * 3. Fluid UX: 使用侧边栏导航，操作路径更清晰。
 */
public class FileManagerAppV16 extends Application implements IManagerAppInterface {

    // --- 核心数据 ---
    private final ObservableList<File> sourceRoots = FXCollections.observableArrayList();
    private final ObservableList<AppStrategy> pipelineStrategies = FXCollections.observableArrayList();
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final Properties appProps = new Properties();
    private final File lastConfigFile = new File(System.getProperty("user.home"), ".echo_music_manager_v18.config");
    private final List<ChangeRecord> changePreviewList = new ArrayList<>();
    // --- 外观配置 ---
    private static final ThemeConfig currentTheme = new ThemeConfig();
    private final StyleFactory styles = new StyleFactory();
    private String bgImagePath = "";
    private final List<AppStrategy> strategyPrototypes = new ArrayList<>();
    private Stage primaryStage;
    private List<ChangeRecord> fullChangeList = new ArrayList<>();
    private ListView<AppStrategy> pipelineListView = new ListView<>();
    // --- UI 容器 ---
    private StackPane rootContainer;
    private ImageView backgroundImageView;
    private Region backgroundOverlay;
    private BorderPane mainContent;
    private StackPane contentArea; // 用于视图切换的区域
    // --- Views ---
    private ComposeView composeView;
    private PreviewView previewView;
    private LogView logView;
    // --- Global Controls (需在 initGlobalControls 中初始化) ---
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;
    // --- Tab 2 Components (需在 initGlobalControls 中初始化) ---
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label progressLabel, etaLabel, statsLabel;
    private JFXTextField txtSearchFilter;
    private JFXComboBox<String> cbStatusFilter;
    private JFXButton btnExecute, btnStop;
    private JFXCheckBox chkHideUnchanged;
    private VBox progressBox;
    // --- Tab 3 Components ---
    private TextArea logArea;
    private JFXCheckBox chkSaveLog;
    // --- 任务状态 ---
    private PrintWriter fileLogger;
    private ExecutorService executorService;
    private Task<?> currentTask;
    private volatile boolean isTaskRunning = false;
    private AnimationTimer uiUpdater;
    private Node btnGoPreview;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo Music Manager - Aero Edition");

        // 1. 初始化策略
        initStrategyPrototypes();

        // 2. 初始化全局控件 (必须在构建 UI 前完成，防止 NPE)
        initGlobalControls();

        // 3. 加载配置
        backgroundOverlay = new Region();
        loadGlobalConfig(lastConfigFile);

        // 4. 构建 UI
        rootContainer = new StackPane();

        backgroundImageView = new ImageView();
        backgroundImageView.setPreserveRatio(false);
        backgroundImageView.fitWidthProperty().bind(rootContainer.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootContainer.heightProperty());



        mainContent = createMainLayout();

        rootContainer.getChildren().addAll(backgroundImageView, backgroundOverlay, mainContent);

        // 5. 应用外观
        applyAppearance();

        Scene scene = new Scene(rootContainer, 1440, 900);
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

    /**
     * 初始化所有跨视图使用的全局控件，确保不为 null
     */
    private void initGlobalControls() {
        // Filter Controls
        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("仅当前目录", "递归所有子目录", "指定目录深度"));
        cbRecursionMode.getSelectionModel().select(1);
        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        ccbFileTypes = new CheckComboBox<>(FXCollections.observableArrayList(
                "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "iso", "jpg", "png", "nfo", "cue",
                "rar", "zip", "7z", "tar", "gz", "bz2"));
        ccbFileTypes.getCheckModel().checkAll();

        // Preview Controls
        txtSearchFilter = new JFXTextField();
        cbStatusFilter = new JFXComboBox<>(FXCollections.observableArrayList("全部", "执行中", "成功", "失败"));
        cbStatusFilter.getSelectionModel().select(0);
        chkHideUnchanged = new JFXCheckBox("仅显示变更");
        chkHideUnchanged.setSelected(true);

        // Progress Controls
        progressLabel = styles.createNormalLabel("就绪");
        etaLabel = styles.createNormalLabel("");
        statsLabel = styles.createNormalLabel("总计: 0");
        mainProgressBar = new ProgressBar(0);
        progressBox = new VBox();

        // Buttons (Initially disabled)
        btnGoPreview = styles.createActionButton("执行预览", "#27ae60", this::runPipelineExecution);
        btnGoPreview.setDisable(true);
        btnExecute = styles.createActionButton("执行变更", "#27ae60", this::runPipelineExecution);
        btnExecute.setDisable(true);
        btnStop = styles.createActionButton("停止", "#e74c3c", this::forceStop);
        btnStop.setDisable(true);

        // Tables & Logs
        previewTable = new TreeTableView<>();
        setupPreviewColumns();
        logArea = new TextArea();
        chkSaveLog = new JFXCheckBox("保存日志");

        // 初始化视图对象
        composeView = new ComposeView(this);
        previewView = new PreviewView();
        logView = new LogView();
    }

    // ==================== UI Layout ====================

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // Left: Navigation Sidebar
        VBox sideMenu = createSideMenu();
        root.setLeft(sideMenu);

        // Center: Content Area
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        // 默认显示编排页
        contentArea.getChildren().add(composeView.getViewNode());

        root.setCenter(contentArea);

        return root;
    }

    private VBox createSideMenu() {
        VBox menu = styles.createGlassPane();
        menu.setPrefWidth(240);
        menu.setPadding(new Insets(30, 20, 30, 20));
        menu.setSpacing(15);
        menu.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        Label logo = styles.createHeader("ECHO MANAGER");
        logo.setFont(Font.font("Segoe UI", FontWeight.BLACK, 24));
        logo.setTextFill(Color.web(currentTheme.accentColor));

        VBox navBox = new VBox(10);
        navBox.getChildren().addAll(
                createNavButton("任务编排", "🔧", e -> switchView(composeView.getViewNode())),
                createNavButton("预览 & 执行", "▶", e -> switchView(previewView.getViewNode())),
                createNavButton("运行日志", "📝", e -> switchView(logView.getViewNode()))
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox bottomActions = new VBox(10);
        bottomActions.getChildren().addAll(
                createNavButton("界面外观", "🎨", e -> showAppearanceDialog()),
                createNavButton("保存配置", "💾", e -> saveConfigAction()),
                createNavButton("加载配置", "📂", e -> loadConfigAction())
        );

        menu.getChildren().addAll(logo, new Separator(), navBox, spacer, bottomActions);
        return menu;
    }

    private JFXButton createNavButton(String text, String icon, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        JFXButton btn = new JFXButton(icon + "  " + text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 15, 12, 15));
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setTextFill(Color.web("#555"));
        btn.setOnAction(handler);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(52, 152, 219, 0.1); -fx-background-radius: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent;"));
        return btn;
    }

    private void switchView(Node node) {
        if (!contentArea.getChildren().contains(node)) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);
            FadeTransition ft = new FadeTransition(Duration.millis(300), node);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    // ==================== Views ====================

    private Node createGlobalFiltersUI() {
        VBox box = new VBox(10);
        box.getChildren().addAll(
                styles.createNormalLabel("递归模式:"), cbRecursionMode, spRecursionDepth,
                styles.createNormalLabel("文件扩展名:"), ccbFileTypes
        );
        return box;
    }

    public void runPipelineAnalysis() {
        if (sourceRoots.isEmpty()) {
            showToast("请先添加源目录！");
            return;
        }
        if (pipelineStrategies.isEmpty()) {
            showToast("请添加操作步骤！");
            return;
        }
        if (isTaskRunning) return;

        switchView(previewView.getViewNode());
        resetProgressUI("初始化扫描...", false);
        changePreviewList.clear();
        previewTable.setRoot(null);

        for (AppStrategy s : pipelineStrategies) s.captureParams();
        int maxDepth = "仅当前目录".equals(cbRecursionMode.getValue()) ? 1 : ("递归所有子目录".equals(cbRecursionMode.getValue()) ? Integer.MAX_VALUE : spRecursionDepth.getValue());
        List<String> exts = new ArrayList<>(ccbFileTypes.getCheckModel().getCheckedItems());

        Task<List<ChangeRecord>> task = new Task<List<ChangeRecord>>() {
            @Override
            protected List<ChangeRecord> call() throws Exception {
                long t0 = System.currentTimeMillis();
                updateMessage("扫描源文件...");
                List<File> initialFiles = new ArrayList<>();
                for (File r : sourceRoots) {
                    if (isCancelled()) break;
                    initialFiles.addAll(scanFilesRobust(r, maxDepth, exts, this::updateMessage));
                }
                if (isCancelled()) return null;
                log("扫描完成，共 " + initialFiles.size() + " 个文件。");

                List<ChangeRecord> currentRecords = initialFiles.stream()
                        .map(f -> new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE))
                        .collect(Collectors.toList());

                for (int i = 0; i < pipelineStrategies.size(); i++) {
                    if (isCancelled()) break;
                    AppStrategy strategy = pipelineStrategies.get(i);
                    updateMessage("分析步骤 " + (i + 1) + ": " + strategy.getName());
                    List<ChangeRecord> stepResults = strategy.analyze(currentRecords, sourceRoots, (p, m) -> updateProgress(p, 1.0));
                    Map<String, ChangeRecord> resultMap = stepResults.stream().collect(Collectors.toMap(r -> r.getFileHandle().getAbsolutePath(), r -> r, (o, n) -> n));
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
            finishTaskUI("预览完成");
            btnExecute.setDisable(count == 0);
            updateStats(0);
        });
        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    /**
     * 执行流水线
     * 优化：增加排序逻辑，确保文件夹操作的安全性
     */
    private void runPipelineExecution() {
        long count = fullChangeList.stream().filter(ChangeRecord::isChanged).count();
        if (count == 0) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定执行 " + count + " 个变更吗？", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        resetProgressUI("正在执行...", true);
        if(chkSaveLog.isSelected()) initFileLogger();

        Task<Void> task = new Task<Void>() {
            @Override protected Void call() throws Exception {
                // [优化] 按路径长度倒序排序 (Deepest First)
                // 这是一个关键的保护措施：确保先重命名/移动/删除子文件，再处理父文件夹。
                // 避免因为父文件夹先被重命名，导致子文件路径失效的问题。
                List<ChangeRecord> todos = fullChangeList.stream()
                        .filter(ChangeRecord::isChanged)
                        .sorted((r1, r2) -> Integer.compare(r2.getNewPath().length(), r1.getNewPath().length()))
                        .collect(Collectors.toList());

                int total = todos.size();
                AtomicInteger curr = new AtomicInteger(0);
                AtomicInteger succ = new AtomicInteger(0);
                long startT = System.currentTimeMillis();

                executorService = Executors.newFixedThreadPool(4);

                for(ChangeRecord rec : todos) {
                    if(isCancelled()) break;
                    executorService.submit(() -> {
                        try {
                            Platform.runLater(()->rec.setStatus(ExecStatus.RUNNING));
                            AppStrategy s = findStrategyForOp(rec.getOpType());
                            if(s!=null) {
                                s.execute(rec);
                                Platform.runLater(()->rec.setStatus(ExecStatus.SUCCESS));
                                succ.incrementAndGet();
                                logAndFile("成功: "+rec.getNewName());
                            } else {
                                Platform.runLater(()->rec.setStatus(ExecStatus.SKIPPED));
                            }
                        } catch(Exception e) {
                            Platform.runLater(()->rec.setStatus(ExecStatus.FAILED));
                            logAndFile("失败: "+e.getMessage());
                        } finally {
                            int c = curr.incrementAndGet();
                            updateProgress(c, total);
                            if(c%10==0) Platform.runLater(()->{
                                updateStats(System.currentTimeMillis()-startT);
                                previewTable.refresh();
                            });
                        }
                    });
                }
                executorService.shutdown();
                while(!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) { if(isCancelled()) { executorService.shutdownNow(); break; } }
                return null;
            }
        };
        task.setOnSucceeded(e -> { finishTaskUI("执行完成"); closeFileLogger(); btnExecute.setDisable(false); });
        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    // [新增] 通用：列表项移动辅助方法
    private <T> void moveListItem(ObservableList<T> list, int index, int direction) {
        int newIndex = index + direction;
        if (newIndex >= 0 && newIndex < list.size()) {
            Collections.swap(list, index, newIndex);
            invalidatePreview("列表顺序变更");
        }
    }



    private void refreshPreviewTableFilter() {
        if (fullChangeList.isEmpty()) return;
        String search = txtSearchFilter.getText() != null ? txtSearchFilter.getText().toLowerCase() : "";
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
                    boolean sm = true;
                    if ("执行中".equals(status)) sm = r.getStatus() == ExecStatus.RUNNING;
                    else if ("成功".equals(status)) sm = r.getStatus() == ExecStatus.SUCCESS;
                    else if ("失败".equals(status)) sm = r.getStatus() == ExecStatus.FAILED;
                    if (!sm) continue;
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

    // ==================== 3. Core Logic ====================

    private void setupPreviewColumns() {
        TreeTableColumn<ChangeRecord, String> c1 = new TreeTableColumn<>("源文件");
        c1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getOriginalName()));
        c1.setPrefWidth(250);
        TreeTableColumn<ChangeRecord, String> cS = new TreeTableColumn<>("大小");
        cS.setCellValueFactory(p -> new SimpleStringProperty(formatFileSize(p.getValue().getValue().getFileHandle())));
        cS.setPrefWidth(80);
        TreeTableColumn<ChangeRecord, String> c2 = new TreeTableColumn<>("目标");
        c2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewName()));
        c2.setPrefWidth(250);
        c2.setCellFactory(c -> new TreeTableCell<ChangeRecord, String>() {
            @Override
            protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                setText(i);
                try {
                    if (getTreeTableRow().getItem() != null && (i != null && !i.equals(getTreeTableRow().getItem().getOriginalName())))
                        setTextFill(Color.web("#27ae60"));
                    else setTextFill(Color.BLACK);
                } catch (Exception e1) {
                    setTextFill(Color.BLACK);
                }
            }
        });
        TreeTableColumn<ChangeRecord, String> c3 = new TreeTableColumn<>("状态");
        c3.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getStatus().toString()));
        c3.setPrefWidth(80);
        TreeTableColumn<ChangeRecord, String> c4 = new TreeTableColumn<>("路径");
        c4.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewPath()));
        c4.setPrefWidth(350);
        previewTable.getColumns().setAll(c1, cS, c2, c3, c4);
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
        btnExecute.setDisable(false);
    }

    private void handleTaskLifecycle(Task<?> t) {
        currentTask = t;
        progressLabel.textProperty().bind(t.messageProperty());
        mainProgressBar.progressProperty().bind(t.progressProperty());
        t.setOnFailed(e -> {
            finishTaskUI("出错");
            log("❌ 失败: " + e.getSource().getException());
            closeFileLogger();
        });
        t.setOnCancelled(e -> {
            finishTaskUI("已取消");
            closeFileLogger();
        });
    }

    private void forceStop() {
        if (isTaskRunning) {
            isTaskRunning = false;
            if (currentTask != null) currentTask.cancel();
            if (executorService != null) executorService.shutdownNow();
            log("🛑 强制停止");
            finishTaskUI("已停止");
        }
    }

    /**
     * 强化的扫描逻辑
     * 修复：确保文件夹被包含在扫描结果中，以便支持文件夹重命名/删除等策略
     */
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

    // --- Appearance & Config ---
    private void showAppearanceDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("界面设置");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        ColorPicker cp = new ColorPicker(Color.web(currentTheme.accentColor));
        Slider sl = new Slider(0.1, 1.0, currentTheme.glassOpacity);
        CheckBox chk = new CheckBox("Dark Mode");
        chk.setSelected(currentTheme.isDarkBackground);
        g.add(styles.createNormalLabel("主色调:"), 0, 0);
        g.add(cp, 1, 0);
        g.add(styles.createNormalLabel("透明度:"), 0, 1);
        g.add(sl, 1, 1);
        g.add(chk, 1, 2);
        dialog.getDialogPane().setContent(g);
        dialog.setResultConverter(b -> b);
        dialog.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                currentTheme.accentColor = toHexString(cp.getValue());
                currentTheme.glassOpacity = sl.getValue();
                currentTheme.isDarkBackground = chk.isSelected();
                applyAppearance();
            }
        });
    }

    private void applyAppearance() {
        backgroundOverlay.setStyle("-fx-background-color: rgba(" + (currentTheme.isDarkBackground ? "0,0,0" : "255,255,255") + ", " + (1 - currentTheme.glassOpacity) + ");");
        if (!bgImagePath.isEmpty()) {
            try {
                backgroundImageView.setImage(new Image(new FileInputStream(bgImagePath)));
            } catch (Exception e) {
            }
        }
        if (pipelineListView != null) pipelineListView.refresh();
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    // --- Utils ---
    private void startLogUpdater() {
        uiUpdater = new AnimationTimer() {
            @Override
            public void handle(long n) {
                String s;
                while ((s = logQueue.poll()) != null) if (logArea != null) logArea.appendText(s + "\n");
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

    public void invalidatePreview(String r) {
        if (!fullChangeList.isEmpty()) {
            fullChangeList.clear();
            previewTable.setRoot(null);
            log(r + ", 需重新预览");
        }
        btnExecute.setDisable(true);
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

    private String formatFileSize(File file) {
        if (file == null) {
            return "-";
        }
        long s = file.length();
        if (s <= 0) return "0";
        final String[] u = {"B", "KB", "MB", "GB"};
        int d = (int) (Math.log10(s) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(s / Math.pow(1024, d)) + " " + u[d];
    }

    private VBox styledHeader(String t, String s) {
        VBox v = new VBox(2);
        Label l1 = styles.createHeader(t);
        Label l2 = styles.createInfoLabel(s);
        v.getChildren().addAll(l1, l2);
        return v;
    }

    private JFXButton createButton(String t, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        return styles.createActionButton(t, null, () -> h.handle(null));
    }

    private void updateStats(long ms) {
        long t = fullChangeList.size(), c = fullChangeList.stream().filter(ChangeRecord::isChanged).count(), s = fullChangeList.stream().filter(r -> r.getStatus() == ExecStatus.SUCCESS).count();
        String tm = ms > 0 ? String.format("%.1fs", ms / 1000.0) : "-";
        Platform.runLater(() -> statsLabel.setText(String.format("总:%d 变:%d 成:%d 耗时:%s", t, c, s, tm)));
    }

    // --- Config IO ---
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

    // [修复 1] 保存配置：在写入前清理旧的流水线数据，防止脏数据残留
    private void saveGlobalConfig(File f) {
        try (FileOutputStream os = new FileOutputStream(f)) {
            appProps.setProperty("g_recMode", String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
            appProps.setProperty("g_recDepth", String.valueOf(spRecursionDepth.getValue()));

            if (!sourceRoots.isEmpty()) {
                String paths = sourceRoots.stream().map(File::getAbsolutePath).collect(Collectors.joining("||"));
                appProps.setProperty("g_sources", paths);
            }

            // 外观配置保存
            appProps.setProperty("ui_accent_color", currentTheme.accentColor);
            appProps.setProperty("ui_text_color", currentTheme.textColor);
            appProps.setProperty("ui_glass_opacity", String.valueOf(currentTheme.glassOpacity));
            appProps.setProperty("ui_dark_bg", String.valueOf(currentTheme.isDarkBackground));
            appProps.setProperty("ui_bg_image", bgImagePath);

            // 保存流水线配置
            propsSavePipeline(appProps);

            appProps.store(os, "Echo Music Manager Config");
            showToast("配置已保存");
        } catch (Exception e) {
            log("保存失败: " + e.getMessage());
        }
    }

    // [修复 2] 加载配置：加载前清空内存配置，加载后自动选中第一项以触发回显
    private void loadGlobalConfig(File f) {
        if (!f.exists()) return;
        try (FileInputStream is = new FileInputStream(f)) {
            appProps.clear(); // 关键：清空内存中的旧配置，防止污染
            appProps.load(is);

            if (appProps.containsKey("g_recMode"))
                cbRecursionMode.getSelectionModel().select(Integer.parseInt(appProps.getProperty("g_recMode")));
            if (appProps.containsKey("g_recDepth"))
                spRecursionDepth.getValueFactory().setValue(Integer.parseInt(appProps.getProperty("g_recDepth")));

            String paths = appProps.getProperty("g_sources");
            if (paths != null && !paths.isEmpty()) {
                sourceRoots.clear();
                for (String p : paths.split("\\|\\|")) {
                    File file = new File(p);
                    if (file.exists()) sourceRoots.add(file);
                }
            }

            // 加载外观
            if(appProps.containsKey("ui_accent_color")) currentTheme.accentColor = appProps.getProperty("ui_accent_color");
            if(appProps.containsKey("ui_text_color")) currentTheme.textColor = appProps.getProperty("ui_text_color");
            if(appProps.containsKey("ui_glass_opacity")) currentTheme.glassOpacity = Double.parseDouble(appProps.getProperty("ui_glass_opacity"));
            if(appProps.containsKey("ui_dark_bg")) currentTheme.isDarkBackground = Boolean.parseBoolean(appProps.getProperty("ui_dark_bg"));
            if(appProps.containsKey("ui_bg_image")) bgImagePath = appProps.getProperty("ui_bg_image");
            applyAppearance();

            // 加载流水线
            loadPipelineConfig(appProps);

        } catch (Exception e) {
            log("加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // [修复 3] 辅助方法：保存流水线时先清理旧 Key
    private void propsSavePipeline(Properties p) {
        // 清理旧的 pipeline 配置 (以 pl. 开头的)
        Set<String> keys = new HashSet<>(p.stringPropertyNames());
        for (String key : keys) {
            if (key.startsWith("pl.")) {
                p.remove(key);
            }
        }

        p.setProperty("pl.size", String.valueOf(pipelineStrategies.size()));
        for (int i = 0; i < pipelineStrategies.size(); i++) {
            AppStrategy s = pipelineStrategies.get(i);
            Properties sp = new Properties();
            s.saveConfig(sp); // 让策略保存自己的配置

            String pre = "pl." + i + ".";
            p.setProperty(pre + "cls", s.getClass().getName());

            for (String k : sp.stringPropertyNames()) {
                p.setProperty(pre + "p." + k, sp.getProperty(k));
            }

            // 保存前置条件
            int condSize = s.getGlobalConditions().size();
            p.setProperty(pre + "c.size", String.valueOf(condSize));
            for(int j=0; j<condSize; j++) {
                RuleCondition rc = s.getGlobalConditions().get(j);
                p.setProperty(pre + "c." + j + ".type", rc.getType().name());
                p.setProperty(pre + "c." + j + ".val", rc.getValue());
            }
        }
    }

    // [修复 4] 辅助方法：加载流水线并恢复 UI 状态
    private void loadPipelineConfig(Properties p) {
        pipelineStrategies.clear();
        composeView.configContainer.getChildren().clear();

        int s = Integer.parseInt(p.getProperty("pl.size", "0"));
        for (int i = 0; i < s; i++) {
            String pre = "pl." + i + ".";
            String cls = p.getProperty(pre + "cls");
            if (cls == null) continue;

            try {
                // 反射创建策略实例
                Class<?> clazz = Class.forName(cls);
                AppStrategy st = (AppStrategy) clazz.getDeclaredConstructor().newInstance();
                st.setContext(this);

                // 提取并加载策略参数
                Properties sp = new Properties();
                String paramPre = pre + "p.";
                for (String k : p.stringPropertyNames()) {
                    if (k.startsWith(paramPre)) {
                        sp.setProperty(k.substring(paramPre.length()), p.getProperty(k));
                    }
                }
                st.loadConfig(sp); // 此时 UI 组件已创建，loadConfig 会更新 UI 值

                // 加载前置条件
                int cSize = Integer.parseInt(p.getProperty(pre + "c.size", "0"));
                st.getGlobalConditions().clear();
                for(int j=0; j<cSize; j++) {
                    String cPre = pre + "c." + j + ".";
                    ConditionType type = ConditionType.valueOf(p.getProperty(cPre + "type"));
                    String val = p.getProperty(cPre + "val");
                    st.getGlobalConditions().add(new RuleCondition(type, val));
                }

                pipelineStrategies.add(st);
            } catch (Exception e) {
                log("策略加载失败 [" + i + "]: " + e.getMessage());
            }
        }

        // 关键：加载完成后自动选中第一项，触发 UI 刷新，解决“不回显”问题
        if (!pipelineStrategies.isEmpty()) {
            Platform.runLater(() -> {
                pipelineListView.getSelectionModel().select(0);
                composeView.refreshConfig(pipelineStrategies.get(0));
            });
        }
    }

    // --- Strategies ---
    private void initStrategyPrototypes() {
        strategyPrototypes.add(new AdvancedRenameStrategy());
        strategyPrototypes.add(new AudioConverterStrategy());
        strategyPrototypes.add(new FileMigrateStrategy());
        strategyPrototypes.add(new AlbumDirNormalizeStrategy());
        strategyPrototypes.add(new TrackNumberStrategy());
        strategyPrototypes.add(new CueSplitterStrategy());
        strategyPrototypes.add(new MetadataScraperStrategy());
        strategyPrototypes.add(new FileCleanupStrategy());
        strategyPrototypes.add(new FileUnzipStrategy());
    }

    private void addStrategyStep(AppStrategy template) {
        if (template != null) {
            try {
                AppStrategy n = template.getClass().getDeclaredConstructor().newInstance();
                n.setContext(this);
                n.loadConfig(appProps);
                pipelineStrategies.add(n);
                pipelineListView.getSelectionModel().select(n);
                invalidatePreview("添加步骤");
            } catch (Exception e) {
            }
        }
    }

    private AppStrategy findStrategyForOp(OperationType op) {
        for (int i = pipelineStrategies.size() - 1; i >= 0; i--) {
            AppStrategy s = pipelineStrategies.get(i);
            if (op == OperationType.RENAME && (s instanceof AdvancedRenameStrategy || s instanceof TrackNumberStrategy || s instanceof AlbumDirNormalizeStrategy))
                return s;
            if (op == OperationType.CONVERT && (s instanceof AudioConverterStrategy || s instanceof MetadataScraperStrategy))
                return s;
            if (op == OperationType.MOVE && s instanceof FileMigrateStrategy) return s;
            if (op == OperationType.SPLIT && s instanceof CueSplitterStrategy) return s;
            if (op == OperationType.DELETE && s instanceof FileCleanupStrategy) return s;
            if (op == OperationType.UNZIP && s instanceof FileUnzipStrategy) return s;
        }
        return null;
    }

    private static class Spacer extends Region {
        public Spacer() {
            HBox.setHgrow(this, Priority.ALWAYS);
        }
    }

    // --- Styles ---
    private static class ThemeConfig implements Cloneable {
        String accentColor = "#3498db";
        String textColor = "#333333";
        double glassOpacity = 0.65;
        boolean isDarkBackground = false;
        double cornerRadius = 10.0;

        @Override
        public ThemeConfig clone() {
            try {
                return (ThemeConfig) super.clone();
            } catch (Exception e) {
                return new ThemeConfig();
            }
        }
    }

    private class ComposeView {
        private final FileManagerAppV16 app;
        private VBox viewNode;
        private ListView<AppStrategy> pipelineListView;
        private VBox configContainer;

        public ComposeView(FileManagerAppV16 app) { this.app = app; buildUI(); }
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
            ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(30);
            ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(30);
            ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(40);
            grid.getColumnConstraints().addAll(col1, col2, col3);

            // --- Left Panel (Source) ---
            VBox leftPanel = styles.createGlassPane();
            leftPanel.setPadding(new Insets(15)); leftPanel.setSpacing(10);

            ListView<File> sourceListView = new ListView<>(app.sourceRoots);
            sourceListView.setPlaceholder(styles.createNormalLabel("拖拽文件夹到此"));
            VBox.setVgrow(sourceListView, Priority.ALWAYS);

            // [增强] 源目录列表单元格：支持完整路径显示 + 行内操作
            sourceListView.setCellFactory(p -> new ListCell<File>() {
                @Override protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if(empty || item == null) {
                        setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
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
                        JFXButton btnUp = createSmallIconButton("▲", e -> moveListItem(app.sourceRoots, getIndex(), -1));
                        JFXButton btnDown = createSmallIconButton("▼", e -> moveListItem(app.sourceRoots, getIndex(), 1));
                        JFXButton btnOpen = createSmallIconButton("📂", e -> openFileInSystem(item));
                        JFXButton btnDel = createSmallIconButton("✕", e -> {
                            app.sourceRoots.remove(item);
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
            sourceListView.setOnDragOver(e -> { if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE); e.consume(); });
            sourceListView.setOnDragDropped(this::handleDragDrop);

            HBox srcBtns = new HBox(10);
            srcBtns.getChildren().addAll(
                    styles.createActionButton("添加文件夹", null, app::addDirectoryAction),
                    styles.createActionButton("全部清空", "#e74c3c", () -> { app.sourceRoots.clear(); app.invalidatePreview("清空源"); })
            );

            TitledPane tpFilters = new TitledPane("全局筛选设置", app.createGlobalFiltersUI());
            tpFilters.setCollapsible(true); tpFilters.setExpanded(true);
            tpFilters.setStyle("-fx-text-fill: " + currentTheme.textColor + ";");

            leftPanel.getChildren().addAll(sourceListView, srcBtns, tpFilters);
            grid.add(leftPanel, 0, 0);

            // --- Center Panel (Pipeline) ---
            VBox centerPanel = styles.createGlassPane();
            centerPanel.setPadding(new Insets(15)); centerPanel.setSpacing(10);

            pipelineListView = new ListView<>(app.pipelineStrategies);
            pipelineListView.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 5;");
            VBox.setVgrow(pipelineListView, Priority.ALWAYS);

            // [增强] 流水线列表单元格：支持序号 + 描述 + 完整操作
            pipelineListView.setCellFactory(param -> new ListCell<AppStrategy>() {
                @Override protected void updateItem(AppStrategy item, boolean empty) {
                    super.updateItem(item, empty);
                    if(empty || item == null) {
                        setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(null);
                        BorderPane pane = new BorderPane();

                        VBox v = new VBox(2);
                        Label n = styles.createLabel((getIndex()+1) + ". " + item.getName(), 14, true);
                        Label d = styles.createInfoLabel(item.getDescription());
                        d.setMaxWidth(180);
                        v.getChildren().addAll(n, d);

                        HBox actions = new HBox(4);
                        actions.setAlignment(Pos.CENTER_RIGHT);

                        // 策略操作：上移、下移、删除
                        // (注：配置详情通过列表选中触发，这里不需要额外按钮，或者可以加一个 '⚙' 指示)
                        JFXButton btnUp = createSmallIconButton("▲", e -> {
                            moveListItem(app.pipelineStrategies, getIndex(), -1);
                            pipelineListView.getSelectionModel().select(getIndex()); // 保持选中
                        });
                        JFXButton btnDown = createSmallIconButton("▼", e -> {
                            moveListItem(app.pipelineStrategies, getIndex(), 1);
                            pipelineListView.getSelectionModel().select(getIndex());
                        });
                        JFXButton btnDel = createSmallIconButton("✕", e -> {
                            app.pipelineStrategies.remove(item);
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
            });

            pipelineListView.getSelectionModel().selectedItemProperty().addListener((o,old,val) -> refreshConfig(val));

            HBox pipeActions = new HBox(5);
            JFXComboBox<AppStrategy> cbStrategyTemplates = new JFXComboBox<>(FXCollections.observableArrayList(app.strategyPrototypes));
            cbStrategyTemplates.setPromptText("选择功能...");
            cbStrategyTemplates.setPrefWidth(150);
            cbStrategyTemplates.setConverter(new javafx.util.StringConverter<AppStrategy>() { @Override public String toString(AppStrategy o) { return o.getName(); } @Override public AppStrategy fromString(String s) { return null; } });

            JFXButton btnAddStep = styles.createActionButton("添加步骤", "#2ecc71", () -> app.addStrategyStep(cbStrategyTemplates.getValue()));

            pipeActions.getChildren().addAll(cbStrategyTemplates, btnAddStep);
            centerPanel.getChildren().addAll(pipelineListView, pipeActions);
            grid.add(centerPanel, 1, 0);

            // --- Right Panel (Config) ---
            VBox rightPanel = styles.createGlassPane();
            rightPanel.setPadding(new Insets(15));

            configContainer = new VBox(10);
            configContainer.setStyle("-fx-background-color: transparent;");

            ScrollPane sc = new ScrollPane(configContainer);
            sc.setFitToWidth(true);
            sc.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            VBox.setVgrow(sc, Priority.ALWAYS);
            rightPanel.getChildren().add(sc);
            grid.add(rightPanel, 2, 0);

            VBox.setVgrow(grid, Priority.ALWAYS);

            // Bottom
            HBox bottom = new HBox();
            bottom.setAlignment(Pos.CENTER_RIGHT);
            JFXButton btnGo = styles.createActionButton("生成预览  ▶", null, app::runPipelineAnalysis);
            btnGo.setPadding(new Insets(10, 30, 10, 30));
            bottom.getChildren().add(btnGo);

            viewNode.getChildren().addAll(headers, grid, bottom);

            // Init select first
            if (!app.pipelineStrategies.isEmpty()) {
                pipelineListView.getSelectionModel().selectFirst();
            }
        }

        private void handleDragDrop(javafx.scene.input.DragEvent e) {
            if (e.getDragboard().hasFiles()) {
                boolean changed = false;
                for(File f : e.getDragboard().getFiles()) {
                    if(f.isDirectory() && !app.sourceRoots.contains(f)) {
                        app.sourceRoots.add(f);
                        changed = true;
                    }
                }
                if(changed) app.invalidatePreview("源变更");
            }
            e.setDropCompleted(true);
            e.consume();
        }

        public void refreshConfig(AppStrategy s) {
            configContainer.getChildren().clear();
            if(s==null) return;

            configContainer.getChildren().addAll(
                    styles.createHeader(s.getName()),
                    styles.createInfoLabel(s.getDescription()),
                    new Separator(),
                    styles.createNormalLabel("前置条件 (可选):"),
                    app.createConditionsUI(s),
                    new Separator(),
                    styles.createNormalLabel("参数配置:"),
                    s.getConfigNode() != null ? s.getConfigNode() : new Label("无")
            );
            styles.forceDarkText(configContainer);
        }
    }


    // [新增] 通用：创建统一风格的微型图标按钮
    private JFXButton createSmallIconButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        JFXButton btn = new JFXButton(text);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px;");
        btn.setTextFill(Color.web("#555"));
        btn.setOnAction(e -> {
            handler.handle(e);
            e.consume(); // 防止事件冒泡触发 ListCell 选中
        });
        // Hover 效果
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #eee; -fx-border-color: #999; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px;"));
        return btn;
    }

    // [变更] createConditionsUI: 使用统一的 ListCell 风格
    private Node createConditionsUI(AppStrategy strategy) {
        VBox box = new VBox(5);

        ListView<RuleCondition> lv = new ListView<>(FXCollections.observableArrayList(strategy.getGlobalConditions()));
        lv.setPrefHeight(100);

        // 使用统一风格的 Cell
        lv.setCellFactory(p -> new ListCell<RuleCondition>() {
            @Override protected void updateItem(RuleCondition item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) {
                    setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null);
                    HBox root = new HBox(10);
                    root.setAlignment(Pos.CENTER_LEFT);

                    Label lbl = styles.createNormalLabel(item.toString());
                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                    JFXButton btnDel = createSmallIconButton("✕", e -> {
                        strategy.getGlobalConditions().remove(item);
                        lv.getItems().setAll(strategy.getGlobalConditions());
                        invalidatePreview("移除条件");
                    });
                    btnDel.setTextFill(Color.RED);

                    root.getChildren().addAll(lbl, sp, btnDel);
                    setGraphic(root);
                    setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        HBox input = new HBox(5);
        ComboBox<ConditionType> cbType = new ComboBox<>(FXCollections.observableArrayList(ConditionType.values()));
        cbType.getSelectionModel().select(0);
        TextField txtVal = new TextField();
        txtVal.setPromptText("条件值");
        HBox.setHgrow(txtVal, Priority.ALWAYS);

        JFXButton btnAdd = styles.createActionButton("+", "#3498db", () -> {
            if(!txtVal.getText().isEmpty()){
                strategy.getGlobalConditions().add(new RuleCondition(cbType.getValue(), txtVal.getText()));
                lv.getItems().setAll(strategy.getGlobalConditions());
                txtVal.clear();
                invalidatePreview("添加条件");
            }
        });

        input.getChildren().addAll(cbType, txtVal, btnAdd);
        box.getChildren().addAll(lv, input);
        return box;
    }

    private class PreviewView {
        private VBox viewNode;

        public PreviewView() {
            buildUI();
        }

        public Node getViewNode() {
            return viewNode;
        }

        private void buildUI() {
            viewNode = new VBox(15);

            VBox toolbar = styles.createGlassPane();
            toolbar.setPadding(new Insets(10));
            toolbar.setSpacing(15);
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.setStyle(toolbar.getStyle() + "-fx-background-radius: 10;");

            HBox filterBox = new HBox(10);
            filterBox.setAlignment(Pos.CENTER_LEFT);
            txtSearchFilter.setPromptText("搜索...");
            txtSearchFilter.textProperty().addListener((o, old, v) -> refreshPreviewTableFilter());
            cbStatusFilter.valueProperty().addListener((o, old, v) -> refreshPreviewTableFilter());
            chkHideUnchanged.selectedProperty().addListener((o, old, v) -> refreshPreviewTableFilter());
            chkHideUnchanged.setTextFill(Color.web(currentTheme.textColor));

            filterBox.getChildren().addAll(styles.createNormalLabel("筛选:"), txtSearchFilter, cbStatusFilter, chkHideUnchanged);
            toolbar.getChildren().addAll(btnExecute, btnStop, new Spacer(), filterBox);

            VBox dashboard = styles.createGlassPane();
            dashboard.setPadding(new Insets(10));
            dashboard.setSpacing(20);
            dashboard.setAlignment(Pos.CENTER_LEFT);
            dashboard.setStyle(dashboard.getStyle() + "-fx-background-radius: 10;");

            mainProgressBar.setPrefWidth(300);
            mainProgressBar.setPrefHeight(18);

            progressBox.getChildren().clear(); // Ensure clean
            progressBox.setAlignment(Pos.CENTER_LEFT);
            progressBox.getChildren().addAll(progressLabel, etaLabel);

            dashboard.getChildren().addAll(
                    styles.createNormalLabel("进度:"), mainProgressBar, progressBox,
                    new Separator(javafx.geometry.Orientation.VERTICAL),
                    statsLabel
            );

            previewTable.setStyle("-fx-background-color: rgba(255,255,255,0.4); -fx-base: rgba(255,255,255,0.1);");
            VBox.setVgrow(previewTable, Priority.ALWAYS);

            viewNode.getChildren().addAll(toolbar, dashboard, previewTable);
        }
    }

    private class LogView {
        private VBox viewNode;

        public LogView() {
            buildUI();
        }

        public Node getViewNode() {
            return viewNode;
        }

        private void buildUI() {
            viewNode = new VBox(15);
            VBox tools = styles.createGlassPane();
            tools.setPadding(new Insets(10));
            tools.setAlignment(Pos.CENTER_LEFT);
            tools.setStyle(tools.getStyle() + "-fx-background-radius: 10;");

            chkSaveLog.setText("保存到文件");
            chkSaveLog.setTextFill(Color.web(currentTheme.textColor));
            JFXButton clr = styles.createActionButton("清空", "#95a5a6", () -> logArea.clear());
            tools.getChildren().addAll(styles.createHeader("运行日志"), new Spacer(), chkSaveLog, clr);

            logArea.setStyle("-fx-font-family: 'Consolas'; -fx-control-inner-background: rgba(255,255,255,0.8); -fx-text-fill: " + currentTheme.textColor + ";");
            VBox.setVgrow(logArea, Priority.ALWAYS);
            viewNode.getChildren().addAll(tools, logArea);
        }
    }

    private class StyleFactory {
        public Label createLabel(String t, int s, boolean b) {
            Label l = new Label(t);
            l.setFont(Font.font("Segoe UI", b ? FontWeight.BOLD : FontWeight.NORMAL, s));
            l.setTextFill(Color.web(currentTheme.textColor));
            return l;
        }

        public Label createHeader(String t) {
            return createLabel(t, 16, true);
        }

        public Label createNormalLabel(String t) {
            return createLabel(t, 12, false);
        }

        public Label createInfoLabel(String t) {
            Label l = createLabel(t, 10, false);
            l.setTextFill(Color.GRAY);
            return l;
        }

        public JFXButton createActionButton(String t, String c, Runnable r) {
            JFXButton b = new JFXButton(t);
            b.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: %.1f;", c != null ? c : currentTheme.accentColor, currentTheme.cornerRadius));
            b.setOnAction(e -> r.run());
            return b;
        }

        public VBox createGlassPane() {
            VBox p = new VBox();
            p.setStyle(String.format("-fx-background-color: rgba(255,255,255,%.2f); -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;", currentTheme.glassOpacity, currentTheme.cornerRadius, currentTheme.textColor));
            return p;
        }

        public VBox createSectionHeader(String t, String s) {
            VBox v = new VBox(2);
            v.getChildren().addAll(createHeader(t), createInfoLabel(s));
            return v;
        }

        public void forceDarkText(Node n) {
            if (n instanceof Labeled) ((Labeled) n).setTextFill(Color.web(currentTheme.textColor));
            if (n instanceof Parent) for (Node c : ((Parent) n).getChildrenUnmodifiable()) forceDarkText(c);
        }
    }

}