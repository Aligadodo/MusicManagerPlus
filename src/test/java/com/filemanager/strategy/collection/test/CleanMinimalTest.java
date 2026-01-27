package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 测试cleanMinimal方法
 */
public class CleanMinimalTest {
    
    @Test
    public void testCleanMinimal() {
        System.out.println("=== 测试cleanMinimal方法 ===");
        
        String test1 = "[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一";
        
        System.out.println("原始字符串: " + test1);
        
        // 步骤1：去除方括号中的CD编号
        String step1 = test1.replaceAll("\\[龙音[海文香港环球]+版[^\\]]*\\d+[^\\]]*\\]", "[龙音海文版]");
        System.out.println("步骤1（去除CD编号）: " + step1);
        
        // 步骤2：去除文件扩展名
        int lastDotIndex = step1.lastIndexOf('.');
        String step2 = step1;
        if (lastDotIndex > 0) {
            step2 = step1.substring(0, lastDotIndex);
        }
        System.out.println("步骤2（去除文件扩展名）: " + step2);
        
        // 步骤3：去除CD序号
        String step3 = step2.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        step3 = step3.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        System.out.println("步骤3（去除CD序号）: " + step3);
        
        // 步骤4：去除CD数量信息
        String step4 = step3.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        System.out.println("步骤4（去除CD数量信息）: " + step4);
        
        // 步骤5：去除方括号中的CD序号
        String step5 = step4.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        step5 = step5.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        System.out.println("步骤5（去除方括号CD序号）: " + step5);
        
        // 步骤6：去除序号
        String step6 = step5.replaceAll("[之][一二三四五六七八九十]+", "");
        System.out.println("步骤6（去除序号）: " + step6);
        
        // 步骤7：去除多余空格和特殊字符
        String step7 = step6.trim();
        step7 = step7.replaceAll("\\s+", " ");
        System.out.println("步骤7（去除多余空格和特殊字符）: " + step7);
        
        System.out.println("\n最终结果: " + step7);
    }
}
