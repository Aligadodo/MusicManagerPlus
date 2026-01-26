package com.filemanager.strategy.collection.test;

import org.junit.Test;

public class RegressionTest {
    
    @Test
    public void runRegressionTests() {
        System.out.println("=== 回归测试运行器 ===\n");
        
        TestFramework framework = new TestFramework();
        framework.validateFromTestDataDir();
        
        System.out.println("\n=== 回归测试完成 ===");
    }
}
