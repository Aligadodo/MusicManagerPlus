package com.filemanager.strategy.collection;

/**
 * 字符串相似度计算器接口
 * 
 * @author FileEditTools Team
 */
public interface StringSimilarityCalculator {
    
    /**
     * 计算两个字符串的相似度
     * 
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 相似度值，范围[0, 1]，1表示完全相同，0表示完全不同
     */
    double calculateSimilarity(String s1, String s2);
}
