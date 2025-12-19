package com.filemanager.app;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import com.filemanager.strategy.*;
import com.filemanager.util.file.FileLockManager;
import com.filemanager.util.file.ParallelStreamWalker;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.ui.*;
import com.jfoenix.controls.*;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.controlsfx.control.CheckComboBox;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FileManager Plus v21.0 (Modularized)
 * 主程序类，负责核心逻辑、数据持有和控制器实现。
 * 视图逻辑已拆分至 com.filemanager.ui 包。
 */
public class FileManagerPlusApp extends Application implements IAppController, IManagerAppInterface {

    // --- Core Data ---
    private final ObservableList<File> sourceRoots = FXCollections.observableArrayList();
    private final ObservableList<AppStrategy> pipelineStrategies = FXCollections.observableArrayList();
    private final List<ChangeRecord> changePreviewList = new ArrayList<>();
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final File lastConfigFile = new File(System.getProperty("user.home"), ".fmplus_config.properties");
    private final ThemeConfig currentTheme = new ThemeConfig();
    private List<AppStrategy> strategyPrototypes = new ArrayList<>();
    private Stage primaryStage;
    // --- Infrastructure ---
    private ConfigFileManager configManager;
    private StyleFactory styles;
    @Getter
    @Setter
    private String bgImagePath = "";
    private List<ChangeRecord> fullChangeList = new ArrayList<>();
    // --- Modular Views (UI Modules) ---
    private GlobalSettingsView globalSettingsView;
    private ComposeView composeView;
    private PreviewView previewView;
    private LogView logView;
    // --- Task & Threading ---
    private PrintWriter fileLogger;
    private ExecutorService executorService;
    private Task<?> currentTask;
    private volatile boolean isTaskRunning = false;
    private AnimationTimer uiUpdater;
    // --- UI Containers ---
    private StackPane rootContainer;
    private ImageView backgroundImageView;
    private Region backgroundOverlay;
    private StackPane contentArea;
    private TabPane mainTabPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo Music Manager - Plus Edition");

        // 1. 基础服务初始化
        this.styles = new StyleFactory(currentTheme);
        this.configManager = new ConfigFileManager(this);
        this.strategyPrototypes = AppStrategyFactory.getAppStrategies();

        // 2. 视图模块初始化 (替代原 initGlobalControls)
        // 实例化各个 View，它们会在内部创建自己的 UI 控件
        this.globalSettingsView = new GlobalSettingsView(this);
        this.logView = new LogView(this);
        this.previewView = new PreviewView(this);
        this.composeView = new ComposeView(this);

        // 3. 构建主布局
        rootContainer = new StackPane();
        backgroundImageView = new ImageView();
        backgroundImageView.setPreserveRatio(false);
        backgroundImageView.fitWidthProperty().bind(rootContainer.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootContainer.heightProperty());
        backgroundOverlay = new Region();

        BorderPane mainContent = createMainLayout();
        rootContainer.getChildren().addAll(backgroundImageView, backgroundOverlay, mainContent);

        // 4. 场景与样式
        Scene scene = new Scene(rootContainer, 1440, 900);
        if (getClass().getResource("/css/jfoenix-components.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/jfoenix-components.css").toExternalForm());
        }

        primaryStage.setScene(scene);

        // 5. 加载配置 & 应用外观
        configManager.loadConfig(lastConfigFile);
        applyAppearance();

        // 6. 生命周期管理
        primaryStage.setOnCloseRequest(e -> {
            configManager.saveConfig(lastConfigFile);
            forceStop();
            closeFileLogger();
            Platform.exit();
            System.exit(0);
        });

        startLogUpdater();
        primaryStage.show();
    }

    // --- UI Layout Orchestration ---

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // Top Menu
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: transparent;");
        Menu fileMenu = new Menu("文件");
        MenuItem loadItem = new MenuItem("加载配置...");
        loadItem.setOnAction(e -> loadConfigAction());
        MenuItem saveItem = new MenuItem("保存配置...");
        saveItem.setOnAction(e -> saveConfigAction());
        fileMenu.getItems().addAll(loadItem, saveItem);
        Menu viewMenu = new Menu("外观");
        MenuItem themeItem = new MenuItem("界面设置...");
        themeItem.setOnAction(e -> showAppearanceDialog());
        viewMenu.getItems().add(themeItem);
        menuBar.getMenus().addAll(fileMenu, viewMenu);

        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        Label logo = new Label("ECHO MANAGER PLUS");
        logo.setFont(Font.font("Segoe UI", FontWeight.BLACK, 20));
        logo.setTextFill(Color.web(currentTheme.getAccentColor()));
        header.getChildren().addAll(logo, new Region(), menuBar);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        VBox top = new VBox(header, new Separator());
        root.setTop(top);

        // Center Content (Tabs are hidden but managed)
        mainTabPane = new JFXTabPane();
        mainTabPane.setStyle("-fx-background-color: transparent;");
        // 将各模块的 View 挂载到 Tab
        mainTabPane.getTabs().addAll(composeView.getTab(), previewView.getTab(), logView.getTab());

        contentArea = new StackPane();
        contentArea.setPadding(new Insets(10));
        root.setCenter(contentArea);

        // Sidebar Navigation
        VBox sideMenu = createSideMenu();
        root.setLeft(sideMenu);

        // Status Bar
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setStyle("-fx-background-color: rgba(240, 240, 240, 0.8); -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        Label lblStatusIcon = new Label("●");
        lblStatusIcon.setTextFill(Color.GREEN);
        Label lblReady = styles.createNormalLabel("就绪");
        statusBar.getChildren().addAll(lblStatusIcon, lblReady); // Stats are now managed by PreviewView internally or via explicit update
        root.setBottom(statusBar);

        // Init Default View
        switchView(composeView.getViewNode());

        return root;
    }

    private VBox createSideMenu() {
        VBox menu = styles.createGlassPane();
        menu.setPrefWidth(120);
        menu.setPadding(new Insets(30, 20, 30, 20));
        menu.setSpacing(15);
        menu.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        VBox navBox = new VBox(3);
        navBox.getChildren().addAll(
                styles.createActionButton("任务编排", null, () -> switchView(composeView.getViewNode())),
                styles.createActionButton("预览执行", null, () -> switchView(previewView.getViewNode())),
                styles.createActionButton("运行日志", null, () -> switchView(logView.getViewNode()))
        );
        navBox.setAlignment(Pos.CENTER);

        menu.getChildren().add(navBox);
        return menu;
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

    // ==================== IAppController Implementations ====================
    // 所有的 Getters 现在都委托给具体的 View 模块
    // 实现了逻辑层与 UI 层的解耦

    @Override
    public ObservableList<File> getSourceRoots() {
        return sourceRoots;
    }

    @Override
    public ObservableList<AppStrategy> getPipelineStrategies() {
        return pipelineStrategies;
    }

    @Override
    public List<AppStrategy> getStrategyPrototypes() {
        return strategyPrototypes;
    }

    @Override
    public ThemeConfig getCurrentTheme() {
        return currentTheme;
    }

    @Override
    public StyleFactory getStyleFactory() {
        return styles;
    }

    // 委托给 GlobalSettingsView
    @Override
    public JFXComboBox<String> getCbRecursionMode() {
        return globalSettingsView.getCbRecursionMode();
    }

    @Override
    public Spinner<Integer> getSpRecursionDepth() {
        return globalSettingsView.getSpRecursionDepth();
    }

    @Override
    public CheckComboBox<String> getCcbFileTypes() {
        return globalSettingsView.getCcbFileTypes();
    }

    @Override
    public Spinner<Integer> getSpGlobalThreads() {
        return globalSettingsView.getSpGlobalThreads();
    }

    // 委托给 PreviewView
    @Override
    public JFXTextField getTxtSearchFilter() {
        return previewView.getTxtSearchFilter();
    }

    @Override
    public JFXComboBox<String> getCbStatusFilter() {
        return previewView.getCbStatusFilter();
    }

    @Override
    public JFXCheckBox getChkHideUnchanged() {
        return previewView.getChkHideUnchanged();
    }

    // 委托给 LogView
    @Override
    public JFXCheckBox getChkSaveLog() {
        return logView.getChkSaveLog();
    }

    public void refreshPipelineSelection() {
        if (!pipelineStrategies.isEmpty() && composeView != null) composeView.selectFirstStrategy();
    }

    // ==================== Business Logic (Pipeline) ====================

    @Override
    public void runPipelineAnalysis() {
        if (sourceRoots.isEmpty()) {
            showToast("请添加源目录");
            return;
        }
        if (pipelineStrategies.isEmpty()) {
            showToast("请添加步骤");
            return;
        }
        if (isTaskRunning) return;

        mainTabPane.getSelectionModel().select(previewView.getTab());
        switchView(previewView.getViewNode());

        resetProgressUI("正在扫描...", false);
        changePreviewList.clear();
        previewView.getPreviewTable().setRoot(null);

        // 捕获所有策略参数
        for (AppStrategy s : pipelineStrategies) s.captureParams();

        // 从 GlobalSettingsView 获取参数
        int maxDepth = "仅当前目录".equals(getCbRecursionMode().getValue()) ? 1 :
                ("递归所有子目录".equals(getCbRecursionMode().getValue()) ? Integer.MAX_VALUE : getSpRecursionDepth().getValue());
        List<String> exts = new ArrayList<>(getCcbFileTypes().getCheckModel().getCheckedItems());

        Task<List<ChangeRecord>> task = new Task<List<ChangeRecord>>() {
            @Override
            protected List<ChangeRecord> call() throws Exception {
                long t0 = System.currentTimeMillis();
                updateMessage("▶ ▶ ▶ 扫描源文件...");
                List<File> initialFiles = new ArrayList<>();
                for (File r : sourceRoots) {
                    if (isCancelled()) break;
                    initialFiles.addAll(scanFilesRobust(r, maxDepth, exts, this::updateMessage));
                }
                if (isCancelled()) return null;
                log("▶ ▶ ▶ 扫描完成，共 " + initialFiles.size() + " 个文件。");

                List<ChangeRecord> currentRecords = initialFiles.stream()
                        .map(f -> new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE))
                        .collect(Collectors.toList());

                for (int i = 0; i < pipelineStrategies.size(); i++) {
                    if (isCancelled()) break;
                    AppStrategy strategy = pipelineStrategies.get(i);
                    updateMessage("▶ ▶ ▶ 执行步骤 " + (i + 1) + ": " + strategy.getName());
                    currentRecords = strategy.analyze(currentRecords, sourceRoots, (p, m) -> updateProgress(p, 1.0));
                }
                return currentRecords;
            }
        };

        task.setOnSucceeded(e -> {
            fullChangeList = task.getValue();
            refreshPreviewTableFilter();
            finishTaskUI("✅️预览完成");
            boolean hasChanges = fullChangeList.stream().anyMatch(ChangeRecord::isChanged);
            previewView.getBtnExecute().setDisable(!hasChanges);
        });
        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    @Override
    public void runPipelineExecution() {
        if (fullChangeList.isEmpty()) return;
        long count = fullChangeList.stream().filter(record -> record.isChanged()
                && record.getOpType() != OperationType.NONE
                && record.getStatus() != ExecStatus.SKIPPED).count();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "执行 " + count + " 个变更?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        resetProgressUI("执行中...", true);
        if (getChkSaveLog().isSelected()) initFileLogger();
        long startT = System.currentTimeMillis();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<ChangeRecord> todos = fullChangeList.stream()
                        .filter(record -> record.isChanged()
                                && record.getOpType() != OperationType.NONE
                                && record.getStatus() != ExecStatus.SKIPPED)
                        .collect(Collectors.toList());
                int total = todos.size();
                AtomicInteger curr = new AtomicInteger(0);

                AtomicLong lastRefresh = new AtomicLong(System.currentTimeMillis());

                int threads = getSpGlobalThreads().getValue();
                executorService = Executors.newFixedThreadPool(threads);
                log("▶ ▶ ▶ 任务启动，并发线程: " + threads);
                log("▶ ▶ ▶ 注意：部分任务依赖同一个原始文件，会因为加锁导致串行执行，任务会一直轮询！");

                AtomicInteger round = new AtomicInteger(1);
                log("▶ ▶ ▶ 第[" + round.incrementAndGet() + "]轮任务扫描，剩余待执行任务数：" + todos.size());
                while (!todos.isEmpty() && !isCancelled()) {
                    AtomicBoolean anyChange = new AtomicBoolean(false);
                    for (ChangeRecord rec : todos) {
                        if (isCancelled()) {
                            break;
                        }
                        // 检查文件锁
                        if (FileLockManager.isLocked(rec.getFileHandle())) continue;
                        // 对原始文件加逻辑锁，避免并发操作同一个文件
                        if (!FileLockManager.lock(rec.getFileHandle())) continue;
                        anyChange.set(true);
                        if (isCancelled()) continue;
                        rec.setStatus(ExecStatus.RUNNING);
                        executorService.submit(() -> {
                            try {
                                // [修改] 策略执行时不再传递线程数，只负责逻辑
                                AppStrategy s = AppStrategyFactory.findStrategyForOp(rec.getOpType(), pipelineStrategies);
                                log("▶ 开始处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
                                if (s != null) {
                                    s.execute(rec);
                                    rec.setStatus(ExecStatus.SUCCESS);
                                    log("✅️ 成功处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName() + "，耗时：");
                                } else {
                                    rec.setStatus(ExecStatus.SKIPPED);
                                }
                            } catch (Exception e) {
                                rec.setStatus(ExecStatus.FAILED);
                                log("❌ 失败处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName() + ",原因" + e.getMessage());
                                log("❌ 失败详细原因:" + ExceptionUtils.getStackTrace(e));
                            } finally {
                                // 文件解锁
                                FileLockManager.unlock(rec.getFileHandle());
                                int c = curr.incrementAndGet();
                                updateProgress(c, total);
                                if (c % 100 == 0 && (System.currentTimeMillis() - lastRefresh.get() > 5000))
                                    Platform.runLater(() -> {
                                        updateStats(System.currentTimeMillis() - startT);
                                        lastRefresh.set(System.currentTimeMillis());
                                        previewView.refresh();
                                    });
                            }
                        });
                    }
                    if (anyChange.get()) {
                        // 捞回那些因为被加锁未执行的变更，继续尝试执行
                        todos = todos.stream().filter(rec -> rec.getStatus() == ExecStatus.PENDING).collect(Collectors.toList());
                        log("▶ ▶ ▶ 第[" + round.incrementAndGet() + "]轮任务扫描，剩余待执行任务数：" + todos.size());
                    }
                    // 适当Sleep，避免反复刷数据
                    Thread.sleep(2000);
                }
                executorService.shutdown();
                while (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    if (isCancelled()) {
                        executorService.shutdownNow();
                        break;
                    }
                }
                updateStats(System.currentTimeMillis() - startT);
                previewView.refresh();
                return null;
            }
        };
        updateStats(System.currentTimeMillis() - startT);

        task.setOnSucceeded(e -> {
            finishTaskUI("➡ ➡ ➡ 执行完成 ⬅ ⬅ ⬅");
            closeFileLogger();
        });
        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    // --- Shared Methods & Utils ---

    @Override
    public void refreshPreviewTableFilter() {
        if (fullChangeList.isEmpty()) return;
        String s = getTxtSearchFilter().getText().toLowerCase();
        String st = getCbStatusFilter().getValue();
        boolean h = getChkHideUnchanged().isSelected();

        Task<TreeItem<ChangeRecord>> t = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() {
                TreeItem<ChangeRecord> root = new TreeItem<>(new ChangeRecord());
                root.setExpanded(true);
                String numberLimit = globalSettingsView.getNumberDisplay().getText();
                int limit = 1000;
                if (StringUtils.isNoneBlank(numberLimit) && StringUtils.isNumeric(numberLimit)) {
                    limit = Integer.parseInt(numberLimit);
                }
                AtomicInteger count = new AtomicInteger();
                for (ChangeRecord r : fullChangeList) {
                    if (h && !r.isChanged() && r.getStatus() != ExecStatus.FAILED) continue;
                    if (!s.isEmpty() && !r.getOriginalName().toLowerCase().contains(s)) continue;
                    boolean sm = true;
                    if ("执行中".equals(st)) sm = r.getStatus() == ExecStatus.RUNNING;
                    else if ("成功".equals(st)) sm = r.getStatus() == ExecStatus.SUCCESS;
                    else if ("失败".equals(st)) sm = r.getStatus() == ExecStatus.FAILED;
                    if (!sm) continue;
                    count.incrementAndGet();
                    root.getChildren().add(new TreeItem<>(r));
                    if (count.get() > limit) {
                        log("注意：实时预览数据限制为" + limit + "条！");
                        break;
                    }
                }
                return root;
            }
        };
        t.setOnSucceeded(e -> {
            previewView.getPreviewTable().setRoot(t.getValue());
            updateStats(0);
        });
        t.setOnFailed(e -> {
            previewView.getPreviewTable().setRoot(t.getValue());
            updateStats(0);
        });
        new Thread(t).start();
    }

    private void updateStats(long ms) {
        long t = fullChangeList.size(),
                c = fullChangeList.stream().filter(ChangeRecord::isChanged).count(),
                s = fullChangeList.stream().filter(r -> r.getStatus() == ExecStatus.SUCCESS).count(),
                f = fullChangeList.stream().filter(r -> r.getStatus() == ExecStatus.FAILED).count();
        String tm = ms > 0 ? String.format("%.1fs", ms / 1000.0) : "-";
        Platform.runLater(() -> previewView.updateStatsDisplay(t, c, s, f, tm));
    }

    private List<File> scanFilesRobust(File root, int maxDepth, List<String> exts, Consumer<String> msg) {
        AtomicInteger countScan = new AtomicInteger(0);
        AtomicInteger countIgnore = new AtomicInteger(0);
        List<File> list = new ArrayList<>();
        if (!root.exists()) return list;
        int threads = getSpGlobalThreads().getValue();
        try (Stream<Path> s = ParallelStreamWalker.walk(root.toPath(), maxDepth, threads)) {
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
                } finally {
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

    // --- Task UI State ---
    private void resetProgressUI(String msg, boolean isExec) {
        isTaskRunning = true;
        currentTask = null;
        previewView.setRunningState(true);
        previewView.updateProgress(msg, -1);
    }

    private void finishTaskUI(String msg) {
        isTaskRunning = false;
        previewView.setRunningState(false);
        previewView.updateProgress(msg, 1.0);
    }

    private void handleTaskLifecycle(Task<?> t) {
        currentTask = t;
        previewView.bindProgress(t);
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

    @Override
    public void forceStop() {
        if (isTaskRunning) {
            isTaskRunning = false;
            if (currentTask != null) currentTask.cancel();
            if (executorService != null) executorService.shutdownNow();
            log("🛑 强制停止");
            finishTaskUI("已停止");
        }
    }

    // --- Actions Impl ---
    @Override
    public void addDirectoryAction() {
        DirectoryChooser dc = new DirectoryChooser();
        File f = dc.showDialog(primaryStage);
        if (f != null && !sourceRoots.contains(f)) {
            sourceRoots.add(f);
            invalidatePreview("源增加");
        }
    }

    @Override
    public void removeSourceDir(File dir) {
        sourceRoots.remove(dir);
        invalidatePreview("源移除");
    }

    @Override
    public void clearSourceDirs() {
        sourceRoots.clear();
        invalidatePreview("清空源");
    }

    @Override
    public void addStrategyStep(AppStrategy template) {
        this.pipelineStrategies.add(template);
    }

    @Override
    public void removeStrategyStep(AppStrategy strategy) {
        this.pipelineStrategies.remove(strategy);
    }

    @Override
    public void openFileInSystem(File f) {
        try {
            if (f != null && f.exists()) Desktop.getDesktop().open(f);
        } catch (Exception e) {
        }
    }

    @Override
    public void openParentDirectory(File f) {
        if (f != null) openFileInSystem(f.isDirectory() ? f : f.getParentFile());
    }

    @Override
    public void clearLog() {
        logView.clearLog();
    }

    @Override
    public void invalidatePreview(String r) {
        if (!fullChangeList.isEmpty()) {
            fullChangeList.clear();
            previewView.getPreviewTable().setRoot(null);
            log(r);
        }
        previewView.getBtnExecute().setDisable(true);
    }

    @Override
    // --- Config IO (包含线程数保存) ---
    public void log(String s) {
        // 增加时间戳
        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String msg = "[" + time + "] ➡➡➡ " + s;
        logQueue.offer(msg);
        if (fileLogger != null) fileLogger.println(msg);
    }

    // --- Config & Log IO ---
    @Override
    public void saveConfigAction() {
        FileChooser fc = new FileChooser();
        File f = fc.showSaveDialog(primaryStage);
        if (f != null) configManager.saveConfig(f);
    }

    @Override
    public void loadConfigAction() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(primaryStage);
        if (f != null) configManager.loadConfig(f);
    }

    @Override
    public void showAppearanceDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("设置");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        ColorPicker cp = new ColorPicker(Color.web(currentTheme.getAccentColor()));
        Slider sl = new Slider(0.1, 1.0, currentTheme.getGlassOpacity());
        CheckBox chk = new CheckBox("Dark Mode");
        chk.setSelected(currentTheme.isDarkBackground());
        TextField tp = new TextField(bgImagePath);
        JFXButton bp = new JFXButton("背景...");
        bp.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) {
                tp.setText(f.getAbsolutePath());
                bgImagePath = f.getAbsolutePath();
                applyAppearance();
            }
        });
        g.add(styles.createNormalLabel("Color:"), 0, 0);
        g.add(cp, 1, 0);
        g.add(styles.createNormalLabel("Opacity:"), 0, 1);
        g.add(sl, 1, 1);
        g.add(chk, 1, 2);
        g.add(styles.createNormalLabel("BG:"), 0, 3);
        g.add(new HBox(5, tp, bp), 1, 3);
        d.getDialogPane().setContent(g);
        d.setResultConverter(b -> b);
        d.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                currentTheme.setAccentColor(String.format("#%02X%02X%02X", (int) (cp.getValue().getRed() * 255), (int) (cp.getValue().getGreen() * 255), (int) (cp.getValue().getBlue() * 255)));
                currentTheme.setGlassOpacity(sl.getValue());
                currentTheme.setDarkBackground(chk.isSelected());
                applyAppearance();
            }
        });
    }

    public void applyAppearance() {
        backgroundOverlay.setStyle("-fx-background-color: rgba(" + (currentTheme.isDarkBackground() ? "0,0,0" : "255,255,255") + ", " + (1 - currentTheme.getGlassOpacity()) + ");");
        if (!bgImagePath.isEmpty()) {
            try {
                backgroundImageView.setImage(new Image(new FileInputStream(bgImagePath)));
            } catch (Exception e) {
            }
        }
        if (composeView != null) composeView.refreshList();
    }

    private void startLogUpdater() {
        uiUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                String s;
                while ((s = logQueue.poll()) != null) if (logView != null) logView.appendLog(s);
            }
        };
        uiUpdater.start();
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

    private void showToast(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).show();
    }
}