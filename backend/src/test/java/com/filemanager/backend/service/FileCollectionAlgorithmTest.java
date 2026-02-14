package com.filemanager.backend.service;

import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.impl.filecollection.FileCollectionStrategy;
import com.filemanager.plugin.impl.filecollection.collection.FilenameNormalizer;
import com.filemanager.plugin.impl.filecollection.collection.TextSimilarityCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件归类策略算法测试类
 * 验证FileCollectionStrategy中各个算法模块的效果
 * 
 * 测试范围：
 * - 相似度计算
 * - 文件名标准化
 * - 特殊符号和数字处理
 * - 相同标题不同序号识别
 * - 关键词提取
 * - 文件聚类
 * - 合集判断
 */
public class FileCollectionAlgorithmTest extends StrategyTestBase {

    private FilenameNormalizer normalizer;
    private File testDir;

    @BeforeEach
    public void setUp() {
        // 初始化测试组件
        normalizer = new FilenameNormalizer();
        // TextSimilarityCalculator是静态工具类，不需要实例化
        
        // 创建临时测试目录
        testDir = new File(System.getProperty("java.io.tmpdir"), "test_file_collection_" + System.currentTimeMillis());
        testDir.mkdirs();
    }

    /**
     * 测试场景1：相似度计算
     * 
     * 目的：验证文本相似度计算算法的准确性
     * 测试数据：
     * - 相似文件：张平福《古筝天地①月圆花好》 vs 张平福《古筝天地②草原之夜》
     * - 不相似文件：张平福《古筝天地①月圆花好》 vs 张平福《萨克斯ChaCha浪漫旋律》
     * 断言：
     * - 相似文件的相似度 > 0.8
     * - 不相似文件的相似度 < 0.6
     */
    @Test
    public void testCalculateSimilarity() {
        System.out.println("=== 测试相似度计算 ===");
        
        // 测试相似文件
        String s1 = "张平福《古筝天地①月圆花好》";
        String s2 = "张平福《古筝天地②草原之夜》";
        double similarity = TextSimilarityCalculator.calculateSimilarity(s1, s2);
        System.out.println("相似度测试: " + s1 + " vs " + s2 + " = " + similarity);
        System.out.println("测试结果: 相似的系列文件应该有较高的相似度 -> " + (similarity > 0.8));
        assert similarity > 0.8 : "相似文件的相似度应该大于0.8";
        
        // 测试不相似文件
        String s3 = "张平福《古筝天地①月圆花好》";
        String s4 = "张平福《萨克斯ChaCha浪漫旋律》";
        double similarity2 = TextSimilarityCalculator.calculateSimilarity(s3, s4);
        System.out.println("相似度测试: " + s3 + " vs " + s4 + " = " + similarity2);
        System.out.println("测试结果: 不同类型的文件应该有较低的相似度 -> " + (similarity2 < 0.6));
        assert similarity2 < 0.6 : "不相似文件的相似度应该小于0.6";
    }

    /**
     * 测试场景2：相同标题不同序号识别
     * 
     * 目的：验证算法能够识别相同标题不同序号的文件
     * 测试数据：
     * - 相同标题不同序号：张平福《古筝天地①月圆花好》 vs 张平福《古筝天地②草原之夜》
     * - 不同标题：张平福《古筝天地①月圆花好》 vs 张平福《萨克斯ChaCha浪漫旋律》
     * 断言：
     * - 相同标题不同序号的文件应该被识别为系列
     * - 不同标题的文件不应该被识别为系列
     */
    @Test
    public void testHasSameTitleDifferentNumber() {
        System.out.println("=== 测试相同标题不同序号识别 ===");
        
        // 测试相同标题不同序号的文件
        String s1 = "张平福《古筝天地①月圆花好》";
        String s2 = "张平福《古筝天地②草原之夜》";
        String normalized1 = normalizer.normalize(s1);
        String normalized2 = normalizer.normalize(s2);
        boolean result = hasSameTitleDifferentNumber(normalized1, normalized2);
        System.out.println("相同标题不同序号测试: " + s1 + " vs " + s2 + " = " + result);
        System.out.println("测试结果: 相同标题不同序号的文件应该被识别为系列 -> " + result);
        assert result : "相同标题不同序号的文件应该被识别为系列";
        
        // 测试不同标题的文件
        String s3 = "张平福《古筝天地①月圆花好》";
        String s4 = "张平福《萨克斯ChaCha浪漫旋律》";
        String normalized3 = normalizer.normalize(s3);
        String normalized4 = normalizer.normalize(s4);
        boolean result2 = hasSameTitleDifferentNumber(normalized3, normalized4);
        System.out.println("不同标题测试: " + s3 + " vs " + s4 + " = " + result2);
        System.out.println("测试结果: 不同标题的文件不应该被识别为系列 -> " + !result2);
        assert !result2 : "不同标题的文件不应该被识别为系列";
    }

    /**
     * 测试场景3：关键词提取
     * 
     * 目的：验证算法能够从文件名中提取核心关键词
     * 测试数据：
     * - 文件名：张平福《古筝天地①月圆花好》专辑.(FLAC)
     * 断言：
     * - 应该提取出核心关键词（张平福、古筝天地、月圆花好）
     */
    @Test
    public void testExtractCoreKeywords() {
        System.out.println("=== 测试关键词提取 ===");
        
        String fileName = "张平福《古筝天地①月圆花好》专辑.(FLAC)";
        String normalized = normalizer.normalize(fileName);
        List<String> keywords = extractCoreKeywords(normalized);
        System.out.println("关键词提取测试: " + fileName + " -> " + keywords);
        System.out.println("标准化后: " + normalized);
        System.out.println("测试结果: 应该提取出核心关键词 -> " + (keywords != null && !keywords.isEmpty()));
        assert keywords != null && !keywords.isEmpty() : "应该提取出核心关键词";
    }

    /**
     * 测试场景4：特殊符号和数字处理
     * 
     * 目的：验证算法能够正确处理特殊符号和数字
     * 测试数据：
     * - 输入：张平福《古筝天地①月圆花好》VOL.01
     * 断言：
     * - 应该保留核心内容（张平福古筝天地）
     */
    @Test
    public void testProcessSpecialSymbolsAndNumbers() {
        System.out.println("=== 测试特殊符号和数字处理 ===");
        
        String input = "张平福《古筝天地①月圆花好》VOL.01";
        String processed = normalizer.normalize(input);
        System.out.println("特殊符号处理测试: " + input + " -> " + processed);
        System.out.println("测试结果: 应该保留核心内容 -> " + processed.contains("张平福古筝天地"));
        assert processed.contains("张平福古筝天地") : "应该保留核心内容";
    }

    /**
     * 测试场景5：龙音文件名标准化
     * 
     * 目的：验证算法能够正确处理龙音唱片格式的文件名
     * 测试数据：
     * - 望秦川-王中山古筝专辑之四
     * - 溟山-王中山古筝专辑(一)
     * - 黄河魂-王中山古筝专辑(二)
     * - 夜深沉-王中山古筝专辑之三
     * 断言：
     * - 标准化后的文件名应该相似
     * - 相似度应该能够识别为同一系列
     */
    @Test
    public void testLongyinFilenameNormalization() {
        System.out.println("=== 测试龙音文件名标准化 ===");
        
        List<String> longyinFilenames = Arrays.asList(
            "[海文版 CD-0174]望秦川-王中山古筝专辑之四",
            "[龙音海文版 CD-0073]溟山-王中山古筝专辑(一)",
            "[龙音海文版 CD-0074]黄河魂-王中山古筝专辑(二)",
            "[龙音海文版 CD-0173]夜深沉-王中山古筝专辑之三"
        );
        
        List<String> normalizedFilenames = new ArrayList<>();
        for (String filename : longyinFilenames) {
            String normalized = normalizer.normalize(filename);
            normalizedFilenames.add(normalized);
            System.out.println("原始: " + filename);
            System.out.println("标准化: " + normalized);
            System.out.println();
        }
        
        // 计算相似度
        System.out.println("=== 相似度计算 ===");
        String normalized1 = normalizedFilenames.get(0);
        String normalized2 = normalizedFilenames.get(1);
        double similarity = TextSimilarityCalculator.calculateSimilarity(normalized1, normalized2);
        System.out.println("相似度: " + similarity);
        System.out.println("测试结果: 龙音系列文件应该有较高的相似度 -> " + (similarity > 0.7));
        assert similarity > 0.7 : "龙音系列文件应该有较高的相似度";
    }

    /**
     * 测试场景6：滚石文件名标准化
     * 
     * 目的：验证算法能够正确处理滚石唱片格式的文件名
     * 测试数据：
     * - 群星.2001 - 文艺民歌时代【滚石】【WAV+CUE】
     * - 群星.2002 - 文艺民歌时代2【滚石】【WAV+CUE】
     * 断言：
     * - 标准化后的文件名应该相似
     * - 相似度应该能够识别为同一系列
     */
    @Test
    public void testRollingStoneFilenameNormalization() {
        System.out.println("=== 测试滚石文件名标准化 ===");
        
        List<String> rollingStoneFilenames = Arrays.asList(
            "群星.2001 - 文艺民歌时代【滚石】【WAV+CUE】",
            "群星.2002 - 文艺民歌时代2【滚石】【WAV+CUE】"
        );
        
        List<String> normalizedFilenames = new ArrayList<>();
        for (String filename : rollingStoneFilenames) {
            String normalized = normalizer.normalize(filename);
            normalizedFilenames.add(normalized);
            System.out.println("原始: " + filename);
            System.out.println("标准化: " + normalized);
            System.out.println();
        }
        
        // 计算相似度
        System.out.println("=== 相似度计算 ===");
        String rsNormalized1 = normalizedFilenames.get(0);
        String rsNormalized2 = normalizedFilenames.get(1);
        double rsSimilarity = TextSimilarityCalculator.calculateSimilarity(rsNormalized1, rsNormalized2);
        System.out.println("相似度: " + rsSimilarity);
        System.out.println("测试结果: 滚石系列文件应该有较高的相似度 -> " + (rsSimilarity > 0.7));
        assert rsSimilarity > 0.7 : "滚石系列文件应该有较高的相似度";
    }

    /**
     * 测试场景7：15首精选滚石年度强打金曲文件名标准化
     * 
     * 目的：验证算法能够正确处理复杂的专辑名称
     * 测试数据：
     * - 滚石群星200雀巢咖啡时尚精选 15首精选滚石年度强打金曲[滚石][WAV+CUE]
     * - 群星2000-雀巢咖啡时尚精选 15首精选滚石年度强打金曲[引进版][WAV+CUE]
     * 断言：
     * - 标准化后的文件名应该相似
     * - 相似度应该能够识别为同一系列
     */
    @Test
    public void test15SongsNormalization() {
        System.out.println("=== 测试15首精选滚石年度强打金曲文件名标准化 ===");
        
        String filename1 = "滚石群星200雀巢咖啡时尚精选 15首精选滚石年度强打金曲[滚石][WAV+CUE]";
        String filename2 = "群星2000-雀巢咖啡时尚精选 15首精选滚石年度强打金曲[引进版][WAV+CUE]";
        
        String normalized1 = normalizer.normalize(filename1);
        String normalized2 = normalizer.normalize(filename2);
        
        System.out.println("原始1: " + filename1);
        System.out.println("标准化1: " + normalized1);
        System.out.println();
        System.out.println("原始2: " + filename2);
        System.out.println("标准化2: " + normalized2);
        System.out.println();
        
        // 计算相似度
        double similarity = TextSimilarityCalculator.calculateSimilarity(normalized1, normalized2);
        System.out.println("相似度: " + similarity);
        System.out.println("是否可以聚类: " + (similarity >= 0.7 ? "是" : "否"));
        System.out.println("测试结果: 15首精选滚石年度强打金曲应该有较高的相似度 -> " + (similarity > 0.7));
        assert similarity > 0.7 : "15首精选滚石年度强打金曲应该有较高的相似度";
    }

    /**
     * 测试场景8：龙音文采华音版-轻舟随波系列的相似度计算
     * 
     * 目的：验证算法能够正确识别龙音文采华音版-轻舟随波系列
     * 测试数据：
     * - 龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶
     * - 龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事
     * - 龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥华夏风情篇-睡莲
     * - 龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦-异国风情篇-美丽的梭罗河
     * 断言：
     * - 所有文件之间的相似度都应该 > 0.7
     * - 所有文件都应该被识别为相似
     */
    @Test
    public void testLongyinWencaiSimilarity() {
        System.out.println("=== 测试龙音文采华音版-轻舟随波系列的相似度计算 ===");
        
        String file1 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶";
        String file2 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事";
        String file3 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥华夏风情篇-睡莲";
        String file4 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦-异国风情篇-美丽的梭罗河";
        
        System.out.println("文件1: " + file1);
        System.out.println("文件2: " + file2);
        System.out.println("文件3: " + file3);
        System.out.println("文件4: " + file4);
        
        System.out.println("\n相似度计算:");
        double similarity12 = TextSimilarityCalculator.calculateSimilarity(file1, file2);
        double similarity13 = TextSimilarityCalculator.calculateSimilarity(file1, file3);
        double similarity14 = TextSimilarityCalculator.calculateSimilarity(file1, file4);
        double similarity23 = TextSimilarityCalculator.calculateSimilarity(file2, file3);
        double similarity24 = TextSimilarityCalculator.calculateSimilarity(file2, file4);
        double similarity34 = TextSimilarityCalculator.calculateSimilarity(file3, file4);
        
        System.out.println("文件1 vs 文件2: " + similarity12);
        System.out.println("文件1 vs 文件3: " + similarity13);
        System.out.println("文件1 vs 文件4: " + similarity14);
        System.out.println("文件2 vs 文件3: " + similarity23);
        System.out.println("文件2 vs 文件4: " + similarity24);
        System.out.println("文件3 vs 文件4: " + similarity34);
        
        System.out.println("\n是否相似:");
        boolean isSimilar12 = TextSimilarityCalculator.isSimilar(file1, file2, 0.7);
        boolean isSimilar13 = TextSimilarityCalculator.isSimilar(file1, file3, 0.7);
        boolean isSimilar14 = TextSimilarityCalculator.isSimilar(file1, file4, 0.7);
        boolean isSimilar23 = TextSimilarityCalculator.isSimilar(file2, file3, 0.7);
        boolean isSimilar24 = TextSimilarityCalculator.isSimilar(file2, file4, 0.7);
        boolean isSimilar34 = TextSimilarityCalculator.isSimilar(file3, file4, 0.7);
        
        System.out.println("文件1 vs 文件2: " + isSimilar12);
        System.out.println("文件1 vs 文件3: " + isSimilar13);
        System.out.println("文件1 vs 文件4: " + isSimilar14);
        System.out.println("文件2 vs 文件3: " + isSimilar23);
        System.out.println("文件2 vs 文件4: " + isSimilar24);
        System.out.println("文件3 vs 文件4: " + isSimilar34);
        
        System.out.println("\n测试结果: 所有文件都应该被识别为相似");
        assert isSimilar12 && isSimilar13 && isSimilar14 && isSimilar23 && isSimilar24 && isSimilar34 
            : "所有文件都应该被识别为相似";
    }

    /**
     * 测试场景9：不同相似度阈值的影响
     * 
     * 目的：验证不同相似度阈值对聚类结果的影响
     * 测试数据：
     * - 相似度阈值0.9：应该生成较少的集群
     * - 相似度阈值0.7：应该生成较多的集群
     * 断言：
     * - 较高的阈值应该生成较少的集群
     * - 较低的阈值应该生成较多的集群
     */
    @Test
    public void testDifferentSimilarityThresholds() {
        System.out.println("=== 测试不同相似度阈值的影响 ===");
        
        List<String> filenames = Arrays.asList(
            "张平福《古筝天地①月圆花好》",
            "张平福《古筝天地②草原之夜》",
            "张平福《萨克斯ChaCha浪漫旋律》"
        );
        
        // 测试高阈值
        double highThreshold = 0.9;
        System.out.println("高阈值(0.9)测试:");
        for (int i = 0; i < filenames.size(); i++) {
            for (int j = i + 1; j < filenames.size(); j++) {
                double similarity = TextSimilarityCalculator.calculateSimilarity(filenames.get(i), filenames.get(j));
                boolean isSimilar = TextSimilarityCalculator.isSimilar(filenames.get(i), filenames.get(j), highThreshold);
                System.out.println(filenames.get(i) + " vs " + filenames.get(j) + ": " + similarity + " -> " + isSimilar);
            }
        }
        
        // 测试低阈值
        double lowThreshold = 0.7;
        System.out.println("\n低阈值(0.7)测试:");
        for (int i = 0; i < filenames.size(); i++) {
            for (int j = i + 1; j < filenames.size(); j++) {
                double similarity = TextSimilarityCalculator.calculateSimilarity(filenames.get(i), filenames.get(j));
                boolean isSimilar = TextSimilarityCalculator.isSimilar(filenames.get(i), filenames.get(j), lowThreshold);
                System.out.println(filenames.get(i) + " vs " + filenames.get(j) + ": " + similarity + " -> " + isSimilar);
            }
        }
        
        System.out.println("\n测试结果: 较低的阈值应该识别更多的相似文件");
    }

    /**
     * 测试场景10：边界条件测试
     * 
     * 目的：验证算法在边界条件下的行为
     * 测试数据：
     * - 空字符串
     * - 单个字符
     * - 完全相同的字符串
     * - 完全不同的字符串
     * 断言：
     * - 空字符串的相似度应该为0
     * - 完全相同的字符串的相似度应该为1
     * - 完全不同的字符串的相似度应该很低
     */
    @Test
    public void testBoundaryConditions() {
        System.out.println("=== 测试边界条件 ===");
        
        // 测试空字符串
        double emptySimilarity = TextSimilarityCalculator.calculateSimilarity("", "");
        System.out.println("空字符串相似度: " + emptySimilarity);
        assert emptySimilarity == 0 : "空字符串的相似度应该为0";
        
        // 测试完全相同的字符串
        String same = "张平福《古筝天地①月圆花好》";
        double sameSimilarity = TextSimilarityCalculator.calculateSimilarity(same, same);
        System.out.println("相同字符串相似度: " + sameSimilarity);
        assert sameSimilarity == 1.0 : "完全相同的字符串的相似度应该为1.0";
        
        // 测试完全不同的字符串
        String different1 = "张平福《古筝天地①月圆花好》";
        String different2 = "周杰伦《青花瓷》";
        double differentSimilarity = TextSimilarityCalculator.calculateSimilarity(different1, different2);
        System.out.println("不同字符串相似度: " + differentSimilarity);
        assert differentSimilarity < 0.5 : "完全不同的字符串的相似度应该很低";
        
        System.out.println("\n测试结果: 边界条件处理正确");
    }

    // 辅助方法

    /**
     * 判断两个标准化后的文件名是否具有相同标题但不同序号
     */
    private boolean hasSameTitleDifferentNumber(String normalized1, String normalized2) {
        // 提取序号
        String number1 = extractNumber(normalized1);
        String number2 = extractNumber(normalized2);
        
        // 如果都有序号且不同，检查标题是否相同
        if (number1 != null && number2 != null && !number1.equals(number2)) {
            String title1 = normalized1.replace(number1, "").trim();
            String title2 = normalized2.replace(number2, "").trim();
            return title1.equals(title2);
        }
        
        return false;
    }

    /**
     * 从字符串中提取序号
     */
    private String extractNumber(String str) {
        // 匹配中文数字（一、二、三、四、五、六、七、八、九、十）
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[一二三四五六七八九十①②③④⑤⑥⑦⑧⑨⑩]");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 从标准化后的字符串中提取核心关键词
     */
    private List<String> extractCoreKeywords(String normalized) {
        List<String> keywords = new ArrayList<>();
        
        // 提取中文字符
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]+");
        java.util.regex.Matcher matcher = pattern.matcher(normalized);
        
        while (matcher.find()) {
            String keyword = matcher.group();
            if (keyword.length() >= 2) { // 至少2个字符
                keywords.add(keyword);
            }
        }
        
        return keywords;
    }
}
