package com.filemanager.app;

import com.filemanager.base.IAppStrategy;
import com.filemanager.base.IAppController;
import com.filemanager.base.IAutoReloadAble;
import com.filemanager.baseui.ComposeView;
import com.filemanager.baseui.GlobalSettingsView;
import com.filemanager.baseui.LogView;
import com.filemanager.baseui.PreviewView;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import com.filemanager.strategy.AppStrategyFactory;
import com.filemanager.tool.MultiThreadTaskEstimator;
import com.filemanager.tool.RetryableThreadPool;
import com.filemanager.tool.ThreadPoolManager;
import com.filemanager.tool.display.FXDialogUtils;
import com.filemanager.tool.display.ProgressBarDisplay;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.tool.file.ConfigFileManager;
import com.filemanager.tool.file.ParallelStreamWalker;
import com.filemanager.tool.log.LogInfo;
import com.filemanager.tool.log.LogType;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.TaskStatus;
import com.filemanager.util.file.FileLockManagerUtil;
import com.google.common.collect.Lists;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTabPane;
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
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
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
public class FileManagerPlusApp extends Application implements IAppController {

    // --- Core Data ---
    private final ObservableList<File> sourceRoots = FXCollections.observableArrayList();
    private final ObservableList<IAppStrategy> pipelineStrategies = FXCollections.observableArrayList();
    private final File lastConfigFile = new File(System.getProperty("user.home"), ".fmplus_config.properties");
    private final ThemeConfig currentTheme = new ThemeConfig();
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private final AtomicLong lastRefresh = new AtomicLong(System.currentTimeMillis());
    @Getter
    private long taskStartTimStamp = System.currentTimeMillis();
    private List<IAppStrategy> strategyPrototypes;
    // 存储根路径线程配置：存储每个根路径对应的最大线程数
    private final java.util.Map<String, Integer> rootPathThreadConfig = new java.util.concurrent.ConcurrentHashMap<>();
    
    // 存储每个根路径对应的线程池（仅在任务执行期间有效）
    private java.util.Map<String, RetryableThreadPool> executorMap = null;
    
    // 存储每个根路径对应的任务估算器（仅在任务执行期间有效）
    private java.util.Map<String, MultiThreadTaskEstimator> rootPathEstimators = null;
    
    // 线程池管理器
    private ThreadPoolManager threadPoolManager;
    
    // 线程池模式：共享或根路径独立
    private String threadPoolMode = ThreadPoolManager.MODE_GLOBAL; // 默认使用全局统一配置

    // --- UI Controls ---
    private JFXCheckBox autoRun;
    private JFXButton btnGo, btnExecute, btnStop;
    @Getter
    private Stage primaryStage;
    // --- Infrastructure ---
    private ConfigFileManager configManager;
    @Getter
    private List<ChangeRecord> fullChangeList = new ArrayList<>();
    // --- Modular Views (UI Modules) ---
    private GlobalSettingsView globalSettingsView;
    private ComposeView composeView;
    private PreviewView previewView;
    private LogView logView;
    private List<IAutoReloadAble> autoReloadNodes;
    // --- Task & Threading ---
    private RetryableThreadPool executorService;
    private Task<?> currentTask;
    private MultiThreadTaskEstimator threadTaskEstimator;
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
        StyleFactory.initStyleFactory(currentTheme);
        this.configManager = new ConfigFileManager(this);
        this.strategyPrototypes = AppStrategyFactory.getAppStrategies();
        this.threadPoolManager = new ThreadPoolManager();

        // 2. 视图模块初始化 (替代原 initGlobalControls)
        // 实例化各个 View，它们会在内部创建自己的 UI 控件
        this.globalSettingsView = new GlobalSettingsView(this);
        this.logView = new LogView(this);
        this.previewView = new PreviewView(this);
        this.composeView = new ComposeView(this);
        
        // 监听源目录列表变化，自动更新根路径线程配置UI
        sourceRoots.addListener((javafx.collections.ListChangeListener.Change<? extends File> change) -> {
            Platform.runLater(() -> {
                previewView.updateRootPathThreadConfigUI();
            });
        });

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
        this.autoReloadNodes = Lists.newArrayList(globalSettingsView, logView, previewView, composeView, currentTheme);
        configManager.loadConfig(lastConfigFile);
        applyAppearance();

        // 6. 生命周期管理
        primaryStage.setOnCloseRequest(e -> {
            configManager.saveConfig(lastConfigFile);
            forceStop();
            Platform.exit();
            System.exit(0);
        });

        // [关键逻辑] 监听执行线程数变化，实时调整运行中的线程池
        getSpExecutionThreads().valueProperty().addListener((obs, oldVal, newVal) -> {
            threadPoolManager.setGlobalExecutionThreads(newVal);
            if (isTaskRunning.get() && executorService != null) {
                executorService.setCorePoolSize(newVal);
                executorService.setMaximumPoolSize(newVal);
                log("动态调整全局执行线程池大小: " + oldVal + " -> " + newVal);
            }
        });
    
        // [关键逻辑] 监听预览线程数变化，实时调整运行中的线程池
        getSpPreviewThreads().valueProperty().addListener((obs, oldVal, newVal) -> {
            threadPoolManager.setGlobalPreviewThreads(newVal);
            log("动态调整全局预览线程池大小: " + oldVal + " -> " + newVal);
        });

        primaryStage.show();
    }

    // --- UI Layout Orchestration ---

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // Top Menu
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: transparent;");
        Menu fileMenu = new Menu("配置管理");
        MenuItem loadItem = new MenuItem("加载配置...");
        loadItem.setOnAction(e -> loadConfigAction());
        MenuItem saveItem = new MenuItem("保存配置...");
        saveItem.setOnAction(e -> saveConfigAction());
        fileMenu.getItems().addAll(loadItem, saveItem);
        Menu viewMenu = new Menu("外观设置");
        MenuItem themeItem = new MenuItem("界面设置...");
        themeItem.setOnAction(e -> showAppearanceDialog());
        viewMenu.getItems().add(themeItem);
        menuBar.getMenus().addAll(fileMenu, viewMenu);

        autoRun = new JFXCheckBox("预览成功立即运行");
        autoRun.setSelected(false);
        btnGo = StyleFactory.createActionButton("预览", null, this::runPipelineAnalysis);
        btnExecute = StyleFactory.createActionButton("执行", "#27ae60", this::runPipelineExecution);
        btnStop = StyleFactory.createActionButton("停止", "#e74c3c", this::forceStop);
        btnStop.setDisable(true);
        btnExecute.setDisable(true);
        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        Label logo = new Label("MUSIC MANAGER PLUS - By chrse1997@163.com");
        logo.setFont(Font.font("Segoe UI", FontWeight.BLACK, 20));
        logo.setTextFill(Color.web(currentTheme.getAccentColor()));

        header.getChildren().addAll(logo, new Region(), menuBar, autoRun, btnGo, btnExecute, btnStop);
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
        Label lblReady = StyleFactory.createChapter("就绪");
        statusBar.getChildren().addAll(lblStatusIcon, lblReady); // Stats are now managed by PreviewView internally or via explicit update
        root.setBottom(statusBar);

        // Init Default View
        switchView(composeView.getViewNode());

        return root;
    }

    private VBox createSideMenu() {
        VBox menu = StyleFactory.createVBoxPanel();
        menu.setPrefWidth(120);
        menu.setPadding(new Insets(30, 20, 30, 20));
        menu.setSpacing(15);
        menu.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        VBox navBox = new VBox(3);
        navBox.getChildren().addAll(
                StyleFactory.createActionButton("任务编排", null, () -> switchView(composeView.getViewNode())),
                StyleFactory.createActionButton("预览执行", null, () -> switchView(previewView.getViewNode())),
                StyleFactory.createActionButton("运行日志", null, () -> switchView(logView.getViewNode()))
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
    public ObservableList<IAppStrategy> getPipelineStrategies() {
        return pipelineStrategies;
    }

    @Override
    public List<IAppStrategy> getStrategyPrototypes() {
        return strategyPrototypes;
    }

    @Override
    public ThemeConfig getCurrentTheme() {
        return currentTheme;
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
    public Spinner<Integer> getSpPreviewThreads() {
        return previewView.getSpPreviewThreads();
    }

    @Override
    public Spinner<Integer> getSpExecutionThreads() {
        return previewView.getSpExecutionThreads();
    }

    public void refreshPipelineSelection() {
        if (!pipelineStrategies.isEmpty() && composeView != null) composeView.selectFirstStrategy();
    }

    // ==================== Business Logic (Pipeline) ====================

    @Override
    public void runPipelineAnalysis() {
        if (sourceRoots.isEmpty()) {
            FXDialogUtils.showToast(getPrimaryStage(), "请先添加源目录！", FXDialogUtils.ToastType.INFO);
            return;
        }
        if (pipelineStrategies.isEmpty()) {
            FXDialogUtils.showToast(getPrimaryStage(), "请先添加步骤！",
                    FXDialogUtils.ToastType.INFO);
            return;
        }
        if (isTaskRunning.get()) {
            FXDialogUtils.showToast(getPrimaryStage(), "任务执行中，请先停止前面的任务再执行预览！",
                    FXDialogUtils.ToastType.INFO);
            return;
        }
        if (autoRun.isSelected()) {
            if (!FXDialogUtils.showConfirm("确认执行", "预览完毕会立即执行，确认要执行?")) {
                autoRun.setSelected(false);
            }
        }

        mainTabPane.getSelectionModel().select(previewView.getTab());
        switchView(previewView.getViewNode());
        fullChangeList.clear();
        taskStartTimStamp = System.currentTimeMillis();
        setStartTaskUI("▶ ▶ ▶ 正在扫描...", null);
        previewView.getPreviewTable().setRoot(null);

        // 捕获所有策略参数
        for (IAppStrategy s : pipelineStrategies) s.captureParams();

        // 从 GlobalSettingsView 获取参数
        int maxDepth = "当前目录".equals(getCbRecursionMode().getValue()) ? 1 :
                ("全部文件".equals(getCbRecursionMode().getValue()) ? Integer.MAX_VALUE : getSpRecursionDepth().getValue());

        Task<List<ChangeRecord>> task = new Task<List<ChangeRecord>>() {
            @Override
            protected List<ChangeRecord> call() throws Exception {
                updateMessage("▶ ▶ ▶ 扫描源文件...");
                List<File> initialFiles = new ArrayList<>();
                for (File r : sourceRoots) {
                    if (isCancelled()) break;
                    initialFiles.addAll(scanFilesRobust(r, maxDepth, msg -> setRunningUI("▶ ▶ ▶ " + msg)));
                }
                if (isCancelled()) return null;
                setRunningUI("▶ ▶ ▶ 扫描完成，共 " + initialFiles.size() + " 个文件。");

                // 应用预览数量限制
                PreviewView previewView = (PreviewView) getPreviewView();
                List<File> limitedFiles = initialFiles;
                
                // 检查全局预览数量限制
                if (!previewView.isUnlimitedPreview()) {
                    int limit = previewView.getGlobalPreviewLimit();
                    if (initialFiles.size() > limit) {
                        limitedFiles = initialFiles.stream().limit(limit).collect(Collectors.toList());
                        log("▶ ▶ ▶ 已应用全局预览数量限制，仅处理 " + limit + " 个文件");
                    }
                }
                
                // 检查根路径预览数量限制
                List<File> finalLimitedFiles = new ArrayList<>();
                java.util.Map<String, Integer> processedCountByRoot = new java.util.concurrent.ConcurrentHashMap<>();
                
                for (File file : limitedFiles) {
                    String filePath = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
                    String rootPath = findRootPathForFile(filePath);
                    
                    // 检查根路径预览数量限制
                    if (!previewView.isRootPathUnlimitedPreview(rootPath)) {
                        int rootLimit = previewView.getRootPathPreviewLimit(rootPath);
                        int processed = processedCountByRoot.computeIfAbsent(rootPath, k -> 0);
                        
                        if (processed >= rootLimit) {
                            continue; // 达到根路径预览数量限制，跳过该文件
                        }
                        
                        processedCountByRoot.put(rootPath, processed + 1);
                    }
                    
                    finalLimitedFiles.add(file);
                }
                
                if (finalLimitedFiles.size() < limitedFiles.size()) {
                    log("▶ ▶ ▶ 已应用根路径预览数量限制，共处理 " + finalLimitedFiles.size() + " 个文件");
                }
                
                List<ChangeRecord> currentRecords = finalLimitedFiles.stream()
                        .map(f -> new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE))
                        .collect(Collectors.toList());

                int total = currentRecords.size();
                AtomicInteger processed = new AtomicInteger(0);
                threadTaskEstimator = new MultiThreadTaskEstimator(total, Math.max(Math.min(50, total / 20), 1));
                threadTaskEstimator.start();
                ConcurrentLinkedDeque<ChangeRecord> newRecords = new ConcurrentLinkedDeque<>();
                currentRecords.parallelStream().forEach(rec -> {
                    try {
                        int curr = processed.incrementAndGet();
                        Platform.runLater(() -> updateProgress(curr, total));
                        if (isCancelled()) {
                            return;
                        }
                        for (int i = 0; i < pipelineStrategies.size(); i++) {
                            IAppStrategy strategy = pipelineStrategies.get(i);
                            List<ChangeRecord> newRecordAfter = strategy.analyzeWithPreCheck(rec, currentRecords, sourceRoots);
                            newRecords.addAll(newRecordAfter);
                        }
                    } catch (Exception e) {
                        rec.setStatus(ExecStatus.ANALYZE_FAILED);
                        rec.setFailReason(ExceptionUtils.getStackTrace(e));
                        logError("❌ 分析失败: " + rec.getFileHandle().getAbsolutePath() + ",原因" + e.getMessage());
                        logError("❌ 失败详细原因:" + ExceptionUtils.getStackTrace(e));
                    } finally {
                        threadTaskEstimator.oneCompleted();
                        if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                            setRunningUI("▶ ▶ ▶ 预览任务进度: " + threadTaskEstimator.getDisplayInfo());
                            lastRefresh.set(System.currentTimeMillis());
                        }
                    }
                });
                if (!newRecords.isEmpty()) {
                    List<ChangeRecord> union = new ArrayList<>(newRecords);
                    union.addAll(currentRecords);
                    return union;
                }
                return currentRecords;
            }
        };
        setStartTaskUI("▶ ▶ ▶ 预览中...", task);

        task.setOnSucceeded(e -> {
            fullChangeList = task.getValue();
            setFinishTaskUI("➡ ➡ ➡ 预览完成 ⬅ ⬅ ⬅", TaskStatus.SUCCESS);
            boolean hasChanges = fullChangeList.stream().anyMatch(ChangeRecord::isChanged);
            btnExecute.setDisable(!hasChanges);
            if (autoRun.isSelected()) {
                FXDialogUtils.showToast(getPrimaryStage(), "预览完毕自动开始执行！",
                        FXDialogUtils.ToastType.INFO);
                runPipelineExecution();
            }
        });
        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    @Override
    public void runPipelineExecution() {
        long count = countPendingTasks();
        if (count == 0) {
            return;
        }
        if (!autoRun.isSelected()) {
            if (!confirmExecution(count)) {
                return;
            }
        }
        prepareExecutionUI();
        taskStartTimStamp = System.currentTimeMillis();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<ChangeRecord> todos = fullChangeList.stream()
                        .filter(record -> record.isChanged()
                                && record.getOpType() != OperationType.NONE
                                && record.getStatus() == ExecStatus.PENDING)
                        .collect(Collectors.toList());
                int total = todos.size();
                AtomicInteger curr = new AtomicInteger(0);
                int threads = getSpExecutionThreads().getValue();
                
                // 线程池和估算器管理
                final java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap = new java.util.concurrent.ConcurrentHashMap<>();
                rootPathEstimators = localEstimatorMap;
                
                // 设置线程池模式
                threadPoolManager.setThreadPoolMode(threadPoolMode);
                
                // 任务数量限制计数器
                final java.util.Map<String, AtomicInteger> executedCountByRootPath = new java.util.concurrent.ConcurrentHashMap<>();
                final AtomicInteger globalExecutedCount = new AtomicInteger(0);
                
                // 创建全局估算器
                threadTaskEstimator = new MultiThreadTaskEstimator(total, Math.max(Math.min(20, total / 20), 1));
                threadTaskEstimator.start();
                log("▶ ▶ ▶ 任务启动，并发线程: " + threads);
                log("▶ ▶ ▶ 当前线程池模式: " + threadPoolMode);
                log("▶ ▶ ▶ 注意：部分任务依赖同一个原始文件，会因为加锁导致串行执行，任务会一直轮询！");
                log("▶ ▶ ▶ 第[" + 1 + "]轮任务扫描，总待执行任务数：" + todos.size());
                AtomicInteger round = new AtomicInteger(1);
                
                while (!todos.isEmpty() && !isCancelled() && todos.stream().anyMatch(rec -> rec.getStatus() == ExecStatus.PENDING)) {
                    AtomicBoolean anyChange = new AtomicBoolean(false);
                    for (ChangeRecord rec : todos) {
                        if (isCancelled()) {
                            break;
                        }
                        if (threadTaskEstimator.getRunningTaskCount() > getSpExecutionThreads().getValue()) {
                            Thread.sleep(1);
                            continue;
                        }
                        if (rec.getStatus() != ExecStatus.PENDING) {
                            continue;
                        }
                        // 检查文件锁
                        if (FileLockManagerUtil.isLocked(rec.getFileHandle())) {
                            continue;
                        }
                        
                        // 获取来源文件的绝对路径
                        File sourceFile = rec.getFileHandle();
                        String sourcePath = sourceFile.getAbsolutePath();
                        if (!sourceFile.isDirectory()) {
                            sourcePath = sourceFile.getParent();
                        }
                        
                        // 找到该文件所在的根路径
                        String rootPath = findRootPathForFile(sourcePath);
                        
                        // 检查任务数量限制
                        PreviewView previewView = (PreviewView) getPreviewView();
                        boolean exceedLimit = false;
                        
                        // 检查全局执行数量限制
                        if (!previewView.isUnlimitedExecution()) {
                            if (globalExecutedCount.get() >= previewView.getGlobalExecutionLimit()) {
                                exceedLimit = true;
                            }
                        }
                        
                        // 检查根路径执行数量限制
                        if (!exceedLimit && !previewView.isRootPathUnlimitedExecution(rootPath)) {
                            AtomicInteger rootExecutedCount = executedCountByRootPath.computeIfAbsent(rootPath, k -> new AtomicInteger(0));
                            if (rootExecutedCount.get() >= previewView.getRootPathExecutionLimit(rootPath)) {
                                exceedLimit = true;
                            }
                        }
                        
                        if (exceedLimit) {
                            // 达到执行数量限制，跳过该任务
                            continue;
                        }
                        
                        // 获取执行线程池
                        RetryableThreadPool sourceExecutor = threadPoolManager.getExecutionThreadPool(rootPath);
                        
                        // 获取或创建该根路径的任务估算器
                        localEstimatorMap.computeIfAbsent(rootPath, k -> {
                            // 计算该根路径下的待执行任务数
                            long rootTaskCount = todos.stream()
                                    .filter(record -> {
                                        File file = record.getFileHandle();
                                        String filePath = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
                                        return findRootPathForFile(filePath).equals(k);
                                    })
                                    .count();
                            MultiThreadTaskEstimator estimator = new MultiThreadTaskEstimator(rootTaskCount, Math.max(Math.min(20, (int)rootTaskCount / 20), 1));
                            estimator.start();
                            log("▶ ▶ ▶ 为根路径创建任务估算器: " + k + "，总任务数: " + rootTaskCount);
                            return estimator;
                        });
                        
                        final String finalRootPath = rootPath;
                        sourceExecutor.execute(() -> {
                            synchronized (rec) {
                                if (rec.getStatus() == ExecStatus.PENDING &&
                                        !FileLockManagerUtil.isLocked(rec.getFileHandle())) {
                                    if (!FileLockManagerUtil.lock(rec.getFileHandle())) {
                                        return;
                                    }
                                    // 对原始文件加逻辑锁，避免并发操作同一个文件
                                    rec.setStatus(ExecStatus.RUNNING);
                                    anyChange.set(true);
                                    threadTaskEstimator.oneStarted();
                                    // 更新根路径估算器
                                    MultiThreadTaskEstimator rootEstimator = localEstimatorMap.get(finalRootPath);
                                    if (rootEstimator != null) {
                                        rootEstimator.oneStarted();
                                    }
                                    // 增加任务数量限制计数器
                                    globalExecutedCount.incrementAndGet();
                                    executedCountByRootPath.computeIfAbsent(finalRootPath, k -> new AtomicInteger(0)).incrementAndGet();
                                } else {
                                    return;
                                }
                            }
                            try {
                                // [修改] 策略执行时不再传递线程数，只负责逻辑
                                IAppStrategy s = AppStrategyFactory.findStrategyForOp(rec.getOpType(), pipelineStrategies);
                                log("▶ 开始处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
                                if (s != null) {
                                    s.execute(rec);
                                    rec.setStatus(ExecStatus.SUCCESS);
                                    log("✅️ 成功处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
                                } else {
                                    rec.setStatus(ExecStatus.SKIPPED);
                                }
                            } catch (Exception e) {
                                rec.setStatus(ExecStatus.FAILED);
                                rec.setFailReason(ExceptionUtils.getStackTrace(e));
                                logError("❌ 失败处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName() + ",原因" + e.getMessage());
                                logError("❌ 失败详细原因:" + ExceptionUtils.getStackTrace(e));
                            } finally {
                                threadTaskEstimator.oneCompleted();
                                // 更新根路径估算器
                                MultiThreadTaskEstimator rootEstimator = localEstimatorMap.get(finalRootPath);
                                if (rootEstimator != null) {
                                    rootEstimator.oneCompleted();
                                }
                                // 文件解锁
                                FileLockManagerUtil.unlock(rec.getFileHandle());
                                int c = curr.incrementAndGet();
                                Platform.runLater(() -> updateProgress(c, total));
                                if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                                    lastRefresh.set(System.currentTimeMillis());
                                    setRunningUI("▶ ▶ ▶ 执行任务进度: " + threadTaskEstimator.getDisplayInfo());
                                    refreshPreviewTableFilter();
                                }
                            }
                        });
                    }
                    // 适当Sleep，避免反复刷数据
                    // 定期更新根路径进度UI
                    if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                        lastRefresh.set(System.currentTimeMillis());
                        previewView.updateRootPathProgress();
                    }
                    Thread.sleep(100);
            }
            
            // 关闭所有线程池
            threadPoolManager.shutdownAll();
                
                // 等待所有线程池终止
                threadPoolManager.awaitTermination();
                
                return null;
            }
        };
        setStartTaskUI("▶ ▶ ▶ 执行中...", task);
        task.setOnSucceeded(e -> setFinishTaskUI("➡ ➡ ➡ 执行成功 ⬅ ⬅ ⬅", TaskStatus.SUCCESS));
        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    /**
     * 统计待执行任务数量
     */
    private long countPendingTasks() {
        return fullChangeList.stream()
                .filter(record -> record.isChanged()
                        && record.getStatus() == ExecStatus.PENDING)
                .count();
    }
    
    /**
     * 确认执行任务
     */
    private boolean confirmExecution(long count) {
        return FXDialogUtils.showConfirm("确认", "执行 " + count + " 个变更?");
    }
    
    /**
     * 准备执行UI
     */
    private void prepareExecutionUI() {
        btnGo.setDisable(true);
        btnExecute.setDisable(true);
    }
    
    /**
     * 创建执行任务
     */
    private Task<Void> createExecutionTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<ChangeRecord> todos = fullChangeList.stream()
                        .filter(record -> record.isChanged()
                                && record.getOpType() != OperationType.NONE
                                && record.getStatus() == ExecStatus.PENDING)
                        .collect(Collectors.toList());
                int total = todos.size();
                AtomicInteger curr = new AtomicInteger(0);
                int threads = getSpExecutionThreads().getValue();
                
                // 线程池和估算器管理
                final java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap = new java.util.concurrent.ConcurrentHashMap<>();
                rootPathEstimators = localEstimatorMap;
                
                // 设置线程池模式
                threadPoolManager.setThreadPoolMode(threadPoolMode);
                
                // 任务数量限制计数器
                final java.util.Map<String, AtomicInteger> executedCountByRootPath = new java.util.concurrent.ConcurrentHashMap<>();
                final AtomicInteger globalExecutedCount = new AtomicInteger(0);
                
                // 创建全局估算器
                threadTaskEstimator = new MultiThreadTaskEstimator(total, Math.max(Math.min(20, total / 20), 1));
                threadTaskEstimator.start();
                log("▶ ▶ ▶ 任务启动，并发线程: " + threads);
                log("▶ ▶ ▶ 当前线程池模式: " + threadPoolMode);
                log("▶ ▶ ▶ 注意：部分任务依赖同一个原始文件，会因为加锁导致串行执行，任务会一直轮询！");
                log("▶ ▶ ▶ 第[" + 1 + "]轮任务扫描，总待执行任务数：" + todos.size());
                AtomicInteger round = new AtomicInteger(1);
                
                while (!todos.isEmpty() && !isCancelled() && todos.stream().anyMatch(rec -> rec.getStatus() == ExecStatus.PENDING)) {
                    AtomicBoolean anyChange = new AtomicBoolean(false);
                    for (ChangeRecord rec : todos) {
                        if (isCancelled()) {
                            break;
                        }
                        if (threadTaskEstimator.getRunningTaskCount() > getSpExecutionThreads().getValue()) {
                            Thread.sleep(1);
                            continue;
                        }
                        if (rec.getStatus() != ExecStatus.PENDING) {
                            continue;
                        }
                        // 检查文件锁
                        if (FileLockManagerUtil.isLocked(rec.getFileHandle())) {
                            continue;
                        }
                        
                        // 获取来源文件的绝对路径
                        File sourceFile = rec.getFileHandle();
                        String sourcePath = sourceFile.getAbsolutePath();
                        if (!sourceFile.isDirectory()) {
                            sourcePath = sourceFile.getParent();
                        }
                        
                        // 找到该文件所在的根路径
                        String rootPath = findRootPathForFile(sourcePath);
                        
                        // 检查任务数量限制
                        boolean exceedLimit = checkExecutionLimits(rootPath, globalExecutedCount, executedCountByRootPath);
                        if (exceedLimit) {
                            continue;
                        }
                        
                        // 获取执行线程池
                        RetryableThreadPool sourceExecutor = threadPoolManager.getExecutionThreadPool(rootPath);
                        
                        // 获取或创建该根路径的任务估算器
                        createRootPathEstimatorIfNeeded(localEstimatorMap, rootPath, todos);
                        
                        final String finalRootPath = rootPath;
                        sourceExecutor.execute(() -> executeSingleTask(rec, curr, total, localEstimatorMap, anyChange,
                                finalRootPath, globalExecutedCount, executedCountByRootPath));
                    }
                    // 适当Sleep，避免反复刷数据
                    // 定期更新根路径进度UI
                    if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                        lastRefresh.set(System.currentTimeMillis());
                        previewView.updateRootPathProgress();
                    }
                    Thread.sleep(100);
                }
                
                // 关闭所有线程池
                threadPoolManager.shutdownAll();
                    
                // 等待所有线程池终止
                threadPoolManager.awaitTermination();
                
                return null;
            }
        };
    }
    
    /**
     * 检查执行限制
     */
    private boolean checkExecutionLimits(String rootPath, AtomicInteger globalExecutedCount,
            java.util.Map<String, AtomicInteger> executedCountByRootPath) {
        PreviewView previewView = (PreviewView) getPreviewView();
        boolean exceedLimit = false;
        
        // 检查全局执行数量限制
        if (!previewView.isUnlimitedExecution()) {
            if (globalExecutedCount.get() >= previewView.getGlobalExecutionLimit()) {
                exceedLimit = true;
            }
        }
        
        // 检查根路径执行数量限制
        if (!exceedLimit && !previewView.isRootPathUnlimitedExecution(rootPath)) {
            AtomicInteger rootExecutedCount = executedCountByRootPath.computeIfAbsent(rootPath, k -> new AtomicInteger(0));
            if (rootExecutedCount.get() >= previewView.getRootPathExecutionLimit(rootPath)) {
                exceedLimit = true;
            }
        }
        
        return exceedLimit;
    }
    
    /**
     * 创建根路径估算器
     */
    private void createRootPathEstimatorIfNeeded(java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap,
            String rootPath, List<ChangeRecord> todos) {
        localEstimatorMap.computeIfAbsent(rootPath, k -> {
            // 计算该根路径下的待执行任务数
            long rootTaskCount = todos.stream()
                    .filter(record -> {
                        File file = record.getFileHandle();
                        String filePath = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
                        return findRootPathForFile(filePath).equals(k);
                    })
                    .count();
            MultiThreadTaskEstimator estimator = new MultiThreadTaskEstimator(rootTaskCount, Math.max(Math.min(20, (int)rootTaskCount / 20), 1));
            estimator.start();
            log("▶ ▶ ▶ 为根路径创建任务估算器: " + k + "，总任务数: " + rootTaskCount);
            return estimator;
        });
    }
    
    /**
     * 执行单个任务
     */
    private void executeSingleTask(ChangeRecord rec, AtomicInteger curr, int total,
            java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap, AtomicBoolean anyChange,
            String finalRootPath, AtomicInteger globalExecutedCount,
            java.util.Map<String, AtomicInteger> executedCountByRootPath) {
        synchronized (rec) {
            if (rec.getStatus() == ExecStatus.PENDING &&
                    !FileLockManagerUtil.isLocked(rec.getFileHandle())) {
                if (!FileLockManagerUtil.lock(rec.getFileHandle())) {
                    return;
                }
                // 对原始文件加逻辑锁，避免并发操作同一个文件
                rec.setStatus(ExecStatus.RUNNING);
                anyChange.set(true);
                threadTaskEstimator.oneStarted();
                // 更新根路径估算器
                MultiThreadTaskEstimator rootEstimator = localEstimatorMap.get(finalRootPath);
                if (rootEstimator != null) {
                    rootEstimator.oneStarted();
                }
                // 增加任务数量限制计数器
                globalExecutedCount.incrementAndGet();
                executedCountByRootPath.computeIfAbsent(finalRootPath, k -> new AtomicInteger(0)).incrementAndGet();
            } else {
                return;
            }
        }
        try {
            // 执行策略
            executeStrategyForTask(rec);
        } catch (Exception e) {
            rec.setStatus(ExecStatus.FAILED);
            rec.setFailReason(ExceptionUtils.getStackTrace(e));
            logError("❌ 失败处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName() + ",原因" + e.getMessage());
            logError("❌ 失败详细原因:" + ExceptionUtils.getStackTrace(e));
        } finally {
            // 完成任务处理
            completeSingleTask(rec, curr, total, localEstimatorMap, finalRootPath);
        }
    }
    
    /**
     * 为任务执行策略
     */
    private void executeStrategyForTask(ChangeRecord rec) throws Exception {
        IAppStrategy s = AppStrategyFactory.findStrategyForOp(rec.getOpType(), pipelineStrategies);
        log("▶ 开始处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
        if (s != null) {
            s.execute(rec);
            rec.setStatus(ExecStatus.SUCCESS);
            log("✅️ 成功处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
        } else {
            rec.setStatus(ExecStatus.SKIPPED);
        }
    }
    
    /**
     * 完成单个任务处理
     */
    private void completeSingleTask(ChangeRecord rec, AtomicInteger curr, int total,
            java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap, String finalRootPath) {
        threadTaskEstimator.oneCompleted();
        // 更新根路径估算器
        MultiThreadTaskEstimator rootEstimator = localEstimatorMap.get(finalRootPath);
        if (rootEstimator != null) {
            rootEstimator.oneCompleted();
        }
        // 文件解锁
        FileLockManagerUtil.unlock(rec.getFileHandle());
        int c = curr.incrementAndGet();
        if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
            lastRefresh.set(System.currentTimeMillis());
            setRunningUI("▶ ▶ ▶ 执行任务进度: " + threadTaskEstimator.getDisplayInfo());
            refreshPreviewTableFilter();
        }
    }
    
    // --- Shared Methods & Utils ---

    @Override
    public void refreshPreviewTableFilter() {
        previewView.refresh();
    }
    
    public PreviewView getPreviewView() {
        return previewView;
    }

    private void updateStats() {
        previewView.updateStats();
    }
    
    /**
     * 获取指定根路径的最大线程数
     * @param rootPath 根路径
     * @return 线程数，如果没有配置则使用默认的执行线程数
     */
    private int getRootPathMaxThreads(String rootPath) {
        return rootPathThreadConfig.getOrDefault(rootPath, getSpExecutionThreads().getValue());
    }
    
    /**
     * 设置指定根路径的预览线程数
     * @param rootPath 根路径
     * @param maxThreads 最大线程数
     */
    public void setRootPathPreviewThreads(String rootPath, int maxThreads) {
        threadPoolManager.setRootPathPreviewThreads(rootPath, maxThreads);
        log("▶ ▶ ▶ 根路径预览线程数已调整: " + rootPath + "，新线程数: " + maxThreads);
    }
    
    /**
     * 设置指定根路径的执行线程数
     * @param rootPath 根路径
     * @param maxThreads 最大线程数
     */
    public void setRootPathExecutionThreads(String rootPath, int maxThreads) {
        threadPoolManager.setRootPathExecutionThreads(rootPath, maxThreads);
        log("▶ ▶ ▶ 根路径执行线程数已调整: " + rootPath + "，新线程数: " + maxThreads);
    }
    
    /**
     * 获取线程池模式
     * @return 线程池模式
     */
    public String getThreadPoolMode() {
        return threadPoolMode;
    }
    
    /**
     * 设置线程池模式
     * @param threadPoolMode 线程池模式
     * @return 是否成功设置模式
     */
    public boolean setThreadPoolMode(String threadPoolMode) {
        if (isTaskRunning.get()) {
            logError("❌ 任务正在运行，不允许切换线程池模式！");
            FXDialogUtils.showAlert("错误", "任务正在运行，不允许切换线程池模式！", Alert.AlertType.ERROR);
            return false;
        }
        this.threadPoolMode = threadPoolMode;
        log("▶ ▶ ▶ 线程池模式已切换: " + threadPoolMode);
        return true;
    }
    
    /**
     * 获取指定根路径的任务估算器
     * @param rootPath 根路径
     * @return MultiThreadTaskEstimator实例
     */
    public MultiThreadTaskEstimator getRootPathEstimator(String rootPath) {
        if (rootPathEstimators == null) {
            return null;
        }
        return rootPathEstimators.get(rootPath);
    }
    
    /**
     * 获取所有根路径线程数配置
     * @return 根路径线程数配置
     */
    public java.util.Map<String, Integer> getRootPathThreadConfig() {
        return rootPathThreadConfig;
    }
    
    /**
     * 找到给定文件路径对应的根路径
     * @param filePath 文件路径
     * @return 对应的根路径，如果没有找到则返回文件路径本身
     */
    private String findRootPathForFile(String filePath) {
        try {
            Path fileP = Paths.get(filePath).toAbsolutePath().normalize();
            for (File root : sourceRoots) {
                Path rootP = root.toPath().toAbsolutePath().normalize();
                if (fileP.startsWith(rootP)) {
                    return rootP.toString();
                }
            }
        } catch (Exception e) {
            logError("查找根路径失败: " + e.getMessage());
        }
        // 如果没有找到对应的根路径，返回文件路径本身
        return filePath;
    }

    private List<File> scanFilesRobust(File root, int maxDepth, Consumer<String> msg) {
        AtomicInteger countScan = new AtomicInteger(0);
        AtomicInteger countIgnore = new AtomicInteger(0);
        List<File> list = new ArrayList<>();
        if (!root.exists()) return list;
        int threads = getSpPreviewThreads().getValue();
        try (Stream<Path> s = ParallelStreamWalker.walk(root.toPath(), maxDepth, threads)) {
            list = s.filter(p -> {
                try {
                    if (!isTaskRunning.get()) {
                        throw new RuntimeException("已中断");
                    }
                    if (globalSettingsView.isFileIncluded(p.toFile())) {
                        return true;
                    }
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
                    logError(path + " 文件扫描异常: " + e.getMessage());
                    return false;
                }
                return true;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (Exception e) {
            logError("扫描文件失败：" + ExceptionUtils.getStackTrace(e));
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

    private void setStartTaskUI(String msg, Task task) {
        btnStop.setDisable(false);
        isTaskRunning.set(true);
        lastRefresh.set(System.currentTimeMillis());
        // 设置进度条为绿色
        ProgressBarDisplay.updateProgressStatus(previewView.getMainProgressBar(), TaskStatus.RUNNING);
        previewView.getMainProgressBar().progressProperty().unbind();
        previewView.getMainProgressBar().progressProperty().set(0);
        if (task != null) {
            previewView.getMainProgressBar().progressProperty().bind(task.progressProperty());
        }
        previewView.updateRunningProgress(msg);
        refreshPreviewTableFilter();
        updateStats();
    }

    private void setRunningUI(String msg) {
        previewView.updateRunningProgress(msg);
        updateStats();
    }

    /**
     * 状态,建议颜色,Hex 代码,视觉感受
     * 执行中 (Running),天蓝色,#BDE0FE,清爽、宁静，表示正在进行
     * 成功 (Success),薄荷绿,#B9FBC0,健康、完成，给予正面反馈
     * 失败 (Failure),珊瑚粉,#FFADAD,柔和的警告，不刺眼但明确
     * 取消 (Canceled),奶油黄/淡灰,#FDFFB6,中性色，表示任务已停止
     *
     * @param msg
     * @param status
     */
    private void setFinishTaskUI(String msg, TaskStatus status) {
        btnGo.setDisable(false);
        btnExecute.setDisable(false);
        btnStop.setDisable(true);
        isTaskRunning.set(false);
        this.setRunningState(false);
        previewView.updateRunningProgress(msg);
        if (TaskStatus.CANCELED == status) {
            previewView.getMainProgressBar().progressProperty().unbind();
            previewView.getMainProgressBar().progressProperty().set(0);
        }
        if (TaskStatus.SUCCESS == status) {
            previewView.getMainProgressBar().progressProperty().unbind();
            previewView.getMainProgressBar().progressProperty().set(1.0);
        }
        // 设置进度条为颜色
        ProgressBarDisplay.updateProgressStatus(previewView.getMainProgressBar(), status);
        refreshPreviewTableFilter();
        updateStats();
        currentTask = null;
    }

    // UI Update Methods
    public void setRunningState(boolean running) {
        btnExecute.setDisable(running);
        btnStop.setDisable(!running);
    }

    private void handleTaskLifecycle(Task<?> t) {
        currentTask = t;
        previewView.bindProgress(t);
        t.setOnFailed(e -> {
            btnExecute.setDisable(false);
            setFinishTaskUI("❌ ❌ ❌ 出错 ❌ ❌ ❌", TaskStatus.FAILURE);
            logError("❌ 失败: " + ExceptionUtils.getStackTrace(e.getSource().getException()));
        });
        t.setOnCancelled(e -> {
            setFinishTaskUI("🛑 🛑 🛑 已取消 🛑 🛑 🛑", TaskStatus.CANCELED);
        });
    }

    @Override
    public void forceStop() {
        if (isTaskRunning.get()) {
            isTaskRunning.set(false);
            if (currentTask != null) {
                currentTask.cancel();
            }
            if (executorService != null) {
                executorService.shutdownNow();
            }
            log("🛑 强制停止");
            setFinishTaskUI("🛑 🛑 🛑 已停止 🛑 🛑 🛑", TaskStatus.CANCELED);
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
    public void addStrategyStep(IAppStrategy template) {
        this.pipelineStrategies.add(template);
    }

    @Override
    public void removeStrategyStep(IAppStrategy strategy) {
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
    public List<IAutoReloadAble> getAutoReloadNodes() {
        return autoReloadNodes;
    }

    @Override
    public void invalidatePreview(String r) {
        if (!fullChangeList.isEmpty()) {
            fullChangeList.clear();
            previewView.getPreviewTable().setRoot(null);
            log(r);
        }
        btnExecute.setDisable(true);
    }

    @Override
    // --- Config IO (包含线程数保存) ---
    public void log(String s) {
        logView.appendLog(new LogInfo(LogType.INFO, s));
    }

    @Override
    public void logError(String s) {
        logView.appendLog(new LogInfo(LogType.ERROR, s));
    }

    @Override
    public Node getGlobalSettingsView() {
        return globalSettingsView.getViewNode();
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
        TextField tp = new TextField(currentTheme.getBgImagePath());
        JFXButton bp = new JFXButton("背景...");
        bp.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) {
                tp.setText(f.getAbsolutePath());
                currentTheme.setBgImagePath(f.getAbsolutePath());
                applyAppearance();
            }
        });
        g.add(StyleFactory.createChapter("Color:"), 0, 0);
        g.add(cp, 1, 0);
        g.add(StyleFactory.createChapter("Opacity:"), 0, 1);
        g.add(sl, 1, 1);
        g.add(chk, 1, 2);
        g.add(StyleFactory.createChapter("BG:"), 0, 3);
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
        if (!currentTheme.getBgImagePath().isEmpty()) {
            try {
                backgroundImageView.setImage(new Image(Files.newInputStream(Paths.get(currentTheme.getBgImagePath()))));
            } catch (Exception e) {
                logError("背景图加载失败："+ExceptionUtils.getStackTrace(e));
            }
        }
        if (composeView != null) composeView.refreshList();
    }

}