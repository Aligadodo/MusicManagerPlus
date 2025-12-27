package com.filemanager.baseui;


import com.filemanager.app.IAppController;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.MultiThreadTaskEstimator;
import com.filemanager.tool.display.DetailWindowHelper;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.type.ExecStatus;
import com.filemanager.util.file.FileSizeFormatUtil;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class PreviewView {
    private final IAppController app;
    private final Tab tabPreview;
    private VBox viewNode;
    // UI Components
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label runningLabel, statsLabel;
    private JFXTextField txtSearchFilter;
    private JFXComboBox<String> cbStatusFilter;
    private JFXCheckBox chkHideUnchanged;
    private Spinner<Integer> spGlobalThreads;
    private JFXComboBox<Integer> numberDisplay;

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
        cbStatusFilter = new JFXComboBox<>(FXCollections.observableArrayList("全部", "执行中", "成功", "失败"));
        cbStatusFilter.getSelectionModel().select(0);
        chkHideUnchanged = new JFXCheckBox("仅显示变更");
        chkHideUnchanged.setSelected(true);
        txtSearchFilter.textProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        cbStatusFilter.valueProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());

        mainProgressBar = new ProgressBar(0);
        runningLabel = StyleFactory.createChapter("无执行中任务");
        statsLabel = StyleFactory.createHeader("暂无统计信息");

        previewTable = new TreeTableView<>();

        spGlobalThreads = new Spinner<>(1, 32, 10);
        spGlobalThreads.setEditable(true);

        // 设置预览数量 默认200
        numberDisplay = new JFXComboBox<>(FXCollections.observableArrayList(50, 100, 200, 500));
        numberDisplay.getSelectionModel().selectFirst();
    }

    private void buildUI() {
        viewNode = new VBox(15);
        viewNode.setPadding(new Insets(10));
        // 运行参数
        HBox configBox = StyleFactory.createHBoxPanel(
                StyleFactory.createChapter(" \uD83D\uDD36[运行参数]  "),
                StyleFactory.createParamPairLine("线程数量:", spGlobalThreads));

        // 进度显示+信息展示
        HBox progressBox = StyleFactory.createHBoxPanel(mainProgressBar);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setFillHeight(true);
        mainProgressBar.setPrefHeight(25);
        mainProgressBar.setPrefWidth(10000.0);
        HBox.setHgrow(mainProgressBar, Priority.ALWAYS);
        VBox dash = StyleFactory.createVBoxPanel(
                StyleFactory.createHBoxPanel(StyleFactory.createChapter(" \uD83D\uDD36[运行状态]  "), runningLabel),
                StyleFactory.createHBoxPanel(StyleFactory.createChapter(" \uD83D\uDD36[统计信息]  "), statsLabel));

        // 表格过滤器
        HBox filterBox = StyleFactory.createHBoxPanel(
                StyleFactory.createChapter(" \uD83D\uDD36[筛选条件]  "), txtSearchFilter,
                StyleFactory.createSeparatorWithChange(false), cbStatusFilter,
                StyleFactory.createSeparatorWithChange(false), chkHideUnchanged,
                StyleFactory.createSeparatorWithChange(false),
                StyleFactory.createParamPairLine("显示数量限制:", numberDisplay),
                StyleFactory.createSpacer(),
                StyleFactory.createRefreshButton(e -> refresh()));

        // 表格
        previewTable.setShowRoot(false);
        previewTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        previewTable.setStyle("-fx-background-color:rgba(255,255,255,0.4);-fx-base:rgba(255,255,255,0.1);");
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        setupPreviewColumns();
        setupPreviewRows();
        viewNode.getChildren().addAll(progressBox, configBox, dash, filterBox, previewTable);
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

    private void setupPreviewColumns() {
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
        cS2.setCellValueFactory(p -> new SimpleStringProperty(FileSizeFormatUtil.formatFileSize(new File(p.getValue().getValue().getNewPath()))));
        TreeTableColumn<ChangeRecord, String> c3 = StyleFactory.createTreeTableColumn(
                "运行状态", false, 60, 60, 60);
        c3.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getStatus().toString()));
        TreeTableColumn<ChangeRecord, String> c4 = StyleFactory.createTreeTableColumn(
                "目标文件路径", true, 250, 150, 600);
        c4.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getNewPath()));
        previewTable.getColumns().setAll(c1, cS, c2, cS2, c3, c4);

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
                        if (getIndex() % 2 == 0) {
                            setStyle("-fx-background-color: #ffffff;");
                        } else {
                            setStyle("-fx-background-color: #f2f2f2;");
                        }
                    }
                }
            };
            ContextMenu cm = new ContextMenu();
            MenuItem i1 = new MenuItem("打开原始文件");
            i1.setOnAction(e -> app.openFileInSystem(row.getItem().getFileHandle()));
            MenuItem i2 = new MenuItem("打开原始目录");
            i2.setOnAction(e -> app.openParentDirectory(row.getItem().getFileHandle()));
            MenuItem i3 = new MenuItem("打开目标文件");
            i3.setOnAction(e -> app.openFileInSystem(new File(row.getItem().getNewPath())));
            MenuItem i4 = new MenuItem("打开目标目录");
            i4.setOnAction(e -> app.openParentDirectory(new File(row.getItem().getNewPath()).getParentFile()));
            cm.getItems().addAll(i1, i2, i3, i4);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(cm));
            // 支持双击查看详情数据
            row.setOnMouseClicked(event -> {
                // 检查双击且行非空
                if (event.getClickCount() > 1 && !row.isEmpty()) {
                    ChangeRecord item = row.getItem();
                    // 获取当前 Stage 实例
                    Stage currentStage = (Stage) previewTable.getScene().getWindow();
                    // 弹出 JSON 详情窗口
                    DetailWindowHelper.showJsonDetail(currentStage, item);
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

    // Getters
    public Node getViewNode() {
        return viewNode;
    }

    public Tab getTab() {
        return tabPreview;
    }
}