package com.filemanager.plugin.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileClusteringAlgorithm {

    private double similarityThreshold;
    private String namingStrategy;

    public FileClusteringAlgorithm(double similarityThreshold, String namingStrategy) {
        this.similarityThreshold = similarityThreshold;
        this.namingStrategy = namingStrategy;
    }

    public List<FileCluster> clusterFiles(List<File> files) {
        List<FileCluster> clusters = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            return clusters;
        }

        List<File> unclusteredFiles = new ArrayList<>(files);

        while (!unclusteredFiles.isEmpty()) {
            File seedFile = unclusteredFiles.get(0);
            FileCluster cluster = new FileCluster();
            cluster.addFile(seedFile);

            List<File> similarFiles = findSimilarFiles(seedFile, unclusteredFiles);
            for (File file : similarFiles) {
                if (!file.equals(seedFile)) {
                    cluster.addFile(file);
                }
            }

            unclusteredFiles.removeAll(cluster.getFiles());

            if (cluster.getFiles().size() >= 2) {
                String clusterName = generateClusterName(cluster);
                cluster.setClusterName(clusterName);
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    private List<File> findSimilarFiles(File seedFile, List<File> files) {
        List<File> similarFiles = new ArrayList<>();
        String seedName = seedFile.getName();

        for (File file : files) {
            String fileName = file.getName();
            double similarity = calculateSimilarity(seedName, fileName);

            if (similarity >= similarityThreshold) {
                similarFiles.add(file);
            }
        }

        return similarFiles;
    }

    private double calculateSimilarity(String name1, String name2) {
        switch (namingStrategy) {
            case "简洁风格":
                return TextSimilarityCalculator.calculateSimilaritySimple(name1, name2);
            case "精确风格":
                return TextSimilarityCalculator.calculateSimilarityPrecise(name1, name2);
            case "选取模板":
                return TextSimilarityCalculator.calculateSimilarityNormalized(name1, name2);
            default:
                return TextSimilarityCalculator.calculateSimilarity(name1, name2);
        }
    }

    private String generateClusterName(FileCluster cluster) {
        List<String> filenames = new ArrayList<>();
        for (File file : cluster.getFiles()) {
            filenames.add(file.getName());
        }

        String commonPrefix;
        switch (namingStrategy) {
            case "简洁风格":
                commonPrefix = FilenameNormalizer.extractCommonPrefix(
                    normalizeFilenamesSimple(filenames));
                break;
            case "精确风格":
                commonPrefix = FilenameNormalizer.extractCommonPrefix(
                    normalizeFilenamesPrecise(filenames));
                break;
            case "选取模板":
                commonPrefix = selectTemplateFilename(filenames);
                break;
            default:
                commonPrefix = FilenameNormalizer.extractCommonPrefix(
                    normalizeFilenames(filenames));
        }

        return commonPrefix;
    }

    private List<String> normalizeFilenames(List<String> filenames) {
        List<String> normalized = new ArrayList<>();
        for (String filename : filenames) {
            normalized.add(FilenameNormalizer.normalize(filename));
        }
        return normalized;
    }

    private List<String> normalizeFilenamesSimple(List<String> filenames) {
        List<String> normalized = new ArrayList<>();
        for (String filename : filenames) {
            normalized.add(FilenameNormalizer.normalizeSimple(filename));
        }
        return normalized;
    }

    private List<String> normalizeFilenamesPrecise(List<String> filenames) {
        List<String> normalized = new ArrayList<>();
        for (String filename : filenames) {
            normalized.add(FilenameNormalizer.normalizePrecise(filename));
        }
        return normalized;
    }

    private String selectTemplateFilename(List<String> filenames) {
        List<String> normalized = new ArrayList<>();
        for (String filename : filenames) {
            normalized.add(FilenameNormalizer.normalizeTemplate(filename));
        }

        normalized.sort(String::compareTo);

        int middleIndex = normalized.size() / 2;
        return normalized.get(middleIndex);
    }

    public static class FileCluster {
        private String clusterName;
        private List<File> files;

        public FileCluster() {
            this.files = new ArrayList<>();
        }

        public void addFile(File file) {
            files.add(file);
        }

        public List<File> getFiles() {
            return files;
        }

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }

        public int size() {
            return files.size();
        }
    }
}
