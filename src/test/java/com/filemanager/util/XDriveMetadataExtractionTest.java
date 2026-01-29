package com.filemanager.util;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * X:\ 目录元数据提取测试用例
 * 基于 X:\ 目录下的真实文件测试元数据提取功能
 * 包含多种风格和格式的音乐文件
 */
public class XDriveMetadataExtractionTest {

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
     * 获取 X:\ 目录测试用例
     */
    private static List<TestCase> getXDriveTestCases() {
        List<TestCase> cases = new ArrayList<>();

        // 中文歌手目录 - 金海心
        cases.add(new TestCase(
            "X:\\0 - 中文歌手\\J - 金海心\\听见.wav",
            "金海心",
            "J - 金海心",
            "听见",
            null,
            null,
            "中文歌手：金海心 - 听见"
        ));

        cases.add(new TestCase(
            "X:\\0 - 中文歌手\\J - 金海心\\天天.wav",
            "金海心",
            "J - 金海心",
            "天天",
            null,
            null,
            "中文歌手：金海心 - 天天"
        ));

        // 中文歌手目录 - 侃侃
        cases.add(new TestCase(
            "X:\\0 - 中文歌手\\侃侃\\侃侃 - 冬语.wav",
            "侃侃",
            "侃侃",
            "冬语",
            null,
            null,
            "中文歌手：侃侃 - 冬语"
        ));

        cases.add(new TestCase(
            "X:\\0 - 中文歌手\\侃侃\\侃侃 - 味道.wav",
            "侃侃",
            "侃侃",
            "味道",
            null,
            null,
            "中文歌手：侃侃 - 味道"
        ));

        cases.add(new TestCase(
            "X:\\0 - 中文歌手\\侃侃\\侃侃 - 嘀嗒.wav",
            "侃侃",
            "侃侃",
            "嘀嗒",
            null,
            null,
            "中文歌手：侃侃 - 嘀嗒"
        ));

        // 中文歌手目录 - 窦唯
        cases.add(new TestCase(
            "X:\\0 - 中文歌手\\窦唯\\窦唯 - 雨吁.wav",
            "窦唯",
            "窦唯",
            "雨吁",
            null,
            null,
            "中文歌手：窦唯 - 雨吁"
        ));

        // 中文歌手目录 - 裘德
        cases.add(new TestCase(
            "X:\\0 - 中文歌手\\裘德\\裘德 - 冰.flac",
            "裘德",
            "裘德",
            "冰",
            null,
            null,
            "中文歌手：裘德 - 冰"
        ));

        // 我的喜欢目录 - 多种格式和风格
        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\7opy - 晚风.flac",
            "7opy",
            "我的喜欢",
            "晚风",
            null,
            null,
            "我的喜欢：7opy - 晚风"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\α·Pav - η.m4a",
            "α·Pav",
            "我的喜欢",
            "η",
            null,
            null,
            "我的喜欢：α·Pav - η"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\一支榴莲 - 海底.lrc",
            "一支榴莲",
            "我的喜欢",
            "海底",
            null,
            null,
            "我的喜欢：一支榴莲 - 海底"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\五月天 - 天使.flac",
            "五月天",
            "我的喜欢",
            "天使",
            null,
            null,
            "我的喜欢：五月天 - 天使"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\五月天 - 温柔.flac",
            "五月天",
            "我的喜欢",
            "温柔",
            null,
            null,
            "我的喜欢：五月天 - 温柔"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\五月天 - 知足.flac",
            "五月天",
            "我的喜欢",
            "知足",
            null,
            null,
            "我的喜欢：五月天 - 知足"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\井胧 - 丢了你.flac",
            "井胧",
            "我的喜欢",
            "丢了你",
            null,
            null,
            "我的喜欢：井胧 - 丢了你"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\井胧 - 骁.flac",
            "井胧",
            "我的喜欢",
            "骁",
            null,
            null,
            "我的喜欢：井胧 - 骁"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\亚东 - 卓玛.mp3",
            "亚东",
            "我的喜欢",
            "卓玛",
            null,
            null,
            "我的喜欢：亚东 - 卓玛"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\任素汐 - 胡广生.lrc",
            "任素汐",
            "我的喜欢",
            "胡广生",
            null,
            null,
            "我的喜欢：任素汐 - 胡广生"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\何柏诚 - 彩虹糖.flac",
            "何柏诚",
            "我的喜欢",
            "彩虹糖",
            null,
            null,
            "我的喜欢：何柏诚 - 彩虹糖"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\凹与山 - 理查.flac",
            "凹与山",
            "我的喜欢",
            "理查",
            null,
            null,
            "我的喜欢：凹与山 - 理查"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\刘森 - 深海.flac",
            "刘森",
            "我的喜欢",
            "深海",
            null,
            null,
            "我的喜欢：刘森 - 深海"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\刘珂矣 - 半壶纱.flac",
            "刘珂矣",
            "我的喜欢",
            "半壶纱",
            null,
            null,
            "我的喜欢：刘珂矣 - 半壶纱"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\刘若英 - 为爱痴狂.lrc",
            "刘若英",
            "我的喜欢",
            "为爱痴狂",
            null,
            null,
            "我的喜欢：刘若英 - 为爱痴狂"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\别野加奈 - キネマ.lrc",
            "别野加奈",
            "我的喜欢",
            "キネマ",
            null,
            null,
            "我的喜欢：别野加奈 - キネマ"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\华晨宇 - 肆无惧燥.lrc",
            "华晨宇",
            "我的喜欢",
            "肆无惧燥",
            null,
            null,
            "我的喜欢：华晨宇 - 肆无惧燥"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\卢冠廷 - 一生所爱.lrc",
            "卢冠廷",
            "我的喜欢",
            "一生所爱",
            null,
            null,
            "我的喜欢：卢冠廷 - 一生所爱"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\厨子和戏子 - 春河.lrc",
            "厨子和戏子",
            "我的喜欢",
            "春河",
            null,
            null,
            "我的喜欢：厨子和戏子 - 春河"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\反光镜 - 还我蔚蓝.lrc",
            "反光镜",
            "我的喜欢",
            "还我蔚蓝",
            null,
            null,
            "我的喜欢：反光镜 - 还我蔚蓝"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\吴青峰 - 起风了.lrc",
            "吴青峰",
            "我的喜欢",
            "起风了",
            null,
            null,
            "我的喜欢：吴青峰 - 起风了"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\告五人 - WEWE.lrc",
            "告五人",
            "我的喜欢",
            "WEWE",
            null,
            null,
            "我的喜欢：告五人 - WEWE"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\告五人 - 夜里无星.lrc",
            "告五人",
            "我的喜欢",
            "夜里无星",
            null,
            null,
            "我的喜欢：告五人 - 夜里无星"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\告五人 - 新世界.flac",
            "告五人",
            "我的喜欢",
            "新世界",
            null,
            null,
            "我的喜欢：告五人 - 新世界"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\告五人 - 红.flac",
            "告五人",
            "我的喜欢",
            "红",
            null,
            null,
            "我的喜欢：告五人 - 红"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\周柏豪 - 小白.flac",
            "周柏豪",
            "我的喜欢",
            "小白",
            null,
            null,
            "我的喜欢：周柏豪 - 小白"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\周深 - 光亮.flac",
            "周深",
            "我的喜欢",
            "光亮",
            null,
            null,
            "我的喜欢：周深 - 光亮"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\周迅 - 外面.flac",
            "周迅",
            "我的喜欢",
            "外面",
            null,
            null,
            "我的喜欢：周迅 - 外面"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\哪吒乐队 - 闹海.mp3",
            "哪吒乐队",
            "我的喜欢",
            "闹海",
            null,
            null,
            "我的喜欢：哪吒乐队 - 闹海"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\唐笑 - 淡水海边.lrc",
            "唐笑",
            "我的喜欢",
            "淡水海边",
            null,
            null,
            "我的喜欢：唐笑 - 淡水海边"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\嘿！！！ - 房子.mp3",
            "嘿！！！",
            "我的喜欢",
            "房子",
            null,
            null,
            "我的喜欢：嘿！！！ - 房子"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\回春丹 - 初恋.flac",
            "回春丹",
            "我的喜欢",
            "初恋",
            null,
            null,
            "我的喜欢：回春丹 - 初恋"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\回春丹 - 艾蜜莉.flac",
            "回春丹",
            "我的喜欢",
            "艾蜜莉",
            null,
            null,
            "我的喜欢：回春丹 - 艾蜜莉"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\因果 - 花火大会.lrc",
            "因果",
            "我的喜欢",
            "花火大会",
            null,
            null,
            "我的喜欢：因果 - 花火大会"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\声无哀乐 - 飞升.lrc",
            "声无哀乐",
            "我的喜欢",
            "飞升",
            null,
            null,
            "我的喜欢：声无哀乐 - 飞升"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\大籽 - 放空.flac",
            "大籽",
            "我的喜欢",
            "放空",
            null,
            null,
            "我的喜欢：大籽 - 放空"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\天门 - 桜花抄.flac",
            "天门",
            "我的喜欢",
            "桜花抄",
            null,
            null,
            "我的喜欢：天门 - 桜花抄"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\天门 - 雪の駅.flac",
            "天门",
            "我的喜欢",
            "雪の駅",
            null,
            null,
            "我的喜欢：天门 - 雪の駅"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\姜云升 - 初.flac",
            "姜云升",
            "我的喜欢",
            "初",
            null,
            null,
            "我的喜欢：姜云升 - 初"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\姜云升 - 淹没.flac",
            "姜云升",
            "我的喜欢",
            "淹没",
            null,
            null,
            "我的喜欢：姜云升 - 淹没"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\姫神 - 千年の祈り.m4a",
            "姫神",
            "我的喜欢",
            "千年の祈り",
            null,
            null,
            "我的喜欢：姫神 - 千年の祈り"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\孙燕姿 - 遇见.flac",
            "孙燕姿",
            "我的喜欢",
            "遇见",
            null,
            null,
            "我的喜欢：孙燕姿 - 遇见"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\安雯 - 月满西楼.flac",
            "安雯",
            "我的喜欢",
            "月满西楼",
            null,
            null,
            "我的喜欢：安雯 - 月满西楼"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\宋冬野 - 安和桥.lrc",
            "宋冬野",
            "我的喜欢",
            "安和桥",
            null,
            null,
            "我的喜欢：宋冬野 - 安和桥"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\小霞 - 向云端.flac",
            "小霞",
            "我的喜欢",
            "向云端",
            null,
            null,
            "我的喜欢：小霞 - 向云端"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\岛屿心情 - 声音.mp3",
            "岛屿心情",
            "我的喜欢",
            "声音",
            null,
            null,
            "我的喜欢：岛屿心情 - 声音"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\常安 - 梅花三弄.flac",
            "常安",
            "我的喜欢",
            "梅花三弄",
            null,
            null,
            "我的喜欢：常安 - 梅花三弄"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\常石磊 - 遇见你.flac",
            "常石磊",
            "我的喜欢",
            "遇见你",
            null,
            null,
            "我的喜欢：常石磊 - 遇见你"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\廖佳琳 - 降临.flac",
            "廖佳琳",
            "我的喜欢",
            "降临",
            null,
            null,
            "我的喜欢：廖佳琳 - 降临"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\张军 - 霓裳羽衣.mp3",
            "张军",
            "我的喜欢",
            "霓裳羽衣",
            null,
            null,
            "我的喜欢：张军 - 霓裳羽衣"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\张曦匀 - 伯虎说.lrc",
            "张曦匀",
            "我的喜欢",
            "伯虎说",
            null,
            null,
            "我的喜欢：张曦匀 - 伯虎说"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\张杰 - 给力青春.mp3",
            "张杰",
            "我的喜欢",
            "给力青春",
            null,
            null,
            "我的喜欢：张杰 - 给力青春"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\张渠 - 柘枝舞.flac",
            "张渠",
            "我的喜欢",
            "柘枝舞",
            null,
            null,
            "我的喜欢：张渠 - 柘枝舞"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\张良成 - 风浪里.lrc",
            "张良成",
            "我的喜欢",
            "风浪里",
            null,
            null,
            "我的喜欢：张良成 - 风浪里"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\很美味 - 假面舞会.lrc",
            "很美味",
            "我的喜欢",
            "假面舞会",
            null,
            null,
            "我的喜欢：很美味 - 假面舞会"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\徐小凤 - 心恋.flac",
            "徐小凤",
            "我的喜欢",
            "心恋",
            null,
            null,
            "我的喜欢：徐小凤 - 心恋"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\徐秉龙 - 孤身.flac",
            "徐秉龙",
            "我的喜欢",
            "孤身",
            null,
            null,
            "我的喜欢：徐秉龙 - 孤身"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\徐秉龙 - 白羊.flac",
            "徐秉龙",
            "我的喜欢",
            "白羊",
            null,
            null,
            "我的喜欢：徐秉龙 - 白羊"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\戴佩妮 - 怎样.flac",
            "戴佩妮",
            "我的喜欢",
            "怎样",
            null,
            null,
            "我的喜欢：戴佩妮 - 怎样"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\旺福 - 姊妹仔.flac",
            "旺福",
            "我的喜欢",
            "姊妹仔",
            null,
            null,
            "我的喜欢：旺福 - 姊妹仔"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\昙轩 - 海の形.flac",
            "昙轩",
            "我的喜欢",
            "海の形",
            null,
            null,
            "我的喜欢：昙轩 - 海の形"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\曾轶可 - 胆小鬼.flac",
            "曾轶可",
            "我的喜欢",
            "胆小鬼",
            null,
            null,
            "我的喜欢：曾轶可 - 胆小鬼"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\朱彦安 - 20.flac",
            "朱彦安",
            "我的喜欢",
            "20",
            null,
            null,
            "我的喜欢：朱彦安 - 20"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\朴树 - 清白之年.lrc",
            "朴树",
            "我的喜欢",
            "清白之年",
            null,
            null,
            "我的喜欢：朴树 - 清白之年"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\朴树 - 生如夏花.lrc",
            "朴树",
            "我的喜欢",
            "生如夏花",
            null,
            null,
            "我的喜欢：朴树 - 生如夏花"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\杉和 - 子莫格尼.lrc",
            "杉和",
            "我的喜欢",
            "子莫格尼",
            null,
            null,
            "我的喜欢：杉和 - 子莫格尼"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\李云霄 - 月中仙.flac",
            "李云霄",
            "我的喜欢",
            "月中仙",
            null,
            null,
            "我的喜欢：李云霄 - 月中仙"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\李健 - 心升明月.flac",
            "李健",
            "我的喜欢",
            "心升明月",
            null,
            null,
            "我的喜欢：李健 - 心升明月"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\李健 - 陀螺.flac",
            "李健",
            "我的喜欢",
            "陀螺",
            null,
            null,
            "我的喜欢：李健 - 陀螺"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\李宗盛 - 山丘.flac",
            "李宗盛",
            "我的喜欢",
            "山丘",
            null,
            null,
            "我的喜欢：李宗盛 - 山丘"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\李杰 - 家园.flac",
            "李杰",
            "我的喜欢",
            "家园",
            null,
            null,
            "我的喜欢：李杰 - 家园"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\李荣浩 - 乌梅子酱.lrc",
            "李荣浩",
            "我的喜欢",
            "乌梅子酱",
            null,
            null,
            "我的喜欢：李荣浩 - 乌梅子酱"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\杨千嬅 - 野孩子.lrc",
            "杨千嬅",
            "我的喜欢",
            "野孩子",
            null,
            null,
            "我的喜欢：杨千嬅 - 野孩子"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\杨宗纬 - 最爱.mp3",
            "杨宗纬",
            "我的喜欢",
            "最爱",
            null,
            null,
            "我的喜欢：杨宗纬 - 最爱"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\杭天琪 - 唱脸谱.flac",
            "杭天琪",
            "我的喜欢",
            "唱脸谱",
            null,
            null,
            "我的喜欢：杭天琪 - 唱脸谱"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\林俊杰 - 小酒窝.flac",
            "林俊杰",
            "我的喜欢",
            "小酒窝",
            null,
            null,
            "我的喜欢：林俊杰 - 小酒窝"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\林俊杰 - 当你.flac",
            "林俊杰",
            "我的喜欢",
            "当你",
            null,
            null,
            "我的喜欢：林俊杰 - 当你"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\林忆莲 - 远走高飞.lrc",
            "林忆莲",
            "我的喜欢",
            "远走高飞",
            null,
            null,
            "我的喜欢：林忆莲 - 远走高飞"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\柳爽 - 我的解放西.lrc",
            "柳爽",
            "我的喜欢",
            "我的解放西",
            null,
            null,
            "我的喜欢：柳爽 - 我的解放西"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\柳爽 - 漠河舞厅.flac",
            "柳爽",
            "我的喜欢",
            "漠河舞厅",
            null,
            null,
            "我的喜欢：柳爽 - 漠河舞厅"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\柳爽 - 莫妮卡.flac",
            "柳爽",
            "我的喜欢",
            "莫妮卡",
            null,
            null,
            "我的喜欢：柳爽 - 莫妮卡"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\格格 - 火苗.flac",
            "格格",
            "我的喜欢",
            "火苗",
            null,
            null,
            "我的喜欢：格格 - 火苗"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\梁兆基 - 求神.mp3",
            "梁兆基",
            "我的喜欢",
            "求神",
            null,
            null,
            "我的喜欢：梁兆基 - 求神"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\梁博 - 日落大道.lrc",
            "梁博",
            "我的喜欢",
            "日落大道",
            null,
            null,
            "我的喜欢：梁博 - 日落大道"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\梁翘柏 - 南国之舞.lrc",
            "梁翘柏",
            "我的喜欢",
            "南国之舞",
            null,
            null,
            "我的喜欢：梁翘柏 - 南国之舞"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\梦然 - 是你.flac",
            "梦然",
            "我的喜欢",
            "是你",
            null,
            null,
            "我的喜欢：梦然 - 是你"
        ));

        // 日韩歌手目录
        cases.add(new TestCase(
            "X:\\0 - 日韩歌手\\姫神\\姫神 - 杜.flac",
            "姫神",
            "日韩歌手",
            "杜",
            null,
            null,
            "日韩歌手：姫神 - 杜"
        ));

        cases.add(new TestCase(
            "X:\\0 - 日韩歌手\\姫神\\姫神 - 虹桥.mp3",
            "姫神",
            "日韩歌手",
            "虹桥",
            null,
            null,
            "日韩歌手：姫神 - 虹桥"
        ));

        // 国歌、战友情目录
        cases.add(new TestCase(
            "X:\\1 - 国歌、战友情\\怀念战友\\雁南飞.wav",
            "未知",
            "怀念战友",
            "雁南飞",
            null,
            null,
            "战友情：雁南飞"
        ));

        cases.add(new TestCase(
            "X:\\1 - 国歌、战友情\\怀念战友\\驼铃.wav",
            "未知",
            "怀念战友",
            "驼铃",
            null,
            null,
            "战友情：驼铃"
        ));

        return cases;
    }

    /**
     * 测试元数据提取功能
     */
    @Test
    public void testXDriveMetadataExtraction() {
        List<TestCase> testCases = getXDriveTestCases();
        List<TestResult> results = new ArrayList<>();

        int totalArtistMatches = 0;
        int totalAlbumMatches = 0;
        int totalTitleMatches = 0;
        int totalTrackMatches = 0;
        int totalYearMatches = 0;
        int totalExpectedFields = 0;

        for (TestCase testCase : testCases) {
            File file = new File(testCase.getFilePath());
            MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);

            TestResult result = new TestResult(testCase, meta);
            results.add(result);

            if (result.isArtistMatch()) totalArtistMatches++;
            if (result.isAlbumMatch()) totalAlbumMatches++;
            if (result.isTitleMatch()) totalTitleMatches++;
            if (result.isTrackMatch()) totalTrackMatches++;
            if (result.isYearMatch()) totalYearMatches++;

            totalExpectedFields += result.getMaxScore();

            if (result.getAccuracy() < 100) {
                System.out.println("测试用例: " + testCase.getDescription());
                System.out.println("文件路径: " + testCase.getFilePath());
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
                System.out.println("匹配情况:");
                System.out.println("  艺术家: " + (result.isArtistMatch() ? "✓" : "✗"));
                System.out.println("  专辑: " + (result.isAlbumMatch() ? "✓" : "✗"));
                System.out.println("  标题: " + (result.isTitleMatch() ? "✓" : "✗"));
                System.out.println("  曲目: " + (result.isTrackMatch() ? "✓" : "✗"));
                System.out.println("  年份: " + (result.isYearMatch() ? "✓" : "✗"));
                System.out.println("准确率: " + String.format("%.1f%%", result.getAccuracy()));
                System.out.println();
            }
        }

        System.out.println("=====================================");
        System.out.println("X:\\ 目录测试报告");
        System.out.println("=====================================");
        System.out.println("总测试用例数: " + testCases.size());
        System.out.println("艺术家匹配: " + totalArtistMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalArtistMatches / testCases.size() * 100) + ")");
        System.out.println("专辑匹配: " + totalAlbumMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalAlbumMatches / testCases.size() * 100) + ")");
        System.out.println("标题匹配: " + totalTitleMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalTitleMatches / testCases.size() * 100) + ")");
        System.out.println("曲目匹配: " + totalTrackMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalTrackMatches / testCases.size() * 100) + ")");
        System.out.println("年份匹配: " + totalYearMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalYearMatches / testCases.size() * 100) + ")");

        int totalScore = totalArtistMatches + totalAlbumMatches + totalTitleMatches + totalTrackMatches + totalYearMatches;
        double averageAccuracy = (double) totalScore / totalExpectedFields * 100;
        System.out.println("平均准确率: " + String.format("%.1f%%", averageAccuracy));

        List<TestResult> failedResults = new ArrayList<>();
        for (TestResult result : results) {
            if (result.getAccuracy() < 100) {
                failedResults.add(result);
            }
        }

        if (!failedResults.isEmpty()) {
            System.out.println();
            System.out.println("=====================================");
            System.out.println("失败的测试用例");
            System.out.println("=====================================");
            for (TestResult result : failedResults) {
                System.out.println(result.getTestCase().getDescription());
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
                System.out.println();
            }
        }

        System.out.println("=====================================");
        System.out.println("X:\\ 目录元数据提取测试完成");
        System.out.println("=====================================");
    }
}
