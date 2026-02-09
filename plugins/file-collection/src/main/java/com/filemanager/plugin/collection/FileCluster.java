package com.filemanager.plugin.collection;

import java.util.*;
import java.util.stream.Collectors;

public class FileCluster {
    
    private String clusterName;
    private List<String> filePaths;
    private double averageSimilarity;
    private String commonPrefix;
    
    public FileCluster() {
        this.filePaths = new ArrayList<>();
        this.averageSimilarity = 0.0;
        this.commonPrefix = "";
    }
    
    public FileCluster(String clusterName) {
        this();
        this.clusterName = clusterName;
    }
    
    public FileCluster(String clusterName, List<String> filePaths) {
        this(clusterName);
        this.filePaths = new ArrayList<>(filePaths);
    }
    
    public String getClusterName() {
        return clusterName;
    }
    
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
    
    public List<String> getFilePaths() {
        return filePaths;
    }
    
    public void setFilePaths(List<String> filePaths) {
        this.filePaths = new ArrayList<>(filePaths);
    }
    
    public void addFilePath(String filePath) {
        this.filePaths.add(filePath);
    }
    
    public void addFilePaths(List<String> filePaths) {
        this.filePaths.addAll(filePaths);
    }
    
    public int size() {
        return filePaths.size();
    }
    
    public boolean isEmpty() {
        return filePaths.isEmpty();
    }
    
    public double getAverageSimilarity() {
        return averageSimilarity;
    }
    
    public void setAverageSimilarity(double averageSimilarity) {
        this.averageSimilarity = averageSimilarity;
    }
    
    public String getCommonPrefix() {
        return commonPrefix;
    }
    
    public void setCommonPrefix(String commonPrefix) {
        this.commonPrefix = commonPrefix;
    }
    
    public void calculateAverageSimilarity() {
        if (filePaths.size() < 2) {
            this.averageSimilarity = 1.0;
            return;
        }
        
        double totalSimilarity = 0.0;
        int count = 0;
        
        for (int i = 0; i < filePaths.size(); i++) {
            for (int j = i + 1; j < filePaths.size(); j++) {
                double similarity = SimilarityCalculator.calculateFilePathSimilarity(
                    filePaths.get(i),
                    filePaths.get(j)
                );
                totalSimilarity += similarity;
                count++;
            }
        }
        
        this.averageSimilarity = count > 0 ? totalSimilarity / count : 0.0;
    }
    
    public void calculateCommonPrefix() {
        if (filePaths.isEmpty()) {
            this.commonPrefix = "";
            return;
        }
        
        List<String> fileNames = filePaths.stream()
            .map(this::extractFileName)
            .collect(Collectors.toList());
        
        this.commonPrefix = SimilarityCalculator.findLongestCommonPrefix(fileNames);
    }
    
    private String extractFileName(String filePath) {
        if (filePath == null) {
            return "";
        }
        
        int lastSlashIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlashIndex >= 0) {
            return filePath.substring(lastSlashIndex + 1);
        }
        
        return filePath;
    }
    
    @Override
    public String toString() {
        return "FileCluster{" +
                "clusterName='" + clusterName + '\'' +
                ", size=" + filePaths.size() +
                ", averageSimilarity=" + String.format("%.2f", averageSimilarity) +
                ", commonPrefix='" + commonPrefix + '\'' +
                '}';
    }
}

class FileClusterer {
    
    public static List<FileCluster> clusterFiles(List<String> filePaths, double threshold) {
        return clusterFiles(filePaths, threshold, SimilarityCalculator.SimilarityType.JARO_WINKLER);
    }
    
    public static List<FileCluster> clusterFiles(List<String> filePaths, double threshold, 
                                           SimilarityCalculator.SimilarityType similarityType) {
        if (filePaths == null || filePaths.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (filePaths.size() == 1) {
            FileCluster cluster = new FileCluster();
            cluster.addFilePath(filePaths.get(0));
            cluster.calculateAverageSimilarity();
            cluster.calculateCommonPrefix();
            return Collections.singletonList(cluster);
        }
        
        List<FileCluster> clusters = new ArrayList<>();
        boolean[] assigned = new boolean[filePaths.size()];
        
        for (int i = 0; i < filePaths.size(); i++) {
            if (assigned[i]) {
                continue;
            }
            
            FileCluster cluster = new FileCluster();
            cluster.addFilePath(filePaths.get(i));
            assigned[i] = true;
            
            for (int j = i + 1; j < filePaths.size(); j++) {
                if (assigned[j]) {
                    continue;
                }
                
                double similarity = SimilarityCalculator.calculateFilePathSimilarity(
                    filePaths.get(i),
                    filePaths.get(j)
                );
                
                if (similarity >= threshold) {
                    cluster.addFilePath(filePaths.get(j));
                    assigned[j] = true;
                }
            }
            
            cluster.calculateAverageSimilarity();
            cluster.calculateCommonPrefix();
            
            if (!cluster.isEmpty()) {
                clusters.add(cluster);
            }
        }
        
        return clusters;
    }
    
    public static List<FileCluster> clusterFilesWithHierarchical(List<String> filePaths, double threshold) {
        return clusterFilesWithHierarchical(filePaths, threshold, SimilarityCalculator.SimilarityType.JARO_WINKLER);
    }
    
    public static List<FileCluster> clusterFilesWithHierarchical(List<String> filePaths, double threshold,
                                                         SimilarityCalculator.SimilarityType similarityType) {
        if (filePaths == null || filePaths.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (filePaths.size() == 1) {
            FileCluster cluster = new FileCluster();
            cluster.addFilePath(filePaths.get(0));
            cluster.calculateAverageSimilarity();
            cluster.calculateCommonPrefix();
            return Collections.singletonList(cluster);
        }
        
        List<FileCluster> clusters = new ArrayList<>();
        
        for (String filePath : filePaths) {
            FileCluster fileCluster = new FileCluster();
            fileCluster.addFilePath(filePath);
            clusters.add(fileCluster);
        }
        
        boolean merged = true;
        while (merged) {
            merged = false;
            
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    if (shouldMerge(clusters.get(i), clusters.get(j), threshold)) {
                        FileCluster mergedCluster = mergeClusters(clusters.get(i), clusters.get(j));
                        clusters.set(i, mergedCluster);
                        clusters.remove(j);
                        merged = true;
                        break;
                    }
                }
                
                if (merged) {
                    break;
                }
            }
        }
        
        return clusters;
    }
    
    private static boolean shouldMerge(FileCluster cluster1, FileCluster cluster2, double threshold) {
        if (cluster1.isEmpty() || cluster2.isEmpty()) {
            return false;
        }
        
        int similarCount = 0;
        int totalCount = cluster1.size() * cluster2.size();
        
        for (String path1 : cluster1.getFilePaths()) {
            for (String path2 : cluster2.getFilePaths()) {
                double similarity = SimilarityCalculator.calculateFilePathSimilarity(path1, path2);
                if (similarity >= threshold) {
                    similarCount++;
                }
            }
        }
        
        double similarityRatio = totalCount > 0 ? (double) similarCount / totalCount : 0.0;
        return similarityRatio >= threshold;
    }
    
    private static FileCluster mergeClusters(FileCluster cluster1, FileCluster cluster2) {
        FileCluster merged = new FileCluster();
        merged.addFilePaths(cluster1.getFilePaths());
        merged.addFilePaths(cluster2.getFilePaths());
        merged.calculateAverageSimilarity();
        merged.calculateCommonPrefix();
        return merged;
    }
    
    public static List<FileCluster> optimizeClusters(List<FileCluster> clusters, int minClusterSize) {
        if (clusters == null || clusters.isEmpty()) {
            return new ArrayList<>();
        }
        
        return clusters.stream()
            .filter(cluster -> cluster.size() >= minClusterSize)
            .sorted((c1, c2) -> Double.compare(c2.getAverageSimilarity(), c1.getAverageSimilarity()))
            .collect(Collectors.toList());
    }
    
    public static Map<String, List<FileCluster>> clusterByDirectory(List<String> filePaths, double threshold) {
        Map<String, List<FileCluster>> result = new HashMap<>();
        
        Map<String, List<String>> filesByDirectory = new HashMap<>();
        for (String filePath : filePaths) {
            String directory = extractDirectory(filePath);
            filesByDirectory.computeIfAbsent(directory, k -> new ArrayList<>()).add(filePath);
        }
        
        for (Map.Entry<String, List<String>> entry : filesByDirectory.entrySet()) {
            List<FileCluster> clusters = clusterFiles(entry.getValue(), threshold);
            result.put(entry.getKey(), clusters);
        }
        
        return result;
    }
    
    private static String extractDirectory(String filePath) {
        if (filePath == null) {
            return "";
        }
        
        int lastSlashIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlashIndex >= 0) {
            return filePath.substring(0, lastSlashIndex);
        }
        
        return "";
    }
    
    public static List<FileCluster> filterClustersBySimilarity(List<FileCluster> clusters, double minSimilarity) {
        if (clusters == null || clusters.isEmpty()) {
            return new ArrayList<>();
        }
        
        return clusters.stream()
            .filter(cluster -> cluster.getAverageSimilarity() >= minSimilarity)
            .collect(Collectors.toList());
    }
    
    public static List<FileCluster> sortClusters(List<FileCluster> clusters, SortBy sortBy) {
        if (clusters == null || clusters.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<FileCluster> sorted = new ArrayList<>(clusters);
        
        switch (sortBy) {
            case SIZE:
                sorted.sort((c1, c2) -> Integer.compare(c2.size(), c1.size()));
                break;
            case SIMILARITY:
                sorted.sort((c1, c2) -> Double.compare(c2.getAverageSimilarity(), c1.getAverageSimilarity()));
                break;
            case NAME:
                sorted.sort((c1, c2) -> c1.getClusterName().compareToIgnoreCase(c2.getClusterName()));
                break;
            default:
                break;
        }
        
        return sorted;
    }
    
    public enum SortBy {
        SIZE,
        SIMILARITY,
        NAME
    }
}
