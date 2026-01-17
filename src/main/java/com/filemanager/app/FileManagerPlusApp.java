/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app;

import com.filemanager.app.base.IAppController;
import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.base.IAutoReloadAble;
import com.filemanager.app.components.FileScanner;
import com.filemanager.app.components.PipelineManager;
import com.filemanager.app.tools.ConfigFileManager;
import com.filemanager.app.tools.MultiThreadTaskEstimator;
import com.filemanager.app.tools.display.ProgressBarDisplay;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.app.tools.display.ThemeConfig;
import com.filemanager.app.tools.display.ThemeManager;
import com.filemanager.app.ui.AppearanceManager;
import com.filemanager.app.ui.ComposeView;
import com.filemanager.app.ui.GlobalSettingsView;
import com.filemanager.app.ui.LogView;
import com.filemanager.app.ui.PreviewView;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.AppStrategyFactory;
import com.filemanager.tool.ThreadPoolManager;
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
import java.util.concurrent.atomic.AtomicInteger;
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

    @Override
    public AtomicBoolean getTaskRunningStatus() {
        return taskRunningStatus;
    }
    /**
     * -- GETTER --
     * 获取所有根路径线程数配置
     *
     * @return 根路径线程数配置
     */
    // 存储根路径线程配置：存储每个根路径对应的最大线程数
    @Getter
    private final Map<String, Integer> rootPathThreadConfig = new ConcurrentHashMap<>();

    @Override
    public Map<String, Integer> getRootPathThreadConfig() {
        return rootPathThreadConfig;
    }

    @Getter
    private long taskStartTimStamp = System.currentTimeMillis();

    @Override
    public long getTaskStartTimStamp() {
        return taskStartTimStamp;
    }

    private List<IAppStrategy> strategyPrototypes;
    // 线程池管理器
    private ThreadPoolManager threadPoolManager;

    // --- UI Controls ---
    @Getter
    private JFXCheckBox autoRun;

    @Override
    public JFXCheckBox getAutoRun() {
        return autoRun;
    }
    private JFXButton btnGo, btnExecute, btnStop;
    @Getter
    private Stage primaryStage;

    @Override
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    // --- Infrastructure ---
    private ConfigFileManager configManager;
    @Getter
    private List<ChangeRecord> fullChangeList = new ArrayList<>();

    @Override
    public List<ChangeRecord> getFullChangeList() {
        return fullChangeList;
    }
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
        // 初始化主题管理器并设置当前主题
        ThemeManager.getInstance().updateCurrentTheme(theme -> {
            // 使用当前主题配置覆盖默认设置
            theme.setBgColor(currentTheme.getBgColor());
            theme.setAccentColor(currentTheme.getAccentColor());
            theme.setTextPrimaryColor(currentTheme.getTextPrimaryColor());
            theme.setTextSecondaryColor(currentTheme.getTextSecondaryColor());
            theme.setTextTertiaryColor(currentTheme.getTextTertiaryColor());
            theme.setTextDisabledColor(currentTheme.getTextDisabledColor());
            theme.setGlassOpacity(currentTheme.getGlassOpacity());
            theme.setDarkBackground(currentTheme.isDarkBackground());
            theme.setCornerRadius(currentTheme.getCornerRadius());
        });
        // 初始化样式工厂
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
        
        // 确保所有组件应用最新主题样式
        Platform.runLater(() -> {
            // 延迟执行，确保所有UI组件已完全初始化
            StyleFactory.refreshAllComponents(rootContainer);
        });

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
        menuBar.getMenus().add(fileMenu);
        
        // 应用菜单样式
        StyleFactory.setMenuStyle(menuBar);

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
        // 应用主题样式，使用透明背景
        header.setStyle(String.format(
                "-fx-background-color: transparent; -fx-border-color: %s; -fx-border-width: 0 0 %.1f 0;",
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        Label logo = new Label("MUSIC MANAGER PLUS - By chrse1997@163.com");
        logo.setFont(Font.font(currentTheme.getFontFamily(), FontWeight.BLACK, 20));
        logo.setTextFill(Color.web(currentTheme.getTextPrimaryColor()));
        logo.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        header.getChildren().addAll(logo, new Region(), menuBar, autoRun, btnGo, btnExecute, btnStop);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        VBox top = new VBox(header, new Separator());
        root.setTop(top);

        // Center Content with visible Tabs
        mainTabPane = StyleFactory.createTabPane();
        // 将各模块的 View 挂载到 Tab
        mainTabPane.getTabs().addAll(composeView.getTab(), previewView.getTab(), logView.getTab());
        
        // 添加界面设置tab页
        Tab appearanceTab = new Tab("界面设置");
        appearanceTab.setContent(appearanceManager.getAppearanceSettingsContent());
        mainTabPane.getTabs().add(appearanceTab);
        
        mainTabPane.setPadding(new Insets(10));
        root.setCenter(mainTabPane);
        // 移除侧边栏菜单，使用TabPane进行视图切换

        // Status Bar
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setStyle(String.format(
                "-fx-background-color: rgba(240, 240, 240, 0.8); -fx-border-color: %s; -fx-border-width: %.1f 0 0 0;",
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        Label lblStatusIcon = new Label("●");
        lblStatusIcon.setTextFill(Color.GREEN);
        Label lblReady = StyleFactory.createChapter("就绪");
        statusBar.getChildren().addAll(lblStatusIcon, lblReady); // Stats are now managed by PreviewView internally or via explicit update
        root.setBottom(statusBar);

        // 设置默认选中的标签页
        mainTabPane.getSelectionModel().selectFirst();

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

    @Override
    public void switchView(Node node) {
        // 适配新的TabPane实现，通过选择对应的Tab来切换视图
        if (mainTabPane == null) {
            return;
        }
        
        // 遍历所有Tab，找到内容与指定节点匹配的Tab
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getContent() == node) {
                mainTabPane.getSelectionModel().select(tab);
                break;
            }
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
        taskStartTimStamp = System.currentTimeMillis();
        pipelineManager.runPipelineAnalysis();
    }

    @Override
    public void runPipelineExecution() {
         taskStartTimStamp = System.currentTimeMillis();
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
    public StackPane getRootContainer() {
        return rootContainer;
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
    public List<File> scanFilesRobust(File root, int minDepth, int maxDepth, AtomicInteger globalLimit, AtomicInteger dirLimit, Consumer<String> msg) {
        return fileScanner.scanFilesRobust(root, minDepth, maxDepth, globalLimit, dirLimit, msg);
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