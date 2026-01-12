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

import com.filemanager.app.base.IAppController;
import com.filemanager.app.base.IAutoReloadAble;
import com.filemanager.app.tools.display.StyleFactory;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
public class LogView implements IAutoReloadAble {
    private final IAppController controller;
    private final Tab tabLog;
    private final ConcurrentLinkedQueue<LogInfo> logQueue = new ConcurrentLinkedQueue<>();
    private final TextArea logArea;
    private final SmartLogAppender infoLogAppender,errorLogAppender;
    private VBox viewNode;

    public LogView(IAppController controller) {
        this.controller = controller;
        this.tabLog = new Tab("日志");
        this.logArea = StyleFactory.createTextArea();
        this.infoLogAppender = new SmartLogAppender(logArea, 500, ".info.log");
        this.errorLogAppender = new SmartLogAppender(logArea, 500, ".error.log");
        this.buildUI();
        this.tabLog.setContent(viewNode);
        StyleFactory.setBasicStyle(viewNode);
        this.startLogUpdater();
    }

    private void buildUI() {
        viewNode = new VBox(15);
        viewNode.setPadding(new Insets(10));
        JFXButton clr = StyleFactory.createSmallActionButton("清空日志", "#dcbecf", this::clearLog);
        JFXButton btnScrollTop = StyleFactory.createSmallActionButton("查看顶部", "#dcbecf", () -> logArea.setScrollTop(0));
        JFXButton btnScrollBottom = StyleFactory.createSmallActionButton("查看底部", "#dcbecf", () -> logArea.setScrollTop(Double.MAX_VALUE));

        logArea.setStyle("-fx-font-family:'Consolas'; -fx-control-inner-background: rgba(255,255,255,0.8);");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        HBox tools = StyleFactory.createHBoxPanel(clr,btnScrollTop, btnScrollBottom);
        tools.setAlignment(Pos.CENTER_RIGHT);
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
                        errorLogAppender.appendLog(s.getMessage());
                    } else {
                        infoLogAppender.appendLog(s.getMessage());
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

    @Override
    public void saveConfig(Properties props) {

    }

    @Override
    public void loadConfig(Properties props) {

    }
}