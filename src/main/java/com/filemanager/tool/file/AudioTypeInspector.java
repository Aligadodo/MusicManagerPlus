/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.tool.file;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.Tika;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AudioTypeInspector {

    //start-slot-5)// Tika 实例，用于检测文件类型
    private static final Tika tika = new Tika();

    // 常见音频 MIME 类型到扩展名的映射
    private static final Map<String, String> MIME_TO_EXT_MAP = new HashMap<>();

    static {
        // === MP3 ===
        // 标准类型
        MIME_TO_EXT_MAP.put("audio/mpeg", ".mp3");
        // 常见变体
        MIME_TO_EXT_MAP.put("audio/mp3", ".mp3");
        MIME_TO_EXT_MAP.put("audio/mpg", ".mp3");
        MIME_TO_EXT_MAP.put("audio/x-mpeg", ".mp3");
        MIME_TO_EXT_MAP.put("audio/x-mp3", ".mp3");

        // === WAV ===
        // Tika 最常返回的标准类型 (RFC 2361) - 之前代码缺失的关键项
        MIME_TO_EXT_MAP.put("audio/vnd.wave", ".wav");
        // 常见变体
        MIME_TO_EXT_MAP.put("audio/wav", ".wav");
        MIME_TO_EXT_MAP.put("audio/wave", ".wav");
        MIME_TO_EXT_MAP.put("audio/x-wav", ".wav");

        // === FLAC ===
        MIME_TO_EXT_MAP.put("audio/flac", ".flac");
        MIME_TO_EXT_MAP.put("audio/x-flac", ".flac");

        // === OGG ===
        MIME_TO_EXT_MAP.put("audio/ogg", ".ogg");
        MIME_TO_EXT_MAP.put("application/ogg", ".ogg"); // 有时会被识别为 application
        MIME_TO_EXT_MAP.put("audio/vorbis", ".ogg");
        MIME_TO_EXT_MAP.put("audio/x-ogg", ".ogg");

        // === AAC ===
        MIME_TO_EXT_MAP.put("audio/aac", ".aac");
        MIME_TO_EXT_MAP.put("audio/x-aac", ".aac");
        MIME_TO_EXT_MAP.put("audio/vnd.dlna.adts", ".aac");

        // === M4A (MP4 Audio) ===
        // M4A 本质是 MP4 容器
        MIME_TO_EXT_MAP.put("audio/mp4", ".m4a");
        MIME_TO_EXT_MAP.put("application/mp4", ".m4a");
        MIME_TO_EXT_MAP.put("audio/x-m4a", ".m4a");

        // === WMA ===
        MIME_TO_EXT_MAP.put("audio/x-ms-wma", ".wma");

        // === AIFF ===
        MIME_TO_EXT_MAP.put("audio/x-aiff", ".aiff");
        MIME_TO_EXT_MAP.put("audio/aiff", ".aiff");
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
            String suggestedExt = MIME_TO_EXT_MAP.get(detectedMimeType);

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
            return new FileTypeCheckResult(false, "读取文件失败: " + ExceptionUtils.getStackTrace(e), null, null, false);
        }
    }

    /**
     * 修复版：强制基于文件内容检测类型（忽略文件名干扰）
     */
    public static FileTypeCheckResult inspectHard(File file) {
        if (file == null || !file.exists()) {
            return new FileTypeCheckResult(false, "文件不存在", null, null, false);
        }

        // 使用 try-with-resources 自动关闭流
        // 关键点：使用 BufferedInputStream 包装，支持 mark/reset，这对 Tika 读取文件头很重要
        try (InputStream stream = new java.io.BufferedInputStream(new java.io.FileInputStream(file))) {

            // 1. 核心修改：只传入流，不传入文件名。Tika 此时完全“盲测”，必须依赖文件头魔数。
            String detectedMimeType = tika.detect(stream);

            // 2. 获取当前文件的扩展名
            String currentName = file.getName();
            String currentExt = "";
            int dotIndex = currentName.lastIndexOf('.');
            if (dotIndex > 0) {
                currentExt = currentName.substring(dotIndex).toLowerCase();
            }

            // 3. 获取建议的扩展名
            String suggestedExt = MIME_TO_EXT_MAP.get(detectedMimeType);

            // 如果检测出的类型不在我们的已知音频列表中
            if (suggestedExt == null) {
                // 如果检测结果是 application/octet-stream，说明文件头可能损坏或无法识别
                String msg = "未知类型 (" + detectedMimeType + ")";
                return new FileTypeCheckResult(true, msg, detectedMimeType, null, false);
            }

            // 4. 判断是否需要修复
            boolean needsFix = !currentExt.equals(suggestedExt);

            return new FileTypeCheckResult(
                    true,
                    "检测成功",
                    detectedMimeType,
                    suggestedExt,
                    needsFix
            );

        } catch (IOException e) {
            return new FileTypeCheckResult(false, "读取文件失败: " + ExceptionUtils.getStackTrace(e), null, null, false);
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
