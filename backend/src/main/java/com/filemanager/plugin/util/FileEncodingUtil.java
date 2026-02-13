package com.filemanager.plugin.util;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileEncodingUtil {
    public static Charset guessCharset(String filePath) throws IOException {
        byte[] bytes = new byte[8192];
        int length = 0;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            length = fis.read(bytes);
        }

        CharsetDetector detector = new CharsetDetector();
        detector.setText(Arrays.copyOfRange(bytes, 0, length));

        CharsetMatch match = detector.detect();
        if (match != null && match.getConfidence() > 50) {
            return Charset.forName(match.getName());
        }
        return StandardCharsets.UTF_8;
    }
}