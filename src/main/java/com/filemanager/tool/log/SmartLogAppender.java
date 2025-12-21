package com.filemanager.tool.log;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 智能日志管理类
 * 1. 自动限制 TextArea 显示行数
 * 2. 自动按时间创建日志文件到 logs/ 目录
 * 3. 线程安全地处理文件写入与 UI 更新
 */
public class SmartLogAppender {

    private final TextArea textArea;
    private final int maxLines;
    private PrintWriter fileWriter;
    
    // 格式化：文件名使用连字符，避免 Windows 不支持冒号的问题
    private final DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ");
    
    // 用于确保文件写入线程安全的锁对象
    private final Object lock = new Object();

    /**
     * @param textArea 要绑定的 JavaFX TextArea
     * @param maxLines UI 界面最多显示的行数
     */
    public SmartLogAppender(TextArea textArea, int maxLines) {
        this.textArea = textArea;
        this.maxLines = maxLines;
        initFileWriter();
    }

    /**
     * 内部初始化：创建文件夹和日志文件流
     */
    private void initFileWriter() {
        try {
            // 1. 确保 logs 文件夹存在
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // 2. 生成基于当前时间的文件名
            String fileName = LocalDateTime.now().format(fileFormatter) + ".log";
            File logFile = new File(logDir, fileName);

            // 3. 初始化流（使用 UTF-8，追加模式，自动刷新）
            this.fileWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8)), true);
            
            appendLog("▶ ▶ ▶ 日志文件已创建: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            appendLog("▶ ▶ ▶ 初始化日志文件失败: " + e.getMessage());
        }
    }

    /**
     * 追加日志（线程安全）
     * @param message 日志内容
     */
    public void appendLog(String message) {
        String timestamp = LocalDateTime.now().format(logFormatter);
        String msg = "[" + timestamp + "] ➡➡➡ " + message;

        // --- 1. 线程安全的文件写入 ---
        synchronized (lock) {
            if (fileWriter != null) {
                fileWriter.print(msg);
                // PrintWriter 构造时开启了 autoFlush，所以这里可以不手动 flush，
                // 但为了极致安全，重要日志可以保留 flush()
            }
        }

        // --- 2. JavaFX UI 更新 ---
        // Platform.runLater 内部是线程安全的队列，无需加锁
        Platform.runLater(() -> {
            textArea.appendText(msg);
            trimTextArea();
        });
    }

    /**
     * 限制 TextArea 行数
     */
    private void trimTextArea() {
        String content = textArea.getText();
        // 快速统计换行符数量
        int currentLines = content.length() - content.replace("\n", "").length();

        if (currentLines > maxLines) {
            int linesToRemove = currentLines - maxLines;
            int endPosition = 0;
            
            // 找到需要删除的边界位置
            for (int i = 0; i < linesToRemove; i++) {
                int nextIndex = content.indexOf("\n", endPosition);
                if (nextIndex != -1) {
                    endPosition = nextIndex + 1;
                } else {
                    break;
                }
            }

            if (endPosition > 0) {
                textArea.deleteText(0, endPosition);
            }
        }
    }

    public void forceFlush() {
        if (this.fileWriter != null) {
            this.fileWriter.flush();
        }
    }

    /**
     * 程序关闭时调用，释放资源
     */
    public void close() {
        synchronized (lock) {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }
}