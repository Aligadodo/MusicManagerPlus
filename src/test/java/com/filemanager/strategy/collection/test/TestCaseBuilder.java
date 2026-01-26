package com.filemanager.strategy.collection.test;

import java.io.File;
import java.util.*;

/**
 * 测试用例构建工具，用于扫描文件夹并构建测试用例
 */
public class TestCaseBuilder {
    
    private FolderScanner scanner;
    private TestCase currentTestCase;
    
    public TestCaseBuilder() {
        this.scanner = new FolderScanner();
    }
    
    /**
     * 从指定路径开始构建测试用例
     * @param path 扫描路径
     * @param testName 测试用例名称
     * @return TestCaseBuilder实例
     */
    public TestCaseBuilder fromPath(String path, String testName) {
        System.out.println("=== 开始构建测试用例 ===");
        System.out.println("扫描路径: " + path);
        
        List<String> folders = scanner.scanFolders(path);
        if (folders.isEmpty()) {
            System.err.println("未找到任何文件夹");
            return null;
        }
        
        this.currentTestCase = new TestCase(testName);
        this.currentTestCase.setAllFolders(folders);
        this.currentTestCase.setDescription("从路径 " + path + " 扫描得到");
        
        System.out.println("扫描完成，找到 " + folders.size() + " 个文件夹");
        
        return this;
    }
    
    /**
     * 添加预期合集
     * @param collectionName 合集名称
     * @param folderNames 文件夹名称列表
     * @return TestCaseBuilder实例
     */
    public TestCaseBuilder addExpectedCollection(String collectionName, List<String> folderNames) {
        if (currentTestCase == null) {
            System.err.println("请先调用 fromPath() 方法");
            return null;
        }
        
        ExpectedCollection collection = new ExpectedCollection(collectionName, folderNames);
        currentTestCase.addExpectedCollection(collection);
        
        System.out.println("添加预期合集: " + collectionName + " (" + folderNames.size() + " 个文件夹)");
        
        return this;
    }
    
    /**
     * 添加预期合集（使用可变参数）
     * @param collectionName 合集名称
     * @param folderNames 文件夹名称
     * @return TestCaseBuilder实例
     */
    public TestCaseBuilder addExpectedCollection(String collectionName, String... folderNames) {
        return addExpectedCollection(collectionName, Arrays.asList(folderNames));
    }
    
    /**
     * 构建并返回测试用例
     * @return 测试用例
     */
    public TestCase build() {
        if (currentTestCase == null) {
            System.err.println("测试用例未初始化");
            return null;
        }
        
        System.out.println("\n=== 测试用例构建完成 ===");
        System.out.println(currentTestCase);
        
        return currentTestCase;
    }
    
    /**
     * 构建并保存测试用例
     * @param filename 文件名
     * @param persister 持久化工具
     * @return 是否保存成功
     */
    public boolean buildAndSave(String filename, TestCasePersister persister) {
        TestCase testCase = build();
        if (testCase == null) {
            return false;
        }
        
        return persister.saveTestCase(testCase, filename);
    }
    
    /**
     * 获取当前测试用例的所有文件夹
     * @return 文件夹列表
     */
    public List<String> getAllFolders() {
        if (currentTestCase == null) {
            return Collections.emptyList();
        }
        return currentTestCase.getAllFolders();
    }
    
    /**
     * 搜索包含特定关键词的文件夹
     * @param keyword 关键词
     * @return 匹配的文件夹列表
     */
    public List<String> searchFolders(String keyword) {
        if (currentTestCase == null || currentTestCase.getAllFolders() == null) {
            return Collections.emptyList();
        }
        
        List<String> matches = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (String folder : currentTestCase.getAllFolders()) {
            if (folder.toLowerCase().contains(lowerKeyword)) {
                matches.add(folder);
            }
        }
        
        return matches;
    }
    
    /**
     * 显示所有文件夹
     */
    public void displayAllFolders() {
        if (currentTestCase == null || currentTestCase.getAllFolders() == null) {
            System.out.println("没有文件夹数据");
            return;
        }
        
        System.out.println("\n=== 所有文件夹列表 ===");
        List<String> folders = currentTestCase.getAllFolders();
        for (int i = 0; i < folders.size(); i++) {
            System.out.printf("%3d. %s\n", i + 1, folders.get(i));
        }
    }
    
    /**
     * 显示搜索结果
     * @param keyword 关键词
     */
    public void displaySearchResults(String keyword) {
        List<String> matches = searchFolders(keyword);
        
        System.out.println("\n=== 搜索结果: \"" + keyword + "\" ===");
        if (matches.isEmpty()) {
            System.out.println("未找到匹配的文件夹");
        } else {
            for (int i = 0; i < matches.size(); i++) {
                System.out.printf("%3d. %s\n", i + 1, matches.get(i));
            }
        }
    }
    
    /**
     * 获取当前测试用例
     * @return 当前测试用例
     */
    public TestCase getCurrentTestCase() {
        return currentTestCase;
    }
}
