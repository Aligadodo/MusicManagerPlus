package com.filemanager.tool.file.demo;

import com.filemanager.tool.file.AdvancedFileTypeManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;

public class FileManagerApp extends Application {

    private AdvancedFileTypeManager fileTypeManager;
    private TextArea logArea;

    @Override
    public void start(Stage primaryStage) {
        fileTypeManager = new AdvancedFileTypeManager();

        // 左侧：过滤器设置
        VBox leftPane = fileTypeManager.getView();
        leftPane.setPadding(new Insets(10));
        leftPane.setPrefWidth(250);

        // 底部：添加一个测试按钮
        Button btnTest = new Button("应用过滤并扫描测试文件");
        btnTest.setMaxWidth(Double.MAX_VALUE);
        btnTest.setOnAction(e -> performTest());
        
        VBox controlBox = new VBox(10, leftPane, btnTest);
        controlBox.setPadding(new Insets(10));

        // 右侧：显示结果
        logArea = new TextArea();
        VBox rightPane = new VBox(new Label("文件列表 (模拟):"), logArea);
        rightPane.setPadding(new Insets(10));

        SplitPane splitPane = new SplitPane(controlBox, rightPane);
        splitPane.setDividerPositions(0.35);

        Scene scene = new Scene(splitPane, 800, 600);
        primaryStage.setTitle("JavaFX 高级文件类型过滤器 (支持级联与自定义)");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 初始运行一次
        performTest();
    }

    private void performTest() {
        // 模拟一个文件系统目录
        File[] mockFiles = new File[] {
                new File("C:/Data"),
                new File("C:/Data/music.mp3"),
                new File("C:/Data/song.flac"),
                new File("C:/Data/photo.jpg"),
                new File("C:/Data/icon.png"),
                new File("C:/Data/work.docx"),
                new File("C:/Data/sheet.xlsx"),
                new File("C:/Data/code.java"),
                new File("C:/Data/script.py"),
                new File("C:/Data/backup.zip"),
                new File("C:/Data/server.log"),
                new File("C:/Data/temp.tmp"),
                new File("C:/Data/unknown.xyz"),
                new File("C:/Data/unknown.lrc"),
                new File("C:/Data/unknown.cue"),
                new File("C:/Data/unknown")
        };

        StringBuilder sb = new StringBuilder();
        int count = 0;
        
        // 注意：refreshCache 在内部监听器中已经自动调用，
        // 但如果大量动态修改树，可以在这里手动调用一次 fileTypeManager.refreshCache(); 确保万无一失
        
        for (File f : mockFiles) {
            if (fileTypeManager.accept(f)) {
                sb.append("✅ [显示] ").append(f.getName()).append("\n");
                count++;
            } else {
                sb.append("❌ [隐藏] ").append(f.getName()).append("\n");
            }
        }
        
        logArea.setText("共显示 " + count + " 个项目\n----------------------\n" + sb.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
