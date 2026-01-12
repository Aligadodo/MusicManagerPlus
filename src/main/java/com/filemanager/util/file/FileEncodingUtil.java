/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.util.file;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
        byte[] bytes = new byte[8192];
        int length = 0;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            length = fis.read(bytes);
        }

        // 使用 CharsetDetector 进行检测
        CharsetDetector detector = new CharsetDetector();
        // 明确告诉探测器实际读到了多少字节，避免处理数组末尾的空字节
        detector.setText(Arrays.copyOfRange(bytes, 0, length));

        CharsetMatch match = detector.detect();
        // 置信度过滤
        if (match != null && match.getConfidence() > 50) {
            return Charset.forName(match.getName());
        }
        // 默认保底方案
        return StandardCharsets.UTF_8;
    }

}
