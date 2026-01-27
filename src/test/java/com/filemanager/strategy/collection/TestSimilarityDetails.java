package com.filemanager.strategy.collection;

import org.junit.Test;

/**
 * 测试相似度计算细节
 */
public class TestSimilarityDetails {
    
    @Test
    public void testSimilarityDetails() {
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        // 测试15首精选滚石年度强打金曲
        String s1 = "滚石群星200雀巢咖啡时尚精选 15首精选滚石年度强打金曲 滚石";
        String s2 = "雀巢咖啡时尚精选 15首精选滚石年度强打金曲";
        
        System.out.println("=== 相似度计算细节 ===");
        System.out.println("字符串1: " + s1);
        System.out.println("字符串2: " + s2);
        System.out.println("字符串1长度: " + s1.length());
        System.out.println("字符串2长度: " + s2.length());
        
        // 计算编辑距离
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        double levenshteinSimilarity = 1.0 - ((double) distance / maxLength);
        
        System.out.println("编辑距离: " + distance);
        System.out.println("编辑距离相似度: " + levenshteinSimilarity);
        
        // 计算最长公共子串
        int lcsLength = findLongestCommonSubstring(s1, s2);
        double lcsSimilarity = (double) lcsLength / maxLength;
        
        System.out.println("最长公共子串长度: " + lcsLength);
        System.out.println("最长公共子串相似度: " + lcsSimilarity);
        
        // 综合相似度
        double combinedSimilarity = levenshteinSimilarity * 0.3 + lcsSimilarity * 0.7;
        System.out.println("综合相似度: " + combinedSimilarity);
        
        // 使用计算器计算
        double calculatorSimilarity = calculator.calculateSimilarity(s1, s2);
        System.out.println("计算器相似度: " + calculatorSimilarity);
    }
    
    /**
     * 计算编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            throw new IllegalArgumentException("Strings cannot be null");
        }

        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0) {
            return len2;
        }
        if (len2 == 0) {
            return len1;
        }

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }
    
    /**
     * 查找最长公共子串长度
     */
    private int findLongestCommonSubstring(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        int maxLength = 0;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    maxLength = Math.max(maxLength, dp[i][j]);
                } else {
                    dp[i][j] = 0;
                }
            }
        }
        
        return maxLength;
    }
}
