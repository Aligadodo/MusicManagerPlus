package com.filemanager.strategy.collection.test;

import com.alibaba.fastjson.JSON;
import java.io.File;
import java.io.FileReader;

public class TestJsonParsing {
    
    public static void main(String[] args) {
        String testDataDir = "test-cases" + File.separator + "test-data";
        File dir = new File(testDataDir);
        
        File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("没有找到JSON文件");
            return;
        }
        
        for (File jsonFile : jsonFiles) {
            System.out.println("\n--- 测试文件: " + jsonFile.getName() + " ---");
            
            try {
                StringBuilder sb = new StringBuilder();
                try (FileReader reader = new FileReader(jsonFile)) {
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        sb.append((char) ch);
                    }
                }
                
                TestCase testCase = JSON.parseObject(sb.toString(), TestCase.class);
                
                if (testCase != null) {
                    System.out.println("解析成功!");
                    System.out.println("测试名称: " + testCase.getTestName());
                    System.out.println("文件夹数量: " + testCase.getAllFolders().size());
                    System.out.println("预期合集数量: " + testCase.getExpectedCollections().size());
                } else {
                    System.out.println("解析失败: testCase为null");
                }
            } catch (Exception e) {
                System.out.println("解析失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
