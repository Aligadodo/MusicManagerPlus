package com.filemanager.strategy.collection.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试用例持久化工具，用于保存和加载测试用例
 */
public class TestCasePersister {
    
    private static final String DEFAULT_STORAGE_DIR = "test-cases";
    
    private String storageDir;
    
    public TestCasePersister() {
        this.storageDir = DEFAULT_STORAGE_DIR;
        createStorageDir();
    }
    
    public TestCasePersister(String storageDir) {
        this.storageDir = storageDir;
        createStorageDir();
    }
    
    /**
     * 创建存储目录
     */
    private void createStorageDir() {
        Path dirPath = Paths.get(storageDir);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                System.out.println("创建测试用例存储目录: " + storageDir);
            } catch (IOException e) {
                System.err.println("创建存储目录失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 保存测试用例
     * @param testCase 测试用例
     * @param filename 文件名（不含扩展名）
     * @return 是否保存成功
     */
    public boolean saveTestCase(TestCase testCase, String filename) {
        try {
            String filepath = storageDir + File.separator + filename + ".json";
            String jsonString = JSON.toJSONString(testCase, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filepath);
                 java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(fos, "UTF-8")) {
                writer.write(jsonString);
            }
            System.out.println("测试用例保存成功: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("保存测试用例失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 加载测试用例
     * @param filename 文件名（不含扩展名）
     * @return 测试用例
     */
    public TestCase loadTestCase(String filename) {
        try {
            String filepath = storageDir + File.separator + filename + ".json";
            java.io.FileInputStream fis = new java.io.FileInputStream(filepath);
            java.io.InputStreamReader reader = new java.io.InputStreamReader(fis, "UTF-8");
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            reader.close();
            fis.close();
            TestCase testCase = JSON.parseObject(sb.toString(), TestCase.class);
            System.out.println("测试用例加载成功: " + filepath);
            return testCase;
        } catch (IOException e) {
            System.err.println("加载测试用例失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 加载指定目录下的所有测试用例
     * @return 测试用例列表
     */
    public List<TestCase> loadAllTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        File dir = new File(storageDir);
        
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    TestCase testCase = loadTestCase(file.getName().replace(".json", ""));
                    if (testCase != null) {
                        testCases.add(testCase);
                    }
                }
            }
        }
        
        System.out.println("共加载 " + testCases.size() + " 个测试用例");
        return testCases;
    }
    
    /**
     * 保存验证结果
     * @param validationResult 验证结果
     * @param filename 文件名（不含扩展名）
     * @return 是否保存成功
     */
    public boolean saveValidationResult(TestValidationResult validationResult, String filename) {
        try {
            String filepath = storageDir + File.separator + filename + "_result.json";
            String jsonString = JSON.toJSONString(validationResult, 
                SerializerFeature.PrettyFormat, 
                SerializerFeature.WriteMapNullValue, 
                SerializerFeature.WriteDateUseDateFormat);
            
            // 使用UTF-8编码写入文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filepath);
                 java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(fos, "UTF-8")) {
                writer.write(jsonString);
            }
            System.out.println("验证结果保存成功: " + filepath);
            return true;
        } catch (IOException e) {
            System.err.println("保存验证结果失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取存储目录
     * @return 存储目录
     */
    public String getStorageDir() {
        return storageDir;
    }
    
    /**
     * 设置存储目录
     * @param storageDir 存储目录
     */
    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
        createStorageDir();
    }
}
