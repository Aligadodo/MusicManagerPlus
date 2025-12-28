package com.filemanager.tool.file.demo;

import com.filemanager.tool.file.FileTypeManager;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.var;
import org.controlsfx.control.CheckComboBox;

import java.io.File;

public class FileManagerDemo extends Application {

    private FileTypeManager typeManager;
    private CheckComboBox<String> ccbFileTypes;
    private TextArea logArea;

    @Override
    public void start(Stage primaryStage) {
        // 1. 初始化并配置类型管理器
        initFileTypeManager();

        // 2. 创建UI控件
        ccbFileTypes = typeManager.createCheckComboBox();
        ccbFileTypes.setMaxWidth(300);

        // 模拟一个文件列表进行测试
        File[] testFiles = new File[]{
            new File("C:/Music/song.mp3"),
            new File("C:/Photos/image.jpg"),
            new File("C:/Docs/report.pdf"),
            new File("C:/Work/archive.zip"),
            new File("C:/Windows"), // 文件夹
            new File("C:/Program.exe")
        };

        logArea = new TextArea();
        logArea.setPrefHeight(200);

        // 添加监听器，当选项改变时重新过滤
        ccbFileTypes.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> {
            updateFilterResult(testFiles);
        });

        // 初始执行一次
        updateFilterResult(testFiles);

        VBox root = new VBox(10, new Label("选择文件类型过滤:"), ccbFileTypes, new Label("过滤结果:"), logArea);
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.setTitle("智能文件类型过滤器");
        primaryStage.show();
    }

    private void initFileTypeManager() {
        typeManager = new FileTypeManager();

        // --- A. 注册基础规则 ---
        
        // 1. 文件夹 (使用自定义逻辑)
        typeManager.registerCustomCategory(FileTypeManager.CAT_DIRECTORY, File::isDirectory);

        // 2. 纯文件 (使用自定义逻辑)
        // typeManager.registerCustomCategory(FileTypeManager.CAT_FILE_ONLY, File::isFile);

        // 3. 具体分类 (使用后缀名注册)
        typeManager.registerExtensionCategory("无损音频", "flac", "wav", "ape", "dsf", "dff");
        typeManager.registerExtensionCategory("MP3音乐", "mp3");
        typeManager.registerExtensionCategory("图片", "jpg", "png", "gif", "bmp");
        typeManager.registerExtensionCategory("文档", "txt", "pdf", "doc", "docx", "nfo", "cue");
        typeManager.registerExtensionCategory("压缩文件", "zip", "rar", "7z", "iso");

        // --- B. 注册组合大类 (可选) ---
        // 这里的逻辑是：如果勾选了"所有音频"，就相当于勾选了"无损音频" OR "MP3音乐"
        // 注意：这种方式允许你创建一个"所有音频"选项，而不必让用户去把mp3、wav都勾一遍
        typeManager.registerCompositeCategory("【所有音频】", "无损音频", "MP3音乐");

        // --- C. 全部文件 ---
        typeManager.registerCustomCategory(FileTypeManager.CAT_ALL_FILES, f -> true);
    }

    private void updateFilterResult(File[] files) {
        StringBuilder sb = new StringBuilder();
        // 获取当前勾选的项
        var selectedItems = ccbFileTypes.getCheckModel().getCheckedItems();

        sb.append("当前选中: ").append(selectedItems).append("\n\n");

        for (File f : files) {
            // 使用工具类判断
            if (typeManager.accept(f, selectedItems)) {
                sb.append("[匹配] ").append(f.getName()).append("\n");
            } else {
                sb.append("[忽略] ").append(f.getName()).append("\n");
            }
        }
        logArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
