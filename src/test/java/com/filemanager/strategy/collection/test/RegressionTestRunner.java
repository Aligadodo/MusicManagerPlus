package com.filemanager.strategy.collection.test;

/**
 * 回归测试运行器
 */
public class RegressionTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== 回归测试运行器 ===\n");
        
        // 创建测试框架
        TestFramework framework = new TestFramework();
        
        // 从test-data目录加载并验证所有测试用例
        framework.validateFromTestDataDir();
        
        System.out.println("\n=== 回归测试完成 ===");
    }
}
