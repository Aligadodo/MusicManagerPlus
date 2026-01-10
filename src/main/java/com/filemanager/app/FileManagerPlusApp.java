package com.filemanager.app;

import com.filemanager.app.baseui.ComposeView;
import com.filemanager.app.baseui.GlobalSettingsView;
import com.filemanager.app.baseui.LogView;
import com.filemanager.app.baseui.PreviewView;
import com.filemanager.app.components.AppearanceManager;
import com.filemanager.app.components.FileScanner;
import com.filemanager.app.components.PipelineManager;
import com.filemanager.app.components.tools.ConfigFileManager;
import com.filemanager.app.components.tools.MultiThreadTaskEstimator;
import com.filemanager.base.IAppController;
import com.filemanager.base.IAppStrategy;
import com.filemanager.base.IAutoReloadAble;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import com.filemanager.strategy.AppStrategyFactory;
import com.filemanager.tool.RetryableThreadPool;
import com.filemanager.tool.ThreadPoolManager;
import com.filemanager.tool.display.ProgressBarDisplay;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.tool.log.LogInfo;
import com.filemanager.tool.log.LogType;
import com.filemanager.type.TaskStatus;
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
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
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

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
    @Getter
    private final AtomicBoolean taskRunningStatus = new AtomicBoolean(false);
    /**
     * -- GETTER --
     * 获取所有根路径线程数配置
     *
     * @return 根路径线程数配置
     */
    // 存储根路径线程配置：存储每个根路径对应的最大线程数
    @Getter
    private final Map<String, Integer> rootPathThreadConfig = new ConcurrentHashMap<>();

    // 存储每个根路径对应的线程池（仅在任务执行期间有效）
    private final Map<String, RetryableThreadPool> executorMap = new HashMap<>();

    // 存储每个根路径对应的任务估算器（仅在任务执行期间有效）
    private final Map<String, MultiThreadTaskEstimator> rootPathEstimators = new HashMap<>();

    @Getter
    private final long taskStartTimStamp = System.currentTimeMillis();

    private List<IAppStrategy> strategyPrototypes;
    // 线程池管理器
    private ThreadPoolManager threadPoolManager;

    // --- UI Controls ---
    @Getter
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
    // --- Business Modules ---
    private PipelineManager pipelineManager;
    private FileScanner fileScanner;
    private AppearanceManager appearanceManager;
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
        // 3. 构建主布局
        this.primaryStage = primaryStage;
        rootContainer = new StackPane();
        backgroundImageView = new ImageView();
        backgroundImageView.setPreserveRatio(false);
        backgroundImageView.fitWidthProperty().bind(rootContainer.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootContainer.heightProperty());
        backgroundOverlay = new Region();
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

        // 3. 业务模块初始化
        this.pipelineManager = new PipelineManager(this, threadPoolManager);
        this.fileScanner = new FileScanner(this, globalSettingsView);
        this.appearanceManager = new AppearanceManager(this, currentTheme, backgroundImageView, backgroundOverlay);

        // 监听源目录列表变化，自动更新根路径线程配置UI
        sourceRoots.addListener((javafx.collections.ListChangeListener.Change<? extends File> change) -> {
            Platform.runLater(() -> previewView.updateRootPathThreadConfigUI());
        });
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
            log("动态调整全局运行线程池大小: " + oldVal + " -> " + newVal);
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
        pipelineManager.runPipelineAnalysis();
    }

    @Override
    public void runPipelineExecution() {
        pipelineManager.runPipelineExecution();
    }

    // --- Shared Methods & Utils ---

    @Override
    public void refreshPreviewTableFilter() {
        previewView.refresh();
    }

    @Override
    public void updateStats() {
        previewView.updateStats();
    }

    /**
     * 设置指定根路径的预览线程数
     *
     * @param rootPath   根路径
     * @param maxThreads 最大线程数
     */
    public void setRootPathPreviewThreads(String rootPath, int maxThreads) {
        threadPoolManager.setRootPathPreviewThreads(rootPath, maxThreads);
        log("▶ ▶ ▶ 根路径预览线程数已调整: " + rootPath + "，新线程数: " + maxThreads);
    }

    /**
     * 设置指定根路径的执行线程数
     *
     * @param rootPath   根路径
     * @param maxThreads 最大线程数
     */
    public void setRootPathExecutionThreads(String rootPath, int maxThreads) {
        threadPoolManager.setRootPathExecutionThreads(rootPath, maxThreads);
        log("▶ ▶ ▶ 根路径执行线程数已调整: " + rootPath + "，新线程数: " + maxThreads);
    }

    @Override
    public void setRunningUI(String msg) {
        previewView.updateRunningProgress(msg);
        updateStats();
    }

    @Override
    public String findRootPathForFile(String filePath) {
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

    @Override
    public PreviewView getPreviewView() {
        return previewView;
    }

    @Override
    public void setFullChangeList(List<ChangeRecord> changeList) {
        this.fullChangeList = changeList;
    }

    @Override
    public void changeExecuteButton(boolean enabled) {
        btnExecute.setDisable(!enabled);
    }

    @Override
    public void changePreviewButton(boolean enabled) {
        btnGo.setDisable(!enabled);
    }

    @Override
    public void changeStopButton(boolean enabled) {
        btnStop.setDisable(!enabled);
    }

    @Override
    public void updateProgressStatus(TaskStatus status) {
        ProgressBarDisplay.updateProgressStatus(previewView.getMainProgressBar(), status);
    }

    @Override
    public void bindProgress(Task<?> task) {
        previewView.getMainProgressBar().progressProperty().unbind();
        if (task != null) {
            previewView.getMainProgressBar().progressProperty().bind(task.progressProperty());
        }
    }

    @Override
    public void updateRunningProgress(String msg) {
        previewView.updateRunningProgress(msg);
    }

    @Override
    public void refreshComposeView() {
        if (composeView != null) {
            composeView.refreshList();
        }
    }

    @Override
    public List<File> scanFilesRobust(File root, int minDepth, int maxDepth, Consumer<String> msg) {
        return fileScanner.scanFilesRobust(root, minDepth, maxDepth, msg);
    }

    @Override
    public boolean setThreadPoolMode(String newVal) {
        if (taskRunningStatus.get()) {
            logError("❌ 任务正在运行，不允许切换线程池模式！");
            return false;
        }
        this.threadPoolManager.setThreadPoolMode(newVal);
        this.log("▶ ▶ ▶ 线程池模式已切换: " + newVal);
        return true;
    }

    @Override
    public MultiThreadTaskEstimator getRootPathEstimator(String rootPath) {
        return pipelineManager.getRootPathEstimator(rootPath);
    }

    @Override
    public void forceStop() {
        pipelineManager.forceStop();
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
        appearanceManager.showAppearanceDialog();
    }

    public void applyAppearance() {
        appearanceManager.applyAppearance();
    }
}