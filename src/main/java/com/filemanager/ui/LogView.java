package com.filemanager.ui;

import com.filemanager.app.IAppController;
import com.filemanager.tool.log.LogInfo;
import com.filemanager.tool.log.LogType;
import com.filemanager.tool.log.SmartLogAppender;
import com.jfoenix.controls.JFXButton;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
public class LogView {
    private final IAppController controller;
    private final StyleFactory styles;
    private final Tab tabLog;
    private final ConcurrentLinkedQueue<LogInfo> logQueue = new ConcurrentLinkedQueue<>();
    private final TextArea logArea;
    private final SmartLogAppender infoLogAppender;
    private final SmartLogAppender errorLogAppender;
    private VBox viewNode;

    public LogView(IAppController controller) {
        this.controller = controller;
        this.styles = controller.getStyleFactory();
        this.tabLog = new Tab("日志");
        this.logArea = styles.createTextArea();
        this.infoLogAppender = new SmartLogAppender(logArea, 500, ".info.log");
        this.errorLogAppender = new SmartLogAppender(logArea, 500, ".error.log");
        this.buildUI();
        this.tabLog.setContent(viewNode);
        this.styles.setBasicStyle(viewNode);
        this.startLogUpdater();
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

    private void startLogUpdater() {
        AnimationTimer uiUpdater;
        uiUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                LogInfo s;
                while ((s = logQueue.poll()) != null) {
                    if (s.getType() == LogType.ERROR) {
                        errorLogAppender.appendLog(s.getMessage() + "\n");
                    } else {
                        infoLogAppender.appendLog(s.getMessage() + "\n");
                    }
                }
            }
        };
        uiUpdater.start();
    }

    public void appendLog(LogInfo logInfo) {
        logQueue.add(logInfo);
    }

    public void clearLog() {
        infoLogAppender.forceFlush();
        errorLogAppender.forceFlush();
        logArea.clear();
    }

    public Node getViewNode() {
        return viewNode;
    }

    public Tab getTab() {
        return tabLog;
    }
}