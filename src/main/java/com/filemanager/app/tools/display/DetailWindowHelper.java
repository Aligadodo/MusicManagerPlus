/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.app.tools.display;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;

public class DetailWindowHelper {

    // 静态 ObjectMapper，开启"美化输出"功能
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 将对象转为 JSON 并弹出窗口展示
     *
     * @param owner 父窗口（用于设置模态）
     * @param data  要展示的数据对象
     */
    public static void showJsonDetail(Stage owner, Object data) {
        if (data == null) return;

        try {
            //1. 对象转 JSON 字符串 (Pretty Print)
            String jsonText = mapper.writeValueAsString(data);

            //2. 创建弹窗
            Stage detailStage = new Stage();
            detailStage.setTitle("详情预览 (JSON)");
            detailStage.initModality(Modality.WINDOW_MODAL); // 模态窗口
            detailStage.initOwner(owner);

            //3. UI 布局
            TextArea textArea = new TextArea(jsonText);
            textArea.setEditable(false);
            textArea.setWrapText(false); // 保持 JSON 结构，不自动换行

            // 使用等宽字体，让 JSON 看起来更整齐
            textArea.setStyle("-fx-font-family: 'Courier New', 'Consolas', monospace; -fx-font-size: 13px;");

            VBox root = new VBox(textArea);
            VBox.setVgrow(textArea, Priority.ALWAYS);

            Scene scene = new Scene(root, 700, 500);
            detailStage.setScene(scene);

            // 快捷键支持：按 ESC 键关闭窗口
            scene.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ESCAPE")) {
                    detailStage.close();
                }
            });

            detailStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            // 这里可以弹出一个简单的 Alert 提示转换失败
        }
    }

    /**
     * 将对象转为 JSON 并弹出窗口展示，支持 processInfo 的多行展示
     *
     * @param owner 父窗口（用于设置模态）
     * @param data  要展示的数据对象
     */
    public static void showJsonDetailWithProcessInfo(Stage owner, Object data) {
        if (data == null) return;

        try {
            //1. 对象转 JsonNode
            JsonNode rootNode = mapper.valueToTree(data);
            ObjectNode objectNode = (ObjectNode) rootNode;

            //2. 检查是否有 processInfo 字段
            if (objectNode.has("processInfo") && objectNode.get("processInfo").isArray()) {
                ArrayNode processInfoArray = (ArrayNode) objectNode.get("processInfo");

                //3. 创建主容器
                VBox mainContainer = new VBox(10);
                mainContainer.setPadding(new Insets(10));

                //4. 创建基本信息的 JSON 展示
                // 创建一个不包含 processInfo 的副本用于展示
                ObjectNode displayNode = objectNode.deepCopy();
                displayNode.remove("processInfo");

                String basicJsonText = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(displayNode);
                TextArea basicInfoArea = new TextArea(basicJsonText);
                basicInfoArea.setEditable(false);
                basicInfoArea.setWrapText(false);
                basicInfoArea.setStyle("-fx-font-family: 'Courier New', 'Consolas', monospace; -fx-font-size: 12px;");
                basicInfoArea.setPrefRowCount(15);

                TitledPane basicInfoPane = new TitledPane("基本信息", basicInfoArea);
                basicInfoPane.setExpanded(true);
                basicInfoPane.setCollapsible(true);

                //5. 创建 processInfo 的多行展示
                VBox processInfoContainer = new VBox(5);
                processInfoContainer.setPadding(new Insets(5));

                if (processInfoArray.size() > 0) {
                    for (int i = 0; i < processInfoArray.size(); i++) {
                        String info = processInfoArray.get(i).asText();
                        TextArea infoTextArea = new TextArea((i + 1) + ". " + info);
                        infoTextArea.setEditable(false);
                        infoTextArea.setWrapText(true);
                        infoTextArea.setStyle("-fx-font-family: 'Courier New', 'Consolas', monospace; -fx-font-size: 12px; -fx-text-fill: #333;");
                        infoTextArea.setPrefRowCount(2);
                        infoTextArea.setMaxHeight(80);
                        processInfoContainer.getChildren().add(infoTextArea);
                    }
                } else {
                    TextArea emptyTextArea = new TextArea("无处理过程信息");
                    emptyTextArea.setEditable(false);
                    emptyTextArea.setWrapText(true);
                    emptyTextArea.setStyle("-fx-font-style: italic; -fx-text-fill: #999; -fx-font-family: 'Courier New', 'Consolas', monospace; -fx-font-size: 12px;");
                    emptyTextArea.setPrefRowCount(2);
                    processInfoContainer.getChildren().add(emptyTextArea);
                }

                ScrollPane processInfoScroll = new ScrollPane(processInfoContainer);
                processInfoScroll.setFitToWidth(true);
                processInfoScroll.setPrefViewportHeight(300);

                TitledPane processInfoPane = new TitledPane("处理过程信息 (" + processInfoArray.size() + " 条)", processInfoScroll);
                processInfoPane.setExpanded(true);
                processInfoPane.setCollapsible(true);

                //6. 添加到主容器
                mainContainer.getChildren().addAll(basicInfoPane, processInfoPane);
                VBox.setVgrow(basicInfoPane, Priority.ALWAYS);
                VBox.setVgrow(processInfoPane, Priority.ALWAYS);

                //7. 创建弹窗
                Stage detailStage = new Stage();
                detailStage.setTitle("详情预览");
                detailStage.initModality(Modality.WINDOW_MODAL);
                detailStage.initOwner(owner);

                Scene scene = new Scene(mainContainer, 800, 600);
                detailStage.setScene(scene);

                // 快捷键支持：按 ESC 键关闭窗口
                scene.setOnKeyPressed(event -> {
                    if (event.getCode().toString().equals("ESCAPE")) {
                        detailStage.close();
                    }
                });

                detailStage.show();
            } else {
                // 如果没有 processInfo 字段，使用原来的展示方式
                showJsonDetail(owner, data);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 如果出错，使用原来的展示方式
            showJsonDetail(owner, data);
        }
    }
}