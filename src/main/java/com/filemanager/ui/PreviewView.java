package com.filemanager.ui;


import com.filemanager.app.IAppController;
import com.filemanager.model.ChangeRecord;
import com.filemanager.util.file.FileSizeFormatUtil;
import com.jfoenix.controls.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.Getter;

import java.io.File;

@Getter
public class PreviewView {
    private final IAppController app;
    private final StyleFactory styles;
    private VBox viewNode;
    private Tab tabPreview;
    
    // UI Components
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label progressLabel, etaLabel, runningLabel, statsLabel;
    private JFXTextField txtSearchFilter;
    private JFXComboBox<String> cbStatusFilter;
    private JFXCheckBox chkHideUnchanged;
    private JFXButton btnExecute, btnStop;

    public PreviewView(IAppController app) {
        this.app = app;
        this.styles = app.getStyleFactory();
        this.tabPreview = new Tab("预览");
        initControls();
        buildUI();
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
        progressLabel = styles.createNormalLabel("就绪");
        etaLabel = styles.createNormalLabel("");
        runningLabel = styles.createNormalLabel("无执行中任务");
        statsLabel = styles.createHeader("总计: 0");
        
        btnExecute = styles.createActionButton("执行变更", "#27ae60", app::runPipelineExecution);
        btnStop = styles.createActionButton("停止", "#e74c3c", app::forceStop);
        btnStop.setDisable(true);
        btnExecute.setDisable(true);
        
        previewTable = new TreeTableView<>();
    }

    private void buildUI() {
        viewNode = new VBox(15); viewNode.setPadding(new Insets(10));
        
        // Toolbar
        VBox toolbar = styles.createGlassPane(); toolbar.setPadding(new Insets(10)); toolbar.setSpacing(15); toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox filterBox = new HBox(10); filterBox.setAlignment(Pos.CENTER_LEFT); filterBox.setStyle("-fx-background-color:rgba(255,255,255,0.5);-fx-background-radius:20;-fx-padding:5 15;");
        filterBox.getChildren().addAll(styles.createNormalLabel("筛选:"), txtSearchFilter, cbStatusFilter, chkHideUnchanged);
        toolbar.getChildren().addAll(btnExecute, btnStop, new Region(), filterBox); HBox.setHgrow(toolbar.getChildren().get(2), Priority.ALWAYS);
        
        // Dashboard
        VBox dash = styles.createGlassPane(); dash.setPadding(new Insets(10)); dash.setSpacing(20); dash.setAlignment(Pos.CENTER_LEFT);
        mainProgressBar.setPrefWidth(300);
        VBox progBox = new VBox(progressLabel, etaLabel);
        dash.getChildren().addAll(styles.createNormalLabel("进度:"), mainProgressBar, progBox, new Separator(javafx.geometry.Orientation.VERTICAL), statsLabel);
        
        // Table
        previewTable.setShowRoot(false);
        previewTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        previewTable.setStyle("-fx-background-color:rgba(255,255,255,0.4);-fx-base:rgba(255,255,255,0.1);");
        VBox.setVgrow(previewTable, Priority.ALWAYS);
        setupPreviewColumns();
        viewNode.getChildren().addAll(toolbar, dash, previewTable);
    }
    
    // UI Update Methods
    public void setRunningState(boolean running) {
        btnExecute.setDisable(running);
        btnStop.setDisable(!running);
    }
    public void updateProgress(String msg, double progress) {
        progressLabel.textProperty().unbind();
        progressLabel.setText(msg);
        mainProgressBar.progressProperty().unbind();
        mainProgressBar.setProgress(progress);
    }
    public void bindProgress(Task<?> task) {
        progressLabel.textProperty().bind(task.messageProperty());
        mainProgressBar.progressProperty().bind(task.progressProperty());
    }

    public void updateRunningInfo(String message) {
        runningLabel.setText(message);
    }

    public void updateStatsDisplay(long t, long c, long s,long f, String tm) {
        statsLabel.setText(String.format("文件总数:%d 需要变更:%d 操作成功:%d 操作失败:%d 过程耗时:%s", t, c, s, f, tm));
    }

    private void setupPreviewColumns() {
        TreeTableColumn<ChangeRecord, String> c1 = new TreeTableColumn<>("源文件");
        c1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue().getOriginalName()));
        c1.setPrefWidth(250);
        TreeTableColumn<ChangeRecord, String> cS = new TreeTableColumn<>("大小");
        cS.setCellValueFactory(p -> new SimpleStringProperty(FileSizeFormatUtil.formatFileSize(p.getValue().getValue().getFileHandle())));
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
            return row;
        });
    }
    
    public void refresh() { previewTable.refresh(); }

    // Getters
    public Node getViewNode() { return viewNode; }
    public Tab getTab() { return tabPreview; }
}