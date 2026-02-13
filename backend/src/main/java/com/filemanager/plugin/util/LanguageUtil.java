package com.filemanager.plugin.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class LanguageUtil {
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Set<Character.UnicodeBlock> CHINESE_UNICODE_BLOCKS = new HashSet<Character.UnicodeBlock>() {{
        add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
        add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);
        add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
        add(Character.UnicodeBlock.GENERAL_PUNCTUATION);
        add(Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION);
        add(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    }};
    private static final Set<Character.UnicodeBlock> JAPANESE_UNICODE_BLOCKS = new HashSet<Character.UnicodeBlock>() {{
        add(Character.UnicodeBlock.HIRAGANA);
        add(Character.UnicodeBlock.KATAKANA);
        add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
    }};

    public static boolean isChineseChar(char c) {
        return CHINESE_UNICODE_BLOCKS.contains(Character.UnicodeBlock.of(c));
    }

    public static boolean isEnglishChar(char c) {
        if (c <= 'z' && c >= 'a') {
            return true;
        }
        return c <= 'Z' && c >= 'A';
    }

    public static boolean isNumChar(char c) {
        return c <= '9' && c >= '0';
    }

    public static boolean isJapaneseChar(char c) {
        return JAPANESE_UNICODE_BLOCKS.contains(Character.UnicodeBlock.of(c));
    }

    public static boolean isKoreaChar(char c) {
        return (c > 0x3130 && c < 0x318F)
                || (c >= 0xAC00 && c <= 0xD7A3);
    }

    public static String toClassicName(String filename, boolean hasChinese) {
        return filename.replaceAll("[^a-zA-Z0-9\\s\\-_()\\[\\]]", "");
    }

    public static String toSimpleChinese(String str) {
        return str;
    }
}