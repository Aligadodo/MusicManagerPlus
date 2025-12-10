package com.filemanager.ui;


import com.filemanager.app.IAppController;
import com.filemanager.model.ChangeRecord;
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

@Getter
public class PreviewView {
    private final IAppController controller;
    private final StyleFactory styles;
    private VBox viewNode;
    private Tab tabPreview;
    
    // UI Components
    private TreeTableView<ChangeRecord> previewTable;
    private ProgressBar mainProgressBar;
    private Label progressLabel, etaLabel, statsLabel;
    private JFXTextField txtSearchFilter;
    private JFXComboBox<String> cbStatusFilter;
    private JFXCheckBox chkHideUnchanged;
    private JFXButton btnExecute, btnStop;

    public PreviewView(IAppController controller) {
        this.controller = controller;
        this.styles = controller.getStyleFactory();
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
        
        mainProgressBar = new ProgressBar(0);
        progressLabel = styles.createNormalLabel("就绪");
        etaLabel = styles.createNormalLabel("");
        statsLabel = styles.createHeader("总计: 0");
        
        btnExecute = styles.createActionButton("执行变更", "#27ae60", controller::runPipelineExecution);
        btnStop = styles.createActionButton("停止", "#e74c3c", controller::forceStop);
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
        
        viewNode.getChildren().addAll(toolbar, dash, previewTable);
    }
    
    // UI Update Methods
    public void setRunningState(boolean running) {
        btnExecute.setDisable(running);
        btnStop.setDisable(!running);
    }
    public void updateProgress(String msg, double progress) {
        progressLabel.setText(msg);
        mainProgressBar.setProgress(progress);
    }
    public void bindProgress(Task<?> task) {
        progressLabel.textProperty().bind(task.messageProperty());
        mainProgressBar.progressProperty().bind(task.progressProperty());
    }
    public void updateStatsDisplay(long t, long c, long s, String tm) {
        statsLabel.setText(String.format("总:%d 变:%d 成:%d 耗时:%s", t, c, s, tm));
    }
    public void refresh() { previewTable.refresh(); }

    // Getters
    public Node getViewNode() { return viewNode; }
    public Tab getTab() { return tabPreview; }
}