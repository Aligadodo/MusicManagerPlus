package com.filemanager.tool.file;

import org.apache.tika.Tika;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AudioTypeInspector {

    //start-slot-5)// Tika 实例，用于检测文件类型
    private static final Tika tika = new Tika();

    // 常见音频 MIME 类型到扩展名的映射
    private static final Map<String, String> MIME_TO_EXT = new HashMap<>();

    static {
        MIME_TO_EXT.put("audio/mpeg", ".mp3");
        MIME_TO_EXT.put("audio/wav", ".wav");
        MIME_TO_EXT.put("audio/x-wav", ".wav");
        MIME_TO_EXT.put("audio/flac", ".flac");
        MIME_TO_EXT.put("audio/x-flac", ".flac");
        MIME_TO_EXT.put("audio/ogg", ".ogg");
        MIME_TO_EXT.put("audio/aac", ".aac");
        MIME_TO_EXT.put("audio/mp4", ".m4a"); // m4a 通常识别为 audio/mp4
        MIME_TO_EXT.put("audio/x-m4a", ".m4a");
        MIME_TO_EXT.put("audio/x-ms-wma", ".wma");
    }

    /**
     * [...](asc_slot://start-slot-7)检查并分析音频文件类型
     * [...](asc_slot://start-slot-9)@param file 待检查的音频文件
     * [...](asc_slot://start-slot-11)@return 检查结果对象
     */
    public static FileTypeCheckResult inspect(File file) {
        if (file == null || !file.exists()) {
            return new FileTypeCheckResult(false, "文件不存在", null, null, false);
        }

        try {
            // 1. [...](asc_slot://start-slot-13)基于文件内容检测真实的 MIME 类型
            String detectedMimeType = tika.detect(file);

            // 2. 获取当前文件的扩展名
            String currentName = file.getName();
            String currentExt = "";
            int dotIndex = currentName.lastIndexOf('.');
            if (dotIndex > 0) {
                currentExt = currentName.substring(dotIndex).toLowerCase();
            }

            // 3. 获取建议的扩展名
            String suggestedExt = MIME_TO_EXT.get(detectedMimeType);

            // 如果检测出的类型不在我们的已知音频列表中，标记为未知或非音频
            if (suggestedExt == null) {
                return new FileTypeCheckResult(true, "检测成功", detectedMimeType, null, false);
            }

            // 4. [...](asc_slot://start-slot-19)判断是否需要修复 (比较当前后缀与建议后缀)
            boolean needsFix = !currentExt.equals(suggestedExt);

            return new FileTypeCheckResult(
                true,
                "检测成功",
                detectedMimeType,
                suggestedExt,
                needsFix
            );

        } catch (IOException e) {
            e.printStackTrace();
            return new FileTypeCheckResult(false, "读取文件失败: " + e.getMessage(), null, null, false);
        }
    }

    /**
     * 内部类：用于封装返回结果
     */
    public static class FileTypeCheckResult {
        public boolean success;       // 操作是否成功
        public String message;        // 消息
        public String realMimeType;   // 实际检测到的 MIME 类型
        public String suggestedExtension; // 建议的后缀名 (如 .mp3)
        public boolean needsFix;      // 是否建议修改后缀

        public FileTypeCheckResult(boolean success, String message, String realMimeType, String suggestedExtension, boolean needsFix) {
            this.success = success;
            this.message = message;
            this.realMimeType = realMimeType;
            this.suggestedExtension = suggestedExtension;
            this.needsFix = needsFix;
        }

        @Override
        public String toString() {
            return "检查结果 {" +
                    "实际MIME类型='" + realMimeType + '\'' +
                    ", 建议后缀='" + suggestedExtension + '\'' +
                    ", 需要修复=" + needsFix +
                    ", 备注='" + message + '\'' +
                    '}';
        }
    }
    
    // ================== 测试 Main 方法 ==================
    public static void main(String[] args) {
        // 假设这里有一个被错误命名为 song.mp3 的 wav 文件
        File testFile = new File("C:\\Music\\test_song.mp3"); 

        FileTypeCheckResult result = AudioTypeInspector.inspect(testFile);
        
        System.out.println(result);

        if (result.success && result.needsFix) {
            System.out.println(">>> 建议执行修复：将文件扩展名修改为 " + result.suggestedExtension);
            // 这里可以添加自动重命名逻辑
            // fixFileExtension(testFile, result.suggestedExtension);
        }
    }

    /**
     * [...](asc_slot://start-slot-21)辅助方法：执行修复（重命名文件）
     */
    public static boolean fixFileExtension(File file, String newExt) {
        String filename = file.getName();
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        File newFile = new File(file.getParent(), nameWithoutExt + newExt);
        return file.renameTo(newFile);
    }
}
