package com.filemanager.ui;

import com.filemanager.app.IAppController;
import com.filemanager.tool.log.SmartLogAppender;
import com.jfoenix.controls.JFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
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
        this.logArea = styles.createTextArea();
        this.logAppender = new SmartLogAppender(logArea, 500);
        buildUI();
        this.tabLog.setContent(viewNode);
    }

    private void buildUI() {
        viewNode = new VBox(15);
        viewNode.setPadding(new Insets(10));
        VBox tools = styles.createGlassPane();
        tools.setPadding(new Insets(10));
        tools.setAlignment(Pos.CENTER_LEFT);

        JFXButton clr = styles.createActionButton("清空", "#95a5a6", this::clearLog);

        tools.getChildren().addAll(styles.createHeader("运行日志"), clr);

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