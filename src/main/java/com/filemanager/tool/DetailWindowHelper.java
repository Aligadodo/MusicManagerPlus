package com.filemanager.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DetailWindowHelper {

    // 静态 ObjectMapper，开启“美化输出”功能
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 将对象转为 JSON 并弹出窗口展示
     * @param owner  父窗口（用于设置模态）
     * @param data   要展示的数据对象
     */
    public static void showJsonDetail(Stage owner, Object data) {
        if (data == null) return;

        try {
            // 1. 对象转 JSON 字符串 (Pretty Print)
            String jsonText = mapper.writeValueAsString(data);

            // 2. 创建弹窗
            Stage detailStage = new Stage();
            detailStage.setTitle("详情预览 (JSON)");
            detailStage.initModality(Modality.WINDOW_MODAL); // 模态窗口
            detailStage.initOwner(owner);

            // 3. UI 布局
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
}