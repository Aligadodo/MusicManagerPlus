package com.filemanager.util;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 元数据提取算法测试用例
 * 基于真实文件路径测试元数据提取功能
 */
public class MetadataExtractionTest {

    /**
     * 测试用例类
     */
    static class TestCase {
        private final String filePath;
        private final String expectedArtist;
        private final String expectedAlbum;
        private final String expectedTitle;
        private final String expectedTrack;
        private final String expectedYear;
        private final String description;

        public TestCase(String filePath, String expectedArtist, String expectedAlbum, 
                     String expectedTitle, String expectedTrack, String expectedYear, String description) {
            this.filePath = filePath;
            this.expectedArtist = expectedArtist;
            this.expectedAlbum = expectedAlbum;
            this.expectedTitle = expectedTitle;
            this.expectedTrack = expectedTrack;
            this.expectedYear = expectedYear;
            this.description = description;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getExpectedArtist() {
            return expectedArtist;
        }

        public String getExpectedAlbum() {
            return expectedAlbum;
        }

        public String getExpectedTitle() {
            return expectedTitle;
        }

        public String getExpectedTrack() {
            return expectedTrack;
        }

        public String getExpectedYear() {
            return expectedYear;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description + ": " + filePath;
        }
    }

    /**
     * 测试结果类
     */
    static class TestResult {
        private final TestCase testCase;
        private final MetadataHelper.AudioMeta actualMeta;
        private final boolean artistMatch;
        private final boolean albumMatch;
        private final boolean titleMatch;
        private final boolean trackMatch;
        private final boolean yearMatch;

        public TestResult(TestCase testCase, MetadataHelper.AudioMeta actualMeta) {
            this.testCase = testCase;
            this.actualMeta = actualMeta;
            this.artistMatch = equals(testCase.getExpectedArtist(), actualMeta.getArtist());
            this.albumMatch = equals(testCase.getExpectedAlbum(), actualMeta.getAlbum());
            this.titleMatch = equals(testCase.getExpectedTitle(), actualMeta.getTitle());
            this.trackMatch = equals(testCase.getExpectedTrack(), actualMeta.getTrack());
            this.yearMatch = equals(testCase.getExpectedYear(), actualMeta.getYear());
        }

        private boolean equals(String expected, String actual) {
            if (expected == null && actual == null) return true;
            if (expected == null || actual == null) return false;
            return expected.trim().equalsIgnoreCase(actual.trim());
        }

        public TestCase getTestCase() {
            return testCase;
        }

        public MetadataHelper.AudioMeta getActualMeta() {
            return actualMeta;
        }

        public boolean isArtistMatch() {
            return artistMatch;
        }

        public boolean isAlbumMatch() {
            return albumMatch;
        }

        public boolean isTitleMatch() {
            return titleMatch;
        }

        public boolean isTrackMatch() {
            return trackMatch;
        }

        public boolean isYearMatch() {
            return yearMatch;
        }

        public int getScore() {
            int score = 0;
            if (artistMatch) score++;
            if (albumMatch) score++;
            if (titleMatch) score++;
            if (trackMatch) score++;
            if (yearMatch) score++;
            return score;
        }

        public int getMaxScore() {
            int maxScore = 0;
            if (testCase.getExpectedArtist() != null) maxScore++;
            if (testCase.getExpectedAlbum() != null) maxScore++;
            if (testCase.getExpectedTitle() != null) maxScore++;
            if (testCase.getExpectedTrack() != null) maxScore++;
            if (testCase.getExpectedYear() != null) maxScore++;
            return maxScore;
        }

        public double getAccuracy() {
            int maxScore = getMaxScore();
            if (maxScore == 0) return 100.0;
            return (double) getScore() / maxScore * 100;
        }
    }

    /**
     * 获取测试用例
     */
    private static List<TestCase> getTestCases() {
        List<TestCase> cases = new ArrayList<>();

        // 标准格式：艺术家 - 歌曲
        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\01 - 坚持到底.wav",
            "阿杜",
            "坚持到底",
            "坚持到底",
            "01",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\02 - 惩罚.wav",
            "阿杜",
            "坚持到底",
            "惩罚",
            "02",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\03 - 放手.wav",
            "阿杜",
            "坚持到底",
            "放手",
            "03",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\04 - 天蝎蝴蝶.wav",
            "阿杜",
            "坚持到底",
            "天蝎蝴蝶",
            "04",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\05 - 幻想.wav",
            "阿杜",
            "坚持到底",
            "幻想",
            "05",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\06 - 你就像个小孩子.wav",
            "阿杜",
            "坚持到底",
            "你就像个小孩子",
            "06",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\07 - 下次如果离开你.wav",
            "阿杜",
            "坚持到底",
            "下次如果离开你",
            "07",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\08 - 恩赐.wav",
            "阿杜",
            "坚持到底",
            "恩赐",
            "08",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\09 - 相容.wav",
            "阿杜",
            "坚持到底",
            "相容",
            "09",
            "2002",
            "标准格式：艺术家 - 歌曲"
        ));

        // 复杂目录结构
        cases.add(new TestCase(
            "W:\\C - 陈奕迅\\20151005 02 陈奕迅 最冷一天 2CD 超级震撼立体环绕音效 WAV CD01\\01.wav",
            "陈奕迅",
            "最冷一天",
            null,
            "01",
            null,
            "复杂目录结构：只有编号的文件"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈奕迅\\20151005 02 陈奕迅 最冷一天 2CD 超级震撼立体环绕音效 WAV CD01\\02.wav",
            "陈奕迅",
            "最冷一天",
            null,
            "02",
            null,
            "复杂目录结构：只有编号的文件"
        ));

        // Beyond专辑
        cases.add(new TestCase(
            "W:\\B - Beyond\\1986-再见理想[香港A字首版][WAV]\\01.wav",
            "Beyond",
            "再见理想",
            null,
            "01",
            "1986",
            "Beyond专辑：只有编号的文件"
        ));

        cases.add(new TestCase(
            "W:\\B - Beyond\\1986-再见理想[香港A字首版][WAV]\\02.wav",
            "Beyond",
            "再见理想",
            null,
            "02",
            "1986",
            "Beyond专辑：只有编号的文件"
        ));

        // 陈婧霏专辑（之前测试的文件）
        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\01 - 我的孤独认出你的孤独.wav",
            "陈婧霏",
            "陈婧霏",
            "我的孤独认出你的孤独",
            "01",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\02 - 夏宫.wav",
            "陈婧霏",
            "陈婧霏",
            "夏宫",
            "02",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\03 - 各位请注意请陈婧霏就位.wav",
            "陈婧霏",
            "陈婧霏",
            "各位请注意请陈婧霏就位",
            "03",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\04 - 消亡史.wav",
            "陈婧霏",
            "陈婧霏",
            "消亡史",
            "04",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\05 - 晕船记.wav",
            "陈婧霏",
            "陈婧霏",
            "晕船记",
            "05",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\06 - 今晚.wav",
            "陈婧霏",
            "陈婧霏",
            "今晚",
            "06",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\07 - 跑了三条街才买到.wav",
            "陈婧霏",
            "陈婧霏",
            "跑了三条街才买到",
            "07",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\08 - 深蓝.wav",
            "陈婧霏",
            "陈婧霏",
            "深蓝",
            "08",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\09 - In Bloom.wav",
            "陈婧霏",
            "陈婧霏",
            "In Bloom",
            "09",
            "2020",
            "陈婧霏专辑：英文标题"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\10 - 舞舞舞.wav",
            "陈婧霏",
            "陈婧霏",
            "舞舞舞",
            "10",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\11 - 现在是12月24号早上5点57分.wav",
            "陈婧霏",
            "陈婧霏",
            "现在是12月24号早上5点57分",
            "11",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\12 - 人间指南.wav",
            "陈婧霏",
            "陈婧霏",
            "人间指南",
            "12",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        cases.add(new TestCase(
            "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\13 - 人间指南.wav",
            "陈婧霏",
            "陈婧霏",
            "人间指南",
            "13",
            "2020",
            "陈婧霏专辑：标准格式"
        ));

        return cases;
    }

    /**
     * 测试元数据提取功能
     */
    @Test
    public void testMetadataExtraction() {
        System.out.println("开始测试元数据提取功能");
        System.out.println("=====================================");

        List<TestCase> testCases = getTestCases();
        List<TestResult> results = new ArrayList<>();

        System.out.println("测试用例数: " + testCases.size());
        System.out.println("=====================================");

        // 对每个测试用例进行测试
        for (TestCase testCase : testCases) {
            System.out.println("\n测试用例: " + testCase.getDescription());
            System.out.println("文件路径: " + testCase.getFilePath());

            File file = new File(testCase.getFilePath());
            MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);

            System.out.println("预期结果:");
            System.out.println("  艺术家: " + testCase.getExpectedArtist());
            System.out.println("  专辑: " + testCase.getExpectedAlbum());
            System.out.println("  标题: " + testCase.getExpectedTitle());
            System.out.println("  曲目: " + testCase.getExpectedTrack());
            System.out.println("  年份: " + testCase.getExpectedYear());

            System.out.println("实际结果:");
            System.out.println("  艺术家: " + meta.getArtist());
            System.out.println("  专辑: " + meta.getAlbum());
            System.out.println("  标题: " + meta.getTitle());
            System.out.println("  曲目: " + meta.getTrack());
            System.out.println("  年份: " + meta.getYear());

            TestResult result = new TestResult(testCase, meta);
            results.add(result);

            System.out.println("匹配情况:");
            System.out.println("  艺术家: " + (result.isArtistMatch() ? "✅" : "❌"));
            System.out.println("  专辑: " + (result.isAlbumMatch() ? "✅" : "❌"));
            System.out.println("  标题: " + (result.isTitleMatch() ? "✅" : "❌"));
            System.out.println("  曲目: " + (result.isTrackMatch() ? "✅" : "❌"));
            System.out.println("  年份: " + (result.isYearMatch() ? "✅" : "❌"));
            System.out.println("  准确率: " + String.format("%.1f%%", result.getAccuracy()));
        }

        // 生成测试报告
        generateTestReport(results);

        System.out.println("=====================================");
        System.out.println("元数据提取测试完成");
    }

    /**
     * 生成测试报告
     */
    private static void generateTestReport(List<TestResult> results) {
        System.out.println("\n=====================================");
        System.out.println("测试报告");
        System.out.println("=====================================");

        int totalTests = results.size();
        int artistMatches = 0;
        int albumMatches = 0;
        int titleMatches = 0;
        int trackMatches = 0;
        int yearMatches = 0;
        double totalAccuracy = 0;

        for (TestResult result : results) {
            if (result.isArtistMatch()) artistMatches++;
            if (result.isAlbumMatch()) albumMatches++;
            if (result.isTitleMatch()) titleMatches++;
            if (result.isTrackMatch()) trackMatches++;
            if (result.isYearMatch()) yearMatches++;
            totalAccuracy += result.getAccuracy();
        }

        System.out.println("总测试用例数: " + totalTests);
        System.out.println("艺术家匹配: " + artistMatches + "/" + totalTests + " (" + String.format("%.1f%%", (double) artistMatches / totalTests * 100) + ")");
        System.out.println("专辑匹配: " + albumMatches + "/" + totalTests + " (" + String.format("%.1f%%", (double) albumMatches / totalTests * 100) + ")");
        System.out.println("标题匹配: " + titleMatches + "/" + totalTests + " (" + String.format("%.1f%%", (double) titleMatches / totalTests * 100) + ")");
        System.out.println("曲目匹配: " + trackMatches + "/" + totalTests + " (" + String.format("%.1f%%", (double) trackMatches / totalTests * 100) + ")");
        System.out.println("年份匹配: " + yearMatches + "/" + totalTests + " (" + String.format("%.1f%%", (double) yearMatches / totalTests * 100) + ")");
        System.out.println("平均准确率: " + String.format("%.1f%%", totalAccuracy / totalTests));

        // 找出失败的测试用例
        System.out.println("\n=====================================");
        System.out.println("失败的测试用例");
        System.out.println("=====================================");

        for (TestResult result : results) {
            if (result.getAccuracy() < 100) {
                System.out.println("\n" + result.getTestCase().getDescription());
                System.out.println("文件路径: " + result.getTestCase().getFilePath());
                System.out.println("准确率: " + String.format("%.1f%%", result.getAccuracy()));

                if (!result.isArtistMatch()) {
                    System.out.println("  艺术家不匹配: 预期='" + result.getTestCase().getExpectedArtist() + "', 实际='" + result.getActualMeta().getArtist() + "'");
                }
                if (!result.isAlbumMatch()) {
                    System.out.println("  专辑不匹配: 预期='" + result.getTestCase().getExpectedAlbum() + "', 实际='" + result.getActualMeta().getAlbum() + "'");
                }
                if (!result.isTitleMatch()) {
                    System.out.println("  标题不匹配: 预期='" + result.getTestCase().getExpectedTitle() + "', 实际='" + result.getActualMeta().getTitle() + "'");
                }
                if (!result.isTrackMatch()) {
                    System.out.println("  曲目不匹配: 预期='" + result.getTestCase().getExpectedTrack() + "', 实际='" + result.getActualMeta().getTrack() + "'");
                }
                if (!result.isYearMatch()) {
                    System.out.println("  年份不匹配: 预期='" + result.getTestCase().getExpectedYear() + "', 实际='" + result.getActualMeta().getYear() + "'");
                }
            }
        }
    }

    /**
     * 主方法用于单独运行测试
     */
    public static void main(String[] args) {
        MetadataExtractionTest test = new MetadataExtractionTest();
        test.testMetadataExtraction();
    }
}
