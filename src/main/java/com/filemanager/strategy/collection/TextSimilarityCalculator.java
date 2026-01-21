package com.filemanager.strategy.collection;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class TextSimilarityCalculator {
    
    private final double similarityThreshold;
    
    public TextSimilarityCalculator() {
        this(0.7);
    }
    
    public TextSimilarityCalculator(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
    
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    public int levenshteinDistance(String s1, String s2) {
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
    
    public boolean isSimilar(String s1, String s2) {
        return calculateSimilarity(s1, s2) >= similarityThreshold;
    }
    
    public List<String> filterOutliers(List<String> filenames, String clusterName) {
        List<String> filtered = new ArrayList<>();
        
        for (String filename : filenames) {
            if (isSimilar(filename, clusterName)) {
                filtered.add(filename);
            }
        }
        
        return filtered;
    }
    
    public double calculateClusterSimilarity(List<String> filenames) {
        if (filenames == null || filenames.size() < 2) {
            return 1.0;
        }
        
        double totalSimilarity = 0.0;
        int comparisons = 0;
        
        for (int i = 0; i < filenames.size(); i++) {
            for (int j = i + 1; j < filenames.size(); j++) {
                totalSimilarity += calculateSimilarity(filenames.get(i), filenames.get(j));
                comparisons++;
            }
        }
        
        return comparisons > 0 ? totalSimilarity / comparisons : 1.0;
    }
    
    public double calculateAverageSimilarityToCluster(String filename, List<String> cluster) {
        if (cluster == null || cluster.isEmpty()) {
            return 0.0;
        }
        
        double totalSimilarity = 0.0;
        
        for (String clusterFilename : cluster) {
            totalSimilarity += calculateSimilarity(filename, clusterFilename);
        }
        
        return totalSimilarity / cluster.size();
    }
    
    public String findMostSimilar(String target, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        
        String mostSimilar = null;
        double maxSimilarity = -1.0;
        
        for (String candidate : candidates) {
            double similarity = calculateSimilarity(target, candidate);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilar = candidate;
            }
        }
        
        return mostSimilar;
    }
    
    public List<String> findSimilarFilenames(String target, List<String> candidates, double threshold) {
        List<String> similar = new ArrayList<>();
        
        if (candidates == null) {
            return similar;
        }
        
        for (String candidate : candidates) {
            if (calculateSimilarity(target, candidate) >= threshold) {
                similar.add(candidate);
            }
        }
        
        return similar;
    }
    
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    public static class Builder {
        private double similarityThreshold = 0.7;
        
        public Builder similarityThreshold(double threshold) {
            this.similarityThreshold = threshold;
            return this;
        }
        
        public TextSimilarityCalculator build() {
            return new TextSimilarityCalculator(similarityThreshold);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
