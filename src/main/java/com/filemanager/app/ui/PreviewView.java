/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.app.ui;


import com.filemanager.app.FileManagerPlusApp;
import com.filemanager.app.base.IAppController;
import com.filemanager.app.base.IAutoReloadAble;
import com.filemanager.app.tools.MultiThreadTaskEstimator;
import com.filemanager.app.tools.display.DetailWindowHelper;
import com.filemanager.app.tools.display.FXDialogUtils;
import com.filemanager.app.tools.display.FloatingTooltip;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.app.tools.display.ThemeConfig;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.ThreadPoolManager;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.util.file.FileSizeFormatUtil;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public class PreviewView implements IAutoReloadAble {
    private static final long AUTO_REFRESH_INTERVAL = 10000; // 10秒自动刷新一次
    private final IAppController app;
    private final Tab tabPreview;
    private final Map<String, Spinner<Integer>> rootPathPreviewLimits = new HashMap<>();
    private final Map<String, Spinner<Integer>> rootPathExecutionLimits = new HashMap<>();
    private final Map<String, JFXCheckBox> rootPathUnlimitedPreview = new HashMap<>();
    private final Map<String, JFXCheckBox> rootPathUnlimitedExecution = new HashMap<>();
    private final Map<String, Spinner<Integer>> rootPathSpinners = new HashMap<>();
    private final Map<String, ProgressBar> rootPathProgressBars = new HashMap<>();
    private final Map<String, Label> rootPathProgressLabels = new HashMap<>();
    private VBox viewNode;
    // UI Components
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label runningLabel, statsLabel;
    private JFXTextField txtSearchFilter;
    private JFXComboBox<String> cbStatusFilter;
    private JFXComboBox<String> cbOperationTypeFilter;
    private JFXCheckBox chkHideUnchanged;
    private Spinner<Integer> spPreviewThreads;
    private Spinner<Integer> spExecutionThreads;
    // 全选复选框
    private JFXCheckBox chkSelectAll;
    // 删除按钮
    private JFXButton btnDeleteOriginal;
    private JFXButton btnDeleteTarget;
    private JFXButton btnExecuteSelected;

    public TreeTableView<ChangeRecord> getPreviewTable() {
        return previewTable;
    }

    public JFXComboBox<String> getCbThreadPoolMode() {
        return cbThreadPoolMode;
    }

    private JFXComboBox<Integer> numberDisplay;
    private JFXComboBox<String> cbThreadPoolMode; // 线程池模式选择：共享或根路径独立
    // 数量上限配置UI
    private Spinner<Integer> spGlobalPreviewLimit;
    private Spinner<Integer> spGlobalExecutionLimit;
    private JFXCheckBox chkUnlimitedPreview;
    private JFXCheckBox chkUnlimitedExecution;
    // 超时设置配置UI
    private Spinner<Integer> spPreviewTimeout;
    private Spinner<Integer> spExecutionTimeout;
    private JFXCheckBox chkUnlimitedPreviewTimeout;
    private JFXCheckBox chkUnlimitedExecutionTimeout;
    // 根路径线程数配置UI
    private VBox rootPathThreadConfigBox;
    // 自动刷新相关
    private boolean autoRefreshEnabled = false;
    private JFXCheckBox chkAutoRefresh;
    private ScheduledExecutorService autoRefreshExecutor;

    // 配置面板相关
    private TitledPane localParamsPane;
    private TitledPane configPane;
    private TitledPane globalParamsPane;
    private VBox configContent;

    public PreviewView(IAppController app) {
        this.app = app;
        this.tabPreview = new Tab("预览");
        this.initControls();
        this.buildUI();
        StyleFactory.setBasicStyle(viewNode);
        this.tabPreview.setContent(viewNode);
    }

    private void initControls() {
        txtSearchFilter = new JFXTextField();
        txtSearchFilter.setPromptText("请输入关键词进行搜索...");
        // 添加透明度效果
        ThemeConfig theme = StyleFactory.getTheme();
        String bgColor = theme.getListBgColor();
        if (bgColor.startsWith("#") && bgColor.length() == 7) {
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            bgColor = bgColor + alphaHex;
        }
        txtSearchFilter.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: %.1f; -fx-background-radius: %.1f; -fx-padding: 4 8; -fx-font-size: 12px;",
                bgColor, theme.getBorderColor(), theme.getCornerRadius(), theme.getCornerRadius()
        ));
        cbStatusFilter = new JFXComboBox<>(FXCollections.observableArrayList("全部", "执行中", "成功", "失败", "跳过", "无需处理"));
        cbStatusFilter.getSelectionModel().select(0);

        // 初始化操作类型筛选下拉框
        ObservableList<String> operationTypes = FXCollections.observableArrayList("全部");
        for (OperationType type : OperationType.values()) {
            operationTypes.add(type.name);
        }
        cbOperationTypeFilter = new JFXComboBox<>(operationTypes);
        cbOperationTypeFilter.getSelectionModel().select(0);

        chkHideUnchanged = new JFXCheckBox("仅显示变更");
        chkHideUnchanged.setSelected(true);

        // 自动刷新复选框
        chkAutoRefresh = new JFXCheckBox("自动刷新");
        chkAutoRefresh.setSelected(true);
        chkAutoRefresh.selectedProperty().addListener((obs, oldVal, newVal) -> toggleAutoRefresh(newVal));

        txtSearchFilter.textProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        cbStatusFilter.valueProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        cbOperationTypeFilter.valueProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());

        // 添加搜索和过滤组件的提示信息
        FloatingTooltip.bindToNode(txtSearchFilter, "搜索过滤", java.util.Arrays.asList(
                "输入关键词搜索文件",
                "支持文件名和路径搜索",
                "实时过滤显示结果"
        ));

        FloatingTooltip.bindToNode(cbStatusFilter, "状态筛选", java.util.Arrays.asList(
                "筛选文件执行状态",
                "可选择：全部、执行中、成功、失败、跳过、无需处理"
        ));

        FloatingTooltip.bindToNode(cbOperationTypeFilter, "操作类型筛选", java.util.Arrays.asList(
                "筛选文件操作类型",
                "根据不同的操作类型过滤结果"
        ));

        FloatingTooltip.bindToNode(chkHideUnchanged, "仅显示变更", java.util.Arrays.asList(
                "勾选后只显示有变更的文件",
                "不显示无需处理的文件"
        ));

        FloatingTooltip.bindToNode(chkAutoRefresh, "自动刷新", java.util.Arrays.asList(
                "启用后每隔10秒自动刷新预览列表",
                "保持数据实时性"
        ));

        // 设置预览数量 默认200
        numberDisplay = new JFXComboBox<>(FXCollections.observableArrayList(50, 100, 200, 500, 1000, 2000, 5000));
        numberDisplay.getSelectionModel().selectFirst();

        FloatingTooltip.bindToNode(numberDisplay, "显示数量限制", java.util.Arrays.asList(
                "设置预览表格显示的最大文件数量",
                "选择合适的值以提高性能"
        ));

        mainProgressBar = StyleFactory.createMainProgressBar(0);
        runningLabel = StyleFactory.createChapter("无执行中任务");
        statsLabel = StyleFactory.createHeader("暂无统计信息");

        previewTable = new TreeTableView<>();
        previewTable.setRoot(new TreeItem<>());
        previewTable.setShowRoot(false);
        previewTable.setEditable(false);
        previewTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        setupPreviewColumns();
        setupPreviewRows();
        // 添加透明度效果，使用更高的透明度值
        String tableBgColor = theme.getListBgColor();
        if (tableBgColor.startsWith("#") && tableBgColor.length() == 7) {
            // 增加透明度值，使表格更透明
            int alpha = (int) (theme.getGlassOpacity() * 200); // 降低不透明度，使表格更透明
            String alphaHex = String.format("%02x", alpha);
            tableBgColor = tableBgColor + alphaHex;
        }
        previewTable.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;",
                tableBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius()
        ));

        spPreviewThreads = new Spinner<>(1, 16, 10);
        spPreviewThreads.setEditable(true);
        spPreviewThreads.setTooltip(new Tooltip("预览线程数：用于文件扫描和分析"));
        spPreviewThreads.setPrefWidth(60);
        spPreviewThreads.setMaxWidth(60);

        spExecutionThreads = new Spinner<>(1, 12, 4);
        spExecutionThreads.setEditable(true);
        spExecutionThreads.setTooltip(new Tooltip("执行线程数：用于管道任务执行"));
        spExecutionThreads.setPrefWidth(60);
        spExecutionThreads.setMaxWidth(60);

        // 线程池模式选择
        cbThreadPoolMode = new JFXComboBox<>(FXCollections.observableArrayList(ThreadPoolManager.MODE_GLOBAL, ThreadPoolManager.MODE_ROOT_PATH));
        cbThreadPoolMode.getSelectionModel().select(0); // 默认使用全局统一配置
        cbThreadPoolMode.setTooltip(new Tooltip("选择线程池模式：全局统一配置或根路径独立配置"));
        cbThreadPoolMode.valueProperty().addListener((o, oldVal, newVal) -> {
            // 检查任务是否正在运行
            if (app.getTaskRunningStatus().get()) {
                // 任务正在运行，显示提示并恢复原来的选择
                FXDialogUtils.showToast(app.getPrimaryStage(), "任务执行中，无法切换线程池模式！", FXDialogUtils.ToastType.INFO);
                cbThreadPoolMode.getSelectionModel().select(oldVal);
                return;
            }

            // 调用App的方法切换线程池模式
            boolean success = app.setThreadPoolMode(newVal);
            if (success) {
                // 线程池模式切换成功，更新根路径配置区域的可见性
                boolean isRootPathMode = ThreadPoolManager.MODE_ROOT_PATH.equals(newVal);
                rootPathThreadConfigBox.setDisable(!isRootPathMode);

                // 切换为根目录模式时，禁用主的预览线程、运行线程数设置
                spPreviewThreads.setDisable(isRootPathMode);
                spExecutionThreads.setDisable(isRootPathMode);

                // 控制局部参数配置面板的显示
                if (isRootPathMode) {
                    // 如果是根路径模式，显示局部参数面板
                    if (!configContent.getChildren().contains(localParamsPane)) {
                        configContent.getChildren().add(localParamsPane);
                    }
                    // 自动展开局部参数面板
                    localParamsPane.setExpanded(true);
                } else {
                    // 如果是全局模式，隐藏局部参数面板
                    configContent.getChildren().remove(localParamsPane);
                    // 清空所有根路径线程配置
                    app.getRootPathThreadConfig().clear();
                }

                updateRootPathThreadConfigUI();
            } else {
                // 切换失败，恢复原来的选择
                cbThreadPoolMode.getSelectionModel().select(oldVal);
            }
        });

        // 数量上限配置初始化
        spGlobalPreviewLimit = new Spinner<>(1, 10000, 100);
        spGlobalPreviewLimit.setEditable(true);
        spGlobalPreviewLimit.setPrefWidth(80);
        spGlobalPreviewLimit.setTooltip(new Tooltip("全局预览数量上限"));
        spGlobalPreviewLimit.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.FALSE.equals(newValue)) { // 当失去焦点时
                spGlobalPreviewLimit.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
            }
        });

        spGlobalExecutionLimit = new Spinner<>(1, 10000, 100);
        spGlobalExecutionLimit.setEditable(true);
        spGlobalExecutionLimit.setPrefWidth(80);
        spGlobalExecutionLimit.setTooltip(new Tooltip("全局执行数量上限"));
        spGlobalExecutionLimit.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.FALSE.equals(newValue)) { // 当失去焦点时
                spGlobalExecutionLimit.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
            }
        });

        chkUnlimitedPreview = new JFXCheckBox("不限制");
        chkUnlimitedPreview.setSelected(true);
        chkUnlimitedPreview.setTooltip(new Tooltip("不限制预览数量"));
        chkUnlimitedPreview.selectedProperty().addListener((obs, oldVal, newVal) -> {
            spGlobalPreviewLimit.setDisable(newVal);
        });

        chkUnlimitedExecution = new JFXCheckBox("不限制");
        chkUnlimitedExecution.setSelected(true);
        chkUnlimitedExecution.setTooltip(new Tooltip("不限制执行数量"));
        chkUnlimitedExecution.selectedProperty().addListener((obs, oldVal, newVal) -> {
            spGlobalExecutionLimit.setDisable(newVal);
        });
        
        // 超时设置初始化
        spPreviewTimeout = new Spinner<>(1, 300, 30);
        spPreviewTimeout.setEditable(true);
        spPreviewTimeout.setPrefWidth(60);
        spPreviewTimeout.setTooltip(new Tooltip("预览超时时间(秒)"));
        spPreviewTimeout.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.FALSE.equals(newValue)) { // 当失去焦点时
                spPreviewTimeout.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
            }
        });
        
        spExecutionTimeout = new Spinner<>(1, 300, 60);
        spExecutionTimeout.setEditable(true);
        spExecutionTimeout.setPrefWidth(60);
        spExecutionTimeout.setTooltip(new Tooltip("执行超时时间(秒)"));
        spExecutionTimeout.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.FALSE.equals(newValue)) { // 当失去焦点时
                spExecutionTimeout.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
            }
        });
        
        chkUnlimitedPreviewTimeout = new JFXCheckBox("不限制");
        chkUnlimitedPreviewTimeout.setSelected(true);
        chkUnlimitedPreviewTimeout.setTooltip(new Tooltip("不限制预览超时时间"));
        chkUnlimitedPreviewTimeout.selectedProperty().addListener((obs, oldVal, newVal) -> {
            spPreviewTimeout.setDisable(newVal);
        });
        
        chkUnlimitedExecutionTimeout = new JFXCheckBox("不限制");
        chkUnlimitedExecutionTimeout.setSelected(true);
        chkUnlimitedExecutionTimeout.setTooltip(new Tooltip("不限制执行超时时间"));
        chkUnlimitedExecutionTimeout.selectedProperty().addListener((obs, oldVal, newVal) -> {
            spExecutionTimeout.setDisable(newVal);
        });
        
        // 添加提示信息
        FloatingTooltip.bindToNode(chkUnlimitedExecution, "不限制执行数量", java.util.Arrays.asList(
                "取消勾选可设置执行数量上限",
                "无限制可能会影响性能"
        ));

        // 添加线程和数量限制设置的提示信息
        FloatingTooltip.bindToNode(spPreviewThreads, "预览线程数", java.util.Arrays.asList(
                "设置文件扫描和分析的线程数",
                "值越大速度越快，但会增加系统负载",
                "建议根据CPU核心数设置"
        ));

        FloatingTooltip.bindToNode(spExecutionThreads, "执行线程数", java.util.Arrays.asList(
                "设置管道任务执行的线程数",
                "值越大速度越快，但会增加系统负载",
                "建议根据CPU核心数设置"
        ));

        FloatingTooltip.bindToNode(cbThreadPoolMode, "线程池模式", java.util.Arrays.asList(
                "全局统一配置：所有根路径共用线程数设置",
                "根路径独立：每个根路径可单独设置线程数",
                "任务执行中无法切换模式"
        ));

        FloatingTooltip.bindToNode(spGlobalPreviewLimit, "全局预览数量上限", java.util.Arrays.asList(
                "设置预览的最大文件数量",
                "值越小性能越好，但显示的文件越少",
                "达到上限后会停止扫描"
        ));

        FloatingTooltip.bindToNode(spGlobalExecutionLimit, "全局执行数量上限", java.util.Arrays.asList(
                "设置执行的最大文件数量",
                "值越小性能越好，但执行的文件越少",
                "达到上限后会停止执行"
        ));

        FloatingTooltip.bindToNode(chkUnlimitedPreview, "不限制预览数量", java.util.Arrays.asList(
                "取消勾选可设置预览数量上限",
                "无限制可能会影响性能"
        ));

        FloatingTooltip.bindToNode(chkUnlimitedExecution, "不限制执行数量", java.util.Arrays.asList(
                "取消勾选可设置执行数量上限",
                "无限制可能会影响性能"
        ));

        // 初始化根路径线程数配置UI
        rootPathThreadConfigBox = new VBox(10);
        rootPathThreadConfigBox.setPadding(new Insets(5));
        rootPathThreadConfigBox.setAlignment(Pos.CENTER_LEFT);

        // 初始设置线程池模式下拉框的可用性
        cbThreadPoolMode.setDisable(app.getTaskRunningStatus().get());
    }

    /**
     * 切换自动刷新功能
     *
     * @param enabled 是否启用自动刷新
     */
    private void toggleAutoRefresh(boolean enabled) {
        if (enabled) {
            // 创建并启动调度服务
            autoRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
            autoRefreshExecutor.scheduleAtFixedRate(() -> {
                Platform.runLater(this::refresh);
            }, AUTO_REFRESH_INTERVAL, AUTO_REFRESH_INTERVAL, TimeUnit.MILLISECONDS);
        } else {
            // 关闭调度服务
            if (autoRefreshExecutor != null) {
                autoRefreshExecutor.shutdownNow();
                autoRefreshExecutor = null;
            }
        }
        autoRefreshEnabled = enabled;
    }

    /**
     * 更新根路径线程数配置UI
     */
    public void updateRootPathThreadConfigUI() {
        rootPathThreadConfigBox.getChildren().clear();
        rootPathSpinners.clear();
        rootPathProgressBars.clear();
        rootPathProgressLabels.clear();

        boolean isRootPathMode = ThreadPoolManager.MODE_ROOT_PATH.equals(cbThreadPoolMode.getValue());

        if (!isRootPathMode) {
            Label modeLabel = new Label("当前使用全局统一配置模式，所有根路径共用线程数设置");
            modeLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            rootPathThreadConfigBox.getChildren().add(modeLabel);
            return;
        }

        if (app.getSourceRoots().isEmpty()) {
            Label emptyLabel = new Label("无来源根路径");
            emptyLabel.setStyle("-fx-text-fill: #999;");
            rootPathThreadConfigBox.getChildren().add(emptyLabel);
            return;
        }

        // 获取主题配置
        ThemeConfig theme = app.getCurrentTheme();

        // 为每个根路径创建折叠面板
        for (File root : app.getSourceRoots()) {
            String rootPath = root.getAbsolutePath();

            // 从应用中获取已保存的根路径线程配置
            int savedExecutionThreads = app.getRootPathThreadConfig().getOrDefault(rootPath, app.getSpExecutionThreads().getValue());
            int savedPreviewThreads = app.getRootPathThreadConfig().getOrDefault(rootPath + "_preview", app.getSpPreviewThreads().getValue());

            // 执行线程数配置
            Spinner<Integer> executionSpinner = new Spinner<>(1, 32, savedExecutionThreads);
            executionSpinner.setEditable(true);
            executionSpinner.setPrefWidth(60);
            executionSpinner.setMaxWidth(60);
            executionSpinner.setTooltip(new Tooltip("执行线程数: " + rootPath));
            executionSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) { // 当失去焦点时
                    executionSpinner.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
                }
            });

            // 监听执行线程数变化，更新配置
            executionSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                app.getRootPathThreadConfig().put(rootPath, newVal);
            });

            // 预览线程数配置
            Spinner<Integer> previewSpinner = new Spinner<>(1, 32, savedPreviewThreads);
            previewSpinner.setEditable(true);
            previewSpinner.setPrefWidth(60);
            previewSpinner.setMaxWidth(60);
            previewSpinner.setTooltip(new Tooltip("预览线程数: " + rootPath));
            previewSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) { // 当失去焦点时
                    previewSpinner.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
                }
            });

            // 监听预览线程数变化，更新配置
            previewSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                app.getRootPathThreadConfig().put(rootPath + "_preview", newVal);
            });

            rootPathSpinners.put(rootPath, executionSpinner);
            rootPathSpinners.put(rootPath + "_preview", previewSpinner);

            // 计算该根路径下的文件数量
            long fileCount = app.getFullChangeList().stream()
                    .filter(record -> record.getOriginalName().startsWith(rootPath))
                    .count();

            // 计算该根路径下待执行的文件数量
            long pendingCount = app.getFullChangeList().stream()
                    .filter(record -> record.getOriginalName().startsWith(rootPath) && record.isChanged() && record.getStatus() == ExecStatus.PENDING)
                    .count();

            // 创建折叠面板内容
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            // 使用主题样式，替换硬编码颜色
            content.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-radius: %.1f; -fx-border-color: %s; -fx-background-radius: %.1f;",
                    theme.getPanelBgColor(), theme.getCornerRadius(), theme.getBorderColor(), theme.getCornerRadius()
            ));

            // 添加路径信息
            Label pathLabel = new Label("完整路径: " + rootPath);
            pathLabel.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s;", theme.getTextSecondaryColor()));
            pathLabel.setWrapText(true);
            pathLabel.setMaxWidth(Double.MAX_VALUE);

            // 添加文件数量信息
            Label fileCountLabel = new Label("总文件数: " + fileCount + "，待执行: " + pendingCount);
            fileCountLabel.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s;", theme.getTextSecondaryColor()));
            fileCountLabel.setMaxWidth(Double.MAX_VALUE);

            // 预览数量上限配置
            Spinner<Integer> previewLimitSpinner = new Spinner<>(1, 10000, 1000);
            previewLimitSpinner.setEditable(true);
            previewLimitSpinner.setPrefWidth(70);
            previewLimitSpinner.setMaxWidth(70);
            previewLimitSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) { // 当失去焦点时
                    previewLimitSpinner.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
                }
            });
            JFXCheckBox unlimitedPreview = new JFXCheckBox("不限制");
            unlimitedPreview.setSelected(true);
            unlimitedPreview.selectedProperty().addListener((obs, oldVal, newVal) -> {
                previewLimitSpinner.setDisable(newVal);
            });
            previewLimitSpinner.setDisable(unlimitedPreview.isSelected());

            // 执行数量上限配置
            Spinner<Integer> executionLimitSpinner = new Spinner<>(1, 10000, 1000);
            executionLimitSpinner.setEditable(true);
            executionLimitSpinner.setPrefWidth(70);
            executionLimitSpinner.setMaxWidth(70);
            executionLimitSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) { // 当失去焦点时
                    executionLimitSpinner.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
                }
            });
            JFXCheckBox unlimitedExecution = new JFXCheckBox("不限制");
            unlimitedExecution.setSelected(true);
            unlimitedExecution.selectedProperty().addListener((obs, oldVal, newVal) -> {
                executionLimitSpinner.setDisable(newVal);
            });
            executionLimitSpinner.setDisable(unlimitedExecution.isSelected());

            // 添加执行进度条
            ProgressBar progressBar = StyleFactory.createRootPathProgressBar(0);
            progressBar.setPrefWidth(100);

            Label progressLabel = new Label("0% (0/" + pendingCount + ")");
            progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            // 所有配置项整合到一行显示
            HBox allConfigRow = new HBox(15);
            allConfigRow.setAlignment(Pos.CENTER_LEFT);
            allConfigRow.setFillHeight(false);
            allConfigRow.getChildren().addAll(
                    new Label("预览线程: "),
                    previewSpinner,
                    new Label("执行线程: "),
                    executionSpinner,
                    new Label("预览数量: "),
                    previewLimitSpinner,
                    unlimitedPreview,
                    new Label("执行数量: "),
                    executionLimitSpinner,
                    unlimitedExecution,
                    new Label("进度: "),
                    progressBar,
                    progressLabel);

            // 保存根路径数量上限配置引用
            rootPathPreviewLimits.put(rootPath, previewLimitSpinner);
            rootPathExecutionLimits.put(rootPath, executionLimitSpinner);
            rootPathUnlimitedPreview.put(rootPath, unlimitedPreview);
            rootPathUnlimitedExecution.put(rootPath, unlimitedExecution);

            // 添加到内容面板
            content.getChildren().addAll(
                    allConfigRow);

            // 创建折叠面板
            TitledPane titledPane = new TitledPane();
            titledPane.setText(root.getName() + " - " + rootPath + " (" + fileCount + "个文件, " + pendingCount + "个待执行)");
            titledPane.setContent(content);
            titledPane.setExpanded(false);
            titledPane.setStyle(String.format(
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                    app.getCurrentTheme().getTextPrimaryColor(), app.getCurrentTheme().getPanelBgColor(), app.getCurrentTheme().getBorderColor(), app.getCurrentTheme().getBorderWidth(), app.getCurrentTheme().getCornerRadius()
            ));

            rootPathThreadConfigBox.getChildren().add(titledPane);

            // 保存进度条和进度标签的引用，以便后续更新
            rootPathProgressBars.put(rootPath, progressBar);
            rootPathProgressLabels.put(rootPath, progressLabel);
        }
    }

    private void buildUI() {
        viewNode = new VBox(5);
        viewNode.setPadding(new Insets(4));

        // 进度显示
        HBox progressBox = StyleFactory.createHBoxPanel(mainProgressBar);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setFillHeight(true);
        HBox.setHgrow(mainProgressBar, Priority.ALWAYS);

        // 配置区域：使用折叠面板组织所有配置
        this.configPane = new TitledPane();
        this.configPane.setText("运行配置");
        this.configPane.setExpanded(true);

        // 使用主题默认的面板背景色，不添加额外的透明度效果
        ThemeConfig theme = app.getCurrentTheme();
        String panelBgColor = theme.getPanelBgColor();

        this.configPane.setStyle(String.format(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                theme.getTextPrimaryColor(), panelBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));

        this.configContent = new VBox(15);
        this.configContent.setPadding(new Insets(10));

        // 全局参数设置面板
        this.globalParamsPane = new TitledPane();
        this.globalParamsPane.setText("全局参数设置");
        this.globalParamsPane.setExpanded(true);
        this.globalParamsPane.setStyle(String.format(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                theme.getTextPrimaryColor(), panelBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
        this.globalParamsPane.setExpanded(false);

        VBox globalParamsContent = new VBox(10);
        globalParamsContent.setPadding(new Insets(10));

        // 全局参数面板 - 排成一行显示
        VBox globalParamsBox = new VBox(10);
        // 使用主题样式，替换硬编码颜色
        globalParamsBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-radius: %.1f; -fx-border-color: %s; -fx-padding: 10;",
                panelBgColor, theme.getCornerRadius(), theme.getBorderColor()
        ));

        // 线程参数行
        HBox threadParamsRow = new HBox(20);
        threadParamsRow.setAlignment(Pos.CENTER_LEFT);
        threadParamsRow.setFillHeight(false);
        threadParamsRow.setPrefHeight(30);

        // 使用默认的参数对创建，不设置最小宽度
        threadParamsRow.getChildren().addAll(
                StyleFactory.createParamPairLine("预览线程数:", spPreviewThreads),
                StyleFactory.createParamPairLine("执行线程数:", spExecutionThreads),
                StyleFactory.createParamPairLine("线程池模式:", cbThreadPoolMode));

        // 设置整个参数行的最小宽度
        threadParamsRow.setMinWidth(500);

        // 数量上限行
        HBox limitParamsRow = new HBox(20);
        limitParamsRow.setAlignment(Pos.CENTER_LEFT);
        limitParamsRow.setFillHeight(false);
        limitParamsRow.setPrefHeight(30);

        // 预览数量限制
        HBox previewLimitBox = new HBox(10);
        previewLimitBox.setAlignment(Pos.CENTER_LEFT);
        previewLimitBox.setFillHeight(false);
        previewLimitBox.getChildren().addAll(
                StyleFactory.createParamPairLine("预览数量:", spGlobalPreviewLimit),
                chkUnlimitedPreview);

        // 执行数量限制
        HBox executionLimitBox = new HBox(10);
        executionLimitBox.setAlignment(Pos.CENTER_LEFT);
        executionLimitBox.setFillHeight(false);
        executionLimitBox.getChildren().addAll(
                StyleFactory.createParamPairLine("执行数量:", spGlobalExecutionLimit),
                chkUnlimitedExecution);

        limitParamsRow.getChildren().addAll(previewLimitBox, executionLimitBox);
        limitParamsRow.setMinWidth(500);

        // 创建预览参数行
        HBox previewParamsRow = new HBox(15);
        previewParamsRow.setAlignment(Pos.CENTER_LEFT);
        previewParamsRow.setFillHeight(false);

        previewParamsRow.getChildren().addAll(
                StyleFactory.createParamPairLine("预览线程数:", spPreviewThreads),
                StyleFactory.createParamPairLine("预览数量:", spGlobalPreviewLimit),
                chkUnlimitedPreview,
                StyleFactory.createParamPairLine("预览超时:", spPreviewTimeout),
                chkUnlimitedPreviewTimeout,
                StyleFactory.createSpacer(),
                StyleFactory.createParamPairLine("线程池模式:", cbThreadPoolMode)
        );

        // 创建执行参数行
        HBox executionParamsRow = new HBox(15);
        executionParamsRow.setAlignment(Pos.CENTER_LEFT);
        executionParamsRow.setFillHeight(false);

        executionParamsRow.getChildren().addAll(
                StyleFactory.createParamPairLine("执行线程数:", spExecutionThreads),
                StyleFactory.createParamPairLine("执行数量:", spGlobalExecutionLimit),
                chkUnlimitedExecution,
                StyleFactory.createParamPairLine("执行超时:", spExecutionTimeout),
                chkUnlimitedExecutionTimeout
        );

        globalParamsBox.getChildren().addAll(
                previewParamsRow,
                executionParamsRow
        );

        globalParamsContent.getChildren().addAll(globalParamsBox);
        globalParamsPane.setContent(globalParamsContent);

        // 局部参数设置面板
        this.localParamsPane = new TitledPane();
        localParamsPane.setText("局部参数设置");
        localParamsPane.setExpanded(true);

        // 为局部参数面板添加透明度效果
        localParamsPane.setStyle(String.format(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                theme.getTextPrimaryColor(), panelBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
        localParamsPane.setExpanded(false);

        VBox localParamsContent = new VBox(10);
        localParamsContent.setPadding(new Insets(10));

        // 根路径线程数配置
        updateRootPathThreadConfigUI();
        VBox rootPathBox = new VBox(10);
        // 使用主题样式，替换硬编码颜色
        rootPathBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-radius: %.1f; -fx-border-color: %s; -fx-padding: 10;",
                panelBgColor, theme.getCornerRadius(), theme.getBorderColor()
        ));
        rootPathBox.getChildren().addAll(
                rootPathThreadConfigBox);

        localParamsContent.getChildren().addAll(rootPathBox);
        localParamsPane.setContent(localParamsContent);

        configContent.getChildren().addAll(globalParamsPane, localParamsPane);
        configPane.setContent(configContent);

        // 表格过滤器
        HBox filterBox = StyleFactory.createHBoxPanel(
                StyleFactory.createChapter("[筛选条件]  "), txtSearchFilter,
                StyleFactory.createSeparatorWithChange(false), cbStatusFilter,
                StyleFactory.createSeparatorWithChange(false), cbOperationTypeFilter,
                StyleFactory.createSeparatorWithChange(false), chkHideUnchanged,
                StyleFactory.createSeparatorWithChange(false), chkAutoRefresh,
                StyleFactory.createSeparatorWithChange(false),
                StyleFactory.createParamPairLine("显示数量限制:", numberDisplay),
                StyleFactory.createSpacer(),
                StyleFactory.createRefreshButton(e -> refresh()));

        // 设置操作类型筛选下拉框的宽度
        cbOperationTypeFilter.setPrefWidth(120);

        // 添加删除操作按钮
        HBox actionBox = StyleFactory.createHBoxPanel();
        btnDeleteOriginal = StyleFactory.createSecondaryButton("删除原始文件", () -> {
            deleteSelectedFiles(true);
        });
        // 移除悬浮效果
        btnDeleteOriginal.setOnMouseEntered(null);
        btnDeleteOriginal.setOnMouseExited(null);

        btnDeleteTarget = StyleFactory.createSecondaryButton("删除目标文件", () -> {
            deleteSelectedFiles(false);
        });
        // 移除悬浮效果
        btnDeleteTarget.setOnMouseEntered(null);
        btnDeleteTarget.setOnMouseExited(null);

        // 添加执行选中行按钮
        btnExecuteSelected = StyleFactory.createSecondaryButton("执行选中的行", () -> {
            executeSelectedRecords();
        });
        // 移除悬浮效果
        btnExecuteSelected.setOnMouseEntered(null);
        btnExecuteSelected.setOnMouseExited(null);

        // 将全选按钮移动到批量操作区域
        chkSelectAll = new JFXCheckBox("全选");
        chkSelectAll.setTooltip(new Tooltip("选择所有可见行"));
        
        // 建立与表头复选框的绑定关系
        if (previewTable != null && !previewTable.getColumns().isEmpty()) {
            TreeTableColumn<ChangeRecord, ?> selectionColumn = previewTable.getColumns().get(0);
            if (selectionColumn.getGraphic() instanceof CheckBox) {
                CheckBox headerCheckBox = (CheckBox) selectionColumn.getGraphic();
                headerCheckBox.selectedProperty().bindBidirectional(chkSelectAll.selectedProperty());
            }
        }
        
        chkSelectAll.selectedProperty().addListener((obs, oldVal, newVal) -> {
            TreeTableView<ChangeRecord> tableView = getPreviewTable();
            if (tableView != null && tableView.getRoot() != null) {
                for (TreeItem<ChangeRecord> item : tableView.getRoot().getChildren()) {
                    ChangeRecord record = item.getValue();
                    if (record != null) {
                        record.setSelected(newVal);
                    }
                }
                // 刷新表格显示
                tableView.refresh();
                // 更新按钮状态
                updateDeleteButtonsState();
            }
        });

        actionBox.getChildren().addAll(
                StyleFactory.createChapter("[批量操作]  "),
                chkSelectAll,
                btnExecuteSelected,
                btnDeleteOriginal,
                btnDeleteTarget);

        // 初始化按钮状态 - 确保在所有按钮都实例化后再调用
        updateDeleteButtonsState();
        actionBox.setPadding(new Insets(5));

        // 添加批量操作按钮的提示信息
        FloatingTooltip.bindToNode(chkSelectAll, "全选", java.util.Arrays.asList(
                "选择所有可见行",
                "可用于批量操作"
        ));

        FloatingTooltip.bindToNode(btnExecuteSelected, "执行选中的行", java.util.Arrays.asList(
                "仅执行选中的文件操作",
                "跳过未选中的文件"
        ));

        FloatingTooltip.bindToNode(btnDeleteOriginal, "删除原始文件", java.util.Arrays.asList(
                "删除选中文件的原始文件",
                "谨慎操作，删除后不可恢复"
        ));

        FloatingTooltip.bindToNode(btnDeleteTarget, "删除目标文件", java.util.Arrays.asList(
                "删除选中文件的目标文件",
                "谨慎操作，删除后不可恢复"
        ));

        // 设置根路径线程配置面板的垂直增长优先级
        VBox.setVgrow(rootPathThreadConfigBox, Priority.ALWAYS);

        // 过滤条件配置伸缩框
        TitledPane filterTitledPane = new TitledPane();
        filterTitledPane.setText("筛选条件");
        filterTitledPane.setExpanded(true);
        filterTitledPane.setStyle(String.format(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                theme.getTextPrimaryColor(), panelBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius()
        ));
        VBox filterContent = new VBox(10);
        filterContent.setPadding(new Insets(10));
        filterContent.getChildren().addAll(filterBox, actionBox);
        filterTitledPane.setContent(filterContent);

        // 顶部信息区域
        VBox topInfoBox = new VBox(5);
        topInfoBox.getChildren().add(runningLabel);

        // 底部信息区域
        VBox bottomInfoBox = new VBox(5);
        bottomInfoBox.getChildren().add(statsLabel);

        // 构建新的布局
        viewNode.getChildren().addAll(
                configPane,
                filterTitledPane,
                progressBox,
                topInfoBox,  // 将runningLabel移到进度条下方
                previewTable,
                bottomInfoBox
        );

        // 设置垂直增长优先级
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        VBox.setVgrow(configPane, Priority.NEVER);
        VBox.setVgrow(filterTitledPane, Priority.NEVER);
        VBox.setVgrow(topInfoBox, Priority.NEVER);
        VBox.setVgrow(bottomInfoBox, Priority.NEVER);
    }

    private void setupPreviewColumns() {
        // 添加选择列
        TreeTableColumn<ChangeRecord, Boolean> selectionColumn = new TreeTableColumn<>();
        selectionColumn.setPrefWidth(40);
        selectionColumn.setMinWidth(40);
        selectionColumn.setMaxWidth(40);
        selectionColumn.setResizable(false);
        selectionColumn.setCellValueFactory(p -> {
            ChangeRecord record = p.getValue().getValue();
            return new javafx.beans.property.SimpleBooleanProperty(record.isSelected());
        });

        // 在选择列的表头添加全选复选框
        CheckBox headerCheckBox = new CheckBox();
        headerCheckBox.setStyle("-fx-padding: 0;");
        if (chkSelectAll != null) {
            headerCheckBox.selectedProperty().bindBidirectional(chkSelectAll.selectedProperty());
        }
        selectionColumn.setGraphic(headerCheckBox);
        selectionColumn.setCellFactory(column -> new TreeTableCell<ChangeRecord, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setStyle("-fx-padding: 0;");
                checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    TreeTableView<ChangeRecord> tableView = getTreeTableView();
                    if (tableView != null) {
                        ChangeRecord record = getTreeTableRow().getItem();
                        if (record != null) {
                            record.setSelected(newVal);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTreeTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ChangeRecord record = getTreeTableRow().getItem();
                    checkBox.setSelected(record.isSelected());
                    setGraphic(checkBox);
                }
            }
        });

        // 原始文件列
        TreeTableColumn<ChangeRecord, String> originalNameColumn = StyleFactory.createTreeTableColumn("原始文件", true, 250, 100, 500);
        originalNameColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getOriginalName()));
        originalNameColumn.setResizable(true);
        originalNameColumn.setSortable(true);

        // 原始文件大小列
        TreeTableColumn<ChangeRecord, String> originalSizeColumn = StyleFactory.createTreeTableColumn("文件大小", false, 80, 60, 120);
        originalSizeColumn.setCellValueFactory(p -> new SimpleStringProperty(FileSizeFormatUtil.formatFileSize(p.getValue().getValue().getFileHandle())));
        originalSizeColumn.setResizable(true);
        originalSizeColumn.setSortable(true);

        // 目标文件列
        TreeTableColumn<ChangeRecord, String> newNameColumn = StyleFactory.createTreeTableColumn("目标文件", true, 250, 100, 500);
        newNameColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewName()));
        newNameColumn.setCellFactory(c -> new TreeTableCell<ChangeRecord, String>() {
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

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (!isEmpty() && getItem() != null) {
                    StyleFactory.updateTreeItemStyle(this, selected);
                }
            }
        });
        newNameColumn.setResizable(true);
        newNameColumn.setSortable(true);

        // 目标文件大小列
        TreeTableColumn<ChangeRecord, String> newSizeColumn = StyleFactory.createTreeTableColumn("目标大小", false, 80, 60, 120);
        newSizeColumn.setCellValueFactory(p -> {
            try {
                if (p.getValue() != null && p.getValue().getValue() != null && p.getValue().getValue().getNewPath() != null) {
                    return new SimpleStringProperty(FileSizeFormatUtil.formatFileSize(new File(p.getValue().getValue().getNewPath())));
                }
            } catch (Exception e) {
                // 捕获可能的异常，避免程序崩溃
            }
            return new SimpleStringProperty("");
        });
        newSizeColumn.setResizable(true);
        newSizeColumn.setSortable(true);

        // 运行状态列
        TreeTableColumn<ChangeRecord, String> statusColumn = StyleFactory.createTreeTableColumn("运行状态", false, 100, 80, 150);
        statusColumn.setCellValueFactory(p -> {
            try {
                if (p.getValue() != null && p.getValue().getValue() != null && p.getValue().getValue().getStatus() != null) {
                    return new SimpleStringProperty(p.getValue().getValue().getStatus().toString());
                }
            } catch (Exception e) {
                // 捕获可能的异常，避免程序崩溃
            }
            return new SimpleStringProperty("");
        });
        // 为状态列添加颜色标识
        statusColumn.setCellFactory(c -> new TreeTableCell<ChangeRecord, String>() {
            @Override
            protected void updateItem(String i, boolean e) {
                super.updateItem(i, e);
                setText(i);
                if (e || i == null) {
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                // 根据状态设置不同的背景色和文字颜色
                ChangeRecord record = getTreeTableRow().getItem();
                if (record != null) {
                    ExecStatus status = record.getStatus();
                    if (status != null) {
                        switch (status) {
                            case RUNNING:
                                setStyle("-fx-background-color: rgba(52, 152, 219, 0.2); -fx-text-fill: #2980b9;");
                                break;
                            case SUCCESS:
                                setStyle("-fx-background-color: rgba(46, 204, 113, 0.2); -fx-text-fill: #27ae60;");
                                break;
                            case FAILED:
                                setStyle("-fx-background-color: rgba(231, 76, 60, 0.2); -fx-text-fill: #e74c3c;");
                                break;
                            case PENDING:
                                setStyle("-fx-background-color: rgba(243, 156, 18, 0.2); -fx-text-fill: #f39c12;");
                                break;
                            default:
                                setStyle("-fx-background-color: transparent;");
                        }
                    } else {
                        // 状态为null时，使用默认样式
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });
        statusColumn.setResizable(true);
        statusColumn.setSortable(true);

        // 分析时间列
        TreeTableColumn<ChangeRecord, String> analyzeTimeColumn = StyleFactory.createTreeTableColumn("分析耗时(ms)", false, 120, 80, 180);
        analyzeTimeColumn.setCellValueFactory(p -> {
            try {
                if (p.getValue() != null && p.getValue().getValue() != null) {
                    return new SimpleStringProperty(String.valueOf(p.getValue().getValue().getAnalyzeTime()));
                }
            } catch (Exception e) {
                // 捕获可能的异常，避免程序崩溃
            }
            return new SimpleStringProperty("");
        });
        analyzeTimeColumn.setResizable(true);
        analyzeTimeColumn.setSortable(true);

        // 执行时间列
        TreeTableColumn<ChangeRecord, String> executeTimeColumn = StyleFactory.createTreeTableColumn("执行耗时(ms)", false, 120, 80, 180);
        executeTimeColumn.setCellValueFactory(p -> {
            try {
                if (p.getValue() != null && p.getValue().getValue() != null) {
                    return new SimpleStringProperty(String.valueOf(p.getValue().getValue().getExecuteTime()));
                }
            } catch (Exception e) {
                // 捕获可能的异常，避免程序崩溃
            }
            return new SimpleStringProperty("");
        });
        executeTimeColumn.setResizable(true);
        executeTimeColumn.setSortable(true);

        // 目标文件路径列
        TreeTableColumn<ChangeRecord, String> newPathColumn = StyleFactory.createTreeTableColumn("目标文件路径", true, 300, 150, 800);
        newPathColumn.setCellValueFactory(p -> {
            try {
                if (p.getValue() != null && p.getValue().getValue() != null && p.getValue().getValue().getNewPath() != null) {
                    return new SimpleStringProperty(p.getValue().getValue().getNewPath());
                }
            } catch (Exception e) {
                // 捕获可能的异常，避免程序崩溃
            }
            return new SimpleStringProperty("");
        });
        newPathColumn.setResizable(true);
        newPathColumn.setSortable(true);

        // 添加所有列到表格
        previewTable.getColumns().setAll(
                selectionColumn, 
                originalNameColumn, 
                originalSizeColumn, 
                newNameColumn, 
                newSizeColumn, 
                statusColumn, 
                analyzeTimeColumn, 
                executeTimeColumn, 
                newPathColumn
        );

    }

    private void setupPreviewRows() {
        previewTable.setRowFactory(tv -> {
            TreeTableRow<ChangeRecord> row = new TreeTableRow<ChangeRecord>() {
                @Override
                protected void updateItem(ChangeRecord item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle(""); // 清空样式
                    } else {
                        // 根据索引判断单双行
                        // getIndex() 会返回当前行在视图中的位置
                        // 使用主题配置中的列表行颜色
                        ThemeConfig theme = app.getCurrentTheme();
                        String bgColor;
                        if (getIndex() % 2 == 0) {
                            bgColor = theme.getListRowEvenBgColor();
                        } else {
                            bgColor = theme.getListRowOddBgColor();
                        }

                        // 添加透明度效果
                        if (bgColor.startsWith("#") && bgColor.length() == 7) {
                            int alpha = (int) (theme.getGlassOpacity() * 255);
                            String alphaHex = String.format("%02x", alpha);
                            bgColor = bgColor + alphaHex;
                        }

                        setStyle(String.format("-fx-background-color: %s;", bgColor));
                    }
                }
            };

            // 添加选中行样式
            row.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal && !row.isEmpty()) {
                    ThemeConfig theme = app.getCurrentTheme();
                    String selectedColor = theme.getListRowSelectedBgColor();
                    if (selectedColor.startsWith("#") && selectedColor.length() == 7) {
                        int alpha = (int) (theme.getGlassOpacity() * 255);
                        String alphaHex = String.format("%02x", alpha);
                        selectedColor = selectedColor + alphaHex;
                    }
                    row.setStyle(String.format("-fx-background-color: %s;", selectedColor));
                }
            });

            return row;
        });
    }

    /**
     * 刷新预览表格
     */
    public void refresh() {
        if (!app.getTaskRunningStatus().get()) {
            app.refreshPreviewTableFilter();
        }
    }

    @Override
    public void saveConfig(Properties props) {
        // 保存配置
        props.setProperty("preview.showUnchanged", String.valueOf(!chkHideUnchanged.isSelected()));
        props.setProperty("preview.autoRefresh", String.valueOf(chkAutoRefresh.isSelected()));
        props.setProperty("preview.threads", String.valueOf(spPreviewThreads.getValue()));
        props.setProperty("preview.execThreads", String.valueOf(spExecutionThreads.getValue()));
        props.setProperty("preview.threadPoolMode", cbThreadPoolMode.getValue());
    }

    @Override
    public void loadConfig(Properties props) {
        // 加载配置
        chkHideUnchanged.setSelected(!Boolean.parseBoolean(props.getProperty("preview.showUnchanged", "false")));
        chkAutoRefresh.setSelected(Boolean.parseBoolean(props.getProperty("preview.autoRefresh", "true")));
        try {
            spPreviewThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("preview.threads", "10")));
            spExecutionThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("preview.execThreads", "4")));
        } catch (NumberFormatException e) {
            // 忽略格式错误
        }
        String threadPoolMode = props.getProperty("preview.threadPoolMode", ThreadPoolManager.MODE_GLOBAL);
        cbThreadPoolMode.setValue(threadPoolMode);
    }

    @Override
    public void reload() {
        // 重新加载样式
        ThemeConfig theme = app.getCurrentTheme();
        String tableBgColor = theme.getListBgColor();
        if (tableBgColor.startsWith("#") && tableBgColor.length() == 7) {
            int alpha = (int) (theme.getGlassOpacity() * 200);
            String alphaHex = String.format("%02x", alpha);
            tableBgColor = tableBgColor + alphaHex;
        }
        previewTable.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;",
                tableBgColor, theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius()
        ));
        // 刷新表格
        previewTable.refresh();
    }

    /**
     * 删除选中的文件
     *
     * @param isOriginal 是否删除原始文件
     */
    private void deleteSelectedFiles(boolean isOriginal) {
        List<ChangeRecord> selectedRecords = app.getFullChangeList().stream()
                .filter(ChangeRecord::isSelected)
                .collect(Collectors.toList());

        if (selectedRecords.isEmpty()) {
            FXDialogUtils.showToast(app.getPrimaryStage(), "请先选择要删除的文件！", FXDialogUtils.ToastType.INFO);
            return;
        }

        String confirmMessage = isOriginal ? "确定要删除选中的原始文件吗？此操作不可恢复！" : "确定要删除选中的目标文件吗？此操作不可恢复！";
        boolean confirmed = FXDialogUtils.showConfirm("删除确认", confirmMessage);
        if (confirmed) {
            AtomicInteger deletedCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);

            Task<Void> deleteTask = new Task<Void>() {
                @Override
                protected Void call() {
                    for (ChangeRecord record : selectedRecords) {
                        try {
                            File fileToDelete;
                            if (isOriginal) {
                                fileToDelete = record.getFileHandle();
                            } else {
                                fileToDelete = new File(record.getNewPath());
                            }

                            if (fileToDelete.exists() && fileToDelete.delete()) {
                                deletedCount.incrementAndGet();
                            } else {
                                failedCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failedCount.incrementAndGet();
                        }
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    FXDialogUtils.showToast(app.getPrimaryStage(), 
                            String.format("删除完成！成功: %d, 失败: %d", deletedCount.get(), failedCount.get()), 
                            FXDialogUtils.ToastType.SUCCESS);
                    // 刷新预览列表
                    app.refreshPreviewTableFilter();
                }
            };

            new Thread(deleteTask).start();
        }
    }

    /**
     * 执行选中的记录
     */
    private void executeSelectedRecords() {
        List<ChangeRecord> selectedRecords = app.getFullChangeList().stream()
                .filter(ChangeRecord::isSelected)
                .collect(Collectors.toList());

        if (selectedRecords.isEmpty()) {
            FXDialogUtils.showToast(app.getPrimaryStage(), "请先选择要执行的文件！", FXDialogUtils.ToastType.INFO);
            return;
        }

        // 执行选中的记录
        app.runPipelineExecution(selectedRecords);
    }

    /**
     * 更新删除按钮状态
     */
    private void updateDeleteButtonsState() {
        boolean hasSelected = app.getFullChangeList().stream().anyMatch(ChangeRecord::isSelected);
        btnDeleteOriginal.setDisable(!hasSelected);
        btnDeleteTarget.setDisable(!hasSelected);
        btnExecuteSelected.setDisable(!hasSelected);
    }

    /**
     * 更新运行进度
     *
     * @param msg 进度消息
     */
    public void updateRunningProgress(String msg) {
        // 这里可以添加更新进度的逻辑，例如更新状态标签或进度条
        Platform.runLater(() -> {
            // 示例：如果有状态标签，可以更新它
            // statusLabel.setText(msg);
        });
    }

    /**
     * 获取筛选条件
     *
     * @return 筛选条件
     */
    public Predicate<ChangeRecord> getFilterPredicate() {
        String searchText = txtSearchFilter.getText().toLowerCase();
        String statusFilter = cbStatusFilter.getValue();
        String operationTypeFilter = cbOperationTypeFilter.getValue();
        boolean hideUnchanged = chkHideUnchanged.isSelected();

        return record -> {
            // 仅显示变更
            if (hideUnchanged && !record.isChanged()) {
                return false;
            }

            // 搜索过滤
            if (!searchText.isEmpty()) {
                boolean matchesOriginalName = record.getOriginalName().toLowerCase().contains(searchText);
                boolean matchesNewName = record.getNewName().toLowerCase().contains(searchText);
                boolean matchesNewPath = record.getNewPath() != null && record.getNewPath().toLowerCase().contains(searchText);
                if (!matchesOriginalName && !matchesNewName && !matchesNewPath) {
                    return false;
                }
            }

            // 状态过滤
            if (!"全部".equals(statusFilter)) {
                ExecStatus status = record.getStatus();
                if (status == null) {
                    return false;
                }

                switch (statusFilter) {
                    case "执行中":
                        if (status != ExecStatus.RUNNING) return false;
                        break;
                    case "成功":
                        if (status != ExecStatus.SUCCESS) return false;
                        break;
                    case "失败":
                        if (status != ExecStatus.FAILED) return false;
                        break;
                    case "跳过":
                        if (status != ExecStatus.SKIPPED) return false;
                        break;
                    case "无需处理":
                        if (status != ExecStatus.PENDING) return false;
                        break;
                }
            }

            // 操作类型过滤
            if (!"全部".equals(operationTypeFilter)) {
                OperationType type = record.getOpType();
                if (type == null || !type.name.equals(operationTypeFilter)) {
                    return false;
                }
            }

            return true;
        };
    }

    /**
     * 获取预览表格
     *
     * @return 预览表格
     */
    public TreeTableView<ChangeRecord> getTreeTableView() {
        return previewTable;
    }

    /**
     * 获取Tab
     *
     * @return Tab
     */
    public Tab getTab() {
        return tabPreview;
    }

    /**
     * 获取搜索文本框
     *
     * @return 搜索文本框
     */
    public JFXTextField getTxtSearchFilter() {
        return txtSearchFilter;
    }

    /**
     * 获取状态筛选下拉框
     *
     * @return 状态筛选下拉框
     */
    public JFXComboBox<String> getCbStatusFilter() {
        return cbStatusFilter;
    }

    /**
     * 获取操作类型筛选下拉框
     *
     * @return 操作类型筛选下拉框
     */
    public JFXComboBox<String> getCbOperationTypeFilter() {
        return cbOperationTypeFilter;
    }

    /**
     * 获取仅显示变更复选框
     *
     * @return 仅显示变更复选框
     */
    public JFXCheckBox getChkHideUnchanged() {
        return chkHideUnchanged;
    }

    /**
     * 获取显示数量下拉框
     *
     * @return 显示数量下拉框
     */
    public JFXComboBox<Integer> getNumberDisplay() {
        return numberDisplay;
    }

    /**
     * 获取预览线程数Spinner
     *
     * @return 预览线程数Spinner
     */
    public Spinner<Integer> getSpPreviewThreads() {
        return spPreviewThreads;
    }

    /**
     * 获取执行线程数Spinner
     *
     * @return 执行线程数Spinner
     */
    public Spinner<Integer> getSpExecutionThreads() {
        return spExecutionThreads;
    }

    /**
     * 获取全局预览数量上限Spinner
     *
     * @return 全局预览数量上限Spinner
     */
    public Spinner<Integer> getSpGlobalPreviewLimit() {
        return spGlobalPreviewLimit;
    }

    /**
     * 获取全局执行数量上限Spinner
     *
     * @return 全局执行数量上限Spinner
     */
    public Spinner<Integer> getSpGlobalExecutionLimit() {
        return spGlobalExecutionLimit;
    }

    /**
     * 获取不限制预览数量复选框
     *
     * @return 不限制预览数量复选框
     */
    public JFXCheckBox getChkUnlimitedPreview() {
        return chkUnlimitedPreview;
    }

    /**
     * 获取不限制执行数量复选框
     *
     * @return 不限制执行数量复选框
     */
    public JFXCheckBox getChkUnlimitedExecution() {
        return chkUnlimitedExecution;
    }

    /**
     * 获取预览超时时间Spinner
     *
     * @return 预览超时时间Spinner
     */
    public Spinner<Integer> getSpPreviewTimeout() {
        return spPreviewTimeout;
    }

    /**
     * 获取执行超时时间Spinner
     *
     * @return 执行超时时间Spinner
     */
    public Spinner<Integer> getSpExecutionTimeout() {
        return spExecutionTimeout;
    }

    /**
     * 获取不限制预览超时时间复选框
     *
     * @return 不限制预览超时时间复选框
     */
    public JFXCheckBox getChkUnlimitedPreviewTimeout() {
        return chkUnlimitedPreviewTimeout;
    }

    /**
     * 获取不限制执行超时时间复选框
     *
     * @return 不限制执行超时时间复选框
     */
    public JFXCheckBox getChkUnlimitedExecutionTimeout() {
        return chkUnlimitedExecutionTimeout;
    }

    /**
     * 获取全局预览数量上限
     *
     * @return 全局预览数量上限
     */
    public int getGlobalPreviewLimit() {
        if (chkUnlimitedPreview.isSelected()) {
            return Integer.MAX_VALUE;
        }
        return spGlobalPreviewLimit.getValue();
    }

    /**
     * 获取根路径预览数量上限
     *
     * @param rootPath 根路径
     * @return 根路径预览数量上限
     */
    public int getRootPathPreviewLimit(String rootPath) {
        JFXCheckBox unlimited = rootPathUnlimitedPreview.get(rootPath);
        if (unlimited != null && unlimited.isSelected()) {
            return Integer.MAX_VALUE;
        }
        Spinner<Integer> spinner = rootPathPreviewLimits.get(rootPath);
        if (spinner != null) {
            return spinner.getValue();
        }
        return getGlobalPreviewLimit();
    }

    /**
     * 获取全局预览超时时间
     *
     * @return 全局预览超时时间
     */
    public int getGlobalPreviewTimeout() {
        if (chkUnlimitedPreviewTimeout.isSelected()) {
            return Integer.MAX_VALUE;
        }
        return spPreviewTimeout.getValue();
    }

    /**
     * 获取全局执行数量上限
     *
     * @return 全局执行数量上限
     */
    public int getGlobalExecutionLimit() {
        if (chkUnlimitedExecution.isSelected()) {
            return Integer.MAX_VALUE;
        }
        return spGlobalExecutionLimit.getValue();
    }

    /**
     * 获取根路径执行数量上限
     *
     * @param rootPath 根路径
     * @return 根路径执行数量上限
     */
    public int getRootPathExecutionLimit(String rootPath) {
        JFXCheckBox unlimited = rootPathUnlimitedExecution.get(rootPath);
        if (unlimited != null && unlimited.isSelected()) {
            return Integer.MAX_VALUE;
        }
        Spinner<Integer> spinner = rootPathExecutionLimits.get(rootPath);
        if (spinner != null) {
            return spinner.getValue();
        }
        return getGlobalExecutionLimit();
    }

    /**
     * 获取全局执行超时时间
     *
     * @return 全局执行超时时间
     */
    public int getGlobalExecutionTimeout() {
        if (chkUnlimitedExecutionTimeout.isSelected()) {
            return Integer.MAX_VALUE;
        }
        return spExecutionTimeout.getValue();
    }

    /**
     * 更新根路径进度
     */
    public void updateRootPathProgress() {
        // 实现根路径进度更新逻辑
        for (Map.Entry<String, ProgressBar> entry : rootPathProgressBars.entrySet()) {
            String rootPath = entry.getKey();
            ProgressBar progressBar = entry.getValue();
            Label progressLabel = rootPathProgressLabels.get(rootPath);
            
            if (progressBar != null && progressLabel != null) {
                // 计算该根路径下的文件总数和已完成数量
                long totalCount = app.getFullChangeList().stream()
                        .filter(record -> record.getOriginalName().startsWith(rootPath))
                        .count();
                
                long completedCount = app.getFullChangeList().stream()
                        .filter(record -> record.getOriginalName().startsWith(rootPath) && 
                                (record.getStatus() == ExecStatus.SUCCESS || record.getStatus() == ExecStatus.FAILED))
                        .count();
                
                double progress = totalCount > 0 ? (double) completedCount / totalCount : 0;
                progressBar.setProgress(progress);
                
                if (progressLabel != null) {
                    progressLabel.setText(String.format("%.1f%% (%d/%d)", progress * 100, completedCount, totalCount));
                }
            }
        }
    }

    /**
     * 更新统计信息
     */
    public void updateStats() {
        // 在后台线程中执行耗时计算
        new Thread(() -> {
            List<ChangeRecord> records = app.getFullChangeList();
            String statsText;
            
            if (records == null || records.isEmpty()) {
                statsText = "暂无统计信息";
            } else {
                long total = records.size();
                long changed = records.stream().filter(ChangeRecord::isChanged).count();
                long success = records.stream().filter(r -> r.getStatus() == ExecStatus.SUCCESS).count();
                long failed = records.stream().filter(r -> r.getStatus() == ExecStatus.FAILED).count();
                long pending = records.stream().filter(r -> r.getStatus() == ExecStatus.PENDING).count();
                
                statsText = String.format("总计: %d, 变更: %d, 成功: %d, 失败: %d, 待处理: %d", 
                        total, changed, success, failed, pending);
            }
            
            // 在UI线程中更新标签
            final String finalStatsText = statsText;
            Platform.runLater(() -> {
                statsLabel.setText(finalStatsText);
            });
        }).start();
    }

    /**
     * 获取根路径预览数量上限Map
     *
     * @return 根路径预览数量上限Map
     */
    public Map<String, Spinner<Integer>> getRootPathPreviewLimits() {
        return rootPathPreviewLimits;
    }

    /**
     * 获取根路径执行数量上限Map
     *
     * @return 根路径执行数量上限Map
     */
    public Map<String, Spinner<Integer>> getRootPathExecutionLimits() {
        return rootPathExecutionLimits;
    }

    /**
     * 获取根路径不限制预览数量Map
     *
     * @return 根路径不限制预览数量Map
     */
    public Map<String, JFXCheckBox> getRootPathUnlimitedPreview() {
        return rootPathUnlimitedPreview;
    }

    /**
     * 获取根路径不限制执行数量Map
     *
     * @return 根路径不限制执行数量Map
     */
    public Map<String, JFXCheckBox> getRootPathUnlimitedExecution() {
        return rootPathUnlimitedExecution;
    }

    /**
     * 获取根路径进度条Map
     *
     * @return 根路径进度条Map
     */
    public Map<String, ProgressBar> getRootPathProgressBars() {
        return rootPathProgressBars;
    }

    /**
     * 获取根路径进度标签Map
     *
     * @return 根路径进度标签Map
     */
    public Map<String, Label> getRootPathProgressLabels() {
        return rootPathProgressLabels;
    }

    /**
     * 获取运行标签
     *
     * @return 运行标签
     */
    public Label getRunningLabel() {
        return runningLabel;
    }

    /**
     * 获取统计标签
     *
     * @return 统计标签
     */
    public Label getStatsLabel() {
        return statsLabel;
    }

    /**
     * 获取主进度条
     *
     * @return 主进度条
     */
    public ProgressBar getMainProgressBar() {
        return mainProgressBar;
    }

    /**
     * 获取自动刷新复选框
     *
     * @return 自动刷新复选框
     */
    public JFXCheckBox getChkAutoRefresh() {
        return chkAutoRefresh;
    }
}
