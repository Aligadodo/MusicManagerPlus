package com.filemanager.plugin.impl.filecollection.collection;

import java.util.ArrayList;
import java.util.List;

public class FilenameNormalizer {

    private static final String[] COMMON_PREFIXES = {"CD", "Disc", "Volume", "Vol", "Track", "Trk"};
    private static final String[] COMMON_SUFFIXES = {"mp3", "flac", "wav", "aac", "m4a", "ogg", "wma", "ape", "mp4", "320kbps", "192kbps", "128kbps", "lossless", "hd", "hq"};
    private static final String[] SPECIAL_CHARS = {"[", "]", "(", ")", "{", "}", "<", ">", "-", "_", ".", ",", ";", ":", "'", "\"", "!", "@", "#", "$", "%", "^", "&", "*", "+", "=", "|", "\\", "/", "~", "`"};
    
    public static String normalize(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        String normalized = filename;

        normalized = removeExtension(normalized);
        normalized = removeSpecialChars(normalized);
        normalized = normalizeSpaces(normalized);
        normalized = removeCommonPrefixes(normalized);
        normalized = removeCommonSuffixes(normalized);
        normalized = removeTrackNumbers(normalized);
        normalized = removeYear(normalized);

        return normalized.trim();
    }

    public static String normalizeSimple(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        String normalized = filename;

        normalized = removeExtension(normalized);
        normalized = removeSpecialChars(normalized);
        normalized = normalizeSpaces(normalized);
        normalized = removeTrackNumbers(normalized);
        normalized = removeYear(normalized);
        normalized = removeAlbumType(normalized);

        return normalized.trim();
    }

    public static String normalizePrecise(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        String normalized = filename;

        normalized = removeExtension(normalized);
        normalized = removeSpecialChars(normalized);
        normalized = normalizeSpaces(normalized);
        normalized = removeTrackNumbers(normalized);

        return normalized.trim();
    }

    public static String normalizeTemplate(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        String normalized = filename;

        normalized = removeExtension(normalized);
        normalized = normalizeSpaces(normalized);

        return normalized.trim();
    }

    private static String removeExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }

    private static String removeSpecialChars(String filename) {
        String result = filename;
        for (String specialChar : SPECIAL_CHARS) {
            result = result.replace(specialChar, " ");
        }
        return result;
    }

    private static String normalizeSpaces(String filename) {
        return filename.replaceAll("\\s+", " ").trim();
    }

    private static String removeCommonPrefixes(String filename) {
        String result = filename;
        for (String prefix : COMMON_PREFIXES) {
            if (result.toLowerCase().startsWith(prefix.toLowerCase())) {
                result = result.substring(prefix.length()).trim();
                break;
            }
        }
        return result;
    }

    private static String removeCommonSuffixes(String filename) {
        String result = filename;
        for (String suffix : COMMON_SUFFIXES) {
            if (result.toLowerCase().endsWith(suffix.toLowerCase())) {
                result = result.substring(0, result.length() - suffix.length()).trim();
            }
        }
        return result;
    }

    private static String removeTrackNumbers(String filename) {
        return filename.replaceAll("^\\d+\\s*[.-]?\\s*", "").trim();
    }

    private static String removeYear(String filename) {
        return filename.replaceAll("\\(\\d{4}\\)", "").replaceAll("\\d{4}", "").trim();
    }

    private static String removeAlbumType(String filename) {
        return filename.replaceAll("(?i)(Album|EP|Single|Compilation|Live|Remix|Deluxe|Edition)", "").trim();
    }

    public static String extractCommonPrefix(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return "";
        }

        if (filenames.size() == 1) {
            return filenames.get(0);
        }

        String first = filenames.get(0);
        String commonPrefix = first;

        for (int i = 1; i < filenames.size(); i++) {
            commonPrefix = findCommonPrefix(commonPrefix, filenames.get(i));
            if (commonPrefix.isEmpty()) {
                break;
            }
        }

        return commonPrefix.trim();
    }

    private static String findCommonPrefix(String str1, String str2) {
        int minLength = Math.min(str1.length(), str2.length());
        int i = 0;

        while (i < minLength && str1.charAt(i) == str2.charAt(i)) {
            i++;
        }

        return str1.substring(0, i);
    }

    public static List<String> splitIntoWords(String filename) {
        List<String> words = new ArrayList<>();
        String[] parts = filename.split("\\s+");
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                words.add(part);
            }
        }
        
        return words;
    }
}