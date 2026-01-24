package com.filemanager.strategy;

import com.filemanager.strategy.collection.CollectionDeterminationAlgorithm;
import com.filemanager.strategy.collection.FileClusteringAlgorithm;
import com.filemanager.strategy.collection.FilenameNormalizer;
import com.filemanager.strategy.collection.TextSimilarityCalculator;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文件归类策略测试类
 * 验证FileCollectionStrategy中各个算法模块的效果
 */
public class FileCollectionStrategyTest {
    private FilenameNormalizer normalizer;
    private TextSimilarityCalculator calculator;
    private FileClusteringAlgorithm clusteringAlgorithm;
    private CollectionDeterminationAlgorithm determinationAlgorithm;
    private File testDir;

    @Before
    public void setUp() {
        // 初始化测试组件
        normalizer = FilenameNormalizer.builder().build();
        calculator = TextSimilarityCalculator.builder().similarityThreshold(0.8).build();
        clusteringAlgorithm = FileClusteringAlgorithm.builder()
                .normalizer(normalizer)
                .similarityCalculator(calculator)
                .similarityThreshold(0.8)
                .minClusterSize(2)
                .build();
        determinationAlgorithm = CollectionDeterminationAlgorithm.builder()
                .minFiles(2)
                .minFileNameLength(8)
                .mustContainKeywords(Arrays.asList("CD", "系列", "合集"))
                .mustNotContainKeywords(Arrays.asList("下载", "Album", "群星"))
                .maxCollectionRatio(80)
                .recognitionStrictness(0.9)
                .skipCollections(true)
                .build();
        determinationAlgorithm.setCollectionSuffix("【合集】");

        // 创建临时测试目录
        testDir = new File(System.getProperty("java.io.tmpdir"), "test_file_collection");
        testDir.mkdirs();
    }

    @Test
    public void testCalculateSimilarity() {
        // 测试相似度计算
        String s1 = "张平福《古筝天地①月圆花好》";
        String s2 = "张平福《古筝天地②草原之夜》";
        double similarity = calculator.calculateSimilarity(s1, s2);
        System.out.println("相似度测试: " + s1 + " vs " + s2 + " = " + similarity);
        System.out.println("测试结果: 相似的系列文件应该有较高的相似度 -> " + (similarity > 0.8));

        // 测试不同类型的文件
        String s3 = "张平福《古筝天地①月圆花好》";
        String s4 = "张平福《萨克斯ChaCha浪漫旋律》";
        double similarity2 = calculator.calculateSimilarity(s3, s4);
        System.out.println("相似度测试: " + s3 + " vs " + s4 + " = " + similarity2);
        System.out.println("测试结果: 不同类型的文件应该有较低的相似度 -> " + (similarity2 < 0.6));
    }

    @Test
    public void testHasSameTitleDifferentNumber() {
        // 测试相同标题不同序号的文件
        String s1 = "张平福《古筝天地①月圆花好》";
        String s2 = "张平福《古筝天地②草原之夜》";
        boolean result = hasSameTitleDifferentNumber(s1, s2);
        System.out.println("相同标题不同序号测试: " + s1 + " vs " + s2 + " = " + result);
        System.out.println("测试结果: 相同标题不同序号的文件应该被识别为系列 -> " + result);

        // 测试不同标题的文件
        String s3 = "张平福《古筝天地①月圆花好》";
        String s4 = "张平福《萨克斯ChaCha浪漫旋律》";
        boolean result2 = hasSameTitleDifferentNumber(s3, s4);
        System.out.println("不同标题测试: " + s3 + " vs " + s4 + " = " + result2);
        System.out.println("测试结果: 不同标题的文件不应该被识别为系列 -> " + !result2);
    }

    @Test
    public void testExtractCoreKeywords() {
        // 测试关键词提取
        String fileName = "张平福《古筝天地①月圆花好》专辑.(FLAC)";
        List<String> keywords = extractCoreKeywords(fileName);
        System.out.println("关键词提取测试: " + fileName + " -> " + keywords);
        System.out.println("测试结果: 应该提取出核心关键词 -> " + (keywords != null && !keywords.isEmpty()));
    }

    @Test
    public void testProcessSpecialSymbolsAndNumbers() {
        // 测试特殊符号和数字处理
        String input = "张平福《古筝天地①月圆花好》VOL.01";
        String processed = normalizer.normalize(input);
        System.out.println("特殊符号处理测试: " + input + " -> " + processed);
        System.out.println("测试结果: 应该保留核心内容 -> " + processed.contains("张平福古筝天地"));
    }

    @Test
    public void testFileClustering() {
        // 模拟文件系统结构
        List<File> testFiles = createTestFiles();

        // 测试文件聚类
        Map<String, List<File>> clusters = clusteringAlgorithm.clusterFiles(testFiles);
        System.out.println("文件聚类测试: 共生成 " + clusters.size() + " 个集群");

        // 验证聚类结果
        int guzhengClusterSize = 0;

        for (Map.Entry<String, List<File>> entry : clusters.entrySet()) {
            System.out.println("集群: " + entry.getKey() + "，包含 " + entry.getValue().size() + " 个文件");
            for (File file : entry.getValue()) {
                System.out.println("  - " + file.getName());
            }

            if (entry.getKey().contains("古筝")) {
                guzhengClusterSize = entry.getValue().size();
            }
        }

        System.out.println("测试结果: 古筝天地系列应该被聚类到一起 -> " + (guzhengClusterSize > 1));
    }

    @Test
    public void testCollectionDetermination() {
        // 测试合集判断
        // 测试文件是否是合集文件夹
        File collectionFolder = new File(testDir, "张平福【合集】");
        boolean isCollection = determinationAlgorithm.isCollectionFolder(collectionFolder);
        System.out.println("合集文件夹判断测试: " + collectionFolder.getPath() + " -> " + isCollection);
        System.out.println("测试结果: 应该识别出合集文件夹 -> " + isCollection);
    }

    /**
     * 创建测试文件
     */
    private List<File> createTestFiles() {
        List<File> testFiles = new ArrayList<>();

        // 古筝天地系列
        String[] guzhengFiles = {
                "张平福《古筝天地①月圆花好》",
                "张平福《古筝天地②草原之夜》",
                "张平福《古筝天地③王昭君》",
                "张平福《古筝天地④何日君再来》",
                "张平福《古筝天地⑤晚风》"
        };

        // SAX系列
        String[] saxFiles = {
                "张平福《难忘SAX-午夜香吻》",
                "【南方唱片】[张平福]《午夜香吻 难忘 Unforgettable SAX》"
        };

        // 双吉他系列
        String[] guitarFiles = {
                "张平福《名典音乐系列·双吉他》",
                "华人音乐大师（张平福）《双吉他 新时代乐难忘旋律》"
        };

        // 新年系列
        String[] newYearFiles = {
                "张平福《华乐迎春贺岁》CD1",
                "张平福《华乐迎春贺岁》CD2",
                "张平福 -《传统华乐贺新春》CD1",
                "张平福 -《传统华乐贺新春》CD2"
        };

        // 创建测试文件
        for (String fileName : guzhengFiles) {
            File file = new File(testDir, fileName);
            file.mkdirs();
            testFiles.add(file);
        }

        for (String fileName : saxFiles) {
            File file = new File(testDir, fileName);
            file.mkdirs();
            testFiles.add(file);
        }

        for (String fileName : guitarFiles) {
            File file = new File(testDir, fileName);
            file.mkdirs();
            testFiles.add(file);
        }

        for (String fileName : newYearFiles) {
            File file = new File(testDir, fileName);
            file.mkdirs();
            testFiles.add(file);
        }

        return testFiles;
    }

    // 辅助方法：检查两个字符串是否有相同的标题和不同的数字
    private boolean hasSameTitleDifferentNumber(String s1, String s2) {
        // 简单实现：检查是否有相同的前缀和不同的数字部分
        String normalized1 = normalizer.normalize(s1);
        String normalized2 = normalizer.normalize(s2);

        int minLength = Math.min(normalized1.length(), normalized2.length());
        int i = 0;
        while (i < minLength && normalized1.charAt(i) == normalized2.charAt(i)) {
            i++;
        }

        // 检查剩余部分是否包含数字
        boolean hasNumber1 = false;
        boolean hasNumber2 = false;
        for (int j = i; j < normalized1.length(); j++) {
            if (Character.isDigit(normalized1.charAt(j))) {
                hasNumber1 = true;
                break;
            }
        }
        for (int j = i; j < normalized2.length(); j++) {
            if (Character.isDigit(normalized2.charAt(j))) {
                hasNumber2 = true;
                break;
            }
        }

        return i > 0 && hasNumber1 && hasNumber2;
    }

    // 辅助方法：提取核心关键词
    private List<String> extractCoreKeywords(String fileName) {
        // 简单实现：分割文件名，提取可能的关键词
        List<String> keywords = new ArrayList<>();
        String processed = normalizer.normalize(fileName);
        String[] parts = processed.split("\\s+");

        for (String part : parts) {
            if (part.length() > 1 && !part.matches("\\d+")) {
                keywords.add(part);
            }
        }

        return keywords;
    }
}
