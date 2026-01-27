package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.TextSimilarityCalculator;
import org.junit.Test;

/**
 * 测试龙音文采华音版-轻舟随波系列的相似度计算
 */
public class LongyinWencaiSimilarityTest {
    
    @Test
    public void testSimilarity() {
        System.out.println("=== 测试龙音文采华音版-轻舟随波系列的相似度计算 ===");
        
        String file1 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶";
        String file2 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事";
        String file3 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥华夏风情篇-睡莲";
        String file4 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦-异国风情篇-美丽的梭罗河";
        
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        System.out.println("文件1: " + file1);
        System.out.println("文件2: " + file2);
        System.out.println("文件3: " + file3);
        System.out.println("文件4: " + file4);
        
        System.out.println("\n相似度计算:");
        System.out.println("文件1 vs 文件2: " + calculator.calculateSimilarity(file1, file2));
        System.out.println("文件1 vs 文件3: " + calculator.calculateSimilarity(file1, file3));
        System.out.println("文件1 vs 文件4: " + calculator.calculateSimilarity(file1, file4));
        System.out.println("文件2 vs 文件3: " + calculator.calculateSimilarity(file2, file3));
        System.out.println("文件2 vs 文件4: " + calculator.calculateSimilarity(file2, file4));
        System.out.println("文件3 vs 文件4: " + calculator.calculateSimilarity(file3, file4));
        
        System.out.println("\n是否相似:");
        System.out.println("文件1 vs 文件2: " + calculator.isSimilar(file1, file2));
        System.out.println("文件1 vs 文件3: " + calculator.isSimilar(file1, file3));
        System.out.println("文件1 vs 文件4: " + calculator.isSimilar(file1, file4));
        System.out.println("文件2 vs 文件3: " + calculator.isSimilar(file2, file3));
        System.out.println("文件2 vs 文件4: " + calculator.isSimilar(file2, file4));
        System.out.println("文件3 vs 文件4: " + calculator.isSimilar(file3, file4));
    }
}
