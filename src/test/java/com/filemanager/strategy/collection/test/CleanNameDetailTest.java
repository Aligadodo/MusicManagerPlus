package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 测试cleanName方法的详细过程
 */
public class CleanNameDetailTest {
    
    @Test
    public void testCleanNameDetail() {
        System.out.println("=== 测试cleanName方法的详细过程 ===");
        
        String test1 = "[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一";
        
        System.out.println("原始字符串: " + test1);
        
        // 步骤1：去除方括号中的CD编号
        String pattern = "\\[龙音[海文香港环球]+版[^\\]]*\\d+[^\\]]*\\]";
        String step1 = test1.replaceAll(pattern, "[龙音海文版]");
        System.out.println("步骤1（去除CD编号）: " + step1);
        
        // 步骤2：去除年份前缀
        String step2 = step1.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        step2 = step2.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        System.out.println("步骤2（去除年份前缀）: " + step2);
        
        // 步骤3：去除多余空格
        String step3 = step2.trim();
        step3 = step3.replaceAll("\\s+", " ");
        System.out.println("步骤3（去除多余空格）: " + step3);
        
        System.out.println("\n最终结果: " + step3);
    }
}
