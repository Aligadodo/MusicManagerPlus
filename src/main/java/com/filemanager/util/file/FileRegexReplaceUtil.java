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

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileRegexReplaceUtil {

    // 正则表达式：匹配以 "FILE" 开头，以 "WAVE" 结尾的行
    // ^ 表示行的开始
    // \s* 表示零个或多个空格/空白符
    // (.*?) 表示捕获中间的任意字符，非贪婪模式
    // $ 表示行的结束
    private static final String REGEX_PATTERN = "^FILE\\s+.*?WAVE$";
    private static final Pattern PATTERN = Pattern.compile(REGEX_PATTERN);
    
    /**
     * 判断文件是否包含满足特定正则表达式模式的行。
     *
     * @param filePath 要检查的文件路径
     * @return 如果找到匹配的行则返回 true，否则返回 false
     * @throws IOException 如果在读取文件时发生 I/O 错误
     */
    public static boolean hasMatchingLine(String filePath){
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                // 检查当前行是否与预编译的模式匹配
                if (PATTERN.matcher(currentLine.trim()).matches()) {
                    return true;
                }
            }
        }catch (Exception e){
            System.out.println("File read error : " + e.getMessage());
            return false;
        }
        return false;
    }



    /**
     * 自动检测编码，读取文件并替换内容，统一以 UTF-8 编码写回。
     *
     * @param filePath 要操作的文件路径
     * @param newLine 要替换成的新行内容
     * @throws IOException 如果在读取或写入文件时发生 I/O 错误
     */
    public static void replaceWithAutoCharset(String filePath, String newLine) throws IOException {
        // 1. 自动检测源文件编码
        Charset sourceCharset = FileEncodingUtil.guessCharset(filePath);

        List<String> fileContent = new ArrayList<>();
        boolean lineReplaced = false;

        // 2. 使用检测到的编码读取文件内容
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(filePath),
                                sourceCharset // <--- 使用自动检测的编码
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

        // 3. 将修改后的内容以 UTF-8 编码写回文件
        if (lineReplaced) {
            // 省略写入部分（与上一个回答相同，使用 OutputStreamWriter("UTF-8")）
            try (
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(filePath),
                                    "UTF-8"
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


    /**
     * 读取文本文件内容，并将满足正则表达式的行替换为新的内容。
     *
     * @param filePath 要操作的文件路径
     * @param newLine 要替换成的新行内容
     * @throws IOException 如果在读取或写入文件时发生 I/O 错误
     */
    public static void replaceMatchingLineInFile(String filePath, String newLine) throws IOException {
        List<String> fileContent = new ArrayList<>();
        boolean lineReplaced = false;

        // 1. 读取文件内容到内存中
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                String trimmedLine = currentLine.trim();
                
                // 检查当前行是否满足正则模式
                if (PATTERN.matcher(trimmedLine).matches()) {
                    fileContent.add(newLine); // 添加新的行内容
                    lineReplaced = true;
                    System.out.println("--- 匹配到并替换旧行: " + trimmedLine);
                } else {
                    fileContent.add(currentLine); // 添加原始行内容
                }
            }
        }

        // 2. 将修改后的内容写回文件
        if (lineReplaced) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (String line : fileContent) {
                    writer.write(line);
                    writer.newLine(); // 写入换行符
                }
            }
            System.out.println("✅ 文件内容替换成功：" + filePath);
        } else {
            System.out.println("⚠️ 未找到满足正则模式的行：" + REGEX_PATTERN);
        }
    }

    // 示例用法
    public static void main(String[] args) {
        // --- 请根据您的实际情况修改以下变量 ---
        String FILE_PATH = "C:\\path\\to\\your\\cue_file.cue"; // 替换为您的文件路径
        String NEW_LINE = "FILE \"NewCDImage.flac\" FLAC";    // 替换成的新行
        // ---------------------------------------------

        try {
            // 步骤 1: 判断文件是否包含匹配的行
            System.out.println("--- 检查文件是否存在匹配行 ---");
            boolean exists = hasMatchingLine(FILE_PATH);
            if (exists) {
                System.out.println("✅ 文件包含满足模式的行。");
            } else {
                System.out.println("❌ 文件不包含满足模式的行。");
                return; // 如果不存在，则停止替换操作
            }
            
            // 步骤 2: 执行替换操作
            System.out.println("\n--- 执行替换操作 ---");
            replaceMatchingLineInFile(FILE_PATH, NEW_LINE);
            
        } catch (IOException e) {
            System.err.println("❌ 发生 I/O 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}