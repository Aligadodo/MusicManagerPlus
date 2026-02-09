package com.filemanager.plugin.collection;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class KeywordFilter {
    
    private final List<String> mustIncludeKeywords;
    private final List<String> mustNotIncludeKeywords;
    private final boolean caseSensitive;
    private final boolean useRegex;
    
    private KeywordFilter(Builder builder) {
        this.mustIncludeKeywords = builder.mustIncludeKeywords;
        this.mustNotIncludeKeywords = builder.mustNotIncludeKeywords;
        this.caseSensitive = builder.caseSensitive;
        this.useRegex = builder.useRegex;
    }
    
    public boolean matchesFilePath(String filePath) {
        if (filePath == null) {
            return false;
        }
        
        File file = new File(filePath);
        String fileName = file.getName();
        
        return matches(fileName);
    }
    
    public boolean matches(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        String textToCheck = caseSensitive ? fileName : fileName.toLowerCase();
        
        if (!mustIncludeKeywords.isEmpty()) {
            boolean mustIncludeMatch = mustIncludeKeywords.stream()
                .anyMatch(keyword -> matchesKeyword(textToCheck, keyword));
            
            if (!mustIncludeMatch) {
                return false;
            }
        }
        
        if (!mustNotIncludeKeywords.isEmpty()) {
            boolean mustNotIncludeMatch = mustNotIncludeKeywords.stream()
                .anyMatch(keyword -> matchesKeyword(textToCheck, keyword));
            
            if (mustNotIncludeMatch) {
                return false;
            }
        }
        
        return true;
    }
    
    public List<String> filterFilePaths(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return new ArrayList<>();
        }
        
        return filePaths.stream()
            .filter(this::matchesFilePath)
            .collect(Collectors.toList());
    }
    
    public List<String> filterFileNames(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        return fileNames.stream()
            .filter(this::matches)
            .collect(Collectors.toList());
    }
    
    private boolean matchesKeyword(String text, String keyword) {
        if (useRegex) {
            try {
                Pattern pattern = caseSensitive 
                    ? Pattern.compile(keyword) 
                    : Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
                return pattern.matcher(text).find();
            } catch (PatternSyntaxException e) {
                return false;
            }
        } else {
            String keywordToCheck = caseSensitive ? keyword : keyword.toLowerCase();
            return text.contains(keywordToCheck);
        }
    }
    
    public List<String> getMustIncludeKeywords() {
        return new ArrayList<>(mustIncludeKeywords);
    }
    
    public List<String> getMustNotIncludeKeywords() {
        return new ArrayList<>(mustNotIncludeKeywords);
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    
    public boolean isUseRegex() {
        return useRegex;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<String> mustIncludeKeywords = new ArrayList<>();
        private List<String> mustNotIncludeKeywords = new ArrayList<>();
        private boolean caseSensitive = false;
        private boolean useRegex = false;
        
        public Builder addMustIncludeKeyword(String keyword) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.mustIncludeKeywords.add(keyword.trim());
            }
            return this;
        }
        
        public Builder addMustIncludeKeywords(List<String> keywords) {
            if (keywords != null) {
                keywords.stream()
                    .filter(k -> k != null && !k.trim().isEmpty())
                    .forEach(k -> this.mustIncludeKeywords.add(k.trim()));
            }
            return this;
        }
        
        public Builder addMustNotIncludeKeyword(String keyword) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.mustNotIncludeKeywords.add(keyword.trim());
            }
            return this;
        }
        
        public Builder addMustNotIncludeKeywords(List<String> keywords) {
            if (keywords != null) {
                keywords.stream()
                    .filter(k -> k != null && !k.trim().isEmpty())
                    .forEach(k -> this.mustNotIncludeKeywords.add(k.trim()));
            }
            return this;
        }
        
        public Builder setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }
        
        public Builder setUseRegex(boolean useRegex) {
            this.useRegex = useRegex;
            return this;
        }
        
        public KeywordFilter build() {
            return new KeywordFilter(this);
        }
    }
    
    @Override
    public String toString() {
        return "KeywordFilter{" +
                "mustIncludeKeywords=" + mustIncludeKeywords +
                ", mustNotIncludeKeywords=" + mustNotIncludeKeywords +
                ", caseSensitive=" + caseSensitive +
                ", useRegex=" + useRegex +
                '}';
    }
}

class KeywordFilterUtils {
    
    public static KeywordFilter createDefaultFilter() {
        return KeywordFilter.builder()
            .setCaseSensitive(false)
            .setUseRegex(false)
            .build();
    }
    
    public static KeywordFilter createRemixFilter() {
        return KeywordFilter.builder()
            .addMustIncludeKeyword("remix")
            .setCaseSensitive(false)
            .setUseRegex(false)
            .build();
    }
    
    public static KeywordFilter createDemoFilter() {
        return KeywordFilter.builder()
            .addMustNotIncludeKeyword("demo")
            .setCaseSensitive(false)
            .setUseRegex(false)
            .build();
    }
    
    public static KeywordFilter createAudioFileFilter() {
        return KeywordFilter.builder()
            .addMustIncludeKeywords(Arrays.asList("mp3", "flac", "wav", "aac", "m4a"))
            .setCaseSensitive(false)
            .setUseRegex(true)
            .build();
    }
    
    public static KeywordFilter createChineseNameFilter() {
        return KeywordFilter.builder()
            .setCaseSensitive(false)
            .setUseRegex(true)
            .addMustIncludeKeyword("[\\u4e00-\\u9fa5]")
            .build();
    }
    
    public static List<String> parseKeywordString(String keywordString) {
        if (keywordString == null || keywordString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(keywordString.split("[,;]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    public static String formatKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        
        return String.join(", ", keywords);
    }
}
