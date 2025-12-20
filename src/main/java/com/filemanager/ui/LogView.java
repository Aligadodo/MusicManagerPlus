package com.filemanager.ui;

import com.filemanager.app.IAppController;
import com.filemanager.tool.SmartLogAppender;
import com.jfoenix.controls.JFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;

@Getter
public class LogView {
    private final IAppController controller;
    private final StyleFactory styles;
    private final Tab tabLog;
    private VBox viewNode;
    private TextArea logArea;
    private SmartLogAppender logAppender;


    public LogView(IAppController controller) {
        this.controller = controller;
        this.styles = controller.getStyleFactory();
        this.tabLog = new Tab("日志");
        initControls();
        buildUI();
        this.tabLog.setContent(viewNode);
    }

    private void initControls() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logAppender = new SmartLogAppender(logArea, 500);
    }

    private void buildUI() {
        viewNode = new VBox(15);
        viewNode.setPadding(new Insets(10));
        VBox tools = styles.createGlassPane();
        tools.setPadding(new Insets(10));
        tools.setAlignment(Pos.CENTER_LEFT);

        JFXButton clr = styles.createActionButton("清空", "#95a5a6", controller::clearLog);
        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);

        tools.getChildren().addAll(styles.createHeader("运行日志"), s, clr);

        logArea.setStyle("-fx-font-family:'Consolas'; -fx-control-inner-background: rgba(255,255,255,0.8);");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        viewNode.getChildren().addAll(tools, logArea);
    }

    public void appendLog(String msg) {
        logAppender.appendLog(msg + "\n");
    }

    public void clearLog() {
        logAppender.forceFlush();
        logArea.clear();
    }

    public Node getViewNode() {
        return viewNode;
    }

    public Tab getTab() {
        return tabLog;
    }
}