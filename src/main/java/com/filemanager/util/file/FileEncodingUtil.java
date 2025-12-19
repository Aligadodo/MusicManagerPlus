package com.filemanager.util.file;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FileEncodingUtil {
    /**
     * 使用 ICU4J 库推断文件的最可能编码。
     *
     * @param filePath 文件路径
     * @return 推断出的编码名称 (如 "GBK", "UTF-8")
     * @throws IOException 如果读取文件失败
     */
    public static Charset guessCharset(String filePath) throws IOException {
        // 读取文件的前一部分字节进行分析
        byte[] bytes = new byte[4096];
        int length = 0;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            length = fis.read(bytes);
        }

        // 使用 CharsetDetector 进行检测
        CharsetDetector detector = new CharsetDetector();
        detector.setText(bytes);

        // 获取最佳匹配结果
        CharsetMatch match = detector.detect();

        if (match != null) {
            // 返回推断出的编码，例如 "GBK", "UTF-8"
            String charset = match.getName();
            System.out.println("🤖 自动推断文件编码为: " + charset + " (置信度: " + match.getConfidence() + "%)");
            return Charset.forName(charset);
        } else {
            // 如果检测失败，退回到 Java 默认的 UTF-8 (或系统默认)
            System.out.println("⚠️ 编码自动检测失败，回退到 UTF-8。");
            return StandardCharsets.UTF_8;
        }
    }

}
