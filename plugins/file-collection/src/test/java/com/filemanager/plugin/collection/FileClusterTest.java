package com.filemanager.plugin.collection;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FileClusterTest {

    @Test
    void testFileClusterConstructor() {
        FileCluster cluster = new FileCluster();
        assertNotNull(cluster);
        assertTrue(cluster.isEmpty());
        assertEquals(0, cluster.size());
    }

    @Test
    void testFileClusterConstructorWithName() {
        FileCluster cluster = new FileCluster("Test Cluster");
        assertEquals("Test Cluster", cluster.getClusterName());
        assertTrue(cluster.isEmpty());
    }

    @Test
    void testFileClusterConstructorWithFiles() {
        List<String> files = Arrays.asList("/path/file1.mp3", "/path/file2.mp3");
        FileCluster cluster = new FileCluster("Test Cluster", files);
        
        assertEquals("Test Cluster", cluster.getClusterName());
        assertEquals(2, cluster.size());
        assertFalse(cluster.isEmpty());
    }

    @Test
    void testAddFilePath() {
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/file1.mp3");
        
        assertEquals(1, cluster.size());
        assertFalse(cluster.isEmpty());
    }

    @Test
    void testAddFilePaths() {
        FileCluster cluster = new FileCluster();
        List<String> files = Arrays.asList("/path/file1.mp3", "/path/file2.mp3", "/path/file3.mp3");
        cluster.addFilePaths(files);
        
        assertEquals(3, cluster.size());
        assertFalse(cluster.isEmpty());
    }

    @Test
    void testSetFilePaths() {
        FileCluster cluster = new FileCluster();
        List<String> files = Arrays.asList("/path/file1.mp3", "/path/file2.mp3");
        cluster.setFilePaths(files);
        
        assertEquals(2, cluster.size());
        assertEquals(files, cluster.getFilePaths());
    }

    @Test
    void testCalculateAverageSimilarityWithSingleFile() {
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/file1.mp3");
        cluster.calculateAverageSimilarity();
        
        assertEquals(1.0, cluster.getAverageSimilarity(), 0.001);
    }

    @Test
    void testCalculateAverageSimilarityWithMultipleFiles() {
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/song1.mp3");
        cluster.addFilePath("/path/song2.mp3");
        cluster.addFilePath("/path/song3.mp3");
        cluster.calculateAverageSimilarity();
        
        assertTrue(cluster.getAverageSimilarity() > 0.0 && cluster.getAverageSimilarity() <= 1.0);
    }

    @Test
    void testCalculateCommonPrefixWithSingleFile() {
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/song1.mp3");
        cluster.calculateCommonPrefix();
        
        assertEquals("song1.mp3", cluster.getCommonPrefix());
    }

    @Test
    void testCalculateCommonPrefixWithMultipleFiles() {
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/周杰伦 - 青花瓷.mp3");
        cluster.addFilePath("/path/周杰伦 - 青花瓷 (Remix).mp3");
        cluster.addFilePath("/path/周杰伦 - 青花瓷 (Live).mp3");
        cluster.calculateCommonPrefix();
        
        assertEquals("周杰伦 - 青花瓷", cluster.getCommonPrefix());
    }

    @Test
    void testToString() {
        FileCluster cluster = new FileCluster("Test Cluster");
        cluster.addFilePath("/path/file1.mp3");
        cluster.addFilePath("/path/file2.mp3");
        cluster.calculateAverageSimilarity();
        cluster.calculateCommonPrefix();
        
        String str = cluster.toString();
        assertNotNull(str);
        assertTrue(str.contains("Test Cluster"));
        assertTrue(str.contains("size=2"));
    }
}

class FileClustererTest {

    @Test
    void testClusterFilesWithEmptyList() {
        List<FileCluster> clusters = FileClusterer.clusterFiles(null, 0.7);
        assertNotNull(clusters);
        assertTrue(clusters.isEmpty());
        
        clusters = FileClusterer.clusterFiles(Arrays.asList(), 0.7);
        assertNotNull(clusters);
        assertTrue(clusters.isEmpty());
    }

    @Test
    void testClusterFilesWithSingleFile() {
        List<String> files = Arrays.asList("/path/song1.mp3");
        List<FileCluster> clusters = FileClusterer.clusterFiles(files, 0.7);
        
        assertEquals(1, clusters.size());
        assertEquals(1, clusters.get(0).size());
        assertEquals(1.0, clusters.get(0).getAverageSimilarity(), 0.001);
    }

    @Test
    void testClusterFilesWithSimilarFiles() {
        List<String> files = Arrays.asList(
            "/path/周杰伦 - 青花瓷.mp3",
            "/path/周杰伦 - 青花瓷 (Remix).mp3",
            "/path/周杰伦 - 青花瓷 (Live).mp3"
        );
        List<FileCluster> clusters = FileClusterer.clusterFiles(files, 0.7);
        
        assertTrue(clusters.size() >= 1);
        assertTrue(clusters.get(0).size() >= 2);
    }

    @Test
    void testClusterFilesWithDifferentFiles() {
        List<String> files = Arrays.asList(
            "/path/song1.mp3",
            "/path/song2.mp3",
            "/path/song3.mp3"
        );
        List<FileCluster> clusters = FileClusterer.clusterFiles(files, 0.9);
        
        assertTrue(clusters.size() >= 1);
    }

    @Test
    void testClusterFilesWithDifferentThresholds() {
        List<String> files = Arrays.asList(
            "/path/周杰伦 - 青花瓷.mp3",
            "/path/周杰伦 - 青花瓷 (Remix).mp3",
            "/path/周杰伦 - 青花瓷 (Live).mp3"
        );
        
        List<FileCluster> clustersHigh = FileClusterer.clusterFiles(files, 0.9);
        List<FileCluster> clustersLow = FileClusterer.clusterFiles(files, 0.5);
        
        assertTrue(clustersLow.size() <= clustersHigh.size());
    }

    @Test
    void testClusterFilesWithHierarchical() {
        List<String> files = Arrays.asList(
            "/path/周杰伦 - 青花瓷.mp3",
            "/path/周杰伦 - 青花瓷 (Remix).mp3",
            "/path/周杰伦 - 青花瓷 (Live).mp3",
            "/path/周杰伦 - 稻香.mp3",
            "/path/周杰伦 - 稻香 (Remix).mp3"
        );
        List<FileCluster> clusters = FileClusterer.clusterFilesWithHierarchical(files, 0.7);
        
        assertNotNull(clusters);
        assertTrue(clusters.size() >= 1);
    }

    @Test
    void testOptimizeClusters() {
        List<FileCluster> clusters = Arrays.asList(
            createCluster("Cluster1", Arrays.asList("/path/file1.mp3", "/path/file2.mp3"), 0.8),
            createCluster("Cluster2", Arrays.asList("/path/file3.mp3"), 0.9),
            createCluster("Cluster3", Arrays.asList("/path/file4.mp3"), 0.7)
        );
        
        List<FileCluster> optimized = FileClusterer.optimizeClusters(clusters, 2);
        
        assertEquals(1, optimized.size());
        assertEquals("Cluster1", optimized.get(0).getClusterName());
    }

    @Test
    void testOptimizeClustersWithEmptyList() {
        List<FileCluster> optimized = FileClusterer.optimizeClusters(null, 2);
        assertNotNull(optimized);
        assertTrue(optimized.isEmpty());
        
        optimized = FileClusterer.optimizeClusters(Arrays.asList(), 2);
        assertNotNull(optimized);
        assertTrue(optimized.isEmpty());
    }

    @Test
    void testClusterByDirectory() {
        List<String> files = Arrays.asList(
            "/dir1/song1.mp3",
            "/dir1/song2.mp3",
            "/dir2/song3.mp3",
            "/dir2/song4.mp3"
        );
        
        Map<String, List<FileCluster>> result = FileClusterer.clusterByDirectory(files, 0.7);
        
        assertNotNull(result);
        assertTrue(result.containsKey("/dir1"));
        assertTrue(result.containsKey("/dir2"));
    }

    @Test
    void testFilterClustersBySimilarity() {
        List<FileCluster> clusters = Arrays.asList(
            createCluster("Cluster1", Arrays.asList("/path/file1.mp3", "/path/file2.mp3"), 0.8),
            createCluster("Cluster2", Arrays.asList("/path/file3.mp3"), 0.9),
            createCluster("Cluster3", Arrays.asList("/path/file4.mp3"), 0.6)
        );
        
        List<FileCluster> filtered = FileClusterer.filterClustersBySimilarity(clusters, 0.7);
        
        assertEquals(2, filtered.size());
    }

    @Test
    void testFilterClustersBySimilarityWithEmptyList() {
        List<FileCluster> filtered = FileClusterer.filterClustersBySimilarity(null, 0.7);
        assertNotNull(filtered);
        assertTrue(filtered.isEmpty());
    }

    @Test
    void testSortClustersBySize() {
        List<FileCluster> clusters = Arrays.asList(
            createCluster("Cluster1", Arrays.asList("/path/file1.mp3"), 0.8),
            createCluster("Cluster2", Arrays.asList("/path/file2.mp3", "/path/file3.mp3"), 0.9),
            createCluster("Cluster3", Arrays.asList("/path/file4.mp3", "/path/file5.mp3", "/path/file6.mp3"), 0.7)
        );
        
        List<FileCluster> sorted = FileClusterer.sortClusters(clusters, FileClusterer.SortBy.SIZE);
        
        assertEquals(3, sorted.get(0).size());
        assertEquals(2, sorted.get(1).size());
        assertEquals(1, sorted.get(2).size());
    }

    @Test
    void testSortClustersBySimilarity() {
        List<FileCluster> clusters = Arrays.asList(
            createCluster("Cluster1", Arrays.asList("/path/file1.mp3"), 0.8),
            createCluster("Cluster2", Arrays.asList("/path/file2.mp3"), 0.9),
            createCluster("Cluster3", Arrays.asList("/path/file3.mp3"), 0.7)
        );
        
        List<FileCluster> sorted = FileClusterer.sortClusters(clusters, FileClusterer.SortBy.SIMILARITY);
        
        assertEquals(0.9, sorted.get(0).getAverageSimilarity(), 0.001);
        assertEquals(0.8, sorted.get(1).getAverageSimilarity(), 0.001);
        assertEquals(0.7, sorted.get(2).getAverageSimilarity(), 0.001);
    }

    @Test
    void testSortClustersByName() {
        List<FileCluster> clusters = Arrays.asList(
            createCluster("Cluster C", Arrays.asList("/path/file1.mp3"), 0.8),
            createCluster("Cluster A", Arrays.asList("/path/file2.mp3"), 0.9),
            createCluster("Cluster B", Arrays.asList("/path/file3.mp3"), 0.7)
        );
        
        List<FileCluster> sorted = FileClusterer.sortClusters(clusters, FileClusterer.SortBy.NAME);
        
        assertEquals("Cluster A", sorted.get(0).getClusterName());
        assertEquals("Cluster B", sorted.get(1).getClusterName());
        assertEquals("Cluster C", sorted.get(2).getClusterName());
    }

    @Test
    void testSortClustersWithEmptyList() {
        List<FileCluster> sorted = FileClusterer.sortClusters(null, FileClusterer.SortBy.SIZE);
        assertNotNull(sorted);
        assertTrue(sorted.isEmpty());
    }

    @Test
    void testClusteringPerformance() {
        List<String> files = Arrays.asList(
            "/path/周杰伦 - 青花瓷.mp3",
            "/path/周杰伦 - 青花瓷 (Remix).mp3",
            "/path/周杰伦 - 青花瓷 (Live).mp3",
            "/path/周杰伦 - 稻香.mp3",
            "/path/周杰伦 - 稻香 (Remix).mp3",
            "/path/周杰伦 - 稻香 (Live).mp3",
            "/path/周杰伦 - 夜曲.mp3",
            "/path/周杰伦 - 夜曲 (Remix).mp3"
        );
        
        long startTime = System.currentTimeMillis();
        List<FileCluster> clusters = FileClusterer.clusterFiles(files, 0.7);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(clusters);
        assertFalse(clusters.isEmpty());
        assertTrue(endTime - startTime < 1000, "Clustering should be fast (< 1s for 8 files)");
    }

    @Test
    void testClusteringAccuracy() {
        List<String> files = Arrays.asList(
            "/path/周杰伦 - 青花瓷.mp3",
            "/path/周杰伦 - 青花瓷 (Remix).mp3",
            "/path/周杰伦 - 青花瓷 (Live).mp3",
            "/path/林俊杰 - 江南.mp3",
            "/path/林俊杰 - 江南 (Remix).mp3"
        );
        
        List<FileCluster> clusters = FileClusterer.clusterFiles(files, 0.7);
        
        assertTrue(clusters.size() >= 1);
        
        boolean hasJayChouCluster = clusters.stream()
            .anyMatch(c -> c.getCommonPrefix().contains("周杰伦"));
        boolean hasJJLinCluster = clusters.stream()
            .anyMatch(c -> c.getCommonPrefix().contains("林俊杰"));
        
        assertTrue(hasJayChouCluster || hasJJLinCluster, "Should cluster similar songs together");
    }

    private FileCluster createCluster(String name, List<String> files, double similarity) {
        FileCluster cluster = new FileCluster(name, files);
        cluster.setAverageSimilarity(similarity);
        cluster.calculateCommonPrefix();
        return cluster;
    }
}
