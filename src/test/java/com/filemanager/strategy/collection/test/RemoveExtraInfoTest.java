package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 测试removeExtraInfo方法是否正确去除CD编号
 */
public class RemoveExtraInfoTest {
    
    @Test
    public void testRemoveExtraInfo() {
        String test1 = "龙音海文版 CD-0221";
        String test2 = "龙音海文版 CD-0176";
        
        System.out.println("原始字符串1: " + test1);
        System.out.println("原始字符串2: " + test2);
        
        // 测试去除CD编号的正则表达式
        String result1 = test1;
        result1 = result1.replaceAll("龙音[海文香港环球]+版\\s+CD\\s*-\\d+", "龙音海文版");
        result1 = result1.replaceAll("龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        result1 = result1.replaceAll("龙音[海文香港环球]+版\\s*CD\\s*-\\d+", "龙音海文版");
        result1 = result1.replaceAll("龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        
        System.out.println("处理后字符串1: " + result1);
        
        String result2 = test2;
        result2 = result2.replaceAll("龙音[海文香港环球]+版\\s+CD\\s*-\\d+", "龙音海文版");
        result2 = result2.replaceAll("龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        result2 = result2.replaceAll("龙音[海文香港环球]+版\\s*CD\\s*-\\d+", "龙音海文版");
        result2 = result2.replaceAll("龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        
        System.out.println("处理后字符串2: " + result2);
    }
}
