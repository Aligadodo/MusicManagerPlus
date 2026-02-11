package com.filemanager.plugin.impl.filecollection.collection;

import java.util.*;
import java.util.stream.Collectors;

public class KeywordFilter {

    private List<String> mustIncludeKeywords;
    private List<String> mustNotIncludeKeywords;
    private boolean caseSensitive;
    private boolean exactMatch;

    private KeywordFilter(Builder builder) {
        this.mustIncludeKeywords = builder.mustIncludeKeywords;
        this.mustNotIncludeKeywords = builder.mustNotIncludeKeywords;
        this.caseSensitive = builder.caseSensitive;
        this.exactMatch = builder.exactMatch;
    }

    /**
     * 检查文本是否匹配过滤条件
     */
    public boolean matches(String text) {
        if (text == null) {
            return false;
        }

        String checkText = caseSensitive ? text : text.toLowerCase();

        // 检查必须包含的关键词
        for (String keyword : mustIncludeKeywords) {
            String checkKeyword = caseSensitive ? keyword : keyword.toLowerCase();
            if (!containsKeyword(checkText, checkKeyword)) {
                return false;
            }
        }

        // 检查不能包含的关键词
        for (String keyword : mustNotIncludeKeywords) {
            String checkKeyword = caseSensitive ? keyword : keyword.toLowerCase();
            if (containsKeyword(checkText, checkKeyword)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查文本是否包含关键词
     */
    private boolean containsKeyword(String text, String keyword) {
        if (exactMatch) {
            return text.equals(keyword);
        } else {
            return text.contains(keyword);
        }
    }

    /**
     * 获取必须包含的关键词
     */
    public List<String> getMustIncludeKeywords() {
        return new ArrayList<>(mustIncludeKeywords);
    }

    /**
     * 获取不能包含的关键词
     */
    public List<String> getMustNotIncludeKeywords() {
        return new ArrayList<>(mustNotIncludeKeywords);
    }

    /**
     * 检查是否大小写敏感
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * 检查是否精确匹配
     */
    public boolean isExactMatch() {
        return exactMatch;
    }

    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类
     */
    public static class Builder {
        private List<String> mustIncludeKeywords = new ArrayList<>();
        private List<String> mustNotIncludeKeywords = new ArrayList<>();
        private boolean caseSensitive = false;
        private boolean exactMatch = false;

        /**
         * 设置必须包含的关键词
         */
        public Builder mustIncludeKeywords(List<String> keywords) {
            this.mustIncludeKeywords = keywords != null ? keywords : new ArrayList<>();
            return this;
        }

        /**
         * 添加必须包含的关键词
         */
        public Builder addMustIncludeKeyword(String keyword) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.mustIncludeKeywords.add(keyword.trim());
            }
            return this;
        }

        /**
         * 添加多个必须包含的关键词
         */
        public Builder addMustIncludeKeywords(List<String> keywords) {
            if (keywords != null) {
                this.mustIncludeKeywords.addAll(
                        keywords.stream()
                                .filter(k -> k != null && !k.trim().isEmpty())
                                .map(String::trim)
                                .collect(Collectors.toList())
                );
            }
            return this;
        }

        /**
         * 设置不能包含的关键词
         */
        public Builder mustNotIncludeKeywords(List<String> keywords) {
            this.mustNotIncludeKeywords = keywords != null ? keywords : new ArrayList<>();
            return this;
        }

        /**
         * 添加不能包含的关键词
         */
        public Builder addMustNotIncludeKeyword(String keyword) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.mustNotIncludeKeywords.add(keyword.trim());
            }
            return this;
        }

        /**
         * 添加多个不能包含的关键词
         */
        public Builder addMustNotIncludeKeywords(List<String> keywords) {
            if (keywords != null) {
                this.mustNotIncludeKeywords.addAll(
                        keywords.stream()
                                .filter(k -> k != null && !k.trim().isEmpty())
                                .map(String::trim)
                                .collect(Collectors.toList())
                );
            }
            return this;
        }

        /**
         * 设置是否大小写敏感
         */
        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        /**
         * 设置是否精确匹配
         */
        public Builder exactMatch(boolean exactMatch) {
            this.exactMatch = exactMatch;
            return this;
        }

        /**
         * 构建KeywordFilter实例
         */
        public KeywordFilter build() {
            return new KeywordFilter(this);
        }
    }

    /**
     * 从逗号分隔的字符串创建KeywordFilter
     */
    public static KeywordFilter fromCommaSeparatedString(String mustInclude, String mustNotInclude) {
        List<String> includeKeywords = parseCommaSeparatedString(mustInclude);
        List<String> excludeKeywords = parseCommaSeparatedString(mustNotInclude);

        return KeywordFilter.builder()
                .mustIncludeKeywords(includeKeywords)
                .mustNotIncludeKeywords(excludeKeywords)
                .build();
    }

    /**
     * 解析逗号分隔的字符串为关键词列表
     */
    private static List<String> parseCommaSeparatedString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(input.split("[,;]", -1))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 创建默认的KeywordFilter实例
     */
    public static KeywordFilter createDefault() {
        return KeywordFilter.builder().build();
    }

    /**
     * 创建严格的KeywordFilter实例
     */
    public static KeywordFilter createStrict() {
        return KeywordFilter.builder()
                .caseSensitive(true)
                .exactMatch(true)
                .build();
    }

    @Override
    public String toString() {
        return "KeywordFilter{" +
                "mustIncludeKeywords=" + mustIncludeKeywords +
                ", mustNotIncludeKeywords=" + mustNotIncludeKeywords +
                ", caseSensitive=" + caseSensitive +
                ", exactMatch=" + exactMatch +
                '}';
    }
}