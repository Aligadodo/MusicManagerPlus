package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 测试removeExtraInfo方法的详细过程
 */
public class RemoveExtraInfoDetailTest {
    
    @Test
    public void testRemoveExtraInfoDetail() {
        System.out.println("=== 测试removeExtraInfo方法的详细过程 ===");
        
        String test1 = "[龙音海文版]茉莉芬芳-陈爱娟古筝独奏之一";
        
        System.out.println("原始字符串: " + test1);
        
        // 步骤1：去除方括号中的CD编号（如CD-0221、CD-0176等）
        String step1 = test1.replaceAll("\\[龙音[海文香港环球]+版\\s+CD\\s*-\\d+\\]", "[龙音海文版]");
        System.out.println("步骤1（去除CD编号1）: " + step1);
        
        step1 = step1.replaceAll("\\[龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        System.out.println("步骤1（去除CD编号2）: " + step1);
        
        step1 = step1.replaceAll("\\[龙音[海文香港环球]+版\\s*CD\\s*-\\d+\\]", "[龙音海文版]");
        System.out.println("步骤1（去除CD编号3）: " + step1);
        
        step1 = step1.replaceAll("\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        System.out.println("步骤1（去除CD编号4）: " + step1);
        
        // 步骤2：去除龙音系列中的CD编号（不包含方括号的情况）
        String step2 = step1.replaceAll("龙音[海文香港环球]+版\\s+CD\\s*-\\d+", "龙音海文版");
        System.out.println("步骤2（去除CD编号5）: " + step2);
        
        step2 = step2.replaceAll("龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        System.out.println("步骤2（去除CD编号6）: " + step2);
        
        step2 = step2.replaceAll("龙音[海文香港环球]+版\\s*CD\\s*-\\d+", "龙音海文版");
        System.out.println("步骤2（去除CD编号7）: " + step2);
        
        step2 = step2.replaceAll("龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        System.out.println("步骤2（去除CD编号8）: " + step2);
        
        // 步骤3：去除CD序号（包括CD1、CD01、CD 1等格式）
        String step3 = step2.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        System.out.println("步骤3（去除CD序号1）: " + step3);
        
        step3 = step3.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        System.out.println("步骤3（去除CD序号2）: " + step3);
        
        // 步骤4：去除CD数量信息（如3CD、2CD等）
        String step4 = step3.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        System.out.println("步骤4（去除CD数量）: " + step4);
        
        // 步骤5：去除文件格式信息（包括WAV、FLAC、MP3、DTS、CUE等）
        String step5 = step4.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*[Cc][Uu][Ee]\\b", "");
        step5 = step5.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*分轨\\b", "");
        step5 = step5.replaceAll("\\s*[Ww][Aa][Vv]\\b", "");
        step5 = step5.replaceAll("\\s*[Ff][Ll][Aa][Cc]\\b", "");
        step5 = step5.replaceAll("\\s*[Mm][Pp]3\\b", "");
        step5 = step5.replaceAll("\\s*[Dd][Tt][Ss]\\b", "");
        step5 = step5.replaceAll("\\s*[Cc][Uu][Ee]\\b", "");
        step5 = step5.replaceAll("\\s*分轨\\b", "");
        step5 = step5.replaceAll("\\s*[Aa][Pp][Ee]\\b", "");
        System.out.println("步骤5（去除文件格式）: " + step5);
        
        // 步骤6：去除方括号中的CD序号（如[CD1]、[CD2]等）
        String step6 = step5.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        System.out.println("步骤6（去除方括号CD序号1）: " + step6);
        
        step6 = step6.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        System.out.println("步骤6（去除方括号CD序号2）: " + step6);
        
        // 步骤7：去除特殊字符和多余空格
        String step7 = step6.replaceAll("\\s+", " ");
        step7 = step7.trim();
        System.out.println("步骤7（去除特殊字符和多余空格）: " + step7);
        
        System.out.println("\n最终结果: " + step7);
    }
}
