package com.filemanager.plugin.impl.filecollection.collection;

public class TextSimilarityCalculator {

    public static double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        if (str1.equals(str2)) {
            return 1.0;
        }

        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = calculateLevenshteinDistance(str1, str2);
        double similarity = 1.0 - ((double) distance / maxLength);

        return similarity;
    }

    public static double calculateSimilarityNormalized(String str1, String str2) {
        String normalized1 = FilenameNormalizer.normalize(str1);
        String normalized2 = FilenameNormalizer.normalize(str2);

        return calculateSimilarity(normalized1, normalized2);
    }

    public static double calculateSimilaritySimple(String str1, String str2) {
        String normalized1 = FilenameNormalizer.normalizeSimple(str1);
        String normalized2 = FilenameNormalizer.normalizeSimple(str2);

        return calculateSimilarity(normalized1, normalized2);
    }

    public static double calculateSimilarityPrecise(String str1, String str2) {
        String normalized1 = FilenameNormalizer.normalizePrecise(str1);
        String normalized2 = FilenameNormalizer.normalizePrecise(str2);

        return calculateSimilarity(normalized1, normalized2);
    }

    private static int calculateLevenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }

    public static boolean isSimilar(String str1, String str2, double threshold) {
        return calculateSimilarity(str1, str2) >= threshold;
    }

    public static boolean isSimilarNormalized(String str1, String str2, double threshold) {
        return calculateSimilarityNormalized(str1, str2) >= threshold;
    }

    public static boolean isSimilarSimple(String str1, String str2, double threshold) {
        return calculateSimilaritySimple(str1, str2) >= threshold;
    }

    public static boolean isSimilarPrecise(String str1, String str2, double threshold) {
        return calculateSimilarityPrecise(str1, str2) >= threshold;
    }
}