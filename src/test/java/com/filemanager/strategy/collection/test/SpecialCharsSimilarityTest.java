package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.TextSimilarityCalculator;
import org.junit.Test;

/**
 * 测试特殊字符对相似度计算的影响
 */
public class SpecialCharsSimilarityTest {
    
    @Test
    public void testSpecialChars() {
        System.out.println("=== 测试特殊字符对相似度计算的影响 ===");
        
        String file1 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶";
        String file2 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事";
        
        String clean1 = "龙音唱片 龙音文采华音版 轻舟随波系列 排箫爱情篇 罗密欧与朱丽叶";
        String clean2 = "龙音唱片 龙音文采华音版 轻舟随波系列 钢琴弄潮篇 爱情故事";
        
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        System.out.println("原始文件名:");
        System.out.println("文件1: " + file1);
        System.out.println("文件2: " + file2);
        System.out.println("相似度: " + calculator.calculateSimilarity(file1, file2));
        
        System.out.println("\n清理后的文件名:");
        System.out.println("文件1: " + clean1);
        System.out.println("文件2: " + clean2);
        System.out.println("相似度: " + calculator.calculateSimilarity(clean1, clean2));
        
        System.out.println("\n结论:");
        System.out.println("特殊字符降低了相似度: " + (calculator.calculateSimilarity(clean1, clean2) > calculator.calculateSimilarity(file1, file2)));
    }
}
