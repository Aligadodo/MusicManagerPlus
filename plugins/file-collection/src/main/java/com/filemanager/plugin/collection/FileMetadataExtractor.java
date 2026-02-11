package com.filemanager.plugin.collection;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileMetadataExtractor {

    /**
     * 从文件名中提取核心关键词
     */
    public static List<String> extractCoreKeywords(String fileName) {
        List<String> keywords = new ArrayList<>();
        
        // 去除常见前缀/后缀和特殊字符
        String processed = fileName.replaceAll("(?i)^DTS-", "");
        processed = processed.replaceAll("[\\[\\]\\(\\)\\《\\》\\{\\}\\<>]", " ");
        
        // 提取艺术家名称
        String artist = extractArtist(processed);
        if (!artist.isEmpty()) {
            keywords.add(artist);
        }
        
        // 提取专辑名称
        String album = extractAlbum(processed);
        if (!album.isEmpty()) {
            // 从专辑名称中提取核心关键词
            String[] albumParts = album.split("[\\s\\-\\_\\.]+");
            for (String part : albumParts) {
                if (part.length() > 1) { // 只保留长度大于1的关键词
                    keywords.add(part);
                }
            }
        }
        
        // 提取其他可能的关键词
        String[] parts = processed.split("[\\s\\-\\_\\.]+");
        for (String part : parts) {
            if (part.length() > 1 && !keywords.contains(part)) {
                // 检查是否是有意义的关键词（不是常见的无意义词）
                if (!isCommonWord(part)) {
                    keywords.add(part);
                }
            }
        }
        
        return keywords;
    }
    
    /**
     * 提取艺术家名称
     */
    public static String extractArtist(String fileName) {
        // 尝试从文件名中提取艺术家名称
        String[] commonPatterns = {
                "^(.+?) - ", // 格式: 艺术家 - 专辑
                "^(.+?)《",   // 格式: 艺术家《专辑》
                "^\\[(.+?)\\]", // 格式: [艺术家]专辑
                "^【(.+?)】", // 格式: 【艺术家】专辑
                "^(.+?)\\s*-", // 格式: 艺术家 - 专辑
                "^(.+?)\\s*《"    // 格式: 艺术家 《专辑》
        };
        
        for (String pattern : commonPatterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(fileName);
            if (m.find()) {
                String artist = m.group(1).trim();
                // 去除可能的括号
                artist = artist.replaceAll("^\\[|\\]$", "");
                artist = artist.replaceAll("^【|】$", "");
                if (!artist.isEmpty()) {
                    return artist;
                }
            }
        }
        
        // 如果没有找到明确的艺术家标识，尝试从常见艺术家列表中匹配
        String[] commonArtists = {
                "张平福", "周杰伦", "林俊杰", "陈奕迅", "张学友", "刘德华",
                "王力宏", "陶喆", "谢霆锋", "张柏芝", "王菲", "那英",
                "木村好夫", "松本英彦"
        };
        
        for (String artist : commonArtists) {
            if (fileName.contains(artist)) {
                return artist;
            }
        }
        
        return "";
    }
    
    /**
     * 提取专辑名称
     */
    public static String extractAlbum(String fileName) {
        // 尝试从文件名中提取专辑名称
        String[] commonPatterns = {
                "《(.+?)》",   // 格式: 艺术家《专辑》
                "\\[(.*?)\\]", // 格式: [专辑]
                "【(.+?)】", // 格式: 【专辑】
                " - (.+?)$", // 格式: 艺术家 - 专辑
                "-(.+?)$"     // 格式: 艺术家-专辑
        };
        
        for (String pattern : commonPatterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(fileName);
            if (m.find()) {
                String album = m.group(1).trim();
                // 去除可能的格式信息
                album = album.replaceAll("\\s*\\(.+?\\)$", "");
                album = album.replaceAll("\\s*\\[.+?\\]$", "");
                if (!album.isEmpty()) {
                    return album;
                }
            }
        }
        
        return "";
    }
    
    /**
     * 检查是否是常见的无意义词
     */
    private static boolean isCommonWord(String word) {
        String[] commonWords = {
                "专辑", "唱片", "音乐", "歌曲", "CD", "VOL", "DISC",
                "Disc", "disc", "cd", "vol", "mp3", "flac", "wav",
                "ape", "ogg", "aac", "m4a", "wma", "opus"
        };
        
        for (String commonWord : commonWords) {
            if (word.equalsIgnoreCase(commonWord)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查两个文件名是否包含相同的标题和不同的数字序号
     */
    public static boolean hasSameTitleDifferentNumber(String s1, String s2) {
        // 1. 检查是否包含相同的艺术家名称
        String artist1 = extractArtist(s1);
        String artist2 = extractArtist(s2);
        
        if (!artist1.isEmpty() && artist1.equals(artist2)) {
            // 2. 提取专辑名称
            String album1 = extractAlbum(s1);
            String album2 = extractAlbum(s2);
            
            if (!album1.isEmpty() && !album2.isEmpty()) {
                // 3. 提取序号
                String num1 = extractNumber(album1);
                String num2 = extractNumber(album2);
                
                // 4. 检查是否都包含序号且序号不同
                if (!num1.isEmpty() && !num2.isEmpty() && !num1.equals(num2)) {
                    // 5. 提取序号的位置
                    int numPos1 = album1.indexOf(num1);
                    int numPos2 = album2.indexOf(num2);
                    
                    if (numPos1 > 0 && numPos2 > 0) {
                        // 6. 提取序号前的核心系列名称
                        String seriesName1 = album1.substring(0, numPos1).trim();
                        String seriesName2 = album2.substring(0, numPos2).trim();
                        
                        // 7. 检查系列名称是否相似
                        double seriesSimilarity = SimilarityCalculator.calculateSimilarityStatic(seriesName1, seriesName2);
                        return seriesSimilarity > 0.8;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 提取文件名中的标题部分（去除序号和格式信息）
     */
    private static String extractTitle(String fileName) {
        // 提取文件名中的标题部分，去除序号和格式信息
        String title = fileName;
        
        // 1. 去除常见的系列标识前缀/后缀
        title = title.replaceAll("(?i)^DTS-", ""); // 去除DTS前缀
        title = title.replaceAll("(?i)(CD|VOL|DISC)\\s*\\d+", ""); // 去除CD、VOL、DISC等标识
        title = title.replaceAll("(?i)(2CD|3CD|4CD)", ""); // 去除多CD标识
        
        // 2. 去除阿拉伯数字序号
        title = title.replaceAll("\\b\\d+\\b", "");
        
        // 3. 去除中文数字序号
        title = title.replaceAll("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]", "");
        
        // 4. 去除圆形序号
        title = title.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "");
        
        // 5. 去除字母序号
        title = title.replaceAll("\\b[A-Za-z]\\b", "");
        
        // 6. 去除括号和噪音字符
        title = title.replaceAll("[\\s\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】\\.\\,\\!\\?\\;\\:\\'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", "");
        
        return title;
    }
    
    /**
     * 提取文件名中的数字部分
     */
    private static String extractNumber(String fileName) {
        // 提取文件名中的数字部分
        Pattern pattern = Pattern.compile("\\b\\d+\\b");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group();
        }
        
        // 尝试提取圆形序号
        Pattern circleNumberPattern = Pattern.compile("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]");
        Matcher circleMatcher = circleNumberPattern.matcher(fileName);
        if (circleMatcher.find()) {
            return circleMatcher.group();
        }
        
        // 尝试提取中文数字
        Pattern chineseNumberPattern = Pattern.compile("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]+");
        Matcher chineseMatcher = chineseNumberPattern.matcher(fileName);
        if (chineseMatcher.find()) {
            return chineseMatcher.group();
        }
        
        return "";
    }
    
    /**
     * 计算两个关键词列表的相似度
     */
    public static double calculateKeywordSimilarity(List<String> keywords1, List<String> keywords2) {
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }
        
        int commonCount = 0;
        for (String keyword1 : keywords1) {
            for (String keyword2 : keywords2) {
                if (keyword1.equals(keyword2) || keyword1.contains(keyword2) || keyword2.contains(keyword1)) {
                    commonCount++;
                    break;
                }
            }
        }
        
        int totalCount = Math.max(keywords1.size(), keywords2.size());
        return (double) commonCount / totalCount;
    }
}
