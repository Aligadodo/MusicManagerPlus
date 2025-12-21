package com.filemanager.baseui;


import com.filemanager.app.IAppController;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.display.DetailWindowHelper;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.util.file.FileSizeFormatUtil;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
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

@Getter
public class PreviewView {
    private final IAppController app;
    private final Tab tabPreview;
    private VBox viewNode;
    // UI Components
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label progressLabel, runningLabel, statsLabel;
    private JFXTextField txtSearchFilter;
    private JFXComboBox<String> cbStatusFilter;
    private JFXCheckBox chkHideUnchanged;

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
        cbStatusFilter = new JFXComboBox<>(FXCollections.observableArrayList("全部", "执行中", "成功", "失败"));
        cbStatusFilter.getSelectionModel().select(0);
        chkHideUnchanged = new JFXCheckBox("仅显示变更");
        chkHideUnchanged.setSelected(true);
        txtSearchFilter.textProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        cbStatusFilter.valueProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());
        chkHideUnchanged.selectedProperty().addListener((o, old, v) -> app.refreshPreviewTableFilter());

        mainProgressBar = new ProgressBar(0);
        progressLabel = StyleFactory.createChapter("就绪");
        runningLabel = StyleFactory.createChapter("无执行中任务");
        statsLabel = StyleFactory.createHeader("暂无统计信息");

        previewTable = new TreeTableView<>();
    }

    private void buildUI() {
        viewNode = new VBox(15);
        viewNode.setPadding(new Insets(10));

        // Toolbar
        VBox toolbar = StyleFactory.createVBoxPanel();
        toolbar.setPadding(new Insets(10));
        toolbar.setSpacing(15);
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setStyle("-fx-background-color:rgba(255,255,255,0.5);-fx-background-radius:20;-fx-padding:5 15;");
        filterBox.getChildren().addAll(StyleFactory.createChapter("筛选:"), txtSearchFilter, cbStatusFilter, chkHideUnchanged);
        toolbar.getChildren().addAll(filterBox);
        // Dashboard
        HBox progressBox = StyleFactory.createHBoxPanel(
                StyleFactory.createChapter("进度:"),
                progressLabel,
                StyleFactory.createChapter("  "),
                mainProgressBar);
        VBox dash = StyleFactory.createVBoxPanel(progressBox,
                StyleFactory.createHBoxPanel(runningLabel),
                StyleFactory.createHBoxPanel(statsLabel));
        // 进度条自适应扩展
        mainProgressBar.setMinWidth(600);
        HBox.setHgrow(mainProgressBar, Priority.ALWAYS);
        HBox.setHgrow(progressBox, Priority.ALWAYS);

        // Table
        previewTable.setShowRoot(false);
        previewTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        previewTable.setStyle("-fx-background-color:rgba(255,255,255,0.4);-fx-base:rgba(255,255,255,0.1);");
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        setupPreviewColumns();
        setupPreviewRows();
        viewNode.getChildren().addAll(toolbar, dash, previewTable);
    }

    public void updateProgress(String msg) {
        runningLabel.textProperty().unbind();
        runningLabel.setText(msg);
        mainProgressBar.progressProperty().unbind();
        mainProgressBar.setProgress(1);
    }

    public void bindProgress(Task<?> task) {
        progressLabel.textProperty().bind(task.progressProperty().multiply(100).asString("%.0f%%"));
        mainProgressBar.progressProperty().bind(task.progressProperty());
    }

    public void updateRunningInfo(String message) {
        runningLabel.setText(message);
    }

    public void updateStatsDisplay(long t, long c, long s, long f, String tm) {
        statsLabel.setText(String.format("文件总数:%d 需要变更:%d 操作成功:%d 操作失败:%d 过程耗时:%s", t, c, s, f, tm));
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

    private void setupPreviewRows(){
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
                if (event.getClickCount() == 2 && !row.isEmpty()) {
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

    public void refresh() {
        previewTable.refresh();
    }

    // Getters
    public Node getViewNode() {
        return viewNode;
    }

    public Tab getTab() {
        return tabPreview;
    }
}