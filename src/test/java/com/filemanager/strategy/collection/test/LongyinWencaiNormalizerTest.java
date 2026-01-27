package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.FilenameNormalizer;
import com.filemanager.strategy.collection.TextSimilarityCalculator;
import org.junit.Test;

/**
 * 测试FilenameNormalizer对龙音文采华音版-轻舟随波系列的影响
 */
public class LongyinWencaiNormalizerTest {
    
    @Test
    public void testNormalizer() {
        System.out.println("=== 测试FilenameNormalizer对龙音文采华音版-轻舟随波系列的影响 ===");
        
        String file1 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶";
        String file2 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事";
        String file3 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥华夏风情篇-睡莲";
        String file4 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦-异国风情篇-美丽的梭罗河";
        
        FilenameNormalizer normalizer = FilenameNormalizer.builder().build();
        
        System.out.println("原始文件名:");
        System.out.println("文件1: " + file1);
        System.out.println("文件2: " + file2);
        System.out.println("文件3: " + file3);
        System.out.println("文件4: " + file4);
        
        String normalized1 = normalizer.normalize(file1);
        String normalized2 = normalizer.normalize(file2);
        String normalized3 = normalizer.normalize(file3);
        String normalized4 = normalizer.normalize(file4);
        
        System.out.println("\n标准化后的文件名:");
        System.out.println("文件1: " + normalized1);
        System.out.println("文件2: " + normalized2);
        System.out.println("文件3: " + normalized3);
        System.out.println("文件4: " + normalized4);
        
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        System.out.println("\n原始文件名的相似度:");
        System.out.println("文件1 vs 文件2: " + calculator.calculateSimilarity(file1, file2));
        System.out.println("文件1 vs 文件3: " + calculator.calculateSimilarity(file1, file3));
        System.out.println("文件1 vs 文件4: " + calculator.calculateSimilarity(file1, file4));
        System.out.println("文件2 vs 文件3: " + calculator.calculateSimilarity(file2, file3));
        System.out.println("文件2 vs 文件4: " + calculator.calculateSimilarity(file2, file4));
        System.out.println("文件3 vs 文件4: " + calculator.calculateSimilarity(file3, file4));
        
        System.out.println("\n标准化后文件名的相似度:");
        System.out.println("文件1 vs 文件2: " + calculator.calculateSimilarity(normalized1, normalized2));
        System.out.println("文件1 vs 文件3: " + calculator.calculateSimilarity(normalized1, normalized3));
        System.out.println("文件1 vs 文件4: " + calculator.calculateSimilarity(normalized1, normalized4));
        System.out.println("文件2 vs 文件3: " + calculator.calculateSimilarity(normalized2, normalized3));
        System.out.println("文件2 vs 文件4: " + calculator.calculateSimilarity(normalized2, normalized4));
        System.out.println("文件3 vs 文件4: " + calculator.calculateSimilarity(normalized3, normalized4));
    }
}
