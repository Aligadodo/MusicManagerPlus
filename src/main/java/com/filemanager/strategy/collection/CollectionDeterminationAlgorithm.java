package com.filemanager.strategy.collection;

import com.filemanager.model.ChangeRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CollectionDeterminationAlgorithm {

    private final int minFiles;
    private final int minFileNameLength;
    private final List<String> mustContainKeywords;
    private final List<String> mustNotContainKeywords;
    private final double maxCollectionRatio;
    private final double recognitionStrictness;
    private final boolean skipCollections;
    private String collectionSuffix;

    public CollectionDeterminationAlgorithm() {
        this(2, 8, new ArrayList<>(), new ArrayList<>(), 0.8, 0.9, true);
    }

    public CollectionDeterminationAlgorithm(int minFiles,
                                            int minFileNameLength,
                                            List<String> mustContainKeywords,
                                            List<String> mustNotContainKeywords,
                                            double maxCollectionRatio,
                                            double recognitionStrictness,
                                            boolean skipCollections) {
        this.minFiles = minFiles;
        this.minFileNameLength = minFileNameLength;
        this.mustContainKeywords = mustContainKeywords != null ? mustContainKeywords : new ArrayList<>();
        this.mustNotContainKeywords = mustNotContainKeywords != null ? mustNotContainKeywords : new ArrayList<>();
        this.maxCollectionRatio = maxCollectionRatio;
        this.recognitionStrictness = recognitionStrictness;
        this.skipCollections = skipCollections;
        this.collectionSuffix = "【合集】";
    }

    public void setCollectionSuffix(String suffix) {
        this.collectionSuffix = suffix != null ? suffix : "【合集】";
    }

    public boolean isValidCollection(List<File> files) {
        if (files == null || files.isEmpty()) {
            return false;
        }

        if (files.size() < minFiles) {
            return false;
        }

        for (File file : files) {
            if (!isValidFile(file)) {
                return false;
            }
        }

        return true;
    }

    public boolean isValidFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String filename = file.getName();

        if (filename.length() < minFileNameLength) {
            return false;
        }

        if (skipCollections && isInCollectionFolder(file)) {
            return false;
        }

        if (isCollectionFolder(file)) {
            return false;
        }

        if (!containsRequiredKeywords(filename)) {
            return false;
        }

        return !containsForbiddenKeywords(filename);
    }

    public boolean containsRequiredKeywords(String filename) {
        if (mustContainKeywords == null || mustContainKeywords.isEmpty()) {
            return true;
        }

        for (String keyword : mustContainKeywords) {
            if (filename.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public boolean containsForbiddenKeywords(String filename) {
        if (mustNotContainKeywords == null || mustNotContainKeywords.isEmpty()) {
            return false;
        }

        for (String keyword : mustNotContainKeywords) {
            if (filename.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public boolean isInCollectionFolder(File file) {
        if (file == null) {
            return false;
        }

        File parent = file.getParentFile();
        if (parent == null) {
            return false;
        }

        String parentName = parent.getName();
        return parentName.contains(collectionSuffix) ||
                parentName.contains("【合集】") ||
                parentName.contains("[合集]") ||
                parentName.contains("(合集)") ||
                parentName.contains("合集");
    }

    public boolean isCollectionFolder(File file) {
        if (file == null || !file.isDirectory()) {
            return false;
        }

        String folderName = file.getName();
        return folderName.contains(collectionSuffix) ||
                folderName.contains("【合集】") ||
                folderName.contains("[合集]") ||
                folderName.contains("(合集)") ||
                folderName.contains("合集");
    }

    public boolean shouldCreateCollection(List<File> allFiles, List<File> clusterFiles) {
        if (clusterFiles == null || clusterFiles.size() < minFiles) {
            return false;
        }

        return !isMostlySingleCollection(allFiles, clusterFiles);
    }

    public boolean isMostlySingleCollection(List<File> allFiles, List<File> clusterFiles) {
        if (allFiles == null || allFiles.isEmpty() || clusterFiles == null || clusterFiles.isEmpty()) {
            return false;
        }

        double ratio = (double) clusterFiles.size() / allFiles.size();
        return ratio >= maxCollectionRatio;
    }

    public boolean shouldAddToExistingCollection(File file, File collectionDir, String collectionName) {
        if (file == null || collectionDir == null || !collectionDir.exists()) {
            return false;
        }

        String filename = file.getName();
        String dirName = collectionDir.getName();

        if (!dirName.contains(collectionName)) {
            return false;
        }

        return isValidFile(file);
    }

    public List<File> filterValidFiles(List<File> files) {
        if (files == null) {
            return new ArrayList<>();
        }

        return files.stream()
                .filter(this::isValidFile)
                .collect(Collectors.toList());
    }

    public Map<String, List<File>> filterValidClusters(Map<String, List<File>> clusters) {
        if (clusters == null) {
            return new HashMap<>();
        }

        Map<String, List<File>> validClusters = new HashMap<>();

        for (Map.Entry<String, List<File>> entry : clusters.entrySet()) {
            List<File> validFiles = filterValidFiles(entry.getValue());

            if (validFiles.size() >= minFiles) {
                validClusters.put(entry.getKey(), validFiles);
            }
        }

        return validClusters;
    }

    public Map<String, List<ChangeRecord>> filterValidChangeRecordClusters(Map<String, List<ChangeRecord>> clusters) {
        if (clusters == null) {
            return new HashMap<>();
        }

        Map<String, List<ChangeRecord>> validClusters = new HashMap<>();

        for (Map.Entry<String, List<ChangeRecord>> entry : clusters.entrySet()) {
            List<ChangeRecord> validRecords = entry.getValue().stream()
                    .filter(record -> isValidFile(record.getFileHandle()))
                    .collect(Collectors.toList());

            if (validRecords.size() >= minFiles) {
                validClusters.put(entry.getKey(), validRecords);
            }
        }

        return validClusters;
    }

    public double calculateCollectionScore(List<File> files, String collectionName) {
        if (files == null || files.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        int validFileCount = (int) files.stream().filter(this::isValidFile).count();
        score += (validFileCount / (double) files.size()) * 0.3;

        int keywordMatchCount = 0;
        for (File file : files) {
            if (containsRequiredKeywords(file.getName())) {
                keywordMatchCount++;
            }
        }
        score += (keywordMatchCount / (double) files.size()) * 0.2;

        if (files.size() >= minFiles) {
            score += 0.3;
        }

        if (!containsForbiddenKeywords(collectionName)) {
            score += 0.2;
        }

        return score * recognitionStrictness;
    }

    public int getMinFiles() {
        return minFiles;
    }

    public int getMinFileNameLength() {
        return minFileNameLength;
    }

    public List<String> getMustContainKeywords() {
        return new ArrayList<>(mustContainKeywords);
    }

    public List<String> getMustNotContainKeywords() {
        return new ArrayList<>(mustNotContainKeywords);
    }

    public double getMaxCollectionRatio() {
        return maxCollectionRatio;
    }

    public double getRecognitionStrictness() {
        return recognitionStrictness;
    }

    public boolean isSkipCollections() {
        return skipCollections;
    }

    public static class Builder {
        private int minFiles = 2;
        private int minFileNameLength = 8;
        private List<String> mustContainKeywords = new ArrayList<>();
        private List<String> mustNotContainKeywords = new ArrayList<>();
        private double maxCollectionRatio = 0.8;
        private double recognitionStrictness = 0.9;
        private boolean skipCollections = true;

        public Builder minFiles(int minFiles) {
            this.minFiles = minFiles;
            return this;
        }

        public Builder minFileNameLength(int length) {
            this.minFileNameLength = length;
            return this;
        }

        public Builder mustContainKeywords(List<String> keywords) {
            this.mustContainKeywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
            return this;
        }

        public Builder mustNotContainKeywords(List<String> keywords) {
            this.mustNotContainKeywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
            return this;
        }

        public Builder maxCollectionRatio(double ratio) {
            this.maxCollectionRatio = ratio;
            return this;
        }

        public Builder recognitionStrictness(double strictness) {
            this.recognitionStrictness = strictness;
            return this;
        }

        public Builder skipCollections(boolean skip) {
            this.skipCollections = skip;
            return this;
        }

        public CollectionDeterminationAlgorithm build() {
            return new CollectionDeterminationAlgorithm(
                    minFiles, minFileNameLength, mustContainKeywords, mustNotContainKeywords,
                    maxCollectionRatio, recognitionStrictness, skipCollections
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
