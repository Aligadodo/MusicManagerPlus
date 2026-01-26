package com.filemanager.strategy.collection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameNormalizer {

    // 文件类型模式
    private static final Pattern FILE_TYPE_PATTERN = Pattern.compile("\\[(?:WAV|FLAC|MP3|APE|AAC|OGG|M4A|DSD|DSF|DFF|WV|TAK|TTA|ALAC)(?:\\+[^\\]]*)?\\]", Pattern.CASE_INSENSITIVE);
    
    // 标签模式
    private static final Pattern TAG_PATTERN = Pattern.compile("【(?:24bit|24-bit|Hi-Res|HD|DSD|SACD|MQA|GOLD|黄金版|无损|HQ|Master|Remaster|Deluxe|Expanded|Complete|Anthology|Best|精选|全集|合集)】", Pattern.CASE_INSENSITIVE);
    
    // CD和专辑序号模式
    private static final Pattern CD_PATTERN = Pattern.compile("\\bCD\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALBUM_PATTERN = Pattern.compile("\\b(?:专辑|Album|Vol|Volume|Part|Disc|Disk|Side)\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);
    
    // 序号模式
    private static final Pattern PARENTHESIS_NUMBER_PATTERN = Pattern.compile("\\(\\s*\\d+\\s*\\)");
    private static final Pattern BRACKET_NUMBER_PATTERN = Pattern.compile("【\\s*\\d+\\s*】");
    private static final Pattern STANDALONE_NUMBER_PATTERN = Pattern.compile("(?<=\\s|^)\\d+(?=\\s|$)");
    private static final Pattern CIRCLE_NUMBER_PATTERN = Pattern.compile("[①②③④⑤⑥⑦⑧⑨⑩]");
    private static final Pattern ROMAN_NUMBER_PATTERN = Pattern.compile("\\b[IVXLCDM]+\\b");
    
    // 年份模式
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    
    // 宝丽金版本信息模式
    private static final Pattern POLYGRAM_VERSION_PATTERN = Pattern.compile("\\[(?:香港首版|台湾首版|香港K1首版|香港02首版|香港01首版|引进版|日本东芝版|日本三洋首版|韩银|天龙虚字版|银圈|复黑王版|环球复黑王|正版CD原抓|宝丽金香港版|宝丽金台湾版|环球复黑版)\\]", Pattern.CASE_INSENSITIVE);
    
    // 宝丽金CD序号模式（更精确）
    private static final Pattern POLYGRAM_CD_PATTERN = Pattern.compile("(?<!\\d)CD\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);
    
    // 宝丽金3CD模式
    private static final Pattern POLYGRAM_3CD_PATTERN = Pattern.compile("\\[WAV\\]\\s*3CD\\d+");
    
    // 宝丽金后缀数字模式（如-1、-2、-3）
    private static final Pattern POLYGRAM_SUFFIX_NUMBER_PATTERN = Pattern.compile("-\\d+$");
    
    // 龙音港版唱片版本信息模式
    private static final Pattern LONGYIN_VERSION_PATTERN = Pattern.compile("\\[(?:海文版|龙音海文版|龙音香港版|龙音)(?:\\s*CD-\\d+)?(?:\\s*RA-\\d+)?\\]", Pattern.CASE_INSENSITIVE);
    
    // 龙音港版唱片CD序号模式
    private static final Pattern LONGYIN_CD_PATTERN = Pattern.compile("\\[.*?CD-\\d+\\]", Pattern.CASE_INSENSITIVE);
    
    // 龙音港版唱片RA序号模式
    private static final Pattern LONGYIN_RA_PATTERN = Pattern.compile("\\[.*?RA-\\d+\\]", Pattern.CASE_INSENSITIVE);
    
    // 古典音乐特有的版本信息模式
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\[(?:\\w+版|\\w+版)\\]");
    
    // 特殊字符模式
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[【】\\[\\]（）\\(\\)《》<>{}|\\\\/]");
    
    // 古典音乐系列名称模式（用于提取合集名称）
    private static final Pattern CLASSICAL_SERIES_PATTERN = Pattern.compile("(《[^》]+》)");
    
    // 艺术家名称模式
    private static final Pattern ARTIST_PATTERN = Pattern.compile("^【古典音乐】([^《]+)《");

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

        // 移除文件类型信息
        result = removeFileType(result);
        
        // 移除龙音港版唱片版本信息
        result = removeLongyinVersionInfo(result);
        
        // 移除宝丽金特定的版本信息
        result = removePolygramVersionInfo(result);
        
        // 移除版本信息
        result = removeVersionInfo(result);
        
        // 移除标签
        if (!preserveTags) {
            result = removeTags(result);
        }

        // 移除序列信息
        if (!preserveSequences) {
            // 移除宝丽金特定的CD序号
            result = removePolygramCDSequences(result);
            // 移除宝丽金3CD模式
            result = removePolygram3CDPattern(result);
            // 移除宝丽金后缀数字
            result = removePolygramSuffixNumbers(result);
            
            result = removeCDSequences(result);
            result = removeAlbumSequences(result);
            result = removeParenthesisNumbers(result);
            result = removeBracketNumbers(result);
            result = removeStandaloneNumbers(result);
            result = removeCircleNumbers(result);
            result = removeRomanNumbers(result);
        }
        
        // 移除年份信息（对古典音乐特别重要）
        result = removeYearInfo(result);

        // 移除特殊字符
        result = removeSpecialCharacters(result);
        
        // 清理空白
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

    /**
     * 移除年份信息
     */
    private String removeYearInfo(String text) {
        Matcher matcher = YEAR_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    /**
     * 移除版本信息
     */
    private String removeVersionInfo(String text) {
        Matcher matcher = VERSION_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    /**
     * 移除龙音港版唱片版本信息
     */
    private String removeLongyinVersionInfo(String text) {
        Matcher matcher = LONGYIN_VERSION_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    /**
     * 移除宝丽金特定的版本信息
     */
    private String removePolygramVersionInfo(String text) {
        Matcher matcher = POLYGRAM_VERSION_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    /**
     * 移除宝丽金特定的CD序号
     */
    private String removePolygramCDSequences(String text) {
        Matcher matcher = POLYGRAM_CD_PATTERN.matcher(text);
        return matcher.replaceAll("");
    }

    /**
     * 移除宝丽金3CD模式
     */
    private String removePolygram3CDPattern(String text) {
        Matcher matcher = POLYGRAM_3CD_PATTERN.matcher(text);
        return matcher.replaceAll("[WAV]");
    }

    /**
     * 移除宝丽金后缀数字
     */
    private String removePolygramSuffixNumbers(String text) {
        Matcher matcher = POLYGRAM_SUFFIX_NUMBER_PATTERN.matcher(text);
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
