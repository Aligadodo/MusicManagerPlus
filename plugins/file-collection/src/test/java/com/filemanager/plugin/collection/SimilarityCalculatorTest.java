package com.filemanager.plugin.collection;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimilarityCalculatorTest {

    @Test
    void testCalculateSimilarityWithNullInput() {
        double similarity = SimilarityCalculator.calculateSimilarity(null, "test");
        assertEquals(0.0, similarity, 0.001);
        
        similarity = SimilarityCalculator.calculateSimilarity("test", null);
        assertEquals(0.0, similarity, 0.001);
        
        similarity = SimilarityCalculator.calculateSimilarity(null, null);
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void testCalculateSimilarityWithEqualStrings() {
        double similarity = SimilarityCalculator.calculateSimilarity("test", "test");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testCalculateSimilarityWithDifferentStrings() {
        double similarity = SimilarityCalculator.calculateSimilarity("test", "test2");
        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void testCalculateSimilarityWithEmptyStrings() {
        double similarity = SimilarityCalculator.calculateSimilarity("", "");
        assertEquals(1.0, similarity, 0.001);
        
        similarity = SimilarityCalculator.calculateSimilarity("test", "");
        assertTrue(similarity >= 0.0 && similarity < 1.0);
    }

    @Test
    void testCalculateSimilarityWithLevenshtein() {
        double similarity = SimilarityCalculator.calculateSimilarity(
            "kitten", 
            "sitting", 
            SimilarityCalculator.SimilarityType.LEVENSHTEIN
        );
        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void testCalculateSimilarityWithJaroWinkler() {
        double similarity = SimilarityCalculator.calculateSimilarity(
            "MARTHA", 
            "MARHTA", 
            SimilarityCalculator.SimilarityType.JARO_WINKLER
        );
        assertTrue(similarity > 0.9);
    }

    @Test
    void testCalculateSimilarityWithCosine() {
        double similarity = SimilarityCalculator.calculateSimilarity(
            "hello world", 
            "hello there", 
            SimilarityCalculator.SimilarityType.COSINE
        );
        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void testCalculateSimilarityWithJaccard() {
        double similarity = SimilarityCalculator.calculateSimilarity(
            "hello world", 
            "hello there", 
            SimilarityCalculator.SimilarityType.JACCARD
        );
        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void testCalculateFileNameSimilarity() {
        double similarity = SimilarityCalculator.calculateFileNameSimilarity(
            "song1.mp3", 
            "song2.mp3"
        );
        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void testCalculateFileNameSimilarityWithNullInput() {
        double similarity = SimilarityCalculator.calculateFileNameSimilarity(null, "test.mp3");
        assertEquals(0.0, similarity, 0.001);
        
        similarity = SimilarityCalculator.calculateFileNameSimilarity("test.mp3", null);
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void testCalculateFileNameSimilarityWithEqualNames() {
        double similarity = SimilarityCalculator.calculateFileNameSimilarity(
            "song.mp3", 
            "song.mp3"
        );
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testCalculateFileNameSimilarityWithDifferentExtensions() {
        double similarity = SimilarityCalculator.calculateFileNameSimilarity(
            "song.mp3", 
            "song.wav"
        );
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testCalculateFilePathSimilarity() {
        double similarity = SimilarityCalculator.calculateFilePathSimilarity(
            "/music/artist/album/song1.mp3", 
            "/music/artist/album/song2.mp3"
        );
        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void testCalculateFilePathSimilarityWithNullInput() {
        double similarity = SimilarityCalculator.calculateFilePathSimilarity(null, "/test/song.mp3");
        assertEquals(0.0, similarity, 0.001);
        
        similarity = SimilarityCalculator.calculateFilePathSimilarity("/test/song.mp3", null);
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void testCalculateFilePathSimilarityWithEqualPaths() {
        double similarity = SimilarityCalculator.calculateFilePathSimilarity(
            "/music/artist/album/song.mp3", 
            "/music/artist/album/song.mp3"
        );
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testFindLongestCommonPrefix() {
        String prefix = SimilarityCalculator.findLongestCommonPrefix(
            java.util.Arrays.asList("interstellar", "internet", "internal")
        );
        assertEquals("inter", prefix);
    }

    @Test
    void testFindLongestCommonPrefixWithEmptyList() {
        String prefix = SimilarityCalculator.findLongestCommonPrefix(null);
        assertEquals("", prefix);
        
        prefix = SimilarityCalculator.findLongestCommonPrefix(java.util.Arrays.asList());
        assertEquals("", prefix);
    }

    @Test
    void testFindLongestCommonPrefixWithSingleString() {
        String prefix = SimilarityCalculator.findLongestCommonPrefix(
            java.util.Arrays.asList("test")
        );
        assertEquals("test", prefix);
    }

    @Test
    void testFindLongestCommonPrefixWithNoCommonPrefix() {
        String prefix = SimilarityCalculator.findLongestCommonPrefix(
            java.util.Arrays.asList("apple", "banana", "cherry")
        );
        assertEquals("", prefix);
    }

    @Test
    void testFindLongestCommonSubstring() {
        String substring = SimilarityCalculator.findLongestCommonSubstring(
            "ABABC", 
            "BABCA"
        );
        assertEquals("BABC", substring);
    }

    @Test
    void testFindLongestCommonSubstringWithNullInput() {
        String substring = SimilarityCalculator.findLongestCommonSubstring(null, "test");
        assertEquals("", substring);
        
        substring = SimilarityCalculator.findLongestCommonSubstring("test", null);
        assertEquals("", substring);
    }

    @Test
    void testFindLongestCommonSubstringWithEqualStrings() {
        String substring = SimilarityCalculator.findLongestCommonSubstring("test", "test");
        assertEquals("test", substring);
    }

    @Test
    void testFindLongestCommonSubstringWithNoCommonSubstring() {
        String substring = SimilarityCalculator.findLongestCommonSubstring("abc", "def");
        assertEquals("", substring);
    }

    @Test
    void testSimilarityAccuracy() {
        double similarity = SimilarityCalculator.calculateSimilarity(
            "周杰伦 - 青花瓷.mp3", 
            "周杰伦 - 青花瓷 (Remix).mp3"
        );
        assertTrue(similarity > 0.5);
    }

    @Test
    void testSimilarityPerformance() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            SimilarityCalculator.calculateSimilarity(
                "周杰伦 - 青花瓷.mp3", 
                "周杰伦 - 青花瓷 (Remix).mp3"
            );
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 1000, "Similarity calculation should be fast (< 1s for 1000 calculations)");
    }
}
