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
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.app.tools.display.ThemeConfig;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.ThreadPoolManager;
import com.filemanager.type.ExecStatus;
import com.filemanager.util.file.FileSizeFormatUtil;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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

@Getter
public class PreviewView implements IAutoReloadAble {
    private static final long AUTO_REFRESH_INTERVAL = 3000; // 3秒自动刷新一次
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
    private JFXCheckBox chkHideUnchanged;
    private Spinner<Integer> spPreviewThreads;
    private Spinner<Integer> spExecutionThreads;
    // 全选复选框
    private JFXCheckBox chkSelectAll;

    public TreeTableView<ChangeRecord> getPreviewTable() {
        return previewTable;
    }
    private JFXComboBox<Integer> numberDisplay;
    private JFXComboBox<String> cbThreadPoolMode; // 线程池模式选择：共享或根路径独立
    // 数量上限配置UI
    private Spinner<Integer> spGlobalPreviewLimit;
    private Spinner<Integer> spGlobalExecutionLimit;
    private JFXCheckBox chkUnlimitedPreview;
    private JFXCheckBox chkUnlimitedExecution;
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
        chkHideUnchanged = new JFXCheckBox("仅显示变更");
        chkHideUnchanged.setSelected(true);

        // 自动刷新复选框
        chkAutoRefresh = new JFXCheckBox("自动刷新");
        chkAutoRefresh.setSelected(false);
        chkAutoRefresh.setTooltip(new Tooltip("启用后每隔3秒自动刷新预览列表"));
        chkAutoRefresh.selectedProperty().addListener((obs, oldVal, newVal) -> toggleAutoRefresh(newVal));

        txtSearchFilter.textProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        cbStatusFilter.valueProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());

        mainProgressBar = StyleFactory.createMainProgressBar(0);
        runningLabel = StyleFactory.createChapter("无执行中任务");
        statsLabel = StyleFactory.createHeader("暂无统计信息");

        previewTable = new TreeTableView<>();
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

        spPreviewThreads = new Spinner<>(1, 32, 10);
        spPreviewThreads.setEditable(true);
        spPreviewThreads.setTooltip(new Tooltip("预览线程数：用于文件扫描和分析"));

        spExecutionThreads = new Spinner<>(1, 32, 10);
        spExecutionThreads.setEditable(true);
        spExecutionThreads.setTooltip(new Tooltip("执行线程数：用于管道任务执行"));

        // 设置预览数量 默认200
        numberDisplay = new JFXComboBox<>(FXCollections.observableArrayList(50, 100, 200, 500, 1000, 2000, 5000));
        numberDisplay.getSelectionModel().selectFirst();

        // 线程池模式选择
        cbThreadPoolMode = new JFXComboBox<>(FXCollections.observableArrayList(ThreadPoolManager.MODE_GLOBAL, ThreadPoolManager.MODE_ROOT_PATH));
        cbThreadPoolMode.getSelectionModel().select(0); // 默认使用全局统一配置
        cbThreadPoolMode.setTooltip(new Tooltip("选择线程池模式：全局统一配置或根路径独立配置"));
        cbThreadPoolMode.valueProperty().addListener((o, oldVal, newVal) -> {
            // 调用App的方法切换线程池模式
            boolean success = app.setThreadPoolMode(newVal);
            if (success) {
                // 线程池模式切换成功，更新根路径配置区域的可见性
                boolean isRootPathMode = ThreadPoolManager.MODE_ROOT_PATH.equals(newVal);
                rootPathThreadConfigBox.setDisable(!isRootPathMode);

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
            if (!newValue) { // 当失去焦点时
                spGlobalPreviewLimit.increment(0); // 这是一个小技巧：触发一次位移为0的增量，强制同步文本
            }
        });

        spGlobalExecutionLimit = new Spinner<>(1, 10000, 100);
        spGlobalExecutionLimit.setEditable(true);
        spGlobalExecutionLimit.setPrefWidth(80);
        spGlobalExecutionLimit.setTooltip(new Tooltip("全局执行数量上限"));
        spGlobalExecutionLimit.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // 当失去焦点时
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

        // 初始化根路径线程数配置UI
        rootPathThreadConfigBox = new VBox(10);
        rootPathThreadConfigBox.setPadding(new Insets(5));
        rootPathThreadConfigBox.setAlignment(Pos.CENTER_LEFT);
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
                Platform.runLater(() -> refresh());
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
            executionSpinner.setPrefWidth(80);
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
            previewSpinner.setPrefWidth(80);
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

            // 线程数配置
            HBox threadConfig = new HBox(10);
            threadConfig.setAlignment(Pos.CENTER_LEFT);
            threadConfig.setFillHeight(false);
            threadConfig.setPrefHeight(25);
            threadConfig.getChildren().addAll(
                    new Label("预览线程: "),
                    previewSpinner,
                    new Label("执行线程: "),
                    executionSpinner);

            // 预览数量上限配置
            Spinner<Integer> previewLimitSpinner = new Spinner<>(1, 10000, 1000);
            previewLimitSpinner.setEditable(true);
            previewLimitSpinner.setPrefWidth(80);
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
            HBox previewLimit = new HBox(10);
            previewLimit.setAlignment(Pos.CENTER_LEFT);
            previewLimit.setFillHeight(false);
            previewLimit.setPrefHeight(25);
            previewLimit.getChildren().addAll(
                    new Label("预览数量: "),
                    previewLimitSpinner,
                    unlimitedPreview);

            // 执行数量上限配置
            Spinner<Integer> executionLimitSpinner = new Spinner<>(1, 10000, 1000);
            executionLimitSpinner.setEditable(true);
            executionLimitSpinner.setPrefWidth(80);
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
            HBox executionLimit = new HBox(10);
            executionLimit.setAlignment(Pos.CENTER_LEFT);
            executionLimit.setFillHeight(false);
            executionLimit.setPrefHeight(25);
            executionLimit.getChildren().addAll(
                    new Label("执行数量: "),
                    executionLimitSpinner,
                    unlimitedExecution);

            // 将所有参数添加到垂直布局中，避免一行显示过多内容
            VBox allParamsBox = new VBox(10);
            allParamsBox.setAlignment(Pos.CENTER_LEFT);
            allParamsBox.getChildren().addAll(threadConfig, previewLimit, executionLimit);

            // 保存根路径数量上限配置引用
            rootPathPreviewLimits.put(rootPath, previewLimitSpinner);
            rootPathExecutionLimits.put(rootPath, executionLimitSpinner);
            rootPathUnlimitedPreview.put(rootPath, unlimitedPreview);
            rootPathUnlimitedExecution.put(rootPath, unlimitedExecution);

            // 添加执行进度条
            ProgressBar progressBar = StyleFactory.createRootPathProgressBar(0);

            Label progressLabel = new Label("执行进度: 0% (0/" + pendingCount + ")");
            progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            HBox progressBox = new HBox(10);
            progressBox.setAlignment(Pos.CENTER_LEFT);
            progressBox.getChildren().addAll(progressBar, progressLabel);

            // 添加到内容面板
            content.getChildren().addAll(
                    pathLabel,
                    fileCountLabel,
                    allParamsBox,
                    progressBox);

            // 创建折叠面板
            TitledPane titledPane = new TitledPane();
            titledPane.setText(root.getName() + " (" + fileCount + "个文件)");
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
        viewNode = new VBox(15);
        viewNode.setPadding(new Insets(10));

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

        globalParamsBox.getChildren().addAll(
                StyleFactory.createChapter("[全局运行参数]  "),
                threadParamsRow,
                limitParamsRow);

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
                StyleFactory.createChapter("[根路径配置]  "),
                rootPathThreadConfigBox);

        localParamsContent.getChildren().addAll(rootPathBox);
        localParamsPane.setContent(localParamsContent);

        configContent.getChildren().addAll(globalParamsPane, localParamsPane);
        configPane.setContent(configContent);

        // 运行状态和统计信息
        VBox dash = StyleFactory.createVBoxPanel(
                StyleFactory.createHBoxPanel(StyleFactory.createChapter("[运行状态]  "), runningLabel),
                StyleFactory.createHBoxPanel(StyleFactory.createChapter("[统计信息]  "), statsLabel));

        // 全选复选框
        chkSelectAll = new JFXCheckBox("全选");
        chkSelectAll.setTooltip(new Tooltip("选择所有可见行"));
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
            }
        });
        
        // 表格过滤器
        HBox filterBox = StyleFactory.createHBoxPanel(
                StyleFactory.createChapter("[筛选条件]  "), txtSearchFilter,
                StyleFactory.createSeparatorWithChange(false), cbStatusFilter,
                StyleFactory.createSeparatorWithChange(false), chkHideUnchanged,
                StyleFactory.createSeparatorWithChange(false), chkAutoRefresh,
                StyleFactory.createSeparatorWithChange(false), chkSelectAll,
                StyleFactory.createSeparatorWithChange(false),
                StyleFactory.createParamPairLine("显示数量限制:", numberDisplay),
                StyleFactory.createSpacer(),
                StyleFactory.createRefreshButton(e -> refresh()));
        
        // 添加删除操作按钮
        HBox actionBox = StyleFactory.createHBoxPanel();
        JFXButton btnDeleteOriginal = StyleFactory.createSecondaryButton("删除原始文件", () -> {
            deleteSelectedFiles(true);
        });
        JFXButton btnDeleteTarget = StyleFactory.createSecondaryButton("删除目标文件", () -> {
            deleteSelectedFiles(false);
        });
        actionBox.getChildren().addAll(
                StyleFactory.createChapter("[批量操作]  "),
                btnDeleteOriginal,
                btnDeleteTarget);
        actionBox.setPadding(new Insets(5));

        // 表格
        previewTable.setShowRoot(false);
        previewTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        // 移除硬编码样式，让StyleFactory统一管理
        setupPreviewColumns();
        setupPreviewRows();

        // 设置根路径线程配置面板的垂直增长优先级
        VBox.setVgrow(rootPathThreadConfigBox, Priority.NEVER);
        // 设置表格的垂直增长优先级为最高
        VBox.setVgrow(previewTable, Priority.ALWAYS);

        viewNode.getChildren().addAll(progressBox, configPane, dash, filterBox, actionBox, previewTable);
    }

    public void updateRunningProgress(String msg) {
        Platform.runLater(() -> {
            runningLabel.textProperty().unbind();
            runningLabel.setText(msg);
        });
    }

    public void bindProgress(Task<?> task) {
        mainProgressBar.progressProperty().bind(task.progressProperty());
    }

    public void updateStatsDisplay(long t, long c, long s, long f, String tm) {
        Platform.runLater(() -> statsLabel.setText(String.format("文件总数:%d 需要变更:%d 操作成功:%d 操作失败:%d 过程耗时:%s", t, c, s, f, tm)));
    }

    /**
     * 更新所有根路径的执行进度
     */
    public void updateRootPathProgress() {
        Platform.runLater(() -> {
            FileManagerPlusApp app = (FileManagerPlusApp) getApp();
            // 遍历所有根路径，更新进度
            for (String rootPath : rootPathProgressBars.keySet()) {
                MultiThreadTaskEstimator estimator = app.getRootPathEstimator(rootPath);
                ProgressBar progressBar = rootPathProgressBars.get(rootPath);
                Label progressLabel = rootPathProgressLabels.get(rootPath);

                if (estimator != null) {
                    double progress = estimator.getProgress();
                    String displayInfo = estimator.getDisplayInfo();
                    progressBar.setProgress(progress);
                    progressLabel.setText(displayInfo);
                } else {
                    // 如果没有估算器，显示默认信息
                    progressBar.setProgress(-1); // -1表示不确定进度
                    progressLabel.setText("执行进度: 准备中...");
                }
            }
        });
    }

    private void setupPreviewColumns() {
        // 添加选择列
        TreeTableColumn<ChangeRecord, Boolean> selectionColumn = new TreeTableColumn<>();
        selectionColumn.setPrefWidth(30);
        selectionColumn.setMinWidth(30);
        selectionColumn.setMaxWidth(30);
        selectionColumn.setCellValueFactory(p -> {
            ChangeRecord record = p.getValue().getValue();
            return new javafx.beans.property.SimpleBooleanProperty(record.isSelected());
        });
        
        // 在选择列的表头添加全选复选框
        CheckBox headerCheckBox = new CheckBox();
        headerCheckBox.setStyle("-fx-padding: 0;");
        headerCheckBox.selectedProperty().bindBidirectional(chkSelectAll.selectedProperty());
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
        
        TreeTableColumn<ChangeRecord, String> c1 = StyleFactory.createTreeTableColumn("原始文件", false, 220, 120, 300);
        c1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getOriginalName()));
        TreeTableColumn<ChangeRecord, String> cS = StyleFactory.createTreeTableColumn("文件大小", false, 60, 60, 60);
        cS.setCellValueFactory(p -> new SimpleStringProperty(FileSizeFormatUtil.formatFileSize(p.getValue().getValue().getFileHandle())));
        TreeTableColumn<ChangeRecord, String> c2 = StyleFactory.createTreeTableColumn("目标文件", false, 220, 120, 300);
        c2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewName()));
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

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (!isEmpty() && getItem() != null) {
                    StyleFactory.updateTreeItemStyle(this, selected);
                }
            }
        });
        TreeTableColumn<ChangeRecord, String> cS2 = StyleFactory.createTreeTableColumn("目标文件大小", false, 60, 60, 60);
        cS2.setCellValueFactory(p -> {
            try {
                if (p.getValue() != null && p.getValue().getValue() != null && p.getValue().getValue().getNewPath() != null) {
                    return new SimpleStringProperty(FileSizeFormatUtil.formatFileSize(new File(p.getValue().getValue().getNewPath())));
                }
            } catch (Exception e) {
                // 捕获可能的异常，避免程序崩溃
            }
            return new SimpleStringProperty("");
        });
        TreeTableColumn<ChangeRecord, String> c3 = StyleFactory.createTreeTableColumn(
                "运行状态", false, 60, 60, 60);
        c3.setCellValueFactory(p -> {
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
        c3.setCellFactory(c -> new TreeTableCell<ChangeRecord, String>() {
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
        TreeTableColumn<ChangeRecord, String> c4 = StyleFactory.createTreeTableColumn(
                "目标文件路径", true, 250, 150, 600);
        c4.setCellValueFactory(p -> {
            try {
                if (p.getValue() != null && p.getValue().getValue() != null && p.getValue().getValue().getNewPath() != null) {
                    return new SimpleStringProperty(p.getValue().getValue().getNewPath());
                }
            } catch (Exception e) {
                // 捕获可能的异常，避免程序崩溃
            }
            return new SimpleStringProperty("");
        });
        previewTable.getColumns().setAll(selectionColumn, c1, cS, c2, cS2, c3, c4);

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
                        
                        // 检查行是否被选中
                        if (this.isSelected()) {
                            // 如果选中，添加边框和阴影效果，而不是改变背景色
                            setStyle(String.format(
                                    "-fx-background-color: %s; " +
                                    "-fx-border-width: 2; -fx-border-color: %s; " +
                                    "-fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.5), 10, 0, 0, 0);",
                                    bgColor, theme.getAccentColor()
                            ));
                        } else {
                            // 如果未选中，只设置背景色
                            setStyle("-fx-background-color: " + bgColor + ";");
                        }
                    }
                }
            };
            ContextMenu cm = new ContextMenu();
            MenuItem i1 = new MenuItem("打开原始文件");
            i1.setOnAction(e -> {
                ChangeRecord item = row.getItem();
                if (item != null && item.getFileHandle() != null) {
                    app.openFileInSystem(item.getFileHandle());
                }
            });
            MenuItem i2 = new MenuItem("打开原始目录");
            i2.setOnAction(e -> {
                ChangeRecord item = row.getItem();
                if (item != null && item.getFileHandle() != null) {
                    app.openParentDirectory(item.getFileHandle());
                }
            });
            MenuItem i3 = new MenuItem("打开目标文件");
            i3.setOnAction(e -> {
                ChangeRecord item = row.getItem();
                if (item != null && item.getNewPath() != null) {
                    try {
                        app.openFileInSystem(new File(item.getNewPath()));
                    } catch (Exception ex) {
                        // 捕获可能的异常
                    }
                }
            });
            MenuItem i4 = new MenuItem("打开目标目录");
            i4.setOnAction(e -> {
                ChangeRecord item = row.getItem();
                if (item != null && item.getNewPath() != null) {
                    try {
                        File newFile = new File(item.getNewPath());
                        File parentFile = newFile.getParentFile();
                        if (parentFile != null) {
                            app.openParentDirectory(parentFile);
                        }
                    } catch (Exception ex) {
                        // 捕获可能的异常
                    }
                }
            });
            cm.getItems().addAll(i1, i2, i3, i4);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(cm));
            // 支持双击查看详情数据
            row.setOnMouseClicked(event -> {
                // 检查双击且行非空
                if (event.getClickCount() > 1 && !row.isEmpty()) {
                    ChangeRecord item = row.getItem();
                    // 添加额外的空值检查，确保安全
                    if (item != null) {
                        try {
                            // 获取当前 Stage 实例
                            Stage currentStage = (Stage) previewTable.getScene().getWindow();
                            // 弹出 JSON 详情窗口
                            DetailWindowHelper.showJsonDetail(currentStage, item);
                        } catch (Exception e) {
                            // 捕获可能的异常，避免程序崩溃
                            e.printStackTrace();
                        }
                    }
                }
            });
            return row;
        });
    }

    /**
     * 刷新列表
     */
    public void refresh() {
        List<ChangeRecord> fullChangeList = app.getFullChangeList();
        if (fullChangeList.isEmpty()) return;
        String s = getTxtSearchFilter().getText().toLowerCase();
        String st = getCbStatusFilter().getValue();
        boolean h = getChkHideUnchanged().isSelected();

        Task<TreeItem<ChangeRecord>> t = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() {
                TreeItem<ChangeRecord> root = new TreeItem<>(new ChangeRecord());
                root.setExpanded(true);
                int limit = numberDisplay.getValue();
                AtomicInteger count = new AtomicInteger();
                for (ChangeRecord r : fullChangeList) {
                    if (h && !r.isChanged() && r.getStatus() != ExecStatus.FAILED) continue;
                    if (!s.isEmpty() && !r.getOriginalName().toLowerCase().contains(s)) continue;
                    boolean sm = true;
                    if ("执行中".equals(st)) sm = r.getStatus() == ExecStatus.RUNNING;
                    else if ("成功".equals(st)) sm = r.getStatus() == ExecStatus.SUCCESS;
                    else if ("失败".equals(st)) sm = r.getStatus() == ExecStatus.FAILED;
                    else if ("跳过".equals(st)) sm = r.getStatus() == ExecStatus.SKIPPED;
                    if (!sm) continue;
                    count.incrementAndGet();
                    root.getChildren().add(new TreeItem<>(r));
                    if (count.get() > limit) {
                        app.log("注意：实时预览数据限制为" + limit + "条！");
                        break;
                    }
                }
                return root;
            }
        };
        t.setOnSucceeded(e -> {
            getPreviewTable().setRoot(t.getValue());
        });
        t.setOnFailed(e -> {
            getPreviewTable().setRoot(t.getValue());
        });
        new Thread(t).start();
        // 顺便也刷新下统计
        updateStats();
    }

    /**
     * 更新统计信息
     */
    public void updateStats() {
        List<ChangeRecord> fullChangeList = app.getFullChangeList();
        long startT = app.getTaskStartTimStamp();
        long t = fullChangeList.size(),
                c = fullChangeList.stream().filter(ChangeRecord::isChanged).count(),
                s = fullChangeList.stream().filter(r -> r.getStatus() == ExecStatus.SUCCESS).count(),
                f = fullChangeList.stream().filter(r -> r.getStatus() == ExecStatus.FAILED).count();
        this.updateStatsDisplay(t, c, s, f, MultiThreadTaskEstimator.formatDuration(System.currentTimeMillis() - startT));
    }
    
    /**
     * 删除选中的文件
     * @param deleteOriginal 是否删除原始文件（true）还是目标文件（false）
     */
    private void deleteSelectedFiles(boolean deleteOriginal) {
        TreeTableView<ChangeRecord> tableView = getPreviewTable();
        if (tableView != null && tableView.getRoot() != null) {
            AtomicInteger deletedCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);
            
            Task<Void> deleteTask = new Task<Void>() {
                @Override
                protected Void call() {
                    Platform.runLater(() -> {
                        app.updateRunningProgress("正在准备删除文件...");
                    });
                    
                    for (TreeItem<ChangeRecord> item : tableView.getRoot().getChildren()) {
                        ChangeRecord record = item.getValue();
                        if (record != null && record.isSelected()) {
                            try {
                                File fileToDelete;
                                if (deleteOriginal) {
                                    fileToDelete = record.getFileHandle();
                                } else {
                                    fileToDelete = new File(record.getNewPath());
                                }
                                
                                if (fileToDelete.exists()) {
                                    if (fileToDelete.delete()) {
                                        deletedCount.incrementAndGet();
                                    } else {
                                        failedCount.incrementAndGet();
                                    }
                                } else {
                                    failedCount.incrementAndGet();
                                }
                                
                                // 处理中间文件
                                if (record.getIntermediateFile() != null && record.getIntermediateFile().exists()) {
                                    record.getIntermediateFile().delete();
                                }
                            } catch (Exception e) {
                                failedCount.incrementAndGet();
                            }
                        }
                    }
                    
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    super.succeeded();
                    Platform.runLater(() -> {
                        app.updateRunningProgress(String.format("文件删除完成：成功删除 %d 个文件，失败 %d 个文件", deletedCount.get(), failedCount.get()));
                        // 刷新表格
                        refresh();
                    });
                }
            };
            
            // 运行删除任务
            new Thread(deleteTask).start();
        }
    }

    // Getters
    public Node getViewNode() {
        return viewNode;
    }

    public Tab getTab() {
        return tabPreview;
    }

    // 获取全局预览数量上限
    public int getGlobalPreviewLimit() {
        if (chkUnlimitedPreview.isSelected()) {
            return Integer.MAX_VALUE;
        }
        return spGlobalPreviewLimit.getValue();
    }

    // 获取全局执行数量上限
    public int getGlobalExecutionLimit() {
        if (chkUnlimitedExecution.isSelected()) {
            return Integer.MAX_VALUE;
        }
        return spGlobalExecutionLimit.getValue();
    }

    // 是否不限制预览数量
    private boolean isUnlimitedPreview() {
        return chkUnlimitedPreview.isSelected();
    }

    // 是否不限制执行数量
    private boolean isUnlimitedExecution() {
        return chkUnlimitedExecution.isSelected();
    }

    // 获取指定根路径的预览数量上限
    public int getRootPathPreviewLimit(String rootPath) {
        if (isRootPathUnlimitedPreview(rootPath)) {
            return Integer.MAX_VALUE;
        }
        Spinner<Integer> spinner = rootPathPreviewLimits.get(rootPath);
        return spinner != null ? spinner.getValue() : getGlobalPreviewLimit();
    }

    // 获取指定根路径的执行数量上限
    public int getRootPathExecutionLimit(String rootPath) {
        if (isRootPathUnlimitedExecution(rootPath)) {
            return Integer.MAX_VALUE;
        }
        Spinner<Integer> spinner = rootPathExecutionLimits.get(rootPath);
        return spinner != null ? spinner.getValue() : getGlobalExecutionLimit();
    }

    // 指定根路径是否不限制预览数量
    private boolean isRootPathUnlimitedPreview(String rootPath) {
        JFXCheckBox checkBox = rootPathUnlimitedPreview.get(rootPath);
        return checkBox != null ? checkBox.isSelected() : isUnlimitedPreview();
    }

    // 指定根路径是否不限制执行数量
    private boolean isRootPathUnlimitedExecution(String rootPath) {
        JFXCheckBox checkBox = rootPathUnlimitedExecution.get(rootPath);
        return checkBox != null ? checkBox.isSelected() : isUnlimitedExecution();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("preview_threads", String.valueOf(spPreviewThreads.getValue()));
        props.setProperty("execution_threads", String.valueOf(spExecutionThreads.getValue()));
        props.setProperty("thread_pool_mode", cbThreadPoolMode.getValue());

        // 保存全局数量上限配置
        props.setProperty("global_preview_limit", String.valueOf(spGlobalPreviewLimit.getValue()));
        props.setProperty("global_execution_limit", String.valueOf(spGlobalExecutionLimit.getValue()));
        props.setProperty("unlimited_preview", String.valueOf(chkUnlimitedPreview.isSelected()));
        props.setProperty("unlimited_execution", String.valueOf(chkUnlimitedExecution.isSelected()));

        // 保存根路径线程配置
        for (java.util.Map.Entry<String, Spinner<Integer>> entry : rootPathSpinners.entrySet()) {
            String key = "root_thread_" + entry.getKey().replaceAll("\\\\", "_");
            props.setProperty(key, String.valueOf(entry.getValue().getValue()));
        }

        // 保存根路径数量上限配置
        for (java.util.Map.Entry<String, Spinner<Integer>> entry : rootPathPreviewLimits.entrySet()) {
            String key = "root_preview_limit_" + entry.getKey().replaceAll("\\\\", "_");
            props.setProperty(key, String.valueOf(entry.getValue().getValue()));
        }
        for (java.util.Map.Entry<String, Spinner<Integer>> entry : rootPathExecutionLimits.entrySet()) {
            String key = "root_execution_limit_" + entry.getKey().replaceAll("\\\\", "_");
            props.setProperty(key, String.valueOf(entry.getValue().getValue()));
        }
        for (java.util.Map.Entry<String, JFXCheckBox> entry : rootPathUnlimitedPreview.entrySet()) {
            String key = "root_unlimited_preview_" + entry.getKey().replaceAll("\\\\", "_");
            props.setProperty(key, String.valueOf(entry.getValue().isSelected()));
        }
        for (java.util.Map.Entry<String, JFXCheckBox> entry : rootPathUnlimitedExecution.entrySet()) {
            String key = "root_unlimited_execution_" + entry.getKey().replaceAll("\\\\", "_");
            props.setProperty(key, String.valueOf(entry.getValue().isSelected()));
        }
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("preview_threads")) {
            spPreviewThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("preview_threads")));
        }
        if (props.containsKey("execution_threads")) {
            spExecutionThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("execution_threads")));
        }
        // 加载线程池模式
        if (props.containsKey("thread_pool_mode")) {
            cbThreadPoolMode.setValue(props.getProperty("thread_pool_mode"));
        }
        // 兼容旧配置
        if (props.containsKey("global_threads")) {
            int globalThreads = Integer.parseInt(props.getProperty("global_threads"));
            spPreviewThreads.getValueFactory().setValue(globalThreads);
            spExecutionThreads.getValueFactory().setValue(globalThreads);
        }

        // 加载全局数量上限配置
        if (props.containsKey("global_preview_limit")) {
            spGlobalPreviewLimit.getValueFactory().setValue(Integer.parseInt(props.getProperty("global_preview_limit")));
        }
        if (props.containsKey("global_execution_limit")) {
            spGlobalExecutionLimit.getValueFactory().setValue(Integer.parseInt(props.getProperty("global_execution_limit")));
        }
        if (props.containsKey("unlimited_preview")) {
            chkUnlimitedPreview.setSelected(Boolean.parseBoolean(props.getProperty("unlimited_preview")));
            spGlobalPreviewLimit.setDisable(chkUnlimitedPreview.isSelected());
        }
        if (props.containsKey("unlimited_execution")) {
            chkUnlimitedExecution.setSelected(Boolean.parseBoolean(props.getProperty("unlimited_execution")));
            spGlobalExecutionLimit.setDisable(chkUnlimitedExecution.isSelected());
        }

        // 加载根路径线程配置
        FileManagerPlusApp fileManagerApp = (FileManagerPlusApp) app;
        for (File root : app.getSourceRoots()) {
            String rootPath = root.getAbsolutePath();
            String key = "root_thread_" + rootPath.replaceAll("\\\\", "_");
            if (props.containsKey(key)) {
                int threads = Integer.parseInt(props.getProperty(key));
                fileManagerApp.setRootPathExecutionThreads(rootPath, threads);
            }

            // 加载预览线程数配置
            String previewKey = "root_preview_thread_" + rootPath.replaceAll("\\\\", "_");
            if (props.containsKey(previewKey)) {
                int previewThreads = Integer.parseInt(props.getProperty(previewKey));
                fileManagerApp.setRootPathPreviewThreads(rootPath, previewThreads);
            }
        }

        // 更新UI显示
        updateRootPathThreadConfigUI();

        // 加载根路径数量上限配置（需要在UI更新后进行，因为spinners在此时才创建）
        for (File root : app.getSourceRoots()) {
            String rootPath = root.getAbsolutePath();

            // 加载预览数量上限
            String previewLimitKey = "root_preview_limit_" + rootPath.replaceAll("\\\\", "_");
            if (props.containsKey(previewLimitKey) && rootPathPreviewLimits.containsKey(rootPath)) {
                int limit = Integer.parseInt(props.getProperty(previewLimitKey));
                rootPathPreviewLimits.get(rootPath).getValueFactory().setValue(limit);
            }

            // 加载执行数量上限
            String executionLimitKey = "root_execution_limit_" + rootPath.replaceAll("\\\\", "_");
            if (props.containsKey(executionLimitKey) && rootPathExecutionLimits.containsKey(rootPath)) {
                int limit = Integer.parseInt(props.getProperty(executionLimitKey));
                rootPathExecutionLimits.get(rootPath).getValueFactory().setValue(limit);
            }

            // 加载预览不限制
            String unlimitedPreviewKey = "root_unlimited_preview_" + rootPath.replaceAll("\\\\", "_");
            if (props.containsKey(unlimitedPreviewKey) && rootPathUnlimitedPreview.containsKey(rootPath)) {
                boolean unlimited = Boolean.parseBoolean(props.getProperty(unlimitedPreviewKey));
                JFXCheckBox checkBox = rootPathUnlimitedPreview.get(rootPath);
                Spinner<Integer> spinner = rootPathPreviewLimits.get(rootPath);
                checkBox.setSelected(unlimited);
                if (spinner != null) {
                    spinner.setDisable(unlimited);
                }
            }

            // 加载执行不限制
            String unlimitedExecutionKey = "root_unlimited_execution_" + rootPath.replaceAll("\\\\", "_");
            if (props.containsKey(unlimitedExecutionKey) && rootPathUnlimitedExecution.containsKey(rootPath)) {
                boolean unlimited = Boolean.parseBoolean(props.getProperty(unlimitedExecutionKey));
                JFXCheckBox checkBox = rootPathUnlimitedExecution.get(rootPath);
                Spinner<Integer> spinner = rootPathExecutionLimits.get(rootPath);
                checkBox.setSelected(unlimited);
                if (spinner != null) {
                    spinner.setDisable(unlimited);
                }
            }
        }
    }
    
    public void reload() {
        // 更新所有TitledPane的样式
        if (configPane != null) {
            configPane.setStyle(String.format(
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                    app.getCurrentTheme().getTextPrimaryColor(), app.getCurrentTheme().getPanelBgColor(), app.getCurrentTheme().getBorderColor(), app.getCurrentTheme().getBorderWidth(), app.getCurrentTheme().getCornerRadius()
            ));
        }
        
        if (globalParamsPane != null) {
            globalParamsPane.setStyle(String.format(
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                    app.getCurrentTheme().getTextPrimaryColor(), app.getCurrentTheme().getPanelBgColor(), app.getCurrentTheme().getBorderColor(), app.getCurrentTheme().getBorderWidth(), app.getCurrentTheme().getCornerRadius()
            ));
        }
        
        if (localParamsPane != null) {
            localParamsPane.setStyle(String.format(
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                    app.getCurrentTheme().getTextPrimaryColor(), app.getCurrentTheme().getPanelBgColor(), app.getCurrentTheme().getBorderColor(), app.getCurrentTheme().getBorderWidth(), app.getCurrentTheme().getCornerRadius()
            ));
        }
        
        // 更新根路径配置的TitledPane样式
        if (rootPathThreadConfigBox != null) {
            for (Node node : rootPathThreadConfigBox.getChildren()) {
                if (node instanceof TitledPane) {
                    TitledPane tp = (TitledPane) node;
                    tp.setStyle(String.format(
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f;",
                            app.getCurrentTheme().getTextPrimaryColor(), app.getCurrentTheme().getPanelBgColor(), app.getCurrentTheme().getBorderColor(), app.getCurrentTheme().getBorderWidth(), app.getCurrentTheme().getCornerRadius()
                    ));
                }
            }
        }
        
        // 更新参数面板样式
        if (configContent != null) {
            for (Node node : configContent.getChildren()) {
                if (node instanceof VBox) {
                    VBox vbox = (VBox) node;
                    vbox.setStyle(String.format(
                            "-fx-background-color: %s; -fx-border-radius: %.1f; -fx-border-color: %s; -fx-padding: 10;",
                            app.getCurrentTheme().getPanelBgColor(), app.getCurrentTheme().getCornerRadius(), app.getCurrentTheme().getBorderColor()
                    ));
                }
            }
        }
    }
}