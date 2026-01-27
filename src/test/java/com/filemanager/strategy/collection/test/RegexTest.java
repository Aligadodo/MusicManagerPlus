package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 测试正则表达式是否正确去除CD编号
 */
public class RegexTest {
    
    @Test
    public void testRegexPattern() {
        String filename1 = "[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一";
        String filename2 = "[龙音海文版 CD-0176]小河淌水-陈爱娟古筝独奏之二";
        
        System.out.println("原始文件名1: " + filename1);
        System.out.println("原始文件名2: " + filename2);
        
        // 测试去除CD编号的正则表达式
        String pattern1 = "\\[龙音[海文香港环球]+版\\s+CD\\s*-\\d+\\]";
        String pattern2 = "\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]";
        String pattern3 = "\\[龙音[海文香港环球]+版\\s*CD\\s*-\\d+\\]";
        String pattern4 = "\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]";
        
        System.out.println("\n测试pattern1: " + pattern1);
        String result1 = filename1.replaceAll(pattern1, "[龙音海文版]");
        System.out.println("结果1: " + result1);
        
        System.out.println("\n测试pattern2: " + pattern2);
        String result2 = filename2.replaceAll(pattern2, "[龙音海文版]");
        System.out.println("结果2: " + result2);
        
        System.out.println("\n测试pattern3: " + pattern3);
        String result3 = filename1.replaceAll(pattern3, "[龙音海文版]");
        System.out.println("结果3: " + result3);
        
        System.out.println("\n测试pattern4: " + pattern4);
        String result4 = filename2.replaceAll(pattern4, "[龙音海文版]");
        System.out.println("结果4: " + result4);
        
        // 测试组合使用
        System.out.println("\n组合使用所有模式:");
        String combined = filename1;
        combined = combined.replaceAll(pattern1, "[龙音海文版]");
        combined = combined.replaceAll(pattern2, "[龙音海文版]");
        combined = combined.replaceAll(pattern3, "[龙音海文版]");
        combined = combined.replaceAll(pattern4, "[龙音海文版]");
        System.out.println("组合结果: " + combined);
    }
}
