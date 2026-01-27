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

                // 1. 检查是否包含相同的系列关键词
                boolean hasSameSeries = hasSameSeriesKeywords(currentName, candidateName);
                
                // 2. 检查是否属于同一宝丽金系列
                boolean isSamePolygram = isSamePolygramSeries(currentName, candidateName);
                
                // 3. 计算基本相似度
                double similarity = similarityCalculator.calculateSimilarity(normalizedCurrent, normalizedCandidate);
                
                // 4. 提取核心专辑名称
                String coreAlbum1 = extractCoreAlbumName(currentName);
                String coreAlbum2 = extractCoreAlbumName(candidateName);
                
                // 5. 综合判断是否相似
                boolean isSimilar = false;
                double adjustedThreshold = similarityThreshold;
                
                // 如果属于同一宝丽金系列，使用更低的阈值
                if (isSamePolygram) {
                    adjustedThreshold = Math.max(similarityThreshold, 0.5);
                    isSimilar = similarity >= adjustedThreshold;
                } else if (hasSameSeries) {
                    // 如果有相同的系列关键词，使用稍低的阈值
                    // 检查核心专辑名称是否相同
                    if (!coreAlbum1.isEmpty() && !coreAlbum2.isEmpty() && coreAlbum1.equals(coreAlbum2)) {
                        adjustedThreshold = Math.max(similarityThreshold, 0.6);
                    } else {
                        adjustedThreshold = Math.max(similarityThreshold, 0.7);
                    }
                    isSimilar = similarity >= adjustedThreshold;
                } else {
                    // 如果没有相同的系列关键词，检查核心专辑名称
                    if (!coreAlbum1.isEmpty() && !coreAlbum2.isEmpty() && coreAlbum1.equals(coreAlbum2)) {
                        // 核心专辑名称相同，使用中等阈值
                        adjustedThreshold = Math.max(similarityThreshold, 0.75);
                        isSimilar = similarity >= adjustedThreshold;
                    } else {
                        // 核心专辑名称不同，使用更高的阈值
                        adjustedThreshold = Math.max(similarityThreshold, 0.85);
                        isSimilar = similarity >= adjustedThreshold;
                    }
                }
                
                // 对于古典音乐文件，使用更高的相似度阈值
                if (isClassicalMusicFile(currentName) || isClassicalMusicFile(candidateName)) {
                    isSimilar = similarity >= Math.max(similarityThreshold, 0.8);
                }

                if (isSimilar) {
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

    /**
     * 判断文件是否为古典音乐文件
     */
    private boolean isClassicalMusicFile(String filename) {
        // 检查文件名是否包含古典音乐相关的关键词
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.contains("古典") || 
               lowerFilename.contains("classical") ||
               lowerFilename.contains("巴赫") ||
               lowerFilename.contains("贝多芬") ||
               lowerFilename.contains("莫扎特") ||
               lowerFilename.contains("舒伯特") ||
               lowerFilename.contains("舒曼") ||
               lowerFilename.contains("勃拉姆斯") ||
               lowerFilename.contains("柴可夫斯基") ||
               lowerFilename.contains("德沃夏克") ||
               lowerFilename.contains("肖邦") ||
               lowerFilename.contains("李斯特") ||
               lowerFilename.contains("门德尔松") ||
               lowerFilename.contains("海顿") ||
               lowerFilename.contains("威尔第") ||
               lowerFilename.contains("普契尼") ||
               lowerFilename.contains("罗西尼") ||
               lowerFilename.contains("比才") ||
               lowerFilename.contains("德彪西") ||
               lowerFilename.contains("拉威尔") ||
               lowerFilename.contains("圣桑") ||
               lowerFilename.contains("布鲁克纳") ||
               lowerFilename.contains("维瓦尔第") ||
               lowerFilename.contains("beethoven") ||
               lowerFilename.contains("mozart") ||
               lowerFilename.contains("bach") ||
               lowerFilename.contains("schubert") ||
               lowerFilename.contains("schumann") ||
               lowerFilename.contains("brahms") ||
               lowerFilename.contains("tchaikovsky") ||
               lowerFilename.contains("dvorak") ||
               lowerFilename.contains("chopin") ||
               lowerFilename.contains("liszt") ||
               lowerFilename.contains("mendelssohn") ||
               lowerFilename.contains("haydn") ||
               lowerFilename.contains("verdi") ||
               lowerFilename.contains("puccini") ||
               lowerFilename.contains("rossini") ||
               lowerFilename.contains("bizet") ||
               lowerFilename.contains("debussy") ||
               lowerFilename.contains("ravel") ||
               lowerFilename.contains("saint-saens") ||
               lowerFilename.contains("bruckner") ||
               lowerFilename.contains("vivaldi");
    }

    /**
     * 检查两个古典音乐文件是否属于同一作品系列
     */
    private boolean isSameClassicalWork(String filename1, String filename2) {
        // 提取两个文件名的系列关键词
        String series1 = extractSeriesKeyword(filename1);
        String series2 = extractSeriesKeyword(filename2);
        
        if (series1.isEmpty() || series2.isEmpty()) {
            return false;
        }
        
        // 提取作曲家名称
        String composer1 = extractComposerFromSeries(series1);
        String composer2 = extractComposerFromSeries(series2);
        
        // 如果作曲家不同，则不属于同一作品系列
        if (!composer1.equals(composer2)) {
            return false;
        }
        
        // 提取作品类型
        String workType1 = extractWorkType(series1);
        String workType2 = extractWorkType(series2);
        
        // 如果作品类型相同，则认为属于同一作品系列
        if (!workType1.isEmpty() && workType1.equals(workType2)) {
            return true;
        }
        
        return false;
    }

    /**
     * 提取作品类型
     */
    private String extractWorkType(String series) {
        if (series.isEmpty()) {
            return "";
        }
        
        // 常见作品类型
        String[] workTypes = {
            "交响曲", "协奏曲", "奏鸣曲", "四重奏", "五重奏", "小夜曲", "嬉游曲",
            "序曲", "组曲", "幻想曲", "变奏曲", "赋格", "前奏曲", "圆舞曲",
            "进行曲", "夜曲", "练习曲", "即兴曲", "叙事曲", "谐谑曲", "随想曲",
            "狂想曲", "摇篮曲", "牧歌", "挽歌", "弥撒曲", "安魂曲", "清唱剧",
            "歌剧", " ballet", "symphony", "concerto", "sonata", "quartet",
            "quintet", "serenade", "overture", "suite", "fantasy", "variations",
            "fugue", "prelude", "waltz", "march", "nocturne", "etude", "impromptu",
            "ballade", "scherzo", "capriccio", "rhapsody", "lullaby", "madrigal",
            "requiem", "mass", "oratorio", "opera"
        };
        
        // 检查系列名称是否包含作品类型
        for (String workType : workTypes) {
            if (series.contains(workType)) {
                return workType;
            }
        }
        
        return "";
    }

    /**
     * 提取核心专辑名称
     */
    private String extractCoreAlbumName(String filename) {
        // 1. 尝试提取《》中的内容
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("《([^》]+)》").matcher(filename);
        if (matcher.find()) {
            String albumName = matcher.group(1);
            // 移除CD序号、VOL序号等信息
            albumName = albumName.replaceAll("\\s*CD\\s*\\d+", "");
            albumName = albumName.replaceAll("\\s*cd\\s*\\d+", "");
            albumName = albumName.replaceAll("\\s*Vol\\s*\\d+", "");
            albumName = albumName.replaceAll("\\s*vol\\s*\\d+", "");
            albumName = albumName.replaceAll("\\s*VOL\\s*\\d+", "");
            albumName = albumName.trim();
            if (albumName.length() >= 3) {
                return albumName;
            }
        }
        
        // 2. 如果没有《》，尝试提取专辑名称（去除年份、版本等信息）
        String normalized = filename;
        
        // 移除年份前缀（如"1996-"、"1997-"）
        normalized = normalized.replaceAll("^\\d{4}-\\d{2}-", "");
        normalized = normalized.replaceAll("^\\d{4}-", "");
        
        // 移除版本信息
        normalized = normalized.replaceAll("\\[.*?\\]", "");
        normalized = normalized.replaceAll("【.*?】", "");
        
        // 移除CD序号
        normalized = normalized.replaceAll("\\s*CD\\s*\\d+\\b", "");
        normalized = normalized.replaceAll("\\s*cd\\s*\\d+\\b", "");
        
        // 移除VOL序号
        normalized = normalized.replaceAll("\\s*Vol\\s*\\d+\\b", "");
        normalized = normalized.replaceAll("\\s*vol\\s*\\d+\\b", "");
        normalized = normalized.replaceAll("\\s*VOL\\s*\\d+\\b", "");
        
        // 移除后缀数字（如"-1"、"-2"）
        normalized = normalized.replaceAll("-\\d+$", "");
        
        // 移除文件类型
        normalized = normalized.replaceAll("\\s*\\[WAV\\]", "");
        normalized = normalized.replaceAll("\\s*\\[FLAC\\]", "");
        normalized = normalized.replaceAll("\\s*\\[MP3\\]", "");
        normalized = normalized.replaceAll("\\s*WAV\\s*\\+\\s*CUE\\b", "");
        normalized = normalized.replaceAll("\\s*WAV\\b", "");
        normalized = normalized.replaceAll("\\s*CUE\\b", "");
        
        // 移除常见前缀（如"群星."、"滚石群星."等）
        normalized = normalized.replaceAll("^(群星\\.|滚石群星\\.|滚石\\.|龙音\\.)", "");
        
        normalized = normalized.trim();
        
        if (normalized.length() >= 3) {
            return normalized;
        }
        
        return "";
    }

    /**
     * 检查两个文件是否属于同一宝丽金系列
     */
    private boolean isSamePolygramSeries(String filename1, String filename2) {
        // 检查是否都是"皇牌"系列
        boolean bothRoyal = filename1.contains("皇牌") && filename2.contains("皇牌");
        if (bothRoyal) {
            // 对于"皇牌"系列，只要都包含"皇牌"和"宝丽金"，就认为是同一系列
            boolean bothPolygram = filename1.contains("宝丽金") && filename2.contains("宝丽金");
            return bothPolygram;
        }
        
        // 检查是否都是"STS+SRS"系列
        boolean bothSTS = filename1.contains("STS+SRS") && filename2.contains("STS+SRS");
        if (bothSTS) {
            return true;
        }
        
        // 检查是否都是"超白金精选"系列
        boolean bothSuperGold = filename1.contains("超白金精选") && filename2.contains("超白金精选");
        if (bothSuperGold) {
            return true;
        }
        
        // 检查是否都是"精选"系列
        boolean bothSelected = filename1.contains("精选") && filename2.contains("精选");
        if (bothSelected) {
            // 提取核心专辑名称
            String core1 = extractCoreAlbumName(filename1);
            String core2 = extractCoreAlbumName(filename2);
            // 如果核心专辑名称相似，则认为是同一系列
            if (!core1.isEmpty() && !core2.isEmpty()) {
                double similarity = similarityCalculator.calculateSimilarity(core1, core2);
                return similarity >= 0.6;
            }
        }
        
        return false;
    }

    /**
     * 检查两个文件名是否包含相同的系列关键词
     */
    private boolean hasSameSeriesKeywords(String filename1, String filename2) {
        // 提取文件名中的系列关键词
        String series1 = extractSeriesKeyword(filename1);
        String series2 = extractSeriesKeyword(filename2);
        
        // 如果两个文件名都包含系列关键词且相同，则认为它们属于同一系列
        if (!series1.isEmpty() && series1.equals(series2)) {
            return true;
        }
        
        // 检查是否包含相同的核心系列关键词（如"柔板"系列）
        String coreSeries1 = extractCoreSeriesKeyword(series1);
        String coreSeries2 = extractCoreSeriesKeyword(series2);
        
        return !coreSeries1.isEmpty() && coreSeries1.equals(coreSeries2);
    }

    /**
     * 从文件名中提取系列关键词
     */
    private String extractSeriesKeyword(String filename) {
        // 提取《》中的内容作为系列关键词
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("《([^》]+)》").matcher(filename);
        if (matcher.find()) {
            String series = matcher.group(1);
            // 移除CD序号等信息
            series = series.replaceAll("\\s*CD\\s*\\d+", "");
            series = series.replaceAll("\\s*cd\\s*\\d+", "");
            series = series.replaceAll("\\s*Vol\\s*\\d+", "");
            series = series.replaceAll("\\s*vol\\s*\\d+", "");
            series = series.trim();
            return series;
        }
        return "";
    }

    /**
     * 提取核心系列关键词（如"傍晚的柔板"中的"傍晚的柔板"）
     */
    private String extractCoreSeriesKeyword(String series) {
        if (series.isEmpty()) {
            return "";
        }
        
        // 对于"柔板"系列，提取完整的系列名称
        if (series.contains("柔板")) {
            return series;
        }
        
        // 对于莫扎特等作曲家的作品，提取作曲家名称
        if (series.contains("莫扎特")) {
            return "莫扎特";
        }
        if (series.contains("贝多芬")) {
            return "贝多芬";
        }
        if (series.contains("巴赫")) {
            return "巴赫";
        }
        
        return series;
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

        // 使用通用合集名称生成器（自动识别模式，不依赖硬编码规则）
        try {
            UniversalCollectionNameGenerator universalGenerator = 
                new UniversalCollectionNameGenerator(new TextSimilarityCalculatorAdapter(similarityCalculator));
            String universalName = universalGenerator.generateCollectionName(filenames);
            if (universalName != null && !universalName.isEmpty() && !universalName.equals("未命名")) {
                return universalName;
            }
        } catch (Exception e) {
            // 如果通用生成器失败，回退到传统方法
            System.err.println("通用合集名称生成失败，使用传统方法: " + e.getMessage());
        }

        // 回退到传统方法
        return generateClusterNameTraditional(filenames);
    }
    
    /**
     * 传统合集名称生成方法（作为回退方案）
     */
    private String generateClusterNameTraditional(List<String> filenames) {
        // 1. 尝试提取宝丽金专辑名称（优先级最高）
        String polygramAlbumName = extractPolygramAlbumName(filenames);
        if (!polygramAlbumName.isEmpty()) {
            return polygramAlbumName;
        }

        // 2. 尝试从古典音乐文件名中提取系列名称
        String classicalSeriesName = extractClassicalSeriesName(filenames);
        if (!classicalSeriesName.isEmpty()) {
            return classicalSeriesName;
        }

        // 3. 尝试提取艺术家名称
        String artistName = extractArtistName(filenames);
        if (!artistName.isEmpty()) {
            return artistName;
        }

        // 4. 尝试提取作曲家名称
        String composerName = extractComposerName(filenames);
        if (!composerName.isEmpty()) {
            return composerName;
        }

        // 5. 提取最长公共前缀（传统方法）
        String longestCommonPrefix = findLongestCommonPrefix(filenames);
        if (longestCommonPrefix.length() >= 5) {
            // 清理合集名称，去除不必要的前缀和后缀
            String cleanedPrefix = cleanCollectionName(longestCommonPrefix);
            // 尝试修复不完整的括号
            String fixedPrefix = fixIncompleteBrackets(cleanedPrefix);
            return fixedPrefix.trim();
        }

        // 6. 提取最频繁的词
        String frequentWords = extractMostFrequentWords(filenames);
        if (!frequentWords.isEmpty()) {
            return frequentWords;
        }

        return filenames.get(0);
    }

    /**
     * 提取宝丽金专辑名称
     */
    private String extractPolygramAlbumName(List<String> filenames) {
        // 统计每个可能的专辑名称出现的次数
        Map<String, Integer> albumCount = new HashMap<>();
        
        for (String filename : filenames) {
            String albumName = "";
            
            // 1. 尝试提取《》中的内容作为专辑名称
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("《([^》]+)》").matcher(filename);
            if (matcher.find()) {
                albumName = matcher.group(1);
                
                // 移除CD序号
                albumName = albumName.replaceAll("\\s*CD\\s*\\d+", "");
                albumName = albumName.replaceAll("\\s*cd\\s*\\d+", "");
                
                // 移除VOL序号
                albumName = albumName.replaceAll("\\s*Vol\\s*\\d+", "");
                albumName = albumName.replaceAll("\\s*vol\\s*\\d+", "");
                albumName = albumName.replaceAll("\\s*VOL\\s*\\d+", "");
                
                // 移除数字序号（如"男人的眼泪精选1"中的"1"）
                albumName = albumName.replaceAll("\\d+$", "");
                
                albumName = albumName.trim();
            } else {
                // 2. 如果没有《》，尝试从文件名中提取专辑名称
                albumName = extractAlbumNameWithoutBrackets(filename);
            }
            
            if (albumName.length() >= 3) {
                albumCount.put(albumName, albumCount.getOrDefault(albumName, 0) + 1);
            }
        }
        
        // 找到出现次数最多的专辑名称
        String bestAlbumName = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : albumCount.entrySet()) {
            if (entry.getValue() > maxCount && entry.getKey().length() >= 3) {
                maxCount = entry.getValue();
                bestAlbumName = entry.getKey();
            }
        }
        
        // 如果找到的专辑名称在至少2个文件中出现，则使用它
        if (maxCount >= 2) {
            return bestAlbumName;
        }
        
        // 如果只有一个文件包含专辑名称，直接使用该专辑名称
        if (albumCount.size() == 1) {
            for (String albumName : albumCount.keySet()) {
                return albumName;
            }
        }
        
        return "";
    }

    /**
     * 从没有《》的文件名中提取专辑名称
     */
    private String extractAlbumNameWithoutBrackets(String filename) {
        String albumName = filename;
        
        // 移除年份前缀（如"1996-"、"1997-"）
        albumName = albumName.replaceAll("^\\d{4}-\\d{2}-", "");
        albumName = albumName.replaceAll("^\\d{4}-", "");
        
        // 移除版本信息
        albumName = albumName.replaceAll("\\[.*?\\]", "");
        
        // 移除CD序号
        albumName = albumName.replaceAll("\\s*CD\\s*\\d+\\b", "");
        albumName = albumName.replaceAll("\\s*cd\\s*\\d+\\b", "");
        
        // 移除VOL序号
        albumName = albumName.replaceAll("\\s*Vol\\s*\\d+\\b", "");
        albumName = albumName.replaceAll("\\s*vol\\s*\\d+\\b", "");
        albumName = albumName.replaceAll("\\s*VOL\\s*\\d+\\b", "");
        
        // 移除后缀数字（如"-1"、"-2"）
        albumName = albumName.replaceAll("-\\d+$", "");
        
        // 移除文件类型
        albumName = albumName.replaceAll("\\s*\\[WAV\\]", "");
        albumName = albumName.replaceAll("\\s*\\[FLAC\\]", "");
        albumName = albumName.replaceAll("\\s*\\[MP3\\]", "");
        
        albumName = albumName.trim();
        
        if (albumName.length() >= 3) {
            return albumName;
        }
        
        return "";
    }

    /**
     * 提取作曲家名称
     */
    private String extractComposerName(List<String> filenames) {
        // 统计每个可能的作曲家名称出现的次数
        Map<String, Integer> composerCount = new HashMap<>();
        
        for (String filename : filenames) {
            // 提取《》中的作曲家名称
            String series = extractSeriesKeyword(filename);
            if (!series.isEmpty()) {
                // 尝试从系列名称中提取作曲家名称
                String composer = extractComposerFromSeries(series);
                if (!composer.isEmpty()) {
                    composerCount.put(composer, composerCount.getOrDefault(composer, 0) + 1);
                }
            }
        }
        
        // 找到出现次数最多的作曲家名称
        String bestComposerName = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : composerCount.entrySet()) {
            if (entry.getValue() > maxCount && entry.getKey().length() >= 2) {
                maxCount = entry.getValue();
                bestComposerName = entry.getKey();
            }
        }
        
        // 如果找到的作曲家名称在至少2个文件中出现，则使用它
        if (maxCount >= 2) {
            return bestComposerName;
        }
        
        return "";
    }

    /**
     * 从系列名称中提取作曲家名称
     */
    private String extractComposerFromSeries(String series) {
        if (series.isEmpty()) {
            return "";
        }
        
        // 常见作曲家列表
        String[] composers = {
            "巴赫", "贝多芬", "莫扎特", "舒伯特", "舒曼", "勃拉姆斯", "柴可夫斯基",
            "德沃夏克", "肖邦", "李斯特", "门德尔松", "海顿", "威尔第", "普契尼",
            "罗西尼", "比才", "德彪西", "拉威尔", "圣桑", "布鲁克纳", "维瓦尔第"
        };
        
        // 检查系列名称是否包含作曲家名称
        for (String composer : composers) {
            if (series.contains(composer)) {
                return composer;
            }
        }
        
        return "";
    }

    /**
     * 从古典音乐文件名中提取系列名称
     */
    private String extractClassicalSeriesName(List<String> filenames) {
        // 统计每个可能的系列名称出现的次数
        Map<String, Integer> seriesCount = new HashMap<>();
        
        for (String filename : filenames) {
            // 提取《》中的内容作为系列名称
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("《([^》]+)》").matcher(filename);
            if (matcher.find()) {
                String seriesName = matcher.group(1);
                // 移除CD序号等信息
                seriesName = seriesName.replaceAll("\\s*CD\\s*\\d+", "");
                seriesName = seriesName.replaceAll("\\s*cd\\s*\\d+", "");
                seriesName = seriesName.trim();
                if (seriesName.length() >= 3) {
                    seriesCount.put(seriesName, seriesCount.getOrDefault(seriesName, 0) + 1);
                }
            }
        }
        
        // 找到出现次数最多的系列名称
        String bestSeriesName = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : seriesCount.entrySet()) {
            if (entry.getValue() > maxCount && entry.getKey().length() >= 3) {
                maxCount = entry.getValue();
                bestSeriesName = entry.getKey();
            }
        }
        
        // 如果找到的系列名称在至少2个文件中出现，则使用它
        if (maxCount >= 2) {
            return bestSeriesName;
        }
        
        return "";
    }

    /**
     * 从文件名中提取艺术家名称
     */
    private String extractArtistName(List<String> filenames) {
        // 统计每个可能的艺术家名称出现的次数
        Map<String, Integer> artistCount = new HashMap<>();
        
        for (String filename : filenames) {
            // 提取【古典音乐】后的艺术家名称
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("【古典音乐】([^《]+)《").matcher(filename);
            if (matcher.find()) {
                String artistName = matcher.group(1).trim();
                if (artistName.length() >= 2) {
                    artistCount.put(artistName, artistCount.getOrDefault(artistName, 0) + 1);
                }
            }
        }
        
        // 找到出现次数最多的艺术家名称
        String bestArtistName = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : artistCount.entrySet()) {
            if (entry.getValue() > maxCount && entry.getKey().length() >= 2) {
                maxCount = entry.getValue();
                bestArtistName = entry.getKey();
            }
        }
        
        // 如果找到的艺术家名称在至少2个文件中出现，则使用它
        if (maxCount >= 2) {
            return bestArtistName;
        }
        
        return "";
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

    private String cleanCollectionName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // 去除常见的不必要前缀
        String[] prefixesToRemove = {
            "缇ゆ槦\\.",
            "缇ゆ槦",
            "滚石合集\\.",
            "滚石合集",
            "滚石\\.",
            "滚石",
            "龙音唱片\\.-\\.",
            "龙音唱片\\.-",
            "龙音唱片\\.",
            "龙音唱片",
            "龙音\\.",
            "龙音",
            "合集\\.",
            "合集",
            "Collection\\.",
            "Collection",
            "缇\\.",
            "缇",
            "唱片\\.",
            "唱片",
            "唱片公司\\.",
            "唱片公司",
            "音乐\\.",
            "音乐",
            "专辑\\.",
            "专辑"
        };

        for (String prefix : prefixesToRemove) {
            if (name.matches(prefix + ".*")) {
                name = name.replaceFirst(prefix, "");
                break;
            }
        }

        // 去除年份前缀（如 .2005 - 、 .1998 - 、 2005 - 、 1998 - 等）
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");

        // 去除括号内的内容（包括中文和英文括号）
        name = name.replaceAll("\\[.*?\\]", "");
        name = name.replaceAll("\\(.*?\\)", "");
        name = name.replaceAll("【.*?】", "");
        name = name.replaceAll("《.*?》", "");
        name = name.replaceAll("「.*?」", "");
        name = name.replaceAll("『.*?』", "");

        // 去除常见的不必要后缀
        String[] suffixesToRemove = {
            "\\s*CD$",
            "\\s*CD\\s*$",
            "\\s*VOL\\.$",
            "\\s*VOL\\.\\s*$",
            "\\s*Disc$",
            "\\s*Disc\\s*$"
        };

        for (String suffix : suffixesToRemove) {
            name = name.replaceAll(suffix, "");
        }

        // 去除多余的空格和特殊字符
        name = name.trim();
        name = name.replaceAll("\\s+", " ");
        name = name.replaceAll("[-_]{2,}", "-");

        // 如果清理后的名称太短或只包含数字，返回原始名称
        if (name.length() < 3 || name.matches("^\\d+$")) {
            return name;
        }

        return name;
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

        return prefix.trim();
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
