package com.filemanager.plugin.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileRegexReplaceUtil {

    private static final String REGEX_PATTERN = "^FILE\\s+.*?WAVE$";
    private static final Pattern PATTERN = Pattern.compile(REGEX_PATTERN);

    public static boolean hasMatchingLine(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                if (PATTERN.matcher(currentLine.trim()).matches()) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("File read error : " + e.getMessage());
            return false;
        }
        return false;
    }

    public static void replaceWithAutoCharset(String filePath, String newLine) throws IOException {
        Charset sourceCharset = FileEncodingUtil.guessCharset(filePath);

        List<String> fileContent = new ArrayList<>();
        boolean lineReplaced = false;

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(filePath),
                                sourceCharset
                        )
                )
        ) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                String trimmedLine = currentLine.trim();

                if (PATTERN.matcher(trimmedLine).matches()) {
                    fileContent.add(newLine);
                    lineReplaced = true;
                    System.out.println("--- 匹配到并替换旧行: " + trimmedLine);
                } else {
                    fileContent.add(currentLine);
                }
            }
        }

        if (lineReplaced) {
            try (
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(filePath),
                                    StandardCharsets.UTF_8
                            )
                    )
            ) {
                for (String line : fileContent) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            System.out.println("✅ 文件内容替换成功，并已统一为 UTF-8 编码保存到：" + filePath);
        } else {
            System.out.println("⚠️ 未找到满足正则模式的行：" + REGEX_PATTERN);
        }
    }
}