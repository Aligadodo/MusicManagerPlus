package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 测试cleanName方法
 */
public class CleanNameTest {
    
    @Test
    public void testCleanName() {
        System.out.println("=== 测试cleanName方法 ===");
        
        String test1 = "[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一";
        String test2 = "[龙音海文版 CD-0176]小河淌水-陈爱娟古筝独奏之二";
        
        System.out.println("原始字符串1: " + test1);
        System.out.println("原始字符串2: " + test2);
        
        // 测试去除CD编号的正则表达式
        String pattern = "\\[龙音[海文香港环球]+版[^\\]]*\\d+[^\\]]*\\]";
        
        String result1 = test1.replaceAll(pattern, "[龙音海文版]");
        String result2 = test2.replaceAll(pattern, "[龙音海文版]");
        
        System.out.println("\n处理后字符串1: " + result1);
        System.out.println("处理后字符串2: " + result2);
    }
}
