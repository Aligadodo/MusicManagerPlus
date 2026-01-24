package com.filemanager.strategy.collection;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class FileClusteringAlgorithm {

    private final FilenameNormalizer normalizer;
    private final TextSimilarityCalculator similarityCalculator;
    private final double similarityThreshold;
    private final int minClusterSize;

    public FileClusteringAlgorithm() {
        this(new FilenameNormalizer(), new TextSimilarityCalculator(), 0.7, 2);
    }

    public FileClusteringAlgorithm(FilenameNormalizer normalizer,
                                   TextSimilarityCalculator similarityCalculator,
                                   double similarityThreshold,
                                   int minClusterSize) {
        this.normalizer = normalizer;
        this.similarityCalculator = similarityCalculator;
        this.similarityThreshold = similarityThreshold;
        this.minClusterSize = minClusterSize;
    }

    public Map<String, List<File>> clusterFiles(List<File> files) {
        Map<String, List<File>> clusters = new HashMap<>();

        if (files == null || files.isEmpty()) {
            return clusters;
        }

        List<File> unassigned = new ArrayList<>(files);

        while (!unassigned.isEmpty()) {
            File currentFile = unassigned.get(0);
            String currentName = normalizer.normalize(currentFile.getName());

            List<File> currentCluster = new ArrayList<>();
            currentCluster.add(currentFile);

            Iterator<File> iterator = unassigned.iterator();
            iterator.next();

            while (iterator.hasNext()) {
                File candidateFile = iterator.next();
                String candidateName = normalizer.normalize(candidateFile.getName());

                if (similarityCalculator.isSimilar(currentName, candidateName)) {
                    currentCluster.add(candidateFile);
                    iterator.remove();
                }
            }

            unassigned.remove(0);

            if (currentCluster.size() >= minClusterSize) {
                String clusterName = generateClusterName(currentCluster);
                clusters.put(clusterName, currentCluster);
            }
        }

        return clusters;
    }

    public Map<String, List<String>> clusterFilenames(List<String> filenames) {
        Map<String, List<String>> clusters = new HashMap<>();

        if (filenames == null || filenames.isEmpty()) {
            return clusters;
        }

        List<String> unassigned = new ArrayList<>(filenames);

        while (!unassigned.isEmpty()) {
            String currentName = unassigned.get(0);
            String normalizedCurrent = normalizer.normalize(currentName);

            List<String> currentCluster = new ArrayList<>();
            currentCluster.add(currentName);

            Iterator<String> iterator = unassigned.iterator();
            iterator.next();

            while (iterator.hasNext()) {
                String candidateName = iterator.next();
                String normalizedCandidate = normalizer.normalize(candidateName);

                if (similarityCalculator.isSimilar(normalizedCurrent, normalizedCandidate)) {
                    currentCluster.add(candidateName);
                    iterator.remove();
                }
            }

            unassigned.remove(0);

            if (currentCluster.size() >= minClusterSize) {
                String clusterName = generateClusterNameFromFilenames(currentCluster);
                clusters.put(clusterName, currentCluster);
            }
        }

        return clusters;
    }

    private String generateClusterName(List<File> files) {
        List<String> filenames = files.stream()
                .map(File::getName)
                .collect(Collectors.toList());

        return generateClusterNameFromFilenames(filenames);
    }

    private String generateClusterNameFromFilenames(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return "未命名";
        }

        // 提取最长公共前缀
        String longestCommonPrefix = findLongestCommonPrefix(filenames);

        if (longestCommonPrefix.length() >= 5) {
            // 尝试修复不完整的括号
            String fixedPrefix = fixIncompleteBrackets(longestCommonPrefix);
            return fixedPrefix.trim();
        }

        return extractMostFrequentWords(filenames);
    }

    /**
     * 从文件名列表中提取艺术家信息
     */
    private String extractArtistFromFilenames(List<String> filenames) {
        for (String filename : filenames) {
            String artist = extractArtist(filename);
            if (!artist.isEmpty()) {
                return artist;
            }
        }
        return "";
    }

    /**
     * 从文件名中提取艺术家信息
     */
    private String extractArtist(String filename) {
        // 简单实现：尝试从文件名中提取艺术家信息
        // 假设文件名格式为 "艺术家 - 专辑" 或 "艺术家《专辑》"

        // 尝试匹配 "艺术家 - 专辑" 格式
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("^(.+?)\\s*-\\s*");
        java.util.regex.Matcher matcher1 = pattern1.matcher(filename);
        if (matcher1.find()) {
            return matcher1.group(1).trim().replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】]", "");
        }

        // 尝试匹配 "[艺术家]" 格式
        java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("^\\[(.+?)\\]");
        java.util.regex.Matcher matcher2 = pattern2.matcher(filename);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }

        return "";
    }

    /**
     * 从文件名列表中提取文件类型信息
     */
    private String extractFileTypesFromFilenames(List<String> filenames) {
        Set<String> fileTypes = new HashSet<>();

        for (String filename : filenames) {
            String fileType = extractFileType(filename);
            if (!fileType.isEmpty()) {
                fileTypes.add(fileType);
            }
        }

        return String.join("\\", fileTypes);
    }

    /**
     * 从文件名中提取文件类型信息
     */
    private String extractFileType(String filename) {
        // 简单实现：尝试从文件名中提取文件类型信息
        // 假设文件类型在括号中，如 "(FLAC)" 或 "(MP3)"

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\((.+?)\\)$");
        java.util.regex.Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            String type = matcher.group(1).trim();
            // 去除常见的格式信息，只保留核心文件类型
            type = type.replaceAll("CUE$", "");
            type = type.replaceAll("+", "\\");
            return type;
        }

        return "";
    }

    /**
     * 修复不完整的括号
     */
    private String fixIncompleteBrackets(String input) {
        // 检查并修复常见的括号组合
        if (input.endsWith("[")) {
            return input.substring(0, input.length() - 1);
        } else if (input.endsWith("(")) {
            return input.substring(0, input.length() - 1);
        } else if (input.endsWith("{") || input.endsWith("<")) {
            return input.substring(0, input.length() - 1);
        }

        // 检查中文括号
        if (input.endsWith("【") || input.endsWith("《")) {
            return input.substring(0, input.length() - 1);
        }

        return input;
    }

    private String findLongestCommonPrefix(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }

        String prefix = strings.get(0);

        for (int i = 1; i < strings.size(); i++) {
            while (strings.get(i).indexOf(prefix) != 0) {
                prefix = prefix.substring(0, prefix.length() - 1);
                if (prefix.isEmpty()) {
                    return "";
                }
            }
        }

        return prefix;
    }

    private String extractMostFrequentWords(List<String> filenames) {
        Map<String, Integer> wordFrequency = new HashMap<>();

        for (String filename : filenames) {
            String normalized = normalizer.normalize(filename);
            String[] words = normalized.split("\\s+");

            for (String word : words) {
                if (word.length() > 1) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedWords = wordFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        StringBuilder result = new StringBuilder();
        int minFrequency = Math.max(2, filenames.size() / 2);

        for (Map.Entry<String, Integer> entry : sortedWords) {
            if (entry.getValue() >= minFrequency) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(entry.getKey());
            }
        }

        return result.length() > 0 ? result.toString() : filenames.get(0);
    }

    public List<String> validateClusters(Map<String, List<File>> clusters) {
        List<String> validClusters = new ArrayList<>();

        for (Map.Entry<String, List<File>> entry : clusters.entrySet()) {
            List<String> filenames = entry.getValue().stream()
                    .map(File::getName)
                    .collect(Collectors.toList());

            double clusterSimilarity = similarityCalculator.calculateClusterSimilarity(filenames);

            if (clusterSimilarity >= similarityThreshold && entry.getValue().size() >= minClusterSize) {
                validClusters.add(entry.getKey());
            }
        }

        return validClusters;
    }

    public void filterOutliers(Map<String, List<File>> clusters) {
        for (Map.Entry<String, List<File>> entry : clusters.entrySet()) {
            List<String> filenames = entry.getValue().stream()
                    .map(File::getName)
                    .collect(Collectors.toList());

            String clusterName = entry.getKey();
            List<String> filteredFilenames = similarityCalculator.filterOutliers(filenames, clusterName);

            if (filteredFilenames.size() < minClusterSize) {
                clusters.remove(entry.getKey());
            } else {
                List<File> filteredFiles = entry.getValue().stream()
                        .filter(file -> filteredFilenames.contains(file.getName()))
                        .collect(Collectors.toList());

                entry.setValue(filteredFiles);
            }
        }
    }

    public int getMinClusterSize() {
        return minClusterSize;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public static class Builder {
        private FilenameNormalizer normalizer = new FilenameNormalizer();
        private TextSimilarityCalculator similarityCalculator = new TextSimilarityCalculator();
        private double similarityThreshold = 0.7;
        private int minClusterSize = 2;

        public Builder normalizer(FilenameNormalizer normalizer) {
            this.normalizer = normalizer;
            return this;
        }

        public Builder similarityCalculator(TextSimilarityCalculator calculator) {
            this.similarityCalculator = calculator;
            return this;
        }

        public Builder similarityThreshold(double threshold) {
            this.similarityThreshold = threshold;
            return this;
        }

        public Builder minClusterSize(int size) {
            this.minClusterSize = size;
            return this;
        }

        public FileClusteringAlgorithm build() {
            return new FileClusteringAlgorithm(normalizer, similarityCalculator, similarityThreshold, minClusterSize);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
