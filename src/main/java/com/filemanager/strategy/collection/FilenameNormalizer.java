package com.filemanager.strategy.collection;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FilenameNormalizer {
    
    private static final Pattern FILE_TYPE_PATTERN = Pattern.compile("\\[(?:WAV|FLAC|MP3|APE|AAC|OGG|M4A|DSD|DSF|DFF|WV|TAK|TTA|ALAC)(?:\\+[^\\]]*)?\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_PATTERN = Pattern.compile("【(?:24bit|24-bit|Hi-Res|HD|DSD|SACD|MQA|GOLD|黄金版|无损|HQ|Master|Remaster|Deluxe|Expanded|Complete|Anthology|Best|精选|全集|合集)】", Pattern.CASE_INSENSITIVE);
    private static final Pattern CD_PATTERN = Pattern.compile("\\bCD\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALBUM_PATTERN = Pattern.compile("\\b(?:专辑|Album|Vol|Volume|Part|Disc|Disk|Side)\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARENTHESIS_NUMBER_PATTERN = Pattern.compile("\\(\\s*\\d+\\s*\\)");
    private static final Pattern BRACKET_NUMBER_PATTERN = Pattern.compile("【\\s*\\d+\\s*】");
    private static final Pattern STANDALONE_NUMBER_PATTERN = Pattern.compile("(?<=\\s|^)\\d+(?=\\s|$)");
    private static final Pattern CIRCLE_NUMBER_PATTERN = Pattern.compile("[①②③④⑤⑥⑦⑧⑨⑩]");
    private static final Pattern ROMAN_NUMBER_PATTERN = Pattern.compile("\\b[IVXLCDM]+\\b");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[【】\\[\\]（）\\(\\)《》<>{}|\\\\/]");
    
    private final boolean preserveTags;
    private final boolean preserveSequences;
    
    public FilenameNormalizer() {
        this(false, false);
    }
    
    public FilenameNormalizer(boolean preserveTags, boolean preserveSequences) {
        this.preserveTags = preserveTags;
        this.preserveSequences = preserveSequences;
    }
    
    public String normalize(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        
        String result = filename;
        
        if (!preserveTags) {
            result = removeFileType(result);
            result = removeTags(result);
        }
        
        if (!preserveSequences) {
            result = removeCDSequences(result);
            result = removeAlbumSequences(result);
            result = removeParenthesisNumbers(result);
            result = removeBracketNumbers(result);
            result = removeStandaloneNumbers(result);
            result = removeCircleNumbers(result);
            result = removeRomanNumbers(result);
        }
        
        result = removeSpecialCharacters(result);
        result = cleanupWhitespace(result);
        
        return result.trim();
    }
    
    private String removeFileType(String text) {
        Matcher matcher = FILE_TYPE_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeTags(String text) {
        Matcher matcher = TAG_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeCDSequences(String text) {
        Matcher matcher = CD_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeAlbumSequences(String text) {
        Matcher matcher = ALBUM_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeParenthesisNumbers(String text) {
        Matcher matcher = PARENTHESIS_NUMBER_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeBracketNumbers(String text) {
        Matcher matcher = BRACKET_NUMBER_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeStandaloneNumbers(String text) {
        Matcher matcher = STANDALONE_NUMBER_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeCircleNumbers(String text) {
        Matcher matcher = CIRCLE_NUMBER_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeRomanNumbers(String text) {
        Matcher matcher = ROMAN_NUMBER_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }
    
    private String removeSpecialCharacters(String text) {
        Matcher matcher = SPECIAL_CHARS_PATTERN.matcher(text);
        return matcher.replaceAll(" ");
    }
    
    private String cleanupWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
    
    public static class Builder {
        private boolean preserveTags = false;
        private boolean preserveSequences = false;
        
        public Builder preserveTags(boolean preserve) {
            this.preserveTags = preserve;
            return this;
        }
        
        public Builder preserveSequences(boolean preserve) {
            this.preserveSequences = preserve;
            return this;
        }
        
        public FilenameNormalizer build() {
            return new FilenameNormalizer(preserveTags, preserveSequences);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
